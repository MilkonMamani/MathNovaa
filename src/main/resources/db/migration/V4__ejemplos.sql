-- MathNova · Ejemplos (Flyway V4, Sprint 9, parte B)
-- V1 a V3 no se tocan: aquí se agregan dos columnas a ejemplo y sus filas.
--
-- expresion: el texto de la sección 9 que "Resolver" pone en el editor, tal cual.
-- enunciado: lo que se le pide al estudiante; la tarjeta lo muestra en mayúsculas pequeñas ("FACTORIZA").
-- nota: el método en pocas palabras, como lo muestra la pizarra ("Ruffini y aspa simple").
-- parametros_json: {"campo": "R"} cuando la factorización no es en Q; nulo en lo demás.
-- resultado_esperado: la línea de resultado del motor. RN20: EjemplosTest resuelve cada fila y la compara;
--   un ejemplo que el motor no resuelve no puede estar aquí.
ALTER TABLE ejemplo ADD COLUMN nota TEXT;
ALTER TABLE ejemplo ADD COLUMN orden INTEGER NOT NULL DEFAULT 0;
CREATE INDEX ix_ejemplo_tema ON ejemplo(tema_id, orden);

-- El tema por curso y nombre: "Límites" y "Límites dobles" son de cursos distintos.
CREATE TEMP TABLE ejemplo_nuevo (curso TEXT, tema TEXT, orden INTEGER, enunciado TEXT, expresion TEXT,
                                 operacion TEXT, parametros TEXT, esperado TEXT, nota TEXT, dificultad INTEGER);

