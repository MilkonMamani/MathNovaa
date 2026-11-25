"""Escribe manual/capturas.json (lista de capturas del manual). Ejecutar: python manual/arnes/definir_capturas.py"""
import json
from pathlib import Path

P = "x^3-2x^2-x+2"
Q4 = "x^4-4"


def ir(s): return {"accion": "ir", "valor": s}
def esc(l): return {"accion": "escribir", "valor": l}
def clic(c): return {"accion": "clic", "control": c}
def pausa(ms): return {"accion": "pausa", "valor": str(ms)}
def esp(c, s=None):
    d = {"accion": "esperar", "control": c}
    if s:
        d["valor"] = str(s)
    return d


RESOLVER = [clic("btn_resolver"), {"accion": "esperar_resultado"}]
OTRA_VEZ = [pausa(3000), {"accion": "esperar_resultado"}]


def resolver(l): return [ir("Resolver"), esc(l)] + RESOLVER


def ver(c): return {"accion": "ver", "control": c}


def a(c, t, lado="derecha", **k):
    d = {"tipo": "accion", "texto": t, "lado": lado}
    d.update({"controles": c} if isinstance(c, list) else {"control": c})
    d.update(k)
    return d


def ok(c, t, lado="derecha", **k):
    d = a(c, t, lado, **k)
    d["tipo"] = "ok"
    return d


def cap(i, tarea, prep, marcas, **k):
    d = {"id": i, "tarea": tarea, "escala": 2, "preparar": prep, "marcas": marcas}
    d.update(k)
    return d


C = []
# ---------- Grupo 1: Resolver (app recién abierta)
C.append(cap("2-2_abrir", "2.2", [ir("Resolver")],
             [ok("titulo_resolver", "MathNova abre en Resolver"), ok("barra_ejercicio", "Lista para escribir", "abajo")],
             reiniciar=True))
C.append(cap("5-1_resolver_01_pasos", "5.1", [ir("Resolver"), clic("pestana_teclado"), esc(P)],
             [a("entrada_ejercicio", "Escribe el ejercicio", "abajo"), a("teclado", "O usa estas teclas"),
              a("btn_resolver", "Haz clic en Resolver", "abajo")]))
C.append(cap("3-1_menu", "3.1", [ir("Resolver"), esc(""), clic("btn_menu")],
             [a("cabecera_menu", "Haz clic en el menú"), a("menu_historial", "Elige una sección")]))
C.append(cap("4_teclado", "4", [ir("Resolver"), clic("btn_menu"), clic("pestana_teclado"), esc("")],
             [ok(["pildora_123", "pildora_fx", "pildora_abc", "pildora_sim"], "Grupos de teclas"),
              ok("tecla_borrar", "Borra"), ok(["tecla_izq", "tecla_der"], "Mueven el cursor", "abajo"),
              ok("tecla_enter", "Resuelve")]))
C.append(cap("4-1_teclado_01", "4.1", [ir("Resolver"), clic("pestana_teclado"), clic("pildora_fx"), esc("")],
             [a("pestana_teclado", "Abre Teclado", "arriba"), a("pildora_fx", "Elige un grupo"),
              a("teclado", "Haz clic en las teclas")]))
C.append(cap("4-2_corregir_01", "4.2",
             [ir("Resolver"), clic("pestana_teclado"), clic("pildora_123"), esc("x^3-2x^2-x+"), esp("aviso_entrada"), pausa(400)],
             [ok("aviso_entrada", "Te dice qué falta"), a("entrada_ejercicio", "Haz clic donde corriges", "abajo"),
              a("tecla_borrar", "Borra con esta tecla")]))
C.append(cap("4-5_plantilla_01", "4.5",
             [ir("Resolver"), clic("pestana_teclado"), clic("pildora_fx"), esc(r"\lim_{x\to2}\frac{x^2-4}{x-2}"), pausa(500)],
             [a("pildora_fx", "Abre f(x)"), a("tecla_lim", "Haz clic en lím"),
              a("entrada_ejercicio", "Llena cada casilla", "abajo"), a("tecla_cursor_der", "Pasa a la siguiente casilla", "abajo")]))
