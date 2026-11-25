package pe.mathnova.domain.expresion;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record IntegralMultiple(Expresion cuerpo, List<Rango> rangos, Coordenadas coordenadas) implements Expresion {

    public enum Coordenadas {
        CARTESIANAS("cartesianas"), POLARES("polares"), CILINDRICAS("cilíndricas"), ESFERICAS("esféricas");

        private final String nombre;

        Coordenadas(String nombre) {
            this.nombre = nombre;
        }

        public String nombre() {
            return nombre;
        }

        public String clave() {
            return switch (this) {
                case CARTESIANAS -> "cartesianas";
                case POLARES -> "polares";
                case CILINDRICAS -> "cilindricas";
                case ESFERICAS -> "esfericas";
            };
        }
    }

    public IntegralMultiple {
        Objects.requireNonNull(cuerpo, "cuerpo");
        Objects.requireNonNull(coordenadas, "coordenadas");
        rangos = List.copyOf(rangos);
        if (rangos.size() < 2 || rangos.size() > 3) {
            throw new IllegalArgumentException("Una integral múltiple es doble o triple");
        }
        var vistas = new HashSet<Character>();
        for (Rango r : rangos) {
            if (!vistas.add(r.variable())) {
                throw new IllegalArgumentException("La variable " + r.variable() + " se repite");
            }
        }
    }

    public IntegralMultiple(Expresion cuerpo, List<Rango> rangos) {
        this(cuerpo, rangos, Coordenadas.CARTESIANAS);
    }

    public boolean esDoble() {
        return rangos.size() == 2;
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
