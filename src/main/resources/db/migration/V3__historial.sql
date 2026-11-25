-- MathNova · Historial (Flyway V3, Sprint 9, HU14–HU15)
-- V1 y V2 no se tocan: aquí solo se agregan columnas y filas de catálogo.

-- ============ resolucion: lo que se escribió y su tema ============
-- entrada_texto: el texto exacto (sección 9) que se resolvió, para "Abrir en Resolver" y para recalcular
-- (RNF19). Un límite se guarda sobre la función: sin esta columna no se sabría a qué punto tendía.
-- entrada_latex: la fórmula que dibuja la tabla del Historial.
-- tema_id: el tema de ESTA resolución (la fila de la tabla); ejercicio_tema junta los de todas.
ALTER TABLE resolucion ADD COLUMN entrada_texto TEXT;
ALTER TABLE resolucion ADD COLUMN entrada_latex TEXT;
ALTER TABLE resolucion ADD COLUMN tema_id INTEGER REFERENCES tema(id);
CREATE INDEX ix_resolucion_tema ON resolucion(tema_id);

-- ============ Catálogos ============
INSERT INTO operacion (codigo, nombre, modulo) VALUES
 ('RESOLVER_ECUACION', 'Resolver ecuación', 'FACTORIZACION');

-- Pasos que los motores ya muestran en la pizarra y que no eran técnicas del catálogo de V2.
-- Sin ellos, paso.tecnica_codigo (clave foránea) no los dejaba guardar.
INSERT INTO tecnica (codigo, nombre, categoria, campo_min) VALUES
 ('PR', 'Pasar a R', 'INTERNO', 'R'),
 ('PC', 'Pasar a C', 'INTERNO', 'C'),
 ('SIMP', 'Simplificar el resultado', 'INTERNO', NULL),
 ('PCR', 'Puntos críticos (f''(x) = 0)', 'INTERNO', NULL),
 ('EC', 'Resolver la ecuación', 'INTERNO', NULL),
 ('EXP', 'Expandir el polinomio', 'INTERNO', NULL),
 ('PROD', 'Cociente como producto f·g⁻¹', 'INTERNO', NULL),
 ('EXPO', 'Potencia como exponencial e^(g·ln f)', 'INTERNO', NULL),
 ('K', 'Constante fuera de la integral', 'INTERNO', NULL),
 ('EQ1', 'Pasar todo a un lado', 'INTERNO', NULL),
 ('EQ2', 'Factorizar', 'INTERNO', NULL),
 ('EQ3', 'Igualar cada factor a cero', 'INTERNO', NULL),
 ('EQ4', 'Despejar', 'INTERNO', NULL);

-- ============ Lo que ya estaba guardado (Sprints 2–8) ============
UPDATE resolucion SET entrada_texto = (SELECT e.expresion_entrada FROM ejercicio e WHERE e.id = resolucion.ejercicio_id)
 WHERE operacion_codigo IN ('FACTORIZAR', 'SIMPLIFICAR');

UPDATE resolucion SET entrada_texto =
   'lim(' || (SELECT valor FROM parametro_resolucion p WHERE p.resolucion_id = resolucion.id AND p.nombre = 'variable')
   || '→' || (SELECT valor FROM parametro_resolucion p WHERE p.resolucion_id = resolucion.id AND p.nombre = 'punto')
   || CASE (SELECT valor FROM parametro_resolucion p WHERE p.resolucion_id = resolucion.id AND p.nombre = 'lado')
        WHEN 'DERECHA' THEN '+' WHEN 'IZQUIERDA' THEN '-' ELSE '' END
   || ', ' || (SELECT e.expresion_entrada FROM ejercicio e WHERE e.id = resolucion.ejercicio_id) || ')'
 WHERE operacion_codigo = 'LIMITE';

UPDATE resolucion SET tema_id = (SELECT t.id FROM tema t WHERE t.nombre = CASE
     WHEN resolucion.operacion_codigo = 'SIMPLIFICAR' THEN 'Fracciones algebraicas'
     WHEN resolucion.operacion_codigo = 'LIMITE' THEN 'Límites'
     WHEN (SELECT e.variables FROM ejercicio e WHERE e.id = resolucion.ejercicio_id) LIKE '%,%'
          THEN 'Factorización en varias variables'
     WHEN resolucion.campo IN ('R', 'C') THEN 'Factorización en R y C'
     ELSE 'Factorización en Q' END);

INSERT OR IGNORE INTO ejercicio_tema (ejercicio_id, tema_id)
 SELECT ejercicio_id, tema_id FROM resolucion WHERE tema_id IS NOT NULL;