C.append(cap("5-1_resolver_02_solucion", "5.1", resolver(P),
             [ok("pizarra_rotulo", "MathNova reconoce qué hacer"), ok("pizarra_pasos", "Cada paso de la solución", "izquierda"),
              ok("caja_resultado", "Este es el resultado", "izquierda")]))
C.append(cap("3_zonas", "3", resolver(P),
             [{"control": "menu_lateral", "tipo": "zona", "pos": [0.5, 0.6]},
              {"control": "barra_ejercicio", "tipo": "zona", "pos": [0.6, 0.5]},
              {"control": "columna_izquierda", "tipo": "zona", "pos": [0.5, 0.35], "ajuste": [0, 0, -14, 0]},
              {"control": "pizarra", "tipo": "zona", "pos": [0.85, 0.45]}], sin_oscurecer=True))
C.append(cap("5-2_pasos_01", "5.2", resolver(P), [a("btn_modo_paso", "Haz clic en Paso a paso", "arriba")]))
C.append(cap("5-2_pasos_02", "5.2", resolver(P) + [clic("btn_modo_paso"), clic("flecha_siguiente"), pausa(800)],
             [a(["flecha_anterior", "flecha_siguiente"], "Avanza o retrocede", "arriba"), ok("etiqueta_pasos", "Paso en el que estás", "abajo"),
              a("btn_ver_todo", "Vuelve a ver todo", "abajo", dist=130)]))
C.append(cap("5-3_explicacion_01", "5.3", resolver(P), [a("btn_nota_1", "Haz clic en ?", "derecha")]))
C.append(cap("5-3_explicacion_02", "5.3", [clic("btn_nota_1"), pausa(800)],
             [ok("nota_paso", "Por qué se hizo", "abajo"),
              a("interruptor_explicaciones", "O muéstralas todas", "abajo")]))
C.append(cap("5-4_comprobacion_01", "5.4", resolver(P) + [ver("ver_comprobacion")],
             [a("ver_comprobacion", "Haz clic en Ver comprobación", "izquierda")]))
C.append(cap("5-4_comprobacion_02", "5.4", [clic("ver_comprobacion"), pausa(800), ver("comprobacion")],
             [ok("comprobacion", "Coincide con el ejercicio", "izquierda")]))
C.append(cap("5-5_exportar_01", "5.5", resolver(P) + [clic("btn_exportar"), pausa(2500)],
             [a("btn_exportar", "Haz clic en Exportar PDF", "izquierda"), a("dlg_incluir_grafica", "Elige si va la gráfica"),
              a("dlg_elegir_donde", "Haz clic aquí", "abajo")]))
C.append(cap("6-1_grafica_01", "6.1", resolver(P) + [clic("pestana_grafica"), pausa(800)],
             [a("pestana_grafica", "Abre Gráfica"), ok("zona_grafica", "Corta el eje x en las raíces", "abajo"),
              a(["graf_mas", "graf_menos", "graf_restablecer"], "Acerca o aleja", "arriba")]))
C.append(cap("6-2_analisis_01", "6.2", resolver(P) + [clic("pestana_analisis"), pausa(800)],
             [a("pestana_analisis", "Abre Análisis", "arriba"), ok("analisis", "Datos de la función", "abajo")]))
C.append(cap("6-3_probar_01", "6.3", resolver(P) + [clic("pestana_exploracion"), pausa(1500)],
             [a("pestana_exploracion", "Abre Exploración"), ok("metodos", "Métodos usados y posibles", "abajo"),
              a("btn_probar", "Haz clic en Probar", "derecha")]))
C.append(cap("6-3_probar_02", "6.3", resolver(P) + [clic("pestana_exploracion"), pausa(1500), clic("btn_probar")] + OTRA_VEZ + [ver("caja_resultado")],
             [ok("pizarra_pasos", "Resuelto con ese método", "izquierda")]))
