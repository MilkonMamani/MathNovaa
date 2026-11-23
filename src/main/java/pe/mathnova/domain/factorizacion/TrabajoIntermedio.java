package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;

/**
 * Lo que el estudiante ve entre el "antes" y el "después" de un paso (HU04). El dominio solo entrega
 * los datos; los dibujos de pizarra (HU06) están en presentation.
 */
public sealed interface TrabajoIntermedio {

    /** F03, F04, F15 y P01: renglones como "x³ − 2x² = x²(x − 2)". */
    record Agrupacion(List<String> grupos) implements TrabajoIntermedio {
        public Agrupacion {
            grupos = List.copyOf(grupos);
        }
    }

    /**
     * F12 y F13: el aspa simple. Las filas son (a1·x | c1) y (a2·x | c2): los factores son
     * (a1x + c1)(a2x + c2). Los textos fila1, fila2 y verificacion son los de siempre.
     * En M04 (homogénea) la columna de la derecha lleva la segunda letra: (a1x + c1y)(a2x + c2y);
     * en F12 y F13 {@code segunda} es 0.
     */
    record AspaSimple(String fila1, String fila2, String verificacion, char variable,
                      Racional a1, Racional c1, Racional a2, Racional c2, char segunda) implements TrabajoIntermedio {

        public AspaSimple(String fila1, String fila2, String verificacion, char variable,
                          Racional a1, Racional c1, Racional a2, Racional c2) {
            this(fila1, fila2, verificacion, variable, a1, c1, a2, c2, (char) 0);
        }

        public AspaSimple {
            Objects.requireNonNull(fila1, "fila1");
            Objects.requireNonNull(fila2, "fila2");
            Objects.requireNonNull(verificacion, "verificacion");
            Objects.requireNonNull(a1, "a1");
            Objects.requireNonNull(c1, "c1");
            Objects.requireNonNull(a2, "a2");
            Objects.requireNonNull(c2, "c2");
        }
    }

    /** F05, F07, F08–F11: la identidad usada con sus valores, como "a² − b² = (a + b)(a − b) con a = 2x y b = 5". */
    record Comparacion(String detalle) implements TrabajoIntermedio {
        public Comparacion {
            Objects.requireNonNull(detalle, "detalle");
        }
    }

    /**
     * F16 y F17: tablas de Ruffini apiladas (el cociente de una es la fila de arriba de la siguiente)
     * y los candidatos p/q probados, en el orden de RN09.
     */
    record TablaRuffini(char variable, List<Candidato> candidatos, List<Division> divisiones)
            implements TrabajoIntermedio {

        public TablaRuffini {
            candidatos = List.copyOf(candidatos);
            divisiones = List.copyOf(divisiones);
            if (divisiones.isEmpty()) {
                throw new IllegalArgumentException("Hace falta al menos una división");
            }
        }

        /** Un candidato p/q y el residuo que deja; es raíz si el residuo es 0. */
        public record Candidato(Racional valor, Racional residuo) {
            public boolean esRaiz() {
                return residuo.esCero();
            }
        }

        /**
         * Una división sintética entre (x − raiz).
         *
         * @param coeficientes fila de arriba, de mayor a menor grado
         * @param productos    fila del medio: resultado anterior × raíz, uno por columna desde la segunda
         * @param resultados   fila de abajo; el último es el residuo
         * @param rellenos     posiciones de la fila de arriba que eran términos faltantes (se completan con 0)
         */
        public record Division(Racional raiz, List<Racional> coeficientes, List<Racional> productos,
                               List<Racional> resultados, List<Integer> rellenos) {
            public Division {
                coeficientes = List.copyOf(coeficientes);
                productos = List.copyOf(productos);
                resultados = List.copyOf(resultados);
                rellenos = List.copyOf(rellenos);
            }

            public Racional residuo() {
                return resultados.getLast();
            }
        }
    }

    /**
     * F19: (a1x² + px + e1)(a2x² + qx + e2). {@code coeficienteX2} es el del término x² de la
     * entrada: se tiene a1·e2 + a2·e1 y falta el resto, que se reparte como p·q en la columna central.
     */
    record AspaDobleEspecial(char variable, Racional a1, Racional p, Racional e1, Racional a2, Racional q,
                             Racional e2, Racional coeficienteX2) implements TrabajoIntermedio {
        public AspaDobleEspecial {
            Objects.requireNonNull(a1, "a1");
            Objects.requireNonNull(p, "p");
            Objects.requireNonNull(e1, "e1");
            Objects.requireNonNull(a2, "a2");
            Objects.requireNonNull(q, "q");
            Objects.requireNonNull(e2, "e2");
            Objects.requireNonNull(coeficienteX2, "coeficienteX2");
        }
    }

    /**
     * F20: el producto planteado con letras, una ecuación por coeficiente y la solución.
     *
     * @param planteo   "(x² + ax + b)(x³ + cx² + dx + e)"
     * @param solucion  "a = 1", "b = 1"…
     * @param resultado el producto con los valores encontrados
     */
    record SistemaCoeficientes(String planteo, List<Ecuacion> ecuaciones, List<String> solucion, String resultado)
            implements TrabajoIntermedio {

        public SistemaCoeficientes {
            Objects.requireNonNull(planteo, "planteo");
            ecuaciones = List.copyOf(ecuaciones);
            solucion = List.copyOf(solucion);
            Objects.requireNonNull(resultado, "resultado");
        }

        /** "coeficiente de x⁴" → "a + c = 1". */
        public record Ecuacion(String potencia, String ecuacion) {
        }
    }