INSERT INTO ejemplo_nuevo VALUES
 -- Fundamentos de Matemática · Factorización en Q
 ('Fundamentos de Matemática', 'Factorización en Q', 1, 'Factoriza', 'x^3 - 2x^2 - x + 2', 'FACTORIZAR', NULL,
  '= (x + 1)(x - 1)(x - 2)', 'Agrupación y diferencia de cuadrados', 1),
 ('Fundamentos de Matemática', 'Factorización en Q', 2, 'Factoriza', '2x^3 - 3x^2 - 3x + 2', 'FACTORIZAR', NULL,
  '= (x + 1)(2x - 1)(x - 2)', 'Ruffini y aspa simple', 2),
 ('Fundamentos de Matemática', 'Factorización en Q', 3, 'Factoriza', 'x^4 + 5x^3 + 11x^2 + 14x + 4', 'FACTORIZAR', NULL,
  '= (x^2 + 2x + 4)(x^2 + 3x + 1)', 'Aspa doble especial', 3),
 ('Fundamentos de Matemática', 'Factorización en Q', 4, 'Factoriza', 'x^4 + 4', 'FACTORIZAR', NULL,
  '= (x^2 + 2x + 2)(x^2 - 2x + 2)', 'Quita y pon: se completa un cuadrado', 2),

 -- Fundamentos de Matemática · Factorización en R y C
 ('Fundamentos de Matemática', 'Factorización en R y C', 1, 'Factoriza en R', 'x^2 - 2', 'FACTORIZAR', '{"campo":"R"}',
  '= (x - √2)(x + √2)', 'Diferencia de cuadrados con radicales', 1),
 ('Fundamentos de Matemática', 'Factorización en R y C', 2, 'Factoriza en C', 'x^2 + 4', 'FACTORIZAR', '{"campo":"C"}',
  '= (x - 2·i)(x + 2·i)', 'Suma de cuadrados con i', 1),
 ('Fundamentos de Matemática', 'Factorización en R y C', 3, 'Factoriza en R', 'x^4 + 1', 'FACTORIZAR', '{"campo":"R"}',
  '= (x^2 - √2·x + 1)(x^2 + √2·x + 1)', 'Irreducible en Q; en R se juntan las raíces conjugadas', 2),
 ('Fundamentos de Matemática', 'Factorización en R y C', 4, 'Factoriza en C', 'x^4 + 1', 'FACTORIZAR', '{"campo":"C"}',
  '= (x - √2/2 - √2·i/2)(x + √2/2 - √2·i/2)(x + √2/2 + √2·i/2)(x - √2/2 + √2·i/2)',
  'Las cuatro raíces cuartas de −1 con De Moivre', 3),
 ('Fundamentos de Matemática', 'Factorización en R y C', 5, 'Factoriza en R', 'x^3 - 3x + 1', 'FACTORIZAR', '{"campo":"R"}',
  '= (x - 2cos((1/3)·arccos(-1/2)))(x - 2cos((1/3)·arccos(-1/2) - 2π/3))(x - 2cos((1/3)·arccos(-1/2) - 4π/3))',
  'Cardano en forma trigonométrica: tres raíces reales', 3),

 -- Fundamentos de Matemática · Fracciones algebraicas
 ('Fundamentos de Matemática', 'Fracciones algebraicas', 1, 'Simplifica', '(x^2 - 4)/(x^2 - 4x + 4)', 'SIMPLIFICAR', NULL,
  '= (x + 2)/(x - 2)', 'Diferencia de cuadrados y trinomio cuadrado perfecto', 1),
 ('Fundamentos de Matemática', 'Fracciones algebraicas', 2, 'Simplifica', '(x^3 - 1)/(x^2 - 1)', 'SIMPLIFICAR', NULL,
  '= (x^2 + x + 1)/(x + 1)', 'Diferencia de cubos y de cuadrados', 2),

 -- Cálculo 1 · Límites
 ('Cálculo 1', 'Límites', 1, 'Calcula el límite', 'lim(x→2, (x^2 - 4)/(x - 2))', 'LIMITE', NULL,
  '= 4', 'Queda 0/0: factorizar y cancelar', 1),
 ('Cálculo 1', 'Límites', 2, 'Calcula el límite', 'lim(x→0, (e^x - 1 - x)/x^2)', 'LIMITE', NULL,
  '= 1/2 ≈ 0.5', 'L''Hôpital dos veces', 2),
 ('Cálculo 1', 'Límites', 3, 'Calcula el límite', 'lim(x→2, 1/(x - 2))', 'LIMITE', NULL,
  'No existe', 'Los límites laterales no coinciden', 1),

 -- Cálculo 1 · Derivadas
 ('Cálculo 1', 'Derivadas', 1, 'Deriva', 'd/dx(x^2 sen x)', 'DERIVAR', NULL,
  '= 2x·sen x + x²·cos x', 'Regla del producto', 1),
 ('Cálculo 1', 'Derivadas', 2, 'Halla los puntos críticos', 'd/dx(x^3 - 3x^2 - 9x + 5)', 'PUNTOS_CRITICOS', NULL,
  '(-1, 10) máximo local; (3, -22) mínimo local', 'f''(x) = 0 y el signo de f''''', 2),

 -- Cálculo 1 · Funciones trascendentes
 ('Cálculo 1', 'Funciones trascendentes', 1, 'Factoriza', 'e^(2x) - 3e^x + 2', 'FACTORIZAR', NULL,
  '= (e^x - 1)·(e^x - 2)', 'Cambio de variable u = eˣ', 2),
 ('Cálculo 1', 'Funciones trascendentes', 2, 'Factoriza', 'ln^2 x - 4', 'FACTORIZAR', NULL,
  '= (ln(x) + 2)·(ln(x) - 2)', 'Cambio u = ln x y diferencia de cuadrados', 1),

 -- Cálculo 2 · Integrales
 ('Cálculo 2', 'Integrales', 1, 'Integra', '∫(e^x sen x, x)', 'INTEGRAR', NULL,
  '= eˣ(sen x − cos x)/2 + C', 'Por partes dos veces: la integral vuelve a aparecer', 3),
 ('Cálculo 2', 'Integrales', 2, 'Integra', '∫(√(1 - x^2), x)', 'INTEGRAR', NULL,
  '= x·√(1 − x²)/2 + (1/2)arcsen x + C', 'Sustitución trigonométrica x = sen θ', 3),
 ('Cálculo 2', 'Integrales', 3, 'Calcula la integral definida', '∫(x^3 - x, x, -1, 1)', 'INTEGRAL_DEFINIDA', NULL,
  '= 0', 'Regla de Barrow; el integrando es impar', 1),

 -- Cálculo 2 · Fracciones parciales
 ('Cálculo 2', 'Fracciones parciales', 1, 'Integra', '∫((3x + 5)/((x - 1)(x + 2)), x)', 'INTEGRAR', NULL,
  '= (8/3)ln|x − 1| + (1/3)ln|x + 2| + C', 'Fracciones parciales con factores lineales', 2),
 ('Cálculo 2', 'Fracciones parciales', 2, 'Integra', '∫(1/(x^2 - 1), x)', 'INTEGRAR', NULL,
  '= (1/2)ln|x − 1| − (1/2)ln|x + 1| + C', 'Diferencia de cuadrados y fracciones parciales', 2),

 -- Cálculo 3 · Factorización en varias variables
 ('Cálculo 3', 'Factorización en varias variables', 1, 'Factoriza', 'x^2 + 3xy + 2y^2 + 4x + 5y + 3', 'FACTORIZAR', NULL,
  '= (x + 2y + 3)(x + y + 1)', 'Aspa doble', 2),
 ('Cálculo 3', 'Factorización en varias variables', 2, 'Factoriza', 'xy + x + y + 1', 'FACTORIZAR', NULL,
  '= (x + 1)(y + 1)', 'Agrupación de términos', 1),

 -- Cálculo 3 · Límites dobles
 ('Cálculo 3', 'Límites dobles', 1, 'Analiza el límite', 'lim((x, y)→(0, 0), x^2 y/(x^4 + y^2))', 'LIMITE_DOBLE', NULL,
  'No existe', 'Las rectas dan 0; la parábola y = x² no', 3),
 ('Cálculo 3', 'Límites dobles', 2, 'Analiza el límite', 'lim((x, y)→(0, 0), xy/(x^2 + y^2))', 'LIMITE_DOBLE', NULL,
  'No existe', 'Por las rectas y = mx el valor depende de m', 2),

 -- Cálculo 3 · Derivadas parciales
 ('Cálculo 3', 'Derivadas parciales', 1, 'Deriva respecto de x', '∂/∂x(x^2 y + sen(xy))', 'DERIVADA_PARCIAL', NULL,
  '= 2y·x + y·cos(y·x)', 'y queda constante; regla de la cadena en sen(xy)', 2),
 ('Cálculo 3', 'Derivadas parciales', 2, 'Calcula el gradiente', 'grad(x^2 + y^2, (1, 2))', 'GRADIENTE', NULL,
  '= (2, 4)', 'Las dos parciales evaluadas en (1, 2)', 1),

 -- Cálculo 3 · Extremos
 ('Cálculo 3', 'Extremos', 1, 'Halla los extremos', 'extremos(x^3 + y^3 - 3xy)', 'EXTREMOS', NULL,
  '(0, 0) punto de silla; (1, 1) mínimo local', 'Puntos críticos y criterio del Hessiano', 2),
 ('Cálculo 3', 'Extremos', 2, 'Optimiza con la restricción', 'extremos(xy; x + y = 10)', 'LAGRANGE', NULL,
  '(5, 5) máximo, f = 25', 'Multiplicadores de Lagrange', 2),

 -- Cálculo 3 · Integrales múltiples
 ('Cálculo 3', 'Integrales múltiples', 1, 'Calcula la integral doble', '∬(e^(x^2), x, y, 1, y, 0, 1)', 'INTEGRAL_MULTIPLE', NULL,
  '= e/2 − 1/2', 'e^(x²) no tiene primitiva: se cambia el orden', 3),
 ('Cálculo 3', 'Integrales múltiples', 2, 'Calcula la integral doble', '∬(xy, x, 0, 1, y, 0, 2)', 'INTEGRAL_MULTIPLE', NULL,
  '= 1', 'Integral iterada sobre un rectángulo', 1);

INSERT INTO ejemplo (tema_id, enunciado, expresion, operacion_codigo, parametros_json, resultado_esperado, dificultad,
                     nota, orden)
SELECT t.id, n.enunciado, n.expresion, n.operacion, n.parametros, n.esperado, n.dificultad, n.nota, n.orden
  FROM ejemplo_nuevo n
  JOIN curso c ON c.nombre = n.curso
  JOIN tema t ON t.curso_id = c.id AND t.nombre = n.tema;

DROP TABLE ejemplo_nuevo;
