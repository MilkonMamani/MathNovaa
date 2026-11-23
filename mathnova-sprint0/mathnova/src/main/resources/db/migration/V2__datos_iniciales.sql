-- MathNova · catálogos iniciales (Flyway V2)

INSERT INTO curso (id, nombre, orden) VALUES
 (1, 'Fundamentos de Matemática', 1), (2, 'Cálculo 1', 2), (3, 'Cálculo 2', 3), (4, 'Cálculo 3', 4);

INSERT INTO tema (curso_id, nombre, orden) VALUES
 (1, 'Factorización en Q', 1), (1, 'Factorización en R y C', 2), (1, 'Fracciones algebraicas', 3),
 (2, 'Límites', 1), (2, 'Derivadas', 2), (2, 'Funciones trascendentes', 3),
 (3, 'Integrales', 1), (3, 'Fracciones parciales', 2),
 (4, 'Factorización en varias variables', 1), (4, 'Límites dobles', 2), (4, 'Derivadas parciales', 3),
 (4, 'Extremos', 4), (4, 'Integrales múltiples', 5);

INSERT INTO operacion (codigo, nombre, modulo) VALUES
 ('FACTORIZAR', 'Factorizar paso a paso', 'FACTORIZACION'),
 ('SIMPLIFICAR', 'Simplificar fracción', 'FRACCIONES'),
 ('LIMITE', 'Calcular límite', 'CALCULO'),
 ('ASINTOTAS', 'Hallar asíntotas', 'CALCULO'),
 ('DERIVAR', 'Derivar', 'CALCULO'),
 ('PUNTOS_CRITICOS', 'Hallar puntos críticos', 'CALCULO'),
 ('INTEGRAR', 'Integrar', 'CALCULO'),
 ('INTEGRAL_DEFINIDA', 'Integral definida', 'CALCULO'),
 ('LIMITE_DOBLE', 'Límite doble', 'VARIAS_VARIABLES'),
 ('DERIVADA_PARCIAL', 'Derivada parcial', 'VARIAS_VARIABLES'),
 ('GRADIENTE', 'Gradiente', 'VARIAS_VARIABLES'),
 ('DERIVADA_DIRECCIONAL', 'Derivada direccional', 'VARIAS_VARIABLES'),
 ('EXTREMOS', 'Extremos con Hessiano', 'VARIAS_VARIABLES'),
 ('LAGRANGE', 'Multiplicadores de Lagrange', 'VARIAS_VARIABLES'),
 ('INTEGRAL_MULTIPLE', 'Integral doble o triple', 'VARIAS_VARIABLES');

