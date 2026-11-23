package pe.mathnova.domain.expresion;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * ∬(cuerpo, x, a, b, y, c, d) o ∭ con tres rangos (HU34, HU31). Los rangos van en el orden escrito:
 * el primero es la integral de más adentro, y sus límites pueden depender de las variables de afuera.
 *
 * <p>Sprint 8 (V10): con "; coord=polares" (o cilindricas, esfericas) como último argumento, los rangos
 * se dan en r, θ (z) o ρ, φ, θ y el cuerpo sigue escrito en x, y, z.
 */
public record IntegralMultiple(Expresion cuerpo, List<Rango> rangos, Coordenadas coordenadas) implements Expresion {

    /** El sistema en que están dados los rangos. */
    public enum Coordenadas {
        CARTESIANAS("cartesianas"), POLARES("polares"), CILINDRICAS("cilíndricas"), ESFERICAS("esféricas");

        private final String nombre;

        Coordenadas(String nombre) {
            this.nombre = nombre;
        }

        /** "polares", "cilíndricas"… */
        public String nombre() {
            return nombre;
        }

        /** Lo que va después de "coord=" en la sintaxis de la sección 9. */
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

    /** En coordenadas cartesianas. */
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
