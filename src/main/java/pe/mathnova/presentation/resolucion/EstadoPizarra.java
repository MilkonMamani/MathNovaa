package pe.mathnova.presentation.resolucion;

import java.util.Set;
import java.util.TreeSet;

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

    public void nuevo(int pasos) {
        total = Math.max(0, pasos);
        modo = Modo.TODOS;
        actual = 1;
        abiertas.clear();
    }

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

    public void setExplicaciones(boolean si) {
        explicaciones = si;
        abiertas.clear();
    }

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

    public boolean ocultarVisible(int paso) {
        return notaVisible(paso) && !explicaciones;
    }

    public boolean botonNotaVisible(int paso) {
        return pasoVisible(paso) && !notaVisible(paso);
    }

    public boolean pasoVisible(int paso) {
        return paso >= 1 && paso <= total && (modo == Modo.TODOS || paso <= actual);
    }

    public boolean resultadoVisible() {
        return modo == Modo.TODOS || actual == total;
    }

    public String etiqueta() {
        return modo == Modo.TODOS ? "Todos los pasos" : "Paso " + actual + " de " + total;
    }

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
