package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;

public sealed interface TrabajoIntermedio {

    record Agrupacion(List<String> grupos) implements TrabajoIntermedio {
        public Agrupacion {
            grupos = List.copyOf(grupos);
        }
    }

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

    record Comparacion(String detalle) implements TrabajoIntermedio {
        public Comparacion {
            Objects.requireNonNull(detalle, "detalle");
        }
    }

    record TablaRuffini(char variable, List<Candidato> candidatos, List<Division> divisiones)
            implements TrabajoIntermedio {

        public TablaRuffini {
            candidatos = List.copyOf(candidatos);
            divisiones = List.copyOf(divisiones);
            if (divisiones.isEmpty()) {
                throw new IllegalArgumentException("Hace falta al menos una división");
            }
        }

        public record Candidato(Racional valor, Racional residuo) {
            public boolean esRaiz() {
                return residuo.esCero();
            }
        }

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

    record SistemaCoeficientes(String planteo, List<Ecuacion> ecuaciones, List<String> solucion, String resultado)
            implements TrabajoIntermedio {

        public SistemaCoeficientes {
            Objects.requireNonNull(planteo, "planteo");
            ecuaciones = List.copyOf(ecuaciones);
            solucion = List.copyOf(solucion);
            Objects.requireNonNull(resultado, "resultado");
        }

        public record Ecuacion(String potencia, String ecuacion) {
        }
    }

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
