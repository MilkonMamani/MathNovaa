package pe.mathnova.presentation.pizarra;

import java.util.List;

import javafx.scene.Node;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.resolucion.Igualdades;

public final class Pizarra {

    private Pizarra() {
    }

    public static Node de(TrabajoIntermedio trabajo) {
        return switch (trabajo) {
            case TrabajoIntermedio.TablaRuffini t -> new TablaRuffini(t);
            case TrabajoIntermedio.AspaSimple a -> new AspaSimple(a);
            case TrabajoIntermedio.AspaDobleEspecial a -> new AspaDobleEspecial(a);
            case TrabajoIntermedio.SistemaCoeficientes s -> new SistemaCoeficientes(s);
            case TrabajoIntermedio.Sustitucion s -> new Sustitucion(s);
            case TrabajoIntermedio.Agrupacion a -> renglones(a.grupos());
            case TrabajoIntermedio.Comparacion c -> renglones(List.of(c.detalle()));
            case TrabajoIntermedio.TablaPartes p -> new TablaPartes(p);
            case TrabajoIntermedio.TrianguloSustitucion t -> new TrianguloSustitucion(t);
            case TrabajoIntermedio.AspaDoble a -> new AspaDoble(a);
            case TrabajoIntermedio.ArbolCadena a -> new ArbolCadena(a);
        };
    }

    public static List<String> lineas(TrabajoIntermedio trabajo) {
        return switch (trabajo) {
            case TrabajoIntermedio.Agrupacion a -> a.grupos();
            case TrabajoIntermedio.Comparacion c -> List.of(c.detalle());
            case null, default -> null;
        };
    }

    static Node renglones(List<String> lineas) {
        Igualdades caja = Igualdades.deTextos(lineas, TAMANO, TAMANO_MINIMO);
        caja.getStyleClass().addAll("trabajo", "pizarra");
        return caja;
    }

    public static final double TAMANO = 18;
    public static final double TAMANO_MINIMO = 14;
}
