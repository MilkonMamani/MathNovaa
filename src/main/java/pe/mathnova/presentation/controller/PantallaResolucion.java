package pe.mathnova.presentation.controller;

import java.util.Objects;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.parser.Parser;

/**
 * El estado de la pantalla "Resolver ejercicio", sin JavaFX. El controlador le avisa lo que pasa
 * (cambió el editor, pasaron los 400 ms, empezó o terminó el cálculo) y después dibuja todos los
 * paneles con lo que dice aquí; ninguno se actualiza por su cuenta.
 *
 * <p>Cada cambio del editor sube la {@link #version()}. Lo que se calculó para una versión anterior no
 * se muestra como actual: los paneles quedan atenuados con "Pulsa Resolver para actualizar" (RF77).
 *
 * <p>La línea de resultado en vivo bajo el editor (HU37, RF71) se retiró: el resultado depende del
 * método y del campo Q/R/C, y mostrarlo antes de Resolver adelantaba la respuesta. Solo aparece al
 * final de la Pizarra.
 */
public final class PantallaResolucion {

    /** La nota sobre los paneles atenuados. */
    public static final String PULSA_RESOLVER = "Pulsa Resolver para actualizar";

    /** Qué se puede hacer con lo que hay en el editor. */
    public enum Tipo { VACIO, INCOMPLETO, ERROR, LISTO }

    /**
     * Lo que se sabe de la entrada sin calcular nada.
     *
     * @param texto   el texto para el parser (sección 9); null si no se pudo traducir
     * @param plan    qué hará Resolver; null si no hay texto
     * @param mensaje el error o la casilla que falta; null si no hay
     * @param casilla el número de la casilla que falta (TraductorLatex.CasillaVacia); -1 si no falta
     */
    public record Analisis(Tipo tipo, String texto, ResolverService.Plan plan, String mensaje, int casilla) {

        static final Analisis VACIA = new Analisis(Tipo.VACIO, "", new ResolverService.Plan.Vacio(), null, -1);

        public boolean resoluble() {
            return tipo == Tipo.LISTO && plan != null && plan.resoluble();
        }
    }

    /** El LaTeX del editor: casilla vacía, error del traductor o lo que decida ResolverService. */
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

    /** Un texto de la sección 9 (del historial, de una sugerencia o traducido del editor). */
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
    /** Pasaron los 400 ms (o se pidió Resolver): el error ya se muestra. */
    private boolean confirmado;

    private boolean calculando;
    private int versionCalculo = -1;

    /** La versión del resultado que está en los paneles; -1 si no hay. */
    private int versionResultado = -1;
    private boolean resultadoVerificado;
    /** El cálculo falló ("No se pudo resolver: …"). */
    private String fallo;

    // ------------------------------------------------------------------ eventos

    /**
     * Cambió lo que hay en el editor. Si es lo mismo que ya había (el editor avisa dos veces, o el
     * historial puso el mismo ejercicio), no cambia nada.
     *
     * @return true si de verdad cambió
     */
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

    /** Lo mismo, pero aunque la entrada sea igual (cambió el campo Q/R/C): todo lo anterior queda viejo. */
    public void invalidar() {
        version++;
        confirmado = false;
    }

    /** Pasaron los 400 ms sin teclas: el error (si lo hay) ya se muestra. */
    public void confirmar(int deVersion) {
        if (deVersion == version) {
            confirmado = true;
        }
    }

    /** Empieza "Resolver": los paneles se vacían y se ve la carga. */
    public void empezarCalculo() {
        calculando = true;
        confirmado = true;
        versionCalculo = version;
        versionResultado = -1;
        fallo = null;
    }

    /**
     * Terminó el cálculo de {@code deVersion}: el resultado se pone en los paneles (atenuado si el texto
     * cambió mientras se calculaba).
     *
     * @param verificado si la etiqueta es "Verificado"
     */
    public void resultado(int deVersion, boolean verificado) {
        calculando = false;
        versionResultado = deVersion;
        resultadoVerificado = verificado;
        fallo = null;
    }

    /** El cálculo de {@code deVersion} falló. */
    public void fallo(int deVersion, String mensaje) {
        calculando = false;
        versionResultado = deVersion;
        resultadoVerificado = false;
        fallo = mensaje;
    }

    /** Se descartó un cálculo (llegó otro ejercicio mientras corría). */
    public void cancelarCalculo() {
        calculando = false;
    }

    // ------------------------------------------------------------------ lo que se dibuja

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

    /** Un cálculo sigue corriendo (aunque sea de una versión anterior). */
    public boolean calculando() {
        return calculando;
    }

    /** La versión que se está calculando. */
    public int versionCalculo() {
        return versionCalculo;
    }

    /** El mensaje rojo bajo el editor (ERROR o INCOMPLETO); null si no hay. El fallo del cálculo va en la tarjeta. */
    public String mensaje() {
        return switch (estado()) {
            case INCOMPLETO -> analisis.mensaje();
            case ERROR -> fallo != null && versionResultado == version ? null : analisis.mensaje();
            default -> null;
        };
    }

    /** El fallo del cálculo para la tarjeta Solución; null si no hay. */
    public String falloCalculo() {
        return versionResultado == version ? fallo : null;
    }

    /** La casilla que falta (para resaltarla); -1 si no falta ninguna o no se sabe cuál. */
    public int casilla() {
        return estado() == EstadoResolucion.INCOMPLETO ? analisis.casilla() : -1;
    }

    /** Resolver está habilitado. */
    public boolean botonActivo() {
        EstadoResolucion e = estado();
        return !calculando && analisis.resoluble()
                && (e == EstadoResolucion.ESCRIBIENDO || e == EstadoResolucion.RESUELTO);
    }

    /** Hay un resultado en los paneles (actual o viejo). */
    public boolean hayResultado() {
        return versionResultado >= 0;
    }

    /** Los paneles muestran un resultado que no es de lo que hay ahora en el editor. */
    public boolean atenuado() {
        return versionResultado >= 0 && versionResultado != version && !(calculando && versionCalculo == version);
    }

    /** Se ve "Verificado" (o la etiqueta del resultado): solo si el resultado es el actual. */
    public boolean etiquetaVisible() {
        return estado() == EstadoResolucion.RESUELTO;
    }

    /** "Verificado" con su marca: el resultado actual y comprobado. */
    public boolean verificado() {
        return etiquetaVisible() && resultadoVerificado;
    }

    // ------------------------------------------------------------------ apoyo

    /** La misma entrada: el mismo tipo y el mismo texto (o el mismo árbol, si los textos difieren en espacios). */
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
