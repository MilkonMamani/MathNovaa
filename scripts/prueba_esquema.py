import sqlite3, json
db = sqlite3.connect(':memory:'); db.execute('PRAGMA foreign_keys = ON')
db.executescript(open('V1__esquema_inicial.sql').read()); db.executescript(open('V2__datos_iniciales.sql').read())
q = lambda s, *a: db.execute(s, a).fetchall()
print('tablas', [r[0] for r in q("select name from sqlite_master where type='table' order by name")])
print('tecnicas', q('select count(*) from tecnica'), 'operaciones', q('select count(*) from operacion'), 'temas', q('select count(*) from tema'))
T = '2026-11-12T19:00:00Z'
db.execute("insert into ejercicio(expresion_entrada,expresion_normalizada,expresion_latex,variables,tipo,creado_en) values(?,?,?,?,?,?)",
           ('x^3-2x^2-x+2','x^3 - 2x^2 - x + 2','x^{3}-2x^{2}-x+2','x','POLINOMIO',T))
eid = q('select id from ejercicio')[0][0]
db.execute('insert into ejercicio_tema values(?,1)', (eid,))
def guardar(op, clave, campo, res, t, params={}):
    db.execute("""insert into resolucion(ejercicio_id,operacion_codigo,campo,clave_parametros,resultado_texto,resultado_latex,version_motor,creado_en,actualizado_en)
                  values(?,?,?,?,?,?,?,?,?)
                  on conflict(ejercicio_id,operacion_codigo,clave_parametros) do update set actualizado_en=excluded.actualizado_en,
                  resultado_texto=excluded.resultado_texto, resultado_latex=excluded.resultado_latex, version_motor=excluded.version_motor""",
               (eid, op, campo, clave, res, res, '1.0.0', t, t))
    rid = q('select id from resolucion where ejercicio_id=? and operacion_codigo=? and clave_parametros=?', eid, op, clave)[0][0]
    for k, v in params.items():
        db.execute('insert or replace into parametro_resolucion values(?,?,?)', (rid, k, v))
    return rid
r1 = guardar('FACTORIZAR', 'campo=Q', 'Q', '(x - 2)(x - 1)(x + 1)', T)
db.execute("insert into paso(resolucion_id,orden,tecnica_codigo,justificacion,antes_texto,antes_latex,es_descartado,motivo_descarte) values(?,1,'F01','Los coeficientes no tienen factor común','x^3 - 2x^2 - x + 2','..',1,'MCD(1,2,1,2) = 1')", (r1,))
db.execute("insert into paso(resolucion_id,orden,tecnica_codigo,justificacion,antes_texto,antes_latex,despues_texto,despues_latex,trabajo_tipo,trabajo_json) values(?,2,'F04','Grupos x^3 - 2x^2 y -x + 2 comparten (x - 2)','x^3 - 2x^2 - x + 2','..','(x - 2)(x^2 - 1)','..','SUSTITUCION',?)", (r1, json.dumps({'grupos': ['x^2(x - 2)', '-(x - 2)']})))
p2 = q('select id from paso where orden=2')[0][0]
db.execute("insert into paso(resolucion_id,paso_padre_id,orden,tecnica_codigo,justificacion,antes_texto,antes_latex,despues_texto,despues_latex) values(?,?,3,'F05','x^2 = (x)^2 y 1 = 1^2','x^2 - 1','..','(x - 1)(x + 1)','..')", (r1, p2))
db.execute("insert into sugerencia(resolucion_id,operacion_codigo,regla_codigo,justificacion,orden) values(?,'DERIVAR','RN18','Es un polinomio: puedes hallar puntos críticos',1)", (r1,))
r2 = guardar('LIMITE', 'punto=2', None, '3', T, {'variable': 'x', 'punto': '2'})
db.execute("insert into restriccion_dominio(resolucion_id,texto,latex,origen) values(?,'x ≠ 2','x \\neq 2','DENOMINADOR')", (r2,))
# RN25: repetir factorización
T2 = '2026-11-13T10:00:00Z'
r1b = guardar('FACTORIZAR', 'campo=Q', 'Q', '(x - 2)(x - 1)(x + 1)', T2)
print('RN25 misma fila:', r1 == r1b, q('select count(*), max(actualizado_en) from resolucion where operacion_codigo="FACTORIZAR"'))
print('campo R es otra resolución:', guardar('FACTORIZAR', 'campo=R', 'R', '(x - 2)(x - 1)(x + 1)', T2) != r1)
# búsqueda RF35
print('buscar:', q("""select e.expresion_entrada, r.operacion_codigo, r.actualizado_en from resolucion r join ejercicio e on e.id=r.ejercicio_id
   left join ejercicio_tema et on et.ejercicio_id=e.id left join tema t on t.id=et.tema_id
   where (e.expresion_entrada like ? or e.expresion_normalizada like ?) and r.operacion_codigo=? and t.nombre=? and r.actualizado_en between ? and ?
   order by r.actualizado_en desc""", '%x^3%', '%x^3%', 'FACTORIZAR', 'Factorización en Q', '2026-11-01', '2026-12-01'))
