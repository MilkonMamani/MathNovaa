package pe.mathnova.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import pe.mathnova.domain.calculo.limites.MotorLimites;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Irreducibilidad;
import pe.mathnova.domain.factorizacion.MetodoFactorizacion;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.infrastructure.persistence.IntentoPasoEntity;
import pe.mathnova.infrastructure.persistence.IntentoPasoRepository;
import pe.mathnova.infrastructure.persistence.SesionPracticaEntity;
import pe.mathnova.infrastructure.persistence.SesionPracticaRepository;
import pe.mathnova.infrastructure.persistence.TecnicaRepository;

/**
 * Modo Práctica (Sprint 9, parte D). El motor resuelve por dentro; el alumno ve el enunciado y en cada paso
 * elige el método entre opciones, sin escribir el álgebra. Si acierta, el paso aparece como en la Pizarra; si
 * no, recibe el motivo por el que ese método no aplica. Un paso cuenta como acierto solo si se acertó al primer
 * intento. Cada sesión queda en sesion_practica y cada elección en intento_paso.
 *
 * <p>Se practican las factorizaciones en Q (con la pregunta final "¿Terminaste?") y los límites de una
 * variable: en ellos el motor sabe decir por qué un método no aplica. Las opciones equivocadas salen primero de
 * los métodos descartados de ese mismo paso (HU05) y, si no alcanzan, de los métodos de la misma categoría que
 * no aplican, cada uno con su propio motivo. Un método que también funciona nunca se ofrece como error.
 */
@Service
public class PracticaService {

    public static final String QUE_METODO = "¿Qué método aplicas?";
    public static final String QUE_HACES = "¿Qué haces ahora?";
    public static final String TERMINASTE = "¿Terminaste?";
    public static final String SI_TERMINE = "Sí, todos los factores son irreducibles";
    public static final String NO_TERMINE = "No, todavía se puede factorizar";
    /** Códigos de las dos opciones de "¿Terminaste?" (no son técnicas del catálogo). */
    public static final String CODIGO_SI = "TERMINADO";
    public static final String CODIGO_NO = "SIGUE";
    static final String CORRECTO = "Correcto";
    static final String SOLO_ESTOS = "Por ahora se practican factorizaciones en Q y límites de una variable.";
    static final int MAXIMO_DISTRACTORES = 3;

    // ------------------------------------------------------------------ lo que ve la pantalla

    /** Una opción: un botón. */
    public record Opcion(String codigo, String texto) {
    }

    /**
     * La pregunta de un paso.
     *
     * @param numero     1, 2, 3…
     * @param latexSobre la expresión con la que se trabaja en este paso
     */
    public record Pregunta(int numero, String pregunta, String latexSobre, List<Opcion> opciones) {
    }

    /** Un paso resuelto, con el formato de la Pizarra (número, título, fórmula y nota). */
    public record PasoMostrado(String numero, int nivel, String titulo, String latex, String nota) {
    }

    /**
     * Una práctica empezada.
     *
     * @param titulo "Factorizar", "Calcular límite"
     */
    public record Practica(int sesionId, int resolucionId, String texto, Campo campo, String titulo,
                           String latexEnunciado, int totalPasos, String resultadoLatex, String resultadoTexto) {
    }

    /**
     * Cómo va.
     *
     * @param paso      la pregunta que toca (1…total); total + 1 al terminar
     * @param aciertos  pasos acertados al primer intento
     * @param fallidos  intentos equivocados
     */
    public record Estado(int paso, int total, int aciertos, int fallidos, boolean terminada) {
    }

    /**
     * Lo que pasó al elegir una opción.
     *
     * @param motivo por qué no aplica; null si acertó
     * @param pasos  los pasos que se muestran al acertar (con los anidados); vacío si falló o si fue "¿Terminaste?"
     */
    public record Respuesta(boolean correcta, String motivo, List<PasoMostrado> pasos, Estado estado) {
    }

    /** Una tarjeta de "Elige un ejercicio". */
    public record Propuesta(String texto, Campo campo, String tema, int pasos, String titulo, String latex) {
    }

    // ------------------------------------------------------------------ estado interno

