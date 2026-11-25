package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.LimiteDoble;

public record ResolucionLimiteDoble(LimiteDoble entrada, Estado estado, Expresion valor, String metodo,
                                    List<Trayectoria> trayectorias, Polar polar, List<PasoLimite> pasos,
                                    String motivo) {

    public enum Estado { EXISTE, NO_EXISTE, NO_DECIDIDO }

    public enum Camino {
        HORIZONTAL,
        VERTICAL,
        RECTA,
        POTENCIA_EN_X,
        POTENCIA_EN_Y
    }

    public record Trayectoria(String nombre, Camino camino, int n, char parametro, Expresion valor,
                              boolean dependeDeM, String detalle) {

        public Trayectoria {
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(camino, "camino");
        }

        public String texto() {
            return valor == null ? "crece sin cota" : ImpresoraPizarra.imprimir(valor);
        }
    }

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
