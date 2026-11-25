package pe.mathnova.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.domain.calculo.derivadas.AnalisisDerivada;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.infrastructure.persistence.CursoEntity;
import pe.mathnova.infrastructure.persistence.CursoRepository;
import pe.mathnova.infrastructure.persistence.EjemploEntity;
import pe.mathnova.infrastructure.persistence.EjemploRepository;
import pe.mathnova.infrastructure.persistence.TemaEntity;
import pe.mathnova.infrastructure.persistence.TemaRepository;

/**
 * Ejemplos (Sprint 9, parte B): ejercicios preparados por curso y tema, que se abren en Resolver o en
 * Práctica. Los llena la migración V4. RN20: cada ejemplo tiene que resolverse; {@link #resultado} da la
 * línea de resultado del motor y EjemplosTest la compara con el resultado esperado de cada fila.
 */
@Service
public class EjemplosService {

    private static final Pattern CAMPO = Pattern.compile("\"campo\"\\s*:\\s*\"([QRC])\"");

    /** Un tema de la lista de la izquierda, con cuántos ejemplos tiene. */
    public record Tema(int id, String nombre, int cantidad) {
    }

    /** Una pestaña: el curso y sus temas en orden. */
    public record Curso(String nombre, List<Tema> temas) {
    }

    /**
     * Una tarjeta.
     *
     * @param texto     lo que se pone en el editor (sección 9)
     * @param latex     la fórmula que dibuja la tarjeta
     * @param operacion el código de la tabla operacion (FACTORIZAR, LIMITE…)
     * @param campo     Q, R o C para una factorización; null si no aplica
     */
    public record Ejemplo(int id, String tema, String enunciado, String texto, String latex, String nota,
                          String operacion, Campo campo, int dificultad, String esperado) {
    }

    private final EjemploRepository ejemplos;
    private final CursoRepository cursos;
    private final TemaRepository temas;
    private final ResolverService resolver;

    public EjemplosService(EjemploRepository ejemplos, CursoRepository cursos, TemaRepository temas,
                           ResolverService resolver) {
        this.ejemplos = ejemplos;
        this.cursos = cursos;
        this.temas = temas;
        this.resolver = resolver;
    }

    /** Las pestañas, en el orden de los cursos, cada una con sus temas en orden. */
    @Transactional(readOnly = true)
    public List<Curso> cursos() {
        Map<Integer, Integer> cantidad = new HashMap<>();
        for (Object[] fila : ejemplos.cantidadPorTema()) {
            cantidad.put(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue());
        }
        Map<Integer, List<TemaEntity>> porCurso = temas.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getCurso().getId()));
        List<Curso> r = new ArrayList<>();
        for (CursoEntity c : cursos.findAllByOrderByOrdenAsc()) {
            List<Tema> deCurso = porCurso.getOrDefault(c.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(TemaEntity::getOrden))
                    .map(t -> new Tema(t.getId(), t.getNombre(), cantidad.getOrDefault(t.getId(), 0)))
                    .toList();
            r.add(new Curso(c.getNombre(), deCurso));
        }
        return r;
    }

    /** Las tarjetas de un tema, en orden. */
    @Transactional(readOnly = true)
    public List<Ejemplo> ejemplos(int temaId) {
        return ejemplos.deTema(temaId).stream().map(EjemplosService::aEjemplo).toList();
    }

    /** Todos los ejemplos, por curso, tema y orden. */
    @Transactional(readOnly = true)
    public List<Ejemplo> todos() {
        return ejemplos.todos().stream().map(EjemplosService::aEjemplo).toList();
    }

    /**
     * RN20: el resultado que da el motor para el ejemplo, en una línea ("= (x + 1)(x - 1)(x - 2)", "No existe",
     * "(1, 1) mínimo local"). Para los puntos críticos, cada punto con su tipo. Vacío si el motor no lo resuelve.
     */
    public Optional<String> resultado(Ejemplo e) {
        if ("PUNTOS_CRITICOS".equals(e.operacion())) {
            try {
                ResolucionDerivada r = resolver.resolverDerivada(e.texto(), null, f -> { });
                AnalisisDerivada a = r.analisis();
                if (a == null || a.criticos().isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(a.criticos().stream().map(EjemplosService::critico).collect(Collectors.joining("; ")));
            } catch (RuntimeException ex) {
                return Optional.empty();
            }
        }
        return resolver.enVivo(e.texto(), e.campo() == null ? Campo.Q : e.campo());
    }

    /** "(-1, 10) máximo local". */
    private static String critico(AnalisisDerivada.PuntoCritico c) {
        String tipo = c.tipo() == AnalisisDerivada.Tipo.NINGUNO ? c.tipo().texto() : c.tipo().texto() + " local";
        return "(" + c.x() + ", " + c.y() + ") " + tipo;
    }

    private static Ejemplo aEjemplo(EjemploEntity e) {
        return new Ejemplo(e.getId(), e.getTema().getNombre(), e.getEnunciado(), e.getExpresion(),
                latex(e.getExpresion(), e.getOperacionCodigo()), e.getNota() == null ? "" : e.getNota(),
                e.getOperacionCodigo(), campo(e.getParametrosJson()), e.getDificultad(), e.getResultadoEsperado());
    }

    /** {"campo":"R"} → R; sin campo, null. */
    static Campo campo(String json) {
        if (json == null) {
            return null;
        }
        Matcher m = CAMPO.matcher(json);
        return m.find() ? Campo.valueOf(m.group(1)) : null;
    }

    /**
     * La fórmula de la tarjeta. Para los puntos críticos se dibuja la función, f(x) = …, no su derivada: la
     * derivada es el primer paso de la solución.
     */
    static String latex(String texto, String operacion) {
        if ("PUNTOS_CRITICOS".equals(operacion)) {
            try {
                if (Parser.parse(texto) instanceof Derivada d) {
                    return "f(" + d.variable() + ") = " + ImpresoraLatex.imprimir(d.cuerpo());
                }
            } catch (RuntimeException e) {
                // sigue con el texto completo
            }
        }
        return HistorialService.latexDe(texto);
    }
}
