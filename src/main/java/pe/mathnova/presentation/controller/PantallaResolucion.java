package pe.mathnova.presentation.controller;

import java.util.Objects;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.parser.Parser;

public final class PantallaResolucion {

    public static final String PULSA_RESOLVER = "Pulsa Resolver para actualizar";

    public enum Tipo { VACIO, INCOMPLETO, ERROR, LISTO }

    public record Analisis(Tipo tipo, String texto, ResolverService.Plan plan, String mensaje, int casilla) {

        static final Analisis VACIA = new Analisis(Tipo.VACIO, "", new ResolverService.Plan.Vacio(), null, -1);

        public boolean resoluble() {
            return tipo == Tipo.LISTO && plan != null && plan.resoluble();
        }
    }

    public static Analisis analizar(String latex, ResolverService servicio) {
        if (latex == null || latex.isBlank()) {
            return Analisis.VACIA;
        }
        var falta = TraductorLatex.casillaVacia(latex);
        if (falta.isPresent()) {
            return new Analisis(Tipo.INCOMPLETO, null, null, falta.get().mensaje(), falta.get().indice());
        }
        String texto;
        try {
            texto = TraductorLatex.traducir(latex);
        } catch (TraductorLatex.LatexNoValido e) {
            return new Analisis(Tipo.ERROR, null, null, e.getMessage(), -1);
        }
        return analizarTexto(texto, servicio);
    }

    public static Analisis analizarTexto(String texto, ResolverService servicio) {
        if (texto == null || texto.isBlank()) {
            return Analisis.VACIA;
        }
        ResolverService.Plan plan = servicio.decidir(texto);
        return switch (plan) {
            case ResolverService.Plan.Vacio v -> Analisis.VACIA;
            case ResolverService.Plan.Invalido inv -> texto.indexOf(TraductorLatex.CASILLA) >= 0
                    ? new Analisis(Tipo.INCOMPLETO, texto, plan, TraductorLatex.CASILLA_RESALTADA, -1)
                    : new Analisis(Tipo.ERROR, texto, plan, inv.mensaje(), -1);
            case ResolverService.Plan.Operacion op -> new Analisis(Tipo.ERROR, texto, plan, op.mensaje(), -1);
            case ResolverService.Plan.NoSoportado no -> new Analisis(Tipo.ERROR, texto, plan, no.mensaje(), -1);
            default -> new Analisis(Tipo.LISTO, texto, plan, null, -1);
        };
    }

    private Analisis analisis = Analisis.VACIA;
    private int version;
    private boolean confirmado;

    private boolean calculando;
    private int versionCalculo = -1;

    private int versionResultado = -1;
    private boolean resultadoVerificado;
    private String fallo;

    public boolean cambiar(Analisis nuevo) {
        Objects.requireNonNull(nuevo, "nuevo");
        if (mismaEntrada(analisis, nuevo)) {
            analisis = nuevo;
            return false;
        }
        analisis = nuevo;
        version++;
        confirmado = false;
        return true;
    }

    public void invalidar() {
        version++;
        confirmado = false;
    }

    public void confirmar(int deVersion) {
        if (deVersion == version) {
            confirmado = true;
        }
    }

    public void empezarCalculo() {
        calculando = true;
        confirmado = true;
        versionCalculo = version;
        versionResultado = -1;
        fallo = null;
    }

    public void resultado(int deVersion, boolean verificado) {
        calculando = false;
        versionResultado = deVersion;
        resultadoVerificado = verificado;
        fallo = null;
    }

    public void fallo(int deVersion, String mensaje) {
        calculando = false;
        versionResultado = deVersion;
        resultadoVerificado = false;
        fallo = mensaje;
    }

    public void cancelarCalculo() {
        calculando = false;
    }

    public EstadoResolucion estado() {
        if (calculando && versionCalculo == version) {
            return EstadoResolucion.CALCULANDO;
        }
        if (versionResultado == version && versionResultado >= 0) {
            return fallo == null ? EstadoResolucion.RESUELTO : EstadoResolucion.ERROR;
        }
        return switch (analisis.tipo()) {
            case VACIO -> EstadoResolucion.VACIO;
            case INCOMPLETO -> EstadoResolucion.INCOMPLETO;
            case ERROR -> confirmado ? EstadoResolucion.ERROR : EstadoResolucion.ESCRIBIENDO;
            case LISTO -> EstadoResolucion.ESCRIBIENDO;
        };
    }

    public int version() {
        return version;
    }

    public Analisis analisis() {
        return analisis;
    }

    public boolean calculando() {
        return calculando;
    }

    public int versionCalculo() {
        return versionCalculo;
    }

    public String mensaje() {
        return switch (estado()) {
            case INCOMPLETO -> analisis.mensaje();
            case ERROR -> fallo != null && versionResultado == version ? null : analisis.mensaje();
            default -> null;
        };
    }

    public String falloCalculo() {
        return versionResultado == version ? fallo : null;
    }

    public int casilla() {
        return estado() == EstadoResolucion.INCOMPLETO ? analisis.casilla() : -1;
    }

    public boolean botonActivo() {
        EstadoResolucion e = estado();
        return !calculando && analisis.resoluble()
                && (e == EstadoResolucion.ESCRIBIENDO || e == EstadoResolucion.RESUELTO);
    }

    public boolean hayResultado() {
        return versionResultado >= 0;
    }

    public boolean atenuado() {
        return versionResultado >= 0 && versionResultado != version && !(calculando && versionCalculo == version);
    }

    public boolean etiquetaVisible() {
        return estado() == EstadoResolucion.RESUELTO;
    }

    public boolean verificado() {
        return etiquetaVisible() && resultadoVerificado;
    }

    private static boolean mismaEntrada(Analisis a, Analisis b) {
        if (a.tipo() != b.tipo()) {
            return false;
        }
        if (a.tipo() == Tipo.VACIO) {
            return true;
        }
        if (a.texto() == null || b.texto() == null) {
            return false;
        }
        if (a.texto().equals(b.texto())) {
            return true;
        }
        try {
            return Parser.parse(a.texto()).equals(Parser.parse(b.texto()));
        } catch (RuntimeException e) {
            return false;
        }
    }
}