C.append(cap("5-6_qrc_01", "5.6", resolver(Q4) + [clic("btn_q")] + OTRA_VEZ,
             [ok("caja_resultado", "Resultado en Q", "izquierda"), a("btn_r", "Haz clic en R", "abajo")]))
C.append(cap("5-6_qrc_02", "5.6", resolver(Q4) + [clic("btn_r")] + OTRA_VEZ,
             [ok("btn_r", "R elegido", "abajo"), ok("caja_resultado", "Ahora con raíces", "izquierda")]))
C.append(cap("6-4_comparar_01", "6.4", resolver(Q4) + [clic("btn_q")] + OTRA_VEZ + [clic("pestana_exploracion"), pausa(3000)],
             [a("pestana_exploracion", "Abre Exploración"), ok("comparacion", "Resultado en Q, R y C")]))

# ---------- Grupo 2: escritura a mano (muestra simulada que trae MathNova)
C.append(cap("4-3_mano_01", "4.3",
             [ir("Resolver"), esc(""), clic("pestana_escribir"), pausa(1000),
              {"accion": "muestra", "valor": "reconocedor/pruebas/A.json"}, esp("listo_escritura", 300), pausa(800)],
             [a("pestana_escribir", "Abre Escribir"), a("zona_escritura", "Escribe con el mouse"),
              ok("entrada_ejercicio", "Aquí aparece lo leído", "abajo"), a("btn_resolver", "Haz clic en Resolver", "abajo")],
             reiniciar=True))
C.append(cap("4-3_mano_02", "4.3", [],
             [ok(["herr_lapiz", "herr_borrador"], "Lápiz y borrador", "abajo"),
              ok(["herr_deshacer", "herr_borrar_todo"], "Deshacer y borrar todo", "derecha"),
              ok("estado_escritura", "Qué está haciendo", "abajo")]))

# ---------- Grupo 3: Historial y Configuración
H = [ir("Resolver"), esc(Q4)] + RESOLVER + [ir("Resolver"), esc(P)] + RESOLVER
C.append(cap("7-1_buscar_01", "7.1",
             H + [ir("Historial"), pausa(1500), {"accion": "texto", "control": "buscador", "valor": "x^3"}, pausa(800)],
             [a("buscador", "Escribe lo que buscas", "arriba"),
              a(["selector_curso", "selector_fecha", "solo_favoritos"], "O filtra la lista", "arriba"),
              ok("filas_historial", "Tus ejercicios", "abajo")], reiniciar=True))
C.append(cap("7-2_hoja_01", "7.2", [ir("Historial"), {"accion": "texto", "control": "buscador", "valor": ""}, pausa(1000)],
             [a("btn_ojo", "Haz clic en el ojo", "abajo")]))
C.append(cap("7-2_hoja_02", "7.2", [clic("btn_ojo"), pausa(3500)],
             [ok("cuerpo_hoja", "Todos los pasos", "izquierda"), a("cerrar_hoja", "Haz clic en Cerrar", "arriba")]))
C.append(cap("7-3_abrir_01", "7.3", [], [a("abrir_en_resolver", "Haz clic en Abrir en Resolver", "izquierda", dist=120)]))
C.append(cap("7-4_exportar_01", "7.4", [], [a("hoja_exportar", "Haz clic en Exportar PDF", "arriba")]))
C.append(cap("7-5_favorito_01", "7.5", [clic("cerrar_hoja"), pausa(800), clic("btn_estrella"), pausa(800)],
             [a("btn_estrella", "Haz clic en la estrella", "abajo"), a("solo_favoritos", "Activa Solo favoritos", "izquierda")]))
C.append(cap("7-6_eliminar_01", "7.6", [clic("btn_papelera"), pausa(1000)],
             [a("btn_papelera", "Haz clic en la papelera", "abajo"), a("confirmar_eliminar", "Haz clic en Eliminar", "abajo")]))