# pasos en orden con anidación
print('pasos:', q('select orden, tecnica_codigo, paso_padre_id is not null, es_descartado from paso where resolucion_id=? order by orden', r1))
# práctica
db.execute("insert into sesion_practica(resolucion_id,iniciada_en,total_pasos,aciertos) values(?,?,2,1)", (r1, T))
sid = q('select id from sesion_practica')[0][0]
db.execute("insert into intento_paso(sesion_id,orden_paso,tecnica_propuesta,expresion_propuesta,es_correcto,retroalimentacion,intentado_en) values(?,2,'F16','(x - 1)(x^2 - x - 2)',1,'Correcto por otro camino',?)", (sid, T))
# restricciones
for bad, sql, args in [
  ('descartado sin motivo', "insert into paso(resolucion_id,orden,tecnica_codigo,justificacion,antes_texto,antes_latex,es_descartado) values(?,9,'F05','x','x','x',1)", (r1,)),
  ('json inválido', "insert into paso(resolucion_id,orden,tecnica_codigo,justificacion,antes_texto,antes_latex,despues_texto,despues_latex,trabajo_tipo,trabajo_json) values(?,9,'F16','x','x','x','y','y','TABLA_RUFFINI','{mal')", (r1,)),
  ('campo X', "update resolucion set campo='X' where id=?", (r1,)),
  ('técnica inexistente', "insert into paso(resolucion_id,orden,tecnica_codigo,justificacion,antes_texto,antes_latex,despues_texto,despues_latex) values(?,9,'F99','x','x','x','y','y')", (r1,)),
  ('variable w', "insert into ejercicio(expresion_entrada,expresion_normalizada,expresion_latex,variables,tipo,creado_en) values('w','w','w','w','POLINOMIO',?)", (T,)),
  ('aciertos > total', "update sesion_practica set aciertos=5", ()),
  ('ejercicio repetido', "insert into ejercicio(expresion_entrada,expresion_normalizada,expresion_latex,variables,tipo,creado_en) values('a','x^3 - 2x^2 - x + 2','l','x','POLINOMIO',?)", (T,)),
]:
    try:
        db.execute(sql, args); print('NO RECHAZÓ:', bad)
    except sqlite3.IntegrityError as ex:
        print('rechaza', bad, '->', ex)
# borrar en cascada RF36
db.execute('delete from ejercicio where id=?', (eid,))
print('tras borrar:', {t: q(f'select count(*) from {t}')[0][0] for t in ['ejercicio','ejercicio_tema','resolucion','parametro_resolucion','paso','sugerencia','restriccion_dominio','sesion_practica','intento_paso']})
print('catálogos intactos:', q('select count(*) from tecnica')[0][0], q('select count(*) from tema')[0][0])
print('fk check:', q('pragma foreign_key_check'), 'integrity:', q('pragma integrity_check'))
