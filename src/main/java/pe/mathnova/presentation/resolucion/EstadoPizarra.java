package pe.mathnova.presentation.resolucion;

import java.util.Set;
import java.util.TreeSet;

/**
 * Qué se ve en la Pizarra, sin JavaFX: el modo (todos los pasos o paso a paso), el paso actual y qué
 * notas "por qué aplica" están abiertas. {@link PizarraResolucion} se dibuja con esto después de cada
 * cambio.
 *
 * <ul>
 *   <li>Las notas empiezan ocultas. Cada paso se abre con su botón "?" y se cierra con "Ocultar".</li>
 *   <li>El interruptor "Explicaciones" muestra u oculta todas a la vez; al cambiarlo se olvidan las
 *       que se abrieron una por una, y mientras está encendido no hay "Ocultar".</li>
 *   <li>Paso a paso empieza en el paso 1; los siguientes son solo un círculo gris hasta llegar a ellos,
 *       y el resultado aparece en el último.</li>
 * </ul>
 * Los pasos se numeran desde 1.
 */
public final class EstadoPizarra {

    public enum Modo { TODOS, PASO_A_PASO }

    private int total;
    private Modo modo = Modo.TODOS;
    private int actual = 1;
    private boolean explicaciones;
    private final Set<Integer> abiertas = new TreeSet<>();

    public EstadoPizarra(boolean explicaciones) {
        this.explicaciones = explicaciones;
    }

    /** Un resultado nuevo: todos los pasos a la vista y ninguna nota abierta a mano. */
    public void nuevo(int pasos) {
        total = Math.max(0, pasos);
        modo = Modo.TODOS;
        actual = 1;
        abiertas.clear();
    }

    // ------------------------------------------------------------------ modo

    public void pasoAPaso() {
        if (total == 0) {
            return;
        }
        modo = Modo.PASO_A_PASO;
        actual = 1;
    }

    public void verTodo() {
        modo = Modo.TODOS;
    }

    /** "Paso a paso" ↔ "Ver todo". */
    public void alternarModo() {
        if (modo == Modo.TODOS) {
            pasoAPaso();
        } else {
            verTodo();
        }
    }

    public void siguiente() {
        if (puedeSiguiente()) {
            actual++;
        }
    }

    public void anterior() {
        if (puedeAnterior()) {
            actual--;
        }
    }

    public boolean puedeSiguiente() {
        return modo == Modo.PASO_A_PASO && actual < total;
    }

    public boolean puedeAnterior() {
        return modo == Modo.PASO_A_PASO && actual > 1;
    }

    // ------------------------------------------------------------------ notas

    /** El interruptor "Explicaciones": se olvidan las notas abiertas una por una. */
    public void setExplicaciones(boolean si) {
        explicaciones = si;
        abiertas.clear();
    }

    /** El "?" del paso (o "Ocultar"). Con el interruptor encendido no hace nada: todas están a la vista. */
    public void alternarNota(int paso) {
        if (explicaciones) {
            return;
        }
        if (!abiertas.remove(paso)) {
            abiertas.add(paso);
        }
    }

    public boolean notaVisible(int paso) {
        return pasoVisible(paso) && (explicaciones || abiertas.contains(paso));
    }

    /** "Ocultar" va solo en una nota abierta a mano. */
    public boolean ocultarVisible(int paso) {
        return notaVisible(paso) && !explicaciones;
    }

    /** Se ve el "?" en lugar de la nota. */
    public boolean botonNotaVisible(int paso) {
        return pasoVisible(paso) && !notaVisible(paso);
    }

    // ------------------------------------------------------------------ lo que se dibuja

    /** Fórmula, flecha y nota a la vista; si no, solo el círculo gris. */
    public boolean pasoVisible(int paso) {
        return paso >= 1 && paso <= total && (modo == Modo.TODOS || paso <= actual);
    }

    /** El recuadro del resultado y la Comprobación. */
    public boolean resultadoVisible() {
        return modo == Modo.TODOS || actual == total;
    }

    /** "Todos los pasos" o "Paso 2 de 4". */
    public String etiqueta() {
        return modo == Modo.TODOS ? "Todos los pasos" : "Paso " + actual + " de " + total;
    }

    /** El texto del botón: lo que hace al pulsarlo. */
    public String textoModo() {
        return modo == Modo.TODOS ? "Paso a paso" : "Ver todo";
    }

    public Modo modo() {
        return modo;
    }

    public int actual() {
        return actual;
    }

    public int total() {
        return total;
    }

    public boolean explicaciones() {
        return explicaciones;
    }
}