    /**
     * I03: la tabla de la integración por partes (∫u dv = uv − ∫v du), con la elección ILATE (RN36).
     *
     * @param u        "ln x"
     * @param du       "(1/x) dx"
     * @param dv       "x dx"
     * @param v        "x²/2"
     * @param ilate    por qué u es esa: "ln x es logarítmica (L) y va antes que x, algebraica (A)"
     * @param lineas   las cuentas: uv − ∫v du con los valores, y el despeje si la integral reaparece
     */
    record TablaPartes(String u, String du, String dv, String v, String ilate, List<String> lineas)
            implements TrabajoIntermedio {
        public TablaPartes {
            Objects.requireNonNull(u, "u");
            Objects.requireNonNull(du, "du");
            Objects.requireNonNull(dv, "dv");
            Objects.requireNonNull(v, "v");
            Objects.requireNonNull(ilate, "ilate");
            lineas = List.copyOf(lineas);
        }
    }

    /**
     * I05: el triángulo rectángulo de la sustitución trigonométrica, para regresar de θ a x.
     *
     * @param sustitucion "x = 3·sen θ"
     * @param opuesto     cateto opuesto a θ ("x")
     * @param adyacente   cateto adyacente ("√(9 − x²)")
     * @param hipotenusa  "3"
     * @param angulo      "θ = arcsen(x/3)"
     * @param razones     las razones que se usan para regresar: "cot θ = √(9 − x²)/x"
     */
    record TrianguloSustitucion(String sustitucion, String opuesto, String adyacente, String hipotenusa,
                                String angulo, List<String> razones, List<String> lineas)
            implements TrabajoIntermedio {
        public TrianguloSustitucion {
            Objects.requireNonNull(sustitucion, "sustitucion");
            Objects.requireNonNull(opuesto, "opuesto");
            Objects.requireNonNull(adyacente, "adyacente");
            Objects.requireNonNull(hipotenusa, "hipotenusa");
            Objects.requireNonNull(angulo, "angulo");
            razones = List.copyOf(razones);
            lineas = List.copyOf(lineas);
        }
    }

    /**
     * M05: el aspa doble en tres columnas (x | y | término independiente) para
     * Ax² + Bxy + Cy² + Dx + Ey + F = (a1x + c1y + f1)(a2x + c2y + f2), con las tres aspas verificadas.
     */
    record AspaDoble(char x, char y, Racional a1, Racional c1, Racional f1, Racional a2, Racional c2, Racional f2)
            implements TrabajoIntermedio {
        public AspaDoble {
            Objects.requireNonNull(a1, "a1");
            Objects.requireNonNull(c1, "c1");
            Objects.requireNonNull(f1, "f1");
            Objects.requireNonNull(a2, "a2");
            Objects.requireNonNull(c2, "c2");
            Objects.requireNonNull(f2, "f2");
        }
    }

    /**
     * V04: el diagrama de árbol de la regla de la cadena: f arriba, las variables intermedias en medio y
     * el parámetro abajo; cada rama lleva su derivada.
     *
     * @param funcion     "f"
     * @param intermedias "x", "y"
     * @param parametro   "t"
     * @param ramasArriba "∂f/∂x = 2xy" una por intermedia
     * @param ramasAbajo  "dx/dt = 2t" una por intermedia
     * @param suma        "df/dt = fx·x' + fy·y' = …"
     */
    record ArbolCadena(String funcion, List<String> intermedias, String parametro, List<String> ramasArriba,
                       List<String> ramasAbajo, String suma) implements TrabajoIntermedio {
        public ArbolCadena {
            Objects.requireNonNull(funcion, "funcion");
            intermedias = List.copyOf(intermedias);
            Objects.requireNonNull(parametro, "parametro");
            ramasArriba = List.copyOf(ramasArriba);
            ramasAbajo = List.copyOf(ramasAbajo);
            Objects.requireNonNull(suma, "suma");
        }
    }

    /**
     * F14 y F18: los tres renglones de un cambio de variable.
     *
     * @param variableNueva "u" o "z"
     * @param definicion    "u = x²" o "z = x + 1/x"
     * @param sustituir     el polinomio escrito en la variable nueva
     * @param factorizar    su factorización en la variable nueva
     * @param regresar      la factorización con la variable original
     */
    record Sustitucion(String variableNueva, String definicion, String sustituir, String factorizar, String regresar)
            implements TrabajoIntermedio {
        public Sustitucion {
            Objects.requireNonNull(variableNueva, "variableNueva");
            Objects.requireNonNull(definicion, "definicion");
            Objects.requireNonNull(sustituir, "sustituir");
            Objects.requireNonNull(factorizar, "factorizar");
            Objects.requireNonNull(regresar, "regresar");
        }
    }
}
