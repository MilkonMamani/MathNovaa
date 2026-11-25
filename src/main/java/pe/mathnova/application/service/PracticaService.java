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

@Service
public class PracticaService {

    public static final String QUE_METODO = "¿Qué método aplicas?";
    public static final String QUE_HACES = "¿Qué haces ahora?";
    public static final String TERMINASTE = "¿Terminaste?";
    public static final String SI_TERMINE = "Sí, todos los factores son irreducibles";
    public static final String NO_TERMINE = "No, todavía se puede factorizar";
    public static final String CODIGO_SI = "TERMINADO";
    public static final String CODIGO_NO = "SIGUE";
    static final String CORRECTO = "Correcto";
    static final String SOLO_ESTOS = "Por ahora se practican factorizaciones en Q y límites de una variable.";
    static final int MAXIMO_DISTRACTORES = 3;

    public record Opcion(String codigo, String texto) {
    }

    public record Pregunta(int numero, String pregunta, String latexSobre, List<Opcion> opciones) {
    }

    public record PasoMostrado(String numero, int nivel, String titulo, String latex, String nota) {
    }

    public record Practica(int sesionId, int resolucionId, String texto, Campo campo, String titulo,
                           String latexEnunciado, int totalPasos, String resultadoLatex, String resultadoTexto) {
    }

    public record Estado(int paso, int total, int aciertos, int fallidos, boolean terminada) {
    }

    public record Respuesta(boolean correcta, String motivo, List<PasoMostrado> pasos, Estado estado) {
    }

    public record Propuesta(String texto, Campo campo, String tema, int pasos, String titulo, String latex) {
    }

    private record PasoPractica(Pregunta pregunta, String correcta, Map<String, String> motivos, String pista,
                                List<PasoMostrado> revelados) {
    }

    private static final class Sesion {
        final Practica practica;
        final List<PasoPractica> pasos;
        int actual;
        int aciertos;
        int fallidos;
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
            }
        }
        propuestas = List.copyOf(r);
        return propuestas;
    }

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

    public Pregunta opciones(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).pregunta();
    }

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

    public String pista(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).pista();
    }

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

    private List<PasoPractica> armar(String texto, Object r) {
        return switch (r) {
            case Resolucion f -> deFactorizacion(f);
            case ResolucionLimite l -> deLimite((Limite) Parser.parse(texto), l);
            default -> throw new IllegalArgumentException(SOLO_ESTOS);
        };
    }

    private List<PasoPractica> deFactorizacion(Resolucion r) {
        List<PasoPractica> pasos = new ArrayList<>();
        for (Paso p : r.pasos()) {
            String correcta = p.codigoTecnica();
            Map<String, String> descartados = new LinkedHashMap<>();
            for (Paso d : r.descartados()) {
                if (d.antes().equals(p.antes()) && !d.codigoTecnica().equals(correcta)
                        && !mismoMetodo(d.codigoTecnica(), correcta)) {
                    descartados.putIfAbsent(d.codigoTecnica(), d.motivoDescarte());
                }
            }
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

    private static boolean mismoMetodo(String a, String b) {
        return a.startsWith("F16") && b.startsWith("F17") || a.startsWith("F17") && b.startsWith("F16");
    }

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

    private List<Opcion> mezclar(String correcta, java.util.Collection<String> distractores) {
        List<Opcion> r = new ArrayList<>();
        r.add(new Opcion(correcta, Tecnicas.nombre(correcta)));
        distractores.forEach(c -> r.add(new Opcion(c, Tecnicas.nombre(c))));
        Collections.shuffle(r, azar);
        return List.copyOf(r);
    }

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

    void setAzar(Random azar) {
        this.azar = azar;
    }

    String correcta(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).correcta();
    }

    Map<String, String> motivos(int sesionId, int paso) {
        return paso(sesion(sesionId), paso).motivos();
    }
}
