package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.LimiteDoble;

/**
 * Un límite doble resuelto (HU29, RF54).
 *
 * @param valor        el límite si existe; null si no
 * @param metodo       "L01" (evaluación directa), "V05" (trayectorias) o "V06" (polares); null si nada decidió
 * @param trayectorias las trayectorias probadas, en orden; vacío si bastó la evaluación directa
 * @param polar        la forma polar con su cota, o null si no se usó
 * @param motivo       por qué no existe o por qué no se pudo decidir; null si existe
 */
public record ResolucionLimiteDoble(LimiteDoble entrada, Estado estado, Expresion valor, String metodo,
                                    List<Trayectoria> trayectorias, Polar polar, List<PasoLimite> pasos,
                                    String motivo) {

    public enum Estado { EXISTE, NO_EXISTE, NO_DECIDIDO }

    /** Por dónde se acerca al punto, para la tabla de Análisis y para dibujarla en Gráfica. */
    public enum Camino {
        /** y = b. */
        HORIZONTAL,
        /** x = a. */
        VERTICAL,
        /** y − b = m(x − a). */
        RECTA,
        /** y − b = (x − a)ⁿ. */
        POTENCIA_EN_X,
        /** x − a = (y − b)ⁿ. */
        POTENCIA_EN_Y
    }

    /**
     * @param nombre    "y = 0", "y = mx", "y = x²"
     * @param camino    el tipo, con {@code n} para las potencias (1 en las demás)
     * @param parametro la letra de la pendiente en y = mx ('m'); 0 en las demás
     * @param valor     a lo que tiende f por ahí (puede depender de m); null si f crece sin cota
     * @param dependeDeM si el valor depende de la pendiente
     * @param detalle   la cuenta: "f(x, mx) = m·x²/(x² + m²x²) = m/(1 + m²)"
     */
    public record Trayectoria(String nombre, Camino camino, int n, char parametro, Expresion valor,
                              boolean dependeDeM, String detalle) {

        public Trayectoria {
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(camino, "camino");
        }

        /** "0", "m/(1 + m²)" o "no está acotada". */
        public String texto() {
            return valor == null ? "crece sin cota" : ImpresoraPizarra.imprimir(valor);
        }
    }

    /**
     * La forma polar alrededor del punto.
     *
     * @param forma la expresión en r y θ: r·cos²θ·sen θ
     * @param k     la potencia de r que queda (la menor, si hay varias)
     * @param g     la parte que depende solo de θ (la de r^k), o null si hay varias potencias
     * @param cota  "|r·cos²θ·sen θ| ≤ r·1 = r"; null si no hay cota que decida
     */
    public record Polar(Expresion forma, int k, Expresion g, String cota) {
    }

    public ResolucionLimiteDoble {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(estado, "estado");
        trayectorias = List.copyOf(trayectorias);
        pasos = List.copyOf(pasos);
    }

    public boolean existe() {
        return estado == Estado.EXISTE;
    }

    /** "0", "No existe" o "No se pudo decidir con los métodos disponibles". */
    public String texto() {
        return switch (estado) {
            case EXISTE -> ImpresoraPizarra.imprimir(valor);
            case NO_EXISTE -> "No existe";
            case NO_DECIDIDO -> MotorLimitesDobles.NO_DECIDIDO;
        };
    }

    public String latex() {
        String izq = ImpresoraLatex.imprimir(entrada);
        return switch (estado) {
            case EXISTE -> izq + " = " + ImpresoraLatex.imprimir(valor);
            case NO_EXISTE -> izq + "\\ \\text{no existe}";
            case NO_DECIDIDO -> izq;
        };
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