C.append(cap("10-1_resolucion_01", "10.1", [clic("cancelar_eliminar"), pausa(500), ir("Configuración"), pausa(1200)],
             [a(["cfg_campo_q", "cfg_campo_r", "cfg_campo_c"], "Elige el campo"),
              a(["cfg_modo_todos", "cfg_modo_paso"], "Elige cómo se abre"),
              a("cfg_explicaciones", "Muestra las explicaciones", "izquierda"),
              a("cfg_codigos", "Muestra los códigos", "izquierda")]))
C.append(cap("10-2_apariencia_01", "10.2", [ir("Configuración"), pausa(800)],
             [a(["cfg_letra_normal", "cfg_letra_grande", "cfg_letra_muy_grande"], "Elige el tamaño"),
              ok("cfg_vista_previa", "Así se verá"), a(["cfg_tema_claro", "cfg_tema_oscuro"], "Elige el tema")]))
C.append(cap("10-4_version_01", "10.4", [ir("Configuración"), pausa(800), ver("cfg_versiones")], [ok("cfg_versiones", "Versión de MathNova", "izquierda")]))
C.append(cap("10-3_borrar_01", "10.3", [ir("Configuración"), pausa(800), clic("cfg_borrar"), pausa(3000)],
             [a("cfg_borrar", "Haz clic en Borrar todo el historial"), a("dlg_borrar_todo", "Haz clic en Borrar todo", "abajo")]))

# ---------- Grupo 4: Práctica y Ejemplos
C.append(cap("8-1_empezar_01", "8.1", [ir("Práctica"), pausa(3000)],
             [a("menu_practica", "Abre Práctica"), a("practica_empezar", "Haz clic en Empezar")], reiniciar=True))
C.append(cap("8-2_metodo_01", "8.2", [clic("practica_empezar"), pausa(2500)],
             [ok("practica_pregunta", "Qué se pregunta", "arriba"), a("practica_opciones", "Elige el método", "izquierda"),
              ok("practica_avance", "Tu avance")]))
C.append(cap("8-3_pista_01", "8.3", [clic("ver_pista"), pausa(800)],
             [a("ver_pista", "Haz clic en Ver una pista", "abajo"), ok("caja_pista", "Esta es la pista", "abajo")]))
C.append(cap("8-2_metodo_02", "8.2", [clic("op_cubo"), pausa(1000)],
             [ok("aviso_no_aplica", "Te dice por qué no", "abajo")]))
C.append(cap("8-2_metodo_03", "8.2", [clic("op_agrupacion"), pausa(1500)],
             [ok("practica_pasos", "El paso se agrega aquí", "arriba"), a("practica_opciones", "Sigue con el próximo", "izquierda")]))
C.append(cap("8-2_metodo_04", "8.2", [clic("op_diferencia"), pausa(1500)],
             [ok("practica_pregunta", "Último paso: ¿Terminaste?", "arriba"),
              a("op_si_terminaste", "Elige Sí si ya no se puede factorizar", "abajo")]))
C.append(cap("8-4_terminar_01", "8.4", [clic("op_si_terminaste"), pausa(2000)],
             [ok("caja_final", "Tu resultado", "izquierda"), a("practicar_otro", "Practica otro", "izquierda"), a("repetir", "O repite este", "abajo")]))
C.append(cap("9-1_ejemplo_01", "9.1", [ir("Ejemplos"), pausa(1500), clic("curso_fundamentos"), pausa(800)],
             [a("curso_fundamentos", "Elige el curso", "derecha", dist=330),
              a("tema_primero", "Elige el tema", "abajo", dist=150), a("ejemplo_resolver", "Haz clic en Resolver", "derecha")]))
C.append(cap("9-2_practicar_01", "9.2", [ir("Ejemplos"), pausa(800)], [a("ejemplo_practicar", "Haz clic en Practicar")]))

salida = Path(__file__).resolve().parents[1] / "capturas.json"
salida.write_text(json.dumps(C, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"{len(C)} capturas -> {salida}")
