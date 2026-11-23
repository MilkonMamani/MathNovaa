package pe.mathnova.domain.calculo.extremos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Extremos;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;

/**
 * Los puntos críticos de f clasificados (HU30, RF55): con el criterio del Hessiano (V07) o, con una restricción,
 * con los multiplicadores de Lagrange (V08).
 *
 * @param variables   las letras de f (y de g), en orden
 * @param gradiente   ∇f, componente por componente
 * @param hessiana    la matriz hessiana de f (en el Hessiano); vacía con Lagrange
 * @param restriccion g(x, y) − c, la restricción igualada a cero; null sin restricción
 * @param puntos      ordenados por coordenadas
 * @param avisos      raíces irracionales que se dejaron fuera, ramas que no se resolvieron
 * @param completo    false si el sistema no se pudo resolver del todo
 */
public record ResolucionExtremos(Extremos entrada, Tipo tipo, Expresion funcion, List<Character> variables,
                                 List<Expresion> gradiente, List<List<Expresion>> hessiana, Expresion restriccion,
                                 List<Punto> puntos, List<PasoLimite> pasos, List<String> avisos, boolean completo) {

    public enum Tipo {
        HESSIANO("V07"), LAGRANGE("V08");

        private final String codigo;

        Tipo(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    public enum Clase {
        MINIMO("mínimo local"), MAXIMO("máximo local"), SILLA("punto de silla"), NO_DECIDE("el criterio no decide"),
        /** Lagrange: el mayor valor de f sobre la restricción entre los puntos comparados. */
        MAXIMO_RESTRINGIDO("máximo"),
        MINIMO_RESTRINGIDO("mínimo"),
        SIN_CLASIFICAR("sin clasificar");

        private final String texto;

        Clase(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }

        public boolean esMinimo() {
            return this == MINIMO || this == MINIMO_RESTRINGIDO;
        }

        public boolean esMaximo() {
            return this == MAXIMO || this == MAXIMO_RESTRINGIDO;
        }
    }

    /**
     * @param lambda el multiplicador (Lagrange); null en el Hessiano
     * @param valor  f en el punto
     * @param fxx    las segundas derivadas en el punto (Hessiano); null con Lagrange
     * @param d      D = fxx·fyy − fxy²; null con Lagrange
     */
    public record Punto(List<Racional> coordenadas, Racional lambda, Racional valor, Racional fxx, Racional fxy,
                        Racional fyy, Racional d, Clase clase) {

        public Punto {
            coordenadas = List.copyOf(coordenadas);
            Objects.requireNonNull(clase, "clase");
        }

        /** "(1, 0)". */
        public String texto() {
            return "(" + String.join(", ", coordenadas.stream().map(ImpresoraPizarra::numero).toList()) + ")";
        }
    }

    public ResolucionExtremos {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(tipo, "tipo");
        variables = List.copyOf(variables);
        gradiente = List.copyOf(gradiente);
        hessiana = List.copyOf(hessiana);
        puntos = List.copyOf(puntos);
        pasos = List.copyOf(pasos);
        avisos = List.copyOf(avisos);
    }

    /** "(0, 0) punto de silla; (1, 1) mínimo local", o "(5, 5) máximo, f = 25". */
    public String texto() {
        if (puntos.isEmpty()) {
            return completo ? "No hay puntos críticos con coordenadas exactas" : "No se pudo resolver el sistema";
        }
        List<String> r = new ArrayList<>();
        for (Punto p : puntos) {
            r.add(p.texto() + " " + p.clase().texto() + (tipo == Tipo.LAGRANGE
                    ? ", f = " + ImpresoraPizarra.numero(p.valor()) : ""));
        }
        return String.join("; ", r);
    }

    public String latex() {
        List<String> r = new ArrayList<>();
        for (Punto p : puntos) {
            r.add(p.texto() + "\\ \\text{" + p.clase().texto() + "}");
        }
        String izq = ImpresoraLatex.imprimir(entrada);
        return r.isEmpty() ? izq : izq + ":\\quad " + String.join(";\\ ", r);
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