    /** Una pregunta con sus respuestas: la correcta, los motivos de las otras y la pista. */
    private record PasoPractica(Pregunta pregunta, String correcta, Map<String, String> motivos, String pista,
                                List<PasoMostrado> revelados) {
    }

    private static final class Sesion {
        final Practica practica;
        final List<PasoPractica> pasos;
        int actual;
        int aciertos;
        int fallidos;
        /** Si la pregunta actual ya tuvo un error (entonces no cuenta como acierto). */
        boolean falloEnActual;

        Sesion(Practica practica, List<PasoPractica> pasos) {
            this.practica = practica;
            this.pasos = pasos;
        }

        Estado estado() {
            return new Estado(actual + 1, pasos.size(), aciertos, fallidos, actual >= pasos.size());
        }
    }

    private final ResolverService resolver;
    private final FactorizacionService factorizacion;
    private final HistorialService historial;
    private final OrquestadorFactorizacion motor;
    private final EjemplosService ejemplos;
    private final SesionPracticaRepository sesiones;
    private final IntentoPasoRepository intentos;
    private final TecnicaRepository tecnicas;
    private final MotorLimites limites;
    private final Map<Integer, Sesion> abiertas = new ConcurrentHashMap<>();
    private volatile List<Propuesta> propuestas;
    private Random azar = new Random();

    public PracticaService(ResolverService resolver, FactorizacionService factorizacion, HistorialService historial,
                           OrquestadorFactorizacion motor, EjemplosService ejemplos, SesionPracticaRepository sesiones,
                           IntentoPasoRepository intentos, TecnicaRepository tecnicas) {
        this.resolver = resolver;
        this.factorizacion = factorizacion;
        this.historial = historial;
        this.motor = motor;
        this.ejemplos = ejemplos;
        this.sesiones = sesiones;
        this.intentos = intentos;
        this.tecnicas = tecnicas;
        this.limites = new MotorLimites(motor);
    }

    // ------------------------------------------------------------------ qué se puede practicar

    /** Si este ejercicio se puede practicar: una factorización en Q o un límite. No resuelve nada. */
    public boolean sePuedePracticar(String texto, Campo campo) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        try {
            return switch (resolver.decidir(texto)) {
                case ResolverService.Plan.Factorizar f -> campo == null || campo == Campo.Q;
                case ResolverService.Plan.CalcularLimite l -> true;
                default -> false;
            };
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** "Elige un ejercicio": los ejemplos que se pueden practicar, con cuántos pasos tiene cada uno. */
    public List<Propuesta> propuestas() {
        List<Propuesta> p = propuestas;
        if (p != null) {
            return p;
        }
        List<Propuesta> r = new ArrayList<>();
        for (EjemplosService.Ejemplo e : ejemplos.todos()) {
            if (!sePuedePracticar(e.texto(), e.campo())) {
                continue;
            }
            try {
                int n = armar(e.texto(), resolver.resolver(e.texto(), Campo.Q, null)).size();
                if (n > 0) {
                    r.add(new Propuesta(e.texto(), e.campo(), e.tema(), n, e.enunciado(), e.latex()));
                }
            } catch (RuntimeException ex) {
                // un ejemplo que no se puede practicar no se propone
            }
        }
        propuestas = List.copyOf(r);
        return propuestas;
    }

    // ------------------------------------------------------------------ iniciar

    /**
     * Resuelve con el motor normal, guarda la resolución (aparece en el Historial) y abre la sesión con
     * aciertos = 0.
     *
     * @throws IllegalArgumentException si no se puede practicar o no tiene pasos
     */
    public Practica iniciar(String texto, Campo campo) {
        if (!sePuedePracticar(texto, campo)) {
            throw new IllegalArgumentException(SOLO_ESTOS);
        }
        Object r = resolver.resolver(texto, Campo.Q, null);
        List<PasoPractica> pasos = armar(texto, r);
        if (pasos.isEmpty()) {
            throw new IllegalArgumentException("Este ejercicio no tiene pasos que practicar: ya está resuelto.");
        }
        int resolucionId = resolver.guardar(texto, r, null, resolver.sugerencias(r))
                .orElseThrow(() -> new IllegalStateException("No se pudo guardar el ejercicio para practicarlo."));
        HistorialService.Hoja h = historial.hoja(resolucionId);
        pasos = conRevelados(pasos, h.pasos());

        SesionPracticaEntity s = new SesionPracticaEntity();
        s.setResolucionId(resolucionId);
        s.setIniciadaEn(Instant.now());
        s.setTotalPasos(pasos.size());
        s.setAciertos(0);
        s = sesiones.save(s);

        Practica p = new Practica(s.getId(), resolucionId, texto, Campo.Q, h.verbo(), h.latexEntrada(), pasos.size(),
                h.resultadoLatex(), h.resultadoTexto());
        abiertas.put(p.sesionId(), new Sesion(p, pasos));
        return p;
    }

    // ------------------------------------------------------------------ opciones, responder, pista

    /** La pregunta de un paso (1…total) con las opciones mezcladas. */
    public Pregunta opciones(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).pregunta();
    }

