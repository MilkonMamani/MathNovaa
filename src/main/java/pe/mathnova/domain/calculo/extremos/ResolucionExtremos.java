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

    public record Punto(List<Racional> coordenadas, Racional lambda, Racional valor, Racional fxx, Racional fxy,
                        Racional fyy, Racional d, Clase clase) {

        public Punto {
            coordenadas = List.copyOf(coordenadas);
            Objects.requireNonNull(clase, "clase");
        }

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