INSERT INTO tecnica (codigo, nombre, categoria, campo_min) VALUES
 ('F01','Factor común monomio','FACTORIZACION','Q'), ('F02','Factor común con coeficientes fraccionarios','FACTORIZACION','Q'),
 ('F03','Factor común polinomio','FACTORIZACION','Q'), ('F04','Agrupación de términos','FACTORIZACION','Q'),
 ('F05','Diferencia de cuadrados','FACTORIZACION','Q'), ('F06','Diferencia de cuadrados repetida','FACTORIZACION','Q'),
 ('F07','Trinomio cuadrado perfecto','FACTORIZACION','Q'), ('F08','Suma de cubos','FACTORIZACION','Q'),
 ('F09','Diferencia de cubos','FACTORIZACION','Q'), ('F10','Cubo perfecto de binomio','FACTORIZACION','Q'),
 ('F11','Suma o diferencia de potencias enésimas','FACTORIZACION','Q'), ('F12','Trinomio x² + bx + c','FACTORIZACION','Q'),
 ('F13','Trinomio ax² + bx + c (aspa simple)','FACTORIZACION','Q'), ('F14','Cambio de variable u = xⁿ','FACTORIZACION','Q'),
 ('F15','Quita y pon','FACTORIZACION','Q'), ('F16','Ruffini (divisores binómicos)','FACTORIZACION','Q'),
 ('F17','Ruffini con raíces repetidas','FACTORIZACION','Q'), ('F18','Polinomio recíproco','FACTORIZACION','Q'),
 ('F19','Aspa doble especial','FACTORIZACION','Q'), ('F20','Coeficientes indeterminados','FACTORIZACION','Q'),
 ('F21','Diferencia de cuadrados con radicales','FACTORIZACION','R'), ('F22','Completar cuadrados','FACTORIZACION','R'),
 ('F23','Fórmula general','FACTORIZACION','R'), ('F24','Suma de cuadrados en complejos','FACTORIZACION','C'),
 ('F25','Cuadrático con discriminante negativo','FACTORIZACION','C'), ('F26','Cubos con radicales','FACTORIZACION','R'),
 ('F27','Fórmula de Cardano','FACTORIZACION','R'), ('F28','Método de Ferrari','FACTORIZACION','R'),
 ('F29','Agrupar raíces conjugadas','FACTORIZACION','R'), ('F30','Raíces n-ésimas (De Moivre)','FACTORIZACION','C'),
 ('T01','Factor común trascendente','TRASCENDENTE','Q'), ('T02','Cambio de variable exponencial','TRASCENDENTE','Q'),
 ('T03','Cambio de variable trigonométrico','TRASCENDENTE','Q'), ('T04','Identidades trigonométricas','TRASCENDENTE','Q'),
 ('T05','Cambio de variable logarítmico','TRASCENDENTE','Q'),
 ('M01','Factor común (varias variables)','VARIAS_VARIABLES','Q'), ('M02','Agrupación (varias variables)','VARIAS_VARIABLES','Q'),
 ('M03','Productos notables en dos variables','VARIAS_VARIABLES','Q'), ('M04','Aspa simple homogénea','VARIAS_VARIABLES','Q'),
 ('M05','Aspa doble','VARIAS_VARIABLES','Q'), ('M06','Polinomio homogéneo','VARIAS_VARIABLES','Q'),
 ('M07','Ruffini respecto de una variable','VARIAS_VARIABLES','Q'), ('M08','Coeficientes indeterminados multivariable','VARIAS_VARIABLES','Q'),
 ('A1','Discriminante y completar cuadrados','INTERNO',NULL), ('A2','Factorización libre de cuadrados','INTERNO',NULL),
 ('A3','MCD y MCM de polinomios','INTERNO',NULL), ('A4','Verificación de irreducibilidad','INTERNO',NULL),
 ('A5','Aproximación de raíces','INTERNO',NULL), ('A6','Factorización multivariable','INTERNO',NULL),
 ('P01','Simplificar fracción algebraica','FRACCIONES',NULL), ('P02','División de polinomios','FRACCIONES',NULL),
 ('L01','Evaluación directa','LIMITE',NULL), ('L02','Factorizar y cancelar','LIMITE',NULL),
 ('L03','Racionalizar','LIMITE',NULL), ('L04','Límite al infinito por grados','LIMITE',NULL),
 ('L05','Límites laterales','LIMITE',NULL), ('L06','Límite notable','LIMITE',NULL), ('L07','Regla de L''Hôpital','LIMITE',NULL),
 ('D01','Derivada de una constante','DERIVADA',NULL), ('D02','Regla de la potencia','DERIVADA',NULL),
 ('D03','Regla de la suma','DERIVADA',NULL), ('D04','Regla del producto','DERIVADA',NULL),
 ('D05','Regla del cociente','DERIVADA',NULL), ('D06','Regla de la cadena','DERIVADA',NULL),
 ('D07','Derivada de función trascendente','DERIVADA',NULL), ('D08','Derivación implícita','DERIVADA',NULL),
 ('D09','Derivación logarítmica','DERIVADA',NULL),
 ('I01','Integral inmediata','INTEGRAL',NULL), ('I02','Sustitución','INTEGRAL',NULL), ('I03','Por partes (ILATE)','INTEGRAL',NULL),
 ('I04','Integral trigonométrica','INTEGRAL',NULL), ('I05','Sustitución trigonométrica','INTEGRAL',NULL),
 ('I06','Fracciones parciales','INTEGRAL',NULL), ('I07','Regla de Barrow','INTEGRAL',NULL),
 ('V01','Derivada parcial','CALCULO_3',NULL), ('V02','Gradiente','CALCULO_3',NULL), ('V03','Derivada direccional','CALCULO_3',NULL),
 ('V04','Regla de la cadena multivariable','CALCULO_3',NULL), ('V05','Límite por trayectorias','CALCULO_3',NULL),
 ('V06','Límite en coordenadas polares','CALCULO_3',NULL), ('V07','Criterio del Hessiano','CALCULO_3',NULL),
 ('V08','Multiplicadores de Lagrange','CALCULO_3',NULL), ('V09','Integral iterada','CALCULO_3',NULL),
 ('V10','Cambio de coordenadas con jacobiano','CALCULO_3',NULL);

INSERT INTO configuracion (clave, valor) VALUES
 ('campo_por_defecto', 'Q'), ('mostrar_descartados', 'false'), ('decimales_aproximacion', '10'), ('tema_interfaz', 'claro');