    /** La pregunta que toca; vacío si ya terminó. */
    public Optional<Pregunta> actual(int sesionId) {
        Sesion s = sesion(sesionId);
        return s.actual < s.pasos.size() ? Optional.of(s.pasos.get(s.actual).pregunta()) : Optional.empty();
    }

    public Estado estado(int sesionId) {
        return sesion(sesionId).estado();
    }

    public Practica practica(int sesionId) {
        return sesion(sesionId).practica;
    }

    /** Una frase que orienta sin decir el nombre del método. */
    public String pista(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).pista();
    }

    /**
     * Guarda el intento. Si acierta, el paso se muestra y se pasa al siguiente; si no, devuelve el motivo y el paso
     * ya no cuenta como acierto.
     *
     * @throws IllegalStateException si {@code paso} no es el que toca
     */
    public synchronized Respuesta responder(int sesionId, int paso, String codigo) {
        Sesion s = sesion(sesionId);
        if (paso != s.actual + 1) {
            throw new IllegalStateException("Ahora toca el paso " + (s.actual + 1) + ", no el " + paso);
        }
        PasoPractica p = s.pasos.get(s.actual);
        boolean correcta = p.correcta().equals(codigo);
        String motivo = correcta ? null : p.motivos().getOrDefault(codigo, "No es el paso que toca aquí.");
        String texto = p.pregunta().opciones().stream().filter(o -> o.codigo().equals(codigo)).map(Opcion::texto)
                .findFirst().orElse(codigo);

        IntentoPasoEntity i = new IntentoPasoEntity();
        i.setSesionId(sesionId);
        i.setOrdenPaso(paso);
        i.setTecnicaPropuesta(codigo != null && tecnicas.existsById(codigo) ? codigo : null);
        i.setExpresionPropuesta(texto);
        i.setEsCorrecto(correcta);
        i.setRetroalimentacion(correcta ? CORRECTO : motivo);
        i.setIntentadoEn(Instant.now());
        intentos.saveAndFlush(i);

        if (!correcta) {
            s.fallidos++;
            s.falloEnActual = true;
            return new Respuesta(false, motivo, List.of(), s.estado());
        }
        if (!s.falloEnActual) {
            s.aciertos++;
        }
        s.falloEnActual = false;
        s.actual++;
        SesionPracticaEntity e = sesiones.findById(sesionId).orElseThrow();
        e.setAciertos(s.aciertos);
        if (s.actual >= s.pasos.size()) {
            e.setTerminadaEn(Instant.now());
        }
        sesiones.saveAndFlush(e);
        return new Respuesta(true, null, p.revelados(), s.estado());
    }

    private Sesion sesion(int id) {
        Sesion s = abiertas.get(id);
        if (s == null) {
            throw new IllegalArgumentException("No hay una práctica abierta con el número " + id);
        }
        return s;
    }

    private static PasoPractica paso(Sesion s, int paso) {
        if (paso < 1 || paso > s.pasos.size()) {
            throw new IllegalArgumentException("La práctica tiene " + s.pasos.size() + " pasos, no " + paso);
        }
        return s.pasos.get(paso - 1);
    }

    // ------------------------------------------------------------------ armar las preguntas

    private List<PasoPractica> armar(String texto, Object r) {
        return switch (r) {
            case Resolucion f -> deFactorizacion(f);
            case ResolucionLimite l -> deLimite((Limite) Parser.parse(texto), l);
            default -> throw new IllegalArgumentException(SOLO_ESTOS);
        };
    }

    /** Un paso por método aplicado y, al final, "¿Terminaste?". */
    private List<PasoPractica> deFactorizacion(Resolucion r) {
        List<PasoPractica> pasos = new ArrayList<>();
        for (Paso p : r.pasos()) {
            String correcta = p.codigoTecnica();
            Map<String, String> descartados = new LinkedHashMap<>();
            // 1) Los descartados de este mismo paso (HU05), con su motivo
            for (Paso d : r.descartados()) {
                if (d.antes().equals(p.antes()) && !d.codigoTecnica().equals(correcta)
                        && !mismoMetodo(d.codigoTecnica(), correcta)) {
                    descartados.putIfAbsent(d.codigoTecnica(), d.motivoDescarte());
                }
            }
            // 2) Si no alcanzan, los de la misma categoría que no aplican, con el motivo que da cada uno
            Map<String, String> otros = new LinkedHashMap<>();
            if (descartados.size() < MAXIMO_DISTRACTORES) {
                motor.noAplican(p.antes()).forEach((codigo, motivo) -> {
                    if (!codigo.equals(correcta) && !mismoMetodo(codigo, correcta)
                            && !descartados.containsKey(codigo)) {
                        otros.put(codigo, motivo);
                    }
                });
            }
            Map<String, String> elegidos = elegir(descartados, otros);
            String pista = motor.metodo(correcta).map(MetodoFactorizacion::pista).orElse(Tecnicas.pista(correcta));
            pasos.add(new PasoPractica(new Pregunta(pasos.size() + 1, QUE_METODO,
                    ImpresoraLatex.imprimir(p.antes().aExpresion()), mezclar(correcta, elegidos.keySet())),
                    correcta, elegidos, pista == null ? "Mira la forma del polinomio: cuántos términos tiene y de qué "
                    + "grado es." : pista, List.of()));
        }
        if (!pasos.isEmpty()) {
            List<Opcion> opciones = new ArrayList<>(List.of(new Opcion(CODIGO_SI, SI_TERMINE),
                    new Opcion(CODIGO_NO, NO_TERMINE)));
            Collections.shuffle(opciones, azar);
            pasos.add(new PasoPractica(new Pregunta(pasos.size() + 1, TERMINASTE,
                    ImpresoraLatex.imprimir(r.resultado().aExpresion()), List.copyOf(opciones)), CODIGO_SI,
                    Map.of(CODIGO_NO, motivoIrreducibles(r)), "Revisa cada factor que queda: ¿alguno se podría partir "
                    + "otra vez con lo que conoces?", List.of()));
        }
        return pasos;
    }

    /** F16 y F17 son Ruffini: no se ofrece uno como error del otro. */
    private static boolean mismoMetodo(String a, String b) {
        return a.startsWith("F16") && b.startsWith("F17") || a.startsWith("F17") && b.startsWith("F16");
    }

    /** "Todos los factores que quedan son de grado 1…" o las razones, sin repetir. */
    static String motivoIrreducibles(Resolucion r) {
        List<String> razones = r.irreducibles().stream().map(Irreducibilidad::justificacion)
                .map(t -> t.endsWith(".") ? t.substring(0, t.length() - 1) : t).distinct().toList();
        if (razones.isEmpty()) {
            return "Cada factor que queda ya no se puede partir en Q.";
        }
        if (razones.size() == 1 && razones.getFirst().equals("Es de grado 1")) {
            return "Todos los factores que quedan son de grado 1: ya no se pueden partir más.";
        }
        return "Cada factor que queda ya es irreducible en Q: " + String.join("; ", razones.subList(0,
                Math.min(3, razones.size()))) + ".";
    }

    /** Un paso por cada paso de arriba del límite (los anidados se muestran con su paso). */
    private List<PasoPractica> deLimite(Limite limite, ResolucionLimite r) {
        List<PasoPractica> pasos = new ArrayList<>();
        List<PasoLimite> todos = r.pasos();
        for (int i = 0; i < todos.size(); i++) {
            PasoLimite p = todos.get(i);
            String correcta = p.codigo();
            Expresion f = p.antes() == null ? limite.cuerpo() : p.antes();
            Map<String, String> motivos = new LinkedHashMap<>();
            for (String codigo : List.of("L01", "L02", "L03", "L04", "L05", "L06", "L07")) {
                if (codigo.equals(correcta)) {
                    continue;
                }
                limites.noAplica(limite, f, codigo, i == 0).ifPresent(m -> motivos.put(codigo, m));
            }
            Map<String, String> elegidos = elegir(motivos, Map.of());
            String latex = ImpresoraLatex.imprimir(new Limite(limite.variable(), limite.punto(), limite.lado(), f));
            String pista = Tecnicas.pista(correcta);
            pasos.add(new PasoPractica(new Pregunta(pasos.size() + 1, QUE_HACES, latex,
                    mezclar(correcta, elegidos.keySet())), correcta, elegidos,
                    pista == null ? "Piensa qué sale si reemplazas la variable por el punto." : pista, List.of()));
        }
        return pasos;
    }

    /**
     * Hasta tres distractores: primero los de {@code primero} (al azar entre ellos) y, si no alcanzan, se completa
     * con {@code despues}.
     */
    private Map<String, String> elegir(Map<String, String> primero, Map<String, String> despues) {
        Map<String, String> r = new LinkedHashMap<>();
        for (Map<String, String> grupo : List.of(primero, despues)) {
            List<String> codigos = new ArrayList<>(grupo.keySet());
            Collections.shuffle(codigos, azar);
            for (String c : codigos) {
                if (r.size() < MAXIMO_DISTRACTORES) {
                    r.put(c, grupo.get(c));
                }
            }
        }
        return r;
    }

    /** La correcta y los distractores, en orden mezclado. */
    private List<Opcion> mezclar(String correcta, java.util.Collection<String> distractores) {
        List<Opcion> r = new ArrayList<>();
        r.add(new Opcion(correcta, Tecnicas.nombre(correcta)));
        distractores.forEach(c -> r.add(new Opcion(c, Tecnicas.nombre(c))));
        Collections.shuffle(r, azar);
        return List.copyOf(r);
    }

    /**
     * Junta a cada pregunta los pasos de la hoja guardada que muestra al acertar: los de arriba en orden, cada uno
     * con sus anidados ("2", "2.1"). "¿Terminaste?" no muestra pasos (al acertarla se ve el resultado).
     */
    private static List<PasoPractica> conRevelados(List<PasoPractica> pasos, List<HistorialService.PasoHoja> hoja) {
        List<List<PasoMostrado>> grupos = new ArrayList<>();
        for (HistorialService.PasoHoja h : hoja) {
            PasoMostrado m = new PasoMostrado(h.numero(), h.nivel(), h.titulo(), h.latex(), h.nota());
            if (h.nivel() == 0 || grupos.isEmpty()) {
                grupos.add(new ArrayList<>(List.of(m)));
            } else {
                grupos.getLast().add(m);
            }
        }
        List<PasoPractica> r = new ArrayList<>();
        int g = 0;
        for (PasoPractica p : pasos) {
            boolean fin = p.pregunta().pregunta().equals(TERMINASTE);
            List<PasoMostrado> revelados = !fin && g < grupos.size() ? List.copyOf(grupos.get(g++)) : List.of();
            r.add(new PasoPractica(p.pregunta(), p.correcta(), p.motivos(), p.pista(), revelados));
        }
        return r;
    }

    // ------------------------------------------------------------------ pruebas

    /** Para las pruebas: el orden de las opciones deja de ser al azar. */
    void setAzar(Random azar) {
        this.azar = azar;
    }

    /** Para las pruebas: el código correcto de un paso. */
    String correcta(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).correcta();
    }

    /** Para las pruebas: los motivos de los distractores de un paso. */
    Map<String, String> motivos(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).motivos();
    }
}
