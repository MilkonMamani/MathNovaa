package pe.mathnova.application.service;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble;
import pe.mathnova.domain.calculo.multiples.ResolucionIntegralMultiple;
import pe.mathnova.domain.calculo.parciales.ResolucionParcial;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.infrastructure.persistence.CursoEntity;
import pe.mathnova.infrastructure.persistence.CursoRepository;
import pe.mathnova.infrastructure.persistence.EjercicioEntity;
import pe.mathnova.infrastructure.persistence.OperacionEntity;
import pe.mathnova.infrastructure.persistence.OperacionRepository;
import pe.mathnova.infrastructure.persistence.PasoEntity;
import pe.mathnova.infrastructure.persistence.PasoMapper;
import pe.mathnova.infrastructure.persistence.ResolucionEntity;
import pe.mathnova.infrastructure.persistence.ResolucionRepository;
import pe.mathnova.infrastructure.persistence.RestriccionDominioEntity;
import pe.mathnova.infrastructure.persistence.TecnicaEntity;
import pe.mathnova.infrastructure.persistence.TecnicaRepository;
import pe.mathnova.infrastructure.persistence.TemaEntity;
import pe.mathnova.infrastructure.persistence.TemaRepository;

@Service
public class HistorialService {

    private static final Logger LOG = LoggerFactory.getLogger(HistorialService.class);

    public static final int TAMANO_PAGINA = 50;
    public static final String TODOS_LOS_CURSOS = "Todos";
    static final String SIN_DATO = "—";

    static final Map<String, String> VERBOS = Map.ofEntries(
            Map.entry("FACTORIZAR", "Factorizar"),
            Map.entry("SIMPLIFICAR", "Simplificar"),
            Map.entry("RESOLVER_ECUACION", "Resolver ecuación"),
            Map.entry("LIMITE", "Calcular límite"),
            Map.entry("ASINTOTAS", "Hallar asíntotas"),
            Map.entry("DERIVAR", "Derivar"),
            Map.entry("PUNTOS_CRITICOS", "Puntos críticos"),
            Map.entry("INTEGRAR", "Integrar"),
            Map.entry("INTEGRAL_DEFINIDA", "Integral definida"),
            Map.entry("LIMITE_DOBLE", "Límite doble"),
            Map.entry("DERIVADA_PARCIAL", "Derivada parcial"),
            Map.entry("GRADIENTE", "Gradiente"),
            Map.entry("DERIVADA_DIRECCIONAL", "Derivada direccional"),
            Map.entry("EXTREMOS", "Extremos"),
            Map.entry("LAGRANGE", "Lagrange"),
            Map.entry("INTEGRAL_MULTIPLE", "Integral múltiple"));

    private static final String[] MESES = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct",
            "nov", "dic"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    public enum Periodo {
        CUALQUIERA("Cualquier fecha"), HOY("Hoy"), SEMANA("Esta semana"), MES("Este mes");

        private final String texto;

        Periodo(String texto) {
            this.texto = texto;
        }

        public String texto() {
            return texto;
        }

        Instant desde(ZonedDateTime ahora) {
            LocalDate hoy = ahora.toLocalDate();
            return switch (this) {
                case CUALQUIERA -> null;
                case HOY -> hoy.atStartOfDay(ahora.getZone()).toInstant();
                case SEMANA -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .atStartOfDay(ahora.getZone()).toInstant();
                case MES -> hoy.withDayOfMonth(1).atStartOfDay(ahora.getZone()).toInstant();
            };
        }
    }

    public record Filtro(String texto, String curso, Periodo periodo, boolean soloFavoritos) {

        public static Filtro todo() {
            return new Filtro("", null, Periodo.CUALQUIERA, false);
        }
    }

    public record Fila(int id, int ejercicioId, String latex, String texto, String operacion, String tema,
                       String curso, String campo, Instant fecha, boolean favorito) {
    }

    public record Pagina(List<Fila> filas, long total, int numero, boolean hayMas) {
    }

    public record PasoHoja(String numero, int nivel, String codigo, String titulo, String latex, String nota) {
    }

    public record Descartado(String codigo, String titulo, String latex, String motivo) {
    }

    public record Hoja(int id, String verbo, String latexEntrada, String entradaTexto, String campo,
                       String metodoForzado, List<PasoHoja> pasos, List<Descartado> descartados,
                       List<String> restricciones, String resultadoLatex, String resultadoTexto, String versionMotor,
                       boolean recalculada) {
    }

    public record Guardado(String texto, Object resolucion, String forzado, List<Sugerencia> sugerencias) {

        public Guardado {
            sugerencias = sugerencias == null ? List.of() : List.copyOf(sugerencias);
        }
    }

    private final HistorialFactorizacionService base;
    private final ResolucionRepository resoluciones;
    private final TemaRepository temas;
    private final CursoRepository cursos;
    private final OperacionRepository operaciones;
    private final TecnicaRepository tecnicas;
    private final ObjectProvider<ResolverService> resolver;
    private final TransactionTemplate lectura;
    private final ZoneId zona = ZoneId.systemDefault();
    private volatile Map<String, String> nombresTecnicas;

    public HistorialService(HistorialFactorizacionService base, ResolucionRepository resoluciones, TemaRepository temas,
                            CursoRepository cursos, OperacionRepository operaciones, TecnicaRepository tecnicas,
                            ObjectProvider<ResolverService> resolver, PlatformTransactionManager transacciones) {
        this.base = base;
        this.resoluciones = resoluciones;
        this.temas = temas;
        this.cursos = cursos;
        this.operaciones = operaciones;
        this.tecnicas = tecnicas;
        this.resolver = resolver;
        this.lectura = new TransactionTemplate(transacciones);
        this.lectura.setReadOnly(true);
    }

    public String versionMotor() {
        return base.versionMotor();
    }

    @Transactional
    public Optional<Integer> guardar(Guardado g) {
        return guardar(g, Instant.now(), base.versionMotor());
    }

    @Transactional
    public Optional<Integer> guardar(Guardado g, Instant ahora, String version) {
        String texto = g.texto();
        List<Sugerencia> sug = g.sugerencias();
        Optional<ResolucionEntity> guardada;
        String tema;
        List<Restriccion> restricciones = List.of();
        switch (g.resolucion()) {
            case Resolucion r -> {
                guardada = base.guardar(texto, r, sug, ahora, version);
                tema = HistorialFactorizacionService.letras(r.entrada()).contains(",")
                        ? "Factorización en varias variables"
                        : r.campo() == Campo.Q ? "Factorización en Q" : "Factorización en R y C";
            }
            case Simplificacion s -> {
                guardada = base.guardarSimplificacion(texto, s, sug, ahora, version);
                tema = "Fracciones algebraicas";
                restricciones = s.restricciones().stream().map(t -> new Restriccion(t, "DENOMINADOR")).toList();
            }
            case ResolucionLimite l -> {
                guardada = base.guardarLimite(texto, l, ahora, version);
                tema = "Límites";
            }
            case ResolucionFunciones f -> {
                guardada = funciones(texto, f, sug, ahora, version);
                tema = "Funciones trascendentes";
                restricciones = f.restricciones().stream().map(t -> new Restriccion(t, origen(t, f.entrada()))).toList();
            }
            case ResolucionEcuacion e -> {
                guardada = ecuacion(e, g.forzado(), ahora, version);
                tema = "Factorización en Q";
            }
            case ResolucionDerivada d -> {
                Map<String, String> p = new LinkedHashMap<>();
                p.put("variable", String.valueOf(d.variable()));
                p.put("orden", String.valueOf(d.orden()));
                String clave = "variable=" + d.variable() + ";orden=" + d.orden()
                        + (d.esImplicita() ? ";implicita=" + d.implicita() : "");
                guardada = calculo("DERIVAR", d.funcion(), String.valueOf(d.variable()), clave, p, g.forzado(),
                        d.texto(), d.latex(), d.pasos(), sug, ahora, version);
                tema = "Derivadas";
            }
            case ResolucionIntegral i -> {
                if (!i.resuelta()) {
                    return Optional.empty();
                }
                Map<String, String> p = new LinkedHashMap<>();
                p.put("variable", String.valueOf(i.variable()));
                String clave = "variable=" + i.variable();
                if (i.definida()) {
                    String a = ImpresoraTexto.imprimir(i.integral().inferior());
                    String b = ImpresoraTexto.imprimir(i.integral().superior());
                    p.put("limite_inferior", a);
                    p.put("limite_superior", b);
                    clave += ";limite_inferior=" + a + ";limite_superior=" + b;
                }
                if (!i.coeficiente().equals(Racional.UNO)) {
                    clave += ";coeficiente=" + i.coeficiente();
                }
                guardada = calculo(i.definida() ? "INTEGRAL_DEFINIDA" : "INTEGRAR", i.integral().cuerpo(),
                        String.valueOf(i.variable()), clave, p, g.forzado(), i.texto(), i.latex(), i.pasos(), sug,
                        ahora, version);
                tema = i.codigos().contains("I06") ? "Fracciones parciales" : "Integrales";
            }
            case ResolucionParcial p -> {
                if (p.resultado() == null && p.vector().isEmpty()) {
                    return Optional.empty();
                }
                String operacion = switch (p.tipo()) {
                    case GRADIENTE -> "GRADIENTE";
                    case DIRECCIONAL -> "DERIVADA_DIRECCIONAL";
                    case PARCIAL, CADENA -> "DERIVADA_PARCIAL";
                };
                String vars = p.variables().stream().map(String::valueOf).collect(Collectors.joining(","));
                guardada = calculo(operacion, p.funcion(), vars, "entrada=" + canonico(texto, p.entrada()), Map.of(),
                        g.forzado(), p.texto(), p.latex(), p.pasos(), sug, ahora, version);
                tema = "Derivadas parciales";
                restricciones = p.dominio().stream().map(t -> new Restriccion(t, origen(t, p.funcion()))).toList();
            }
            case ResolucionLimiteDoble l -> {
                if (l.estado() == ResolucionLimiteDoble.Estado.NO_DECIDIDO) {
                    return Optional.empty();
                }
                var e = l.entrada();
                String punto = "(" + ImpresoraTexto.imprimir(e.a()) + ", " + ImpresoraTexto.imprimir(e.b()) + ")";
                Map<String, String> p = new LinkedHashMap<>();
                p.put("variable", e.x() + "," + e.y());
                p.put("punto", punto);
                guardada = calculo("LIMITE_DOBLE", e.cuerpo(), e.x() + "," + e.y(),
                        "variable=" + e.x() + "," + e.y() + ";punto=" + punto, p, g.forzado(), l.texto(), l.latex(),
                        l.pasos(), sug, ahora, version);
                tema = "Límites dobles";
            }
            case ResolucionExtremos x -> {
                if (x.puntos().isEmpty() && !x.completo()) {
                    return Optional.empty();
                }
                Map<String, String> p = new LinkedHashMap<>();
                String clave = "";
                if (x.restriccion() != null) {
                    String g2 = ImpresoraTexto.imprimir(x.restriccion());
                    p.put("restriccion", g2);
                    clave = "restriccion=" + g2;
                }
                String vars = x.variables().stream().map(String::valueOf).collect(Collectors.joining(","));
                guardada = calculo(x.tipo() == ResolucionExtremos.Tipo.LAGRANGE ? "LAGRANGE" : "EXTREMOS",
                        x.funcion(), vars, clave, p, g.forzado(), x.texto(), x.latex(), x.pasos(), sug, ahora, version);
                tema = "Extremos";
            }
            case ResolucionIntegralMultiple m -> {
                if (!m.resuelta()) {
                    return Optional.empty();
                }
                Map<String, String> p = new LinkedHashMap<>();
                p.put("sistema_coordenadas", m.coordenadas().name());
                guardada = calculo("INTEGRAL_MULTIPLE", m.entrada().cuerpo(), "x",
                        "entrada=" + canonico(texto, m.entrada()), p, g.forzado(), m.texto(), m.latex(), m.pasos(),
                        sug, ahora, version);
                tema = "Integrales múltiples";
            }
            case null, default -> {
                return Optional.empty();
            }
        }
        if (guardada.isEmpty()) {
            return Optional.empty();
        }
        ResolucionEntity res = guardada.get();
        completar(res, texto, tema, restricciones);
        return Optional.of(res.getId());
    }

    private void completar(ResolucionEntity res, String texto, String tema, List<Restriccion> restricciones) {
        res.setEntradaTexto(texto);
        res.setEntradaLatex(latexDe(texto));
        temas.findByNombre(tema).ifPresent(t -> {
            res.setTema(t);
            res.getEjercicio().getTemas().add(t);
        });
        if (res.getRestricciones().isEmpty()) {
            for (Restriccion r : restricciones) {
                RestriccionDominioEntity e = new RestriccionDominioEntity();
                e.setTexto(r.texto());
                e.setLatex(latexRestriccion(r.texto()));
                e.setOrigen(r.origen());
                res.agregarRestriccion(e);
            }
        }
        resoluciones.saveAndFlush(res);
    }

    private record Restriccion(String texto, String origen) {
    }

    private Optional<ResolucionEntity> calculo(String operacion, Expresion funcion, String variablesSiConstante,
                                               String clave, Map<String, String> parametros, String forzado,
                                               String resultadoTexto, String resultadoLatex, List<PasoLimite> pasos,
                                               List<Sugerencia> sug, Instant ahora, String version) {
        String variables = HistorialFactorizacionService.letras(funcion);
        if (variables.isEmpty()) {
            variables = variablesSiConstante;
        }
        Instant fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        EjercicioEntity ejercicio = base.ejercicio(ImpresoraTexto.imprimir(funcion), funcion, variables, fecha);
        String conMetodo = forzado == null ? clave : (clave.isEmpty() ? "" : clave + ";") + "metodo=" + forzado;
        return Optional.of(base.guardarResolucion(ejercicio, operacion, conMetodo, fecha, version, res -> {
            res.setCampo(null);
            res.setTecnicaForzada(base.enCatalogo(forzado) ? forzado : null);
            res.setResultadoTexto(resultadoTexto);
            res.setResultadoLatex(resultadoLatex);
            res.setEsAproximado(false);
            res.getParametros().putAll(parametros);
            int[] orden = {1};
            for (PasoLimite p : pasos) {
                base.agregarPasoLimite(res, p, null, orden);
            }
            HistorialFactorizacionService.agregarSugerencias(res, sug);
        }));
    }

    private Optional<ResolucionEntity> funciones(String texto, ResolucionFunciones f, List<Sugerencia> sug,
                                                 Instant ahora, String version) {
        if (!f.factorizable() || f.resultado() == null) {
            return Optional.empty();
        }
        String variables = HistorialFactorizacionService.letras(f.entrada());
        Instant fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        EjercicioEntity ejercicio = base.ejercicio(texto, f.entrada(), variables.isEmpty() ? "x" : variables, fecha);
        return Optional.of(base.guardarResolucion(ejercicio, "FACTORIZAR", HistorialFactorizacionService.CLAVE_Q, fecha,
                version, res -> {
                    res.setCampo("Q");
                    res.setResultadoTexto(f.texto());
                    res.setResultadoLatex(ImpresoraLatex.imprimir(f.resultado()));
                    res.setEsAproximado(false);
                    int orden = 1;
                    PasoEntity padre = null;
                    if (base.enCatalogo(f.codigo())) {
                        String justificacion = f.identidades().isEmpty() ? Tecnicas.nombre(f.codigo())
                                : String.join("; ", f.identidades());
                        padre = PasoMapper.aEntidad(f.codigo(), justificacion, f.entrada(), f.resultado(),
                                f.sustitucion(), orden++);
                        res.agregarPaso(padre);
                    }
                    if (f.enLetras() != null) {
                        for (Paso p : f.enLetras().pasos()) {
                            PasoEntity e = PasoMapper.aEntidad(p, orden++);
                            e.setPadre(padre);
                            res.agregarPaso(e);
                        }
                        for (Paso p : f.enLetras().descartados()) {
                            PasoEntity e = PasoMapper.aEntidad(p, orden++);
                            e.setPadre(padre);
                            res.agregarPaso(e);
                        }
                    }
                    HistorialFactorizacionService.agregarSugerencias(res, sug);
                }));
    }

    private Optional<ResolucionEntity> ecuacion(ResolucionEcuacion r, String forzado, Instant ahora, String version) {
        Instant fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        String variable = String.valueOf(r.variable());
        EjercicioEntity ejercicio = base.ejercicio(ImpresoraTexto.imprimir(r.entrada()), r.entrada(), variable, fecha);
        String clave = "variable=" + variable + (forzado == null ? "" : ";metodo=" + forzado);
        return Optional.of(base.guardarResolucion(ejercicio, "RESOLVER_ECUACION", clave, fecha, version, res -> {
            res.setCampo("Q");
            res.setTecnicaForzada(base.enCatalogo(forzado) ? forzado : null);
            res.setResultadoTexto(r.resumen());
            res.setResultadoLatex(latexSoluciones(r));
            res.setEsAproximado(false);
            res.getParametros().put("variable", variable);
            int orden = 1;
            for (ResolucionEcuacion.PasoEcuacion p : r.pasos()) {
                String codigo = switch (p.tipo()) {
                    case PASAR_A_UN_LADO -> "EQ1";
                    case FACTORIZAR -> "EQ2";
                    case IGUALAR_A_CERO -> "EQ3";
                    case DESPEJAR -> "EQ4";
                };
                PasoEntity e = pasoDeLineas(codigo, p, orden++);
                res.agregarPaso(e);
                if (p.tipo() == ResolucionEcuacion.PasoEcuacion.Tipo.FACTORIZAR && r.factorizacion() != null) {
                    for (Paso q : r.factorizacion().pasos()) {
                        PasoEntity h = PasoMapper.aEntidad(q, orden++);
                        h.setPadre(e);
                        res.agregarPaso(h);
                    }
                    for (Paso q : r.factorizacion().descartados()) {
                        PasoEntity h = PasoMapper.aEntidad(q, orden++);
                        h.setPadre(e);
                        res.agregarPaso(h);
                    }
                }
            }
        }));
    }

    private static PasoEntity pasoDeLineas(String codigo, ResolucionEcuacion.PasoEcuacion p, int orden) {
        PasoEntity e = new PasoEntity();
        e.setOrden(orden);
        e.setTecnicaCodigo(codigo);
        String justificacion = p.detalle() == null || p.detalle().isBlank() ? p.titulo() : p.detalle();
        e.setJustificacion(justificacion == null ? "" : justificacion);
        List<String> lineas = p.lineas();
        String antes = lineas.isEmpty() ? "" : lineas.getFirst();
        String despues = lineas.isEmpty() ? p.titulo() : lineas.getLast();
        e.setAntesTexto(antes);
        e.setAntesLatex(antes.isEmpty() ? "" : latexDe(antes));
        e.setDespuesTexto(despues);
        List<String> latex = lineas.stream().map(HistorialService::latexDe).toList();
        e.setDespuesLatex(latex.isEmpty() ? textoLatex(despues) : latex.size() == 1 ? latex.getFirst()
                : "\\begin{array}{l}" + String.join(" \\\\ ", latex) + "\\end{array}");
        e.setTrabajoTipo("OTRO");
        e.setTrabajoJson("{\"tipo\":\"LINEAS\",\"lineas\":[" + lineas.stream().map(PasoMapper::texto)
                .collect(Collectors.joining(",")) + "]}");
        e.setEsDescartado(false);
        return e;
    }

    private static String latexSoluciones(ResolucionEcuacion r) {
        if (r.tipo() != ResolucionEcuacion.Tipo.CON_SOLUCIONES) {
            return textoLatex(r.resumen());
        }
        List<String> partes = new ArrayList<>();
        List<ResolucionEcuacion.Solucion> s = r.soluciones();
        for (int i = 0; i < s.size(); i++) {
            String nombre = s.size() == 1 ? String.valueOf(r.variable()) : r.variable() + "_{" + (i + 1) + "}";
            String valor = ImpresoraLatex.imprimir(new pe.mathnova.domain.expresion.Constante(s.get(i).valor()));
            String extra = s.get(i).multiplicidad() > 1 ? "\\;\\text{(" + s.get(i).nombreMultiplicidad() + ")}" : "";
            partes.add(nombre + " = " + valor + extra);
        }
        return String.join(",\\quad ", partes);
    }

    private static String canonico(String texto, Expresion entrada) {
        try {
            return ImpresoraTexto.imprimir(entrada);
        } catch (RuntimeException e) {
            return texto.replace(" ", "");
        }
    }

    static String origen(String restriccion, Expresion funcion) {
        String f = funcion == null ? "" : ImpresoraTexto.imprimir(funcion);
        if (restriccion.contains("π") && (f.contains("tan") || f.contains("tg") || f.contains("sec"))) {
            return "TANGENTE";
        }
        if (restriccion.contains("≠")) {
            return f.contains("tan") || f.contains("tg") ? "TANGENTE" : "DENOMINADOR";
        }
        if (f.contains("ln") || f.contains("log")) {
            return "LOGARITMO";
        }
        if (f.contains("√") || f.contains("sqrt") || f.contains("raiz")) {
            return "RAIZ_PAR";
        }
        return "OTRA";
    }

    @Transactional(readOnly = true)
    public List<String> cursos() {
        List<String> r = new ArrayList<>();
        r.add(TODOS_LOS_CURSOS);
        cursos.findAllByOrderByOrdenAsc().stream().map(CursoEntity::getNombre).forEach(r::add);
        return r;
    }

    @Transactional(readOnly = true)
    public Pagina listar(Filtro filtro, int pagina) {
        return listar(filtro, pagina, ZonedDateTime.now(zona));
    }

    @Transactional(readOnly = true)
    public Pagina listar(Filtro filtro, int pagina, ZonedDateTime ahora) {
        Filtro f = filtro == null ? Filtro.todo() : filtro;
        Specification<ResolucionEntity> spec = especificacion(f, ahora);
        Sort orden = Sort.by(Sort.Order.desc("actualizadoEn"), Sort.Order.desc("id"));
        Page<ResolucionEntity> page = resoluciones.findAll(spec, PageRequest.of(Math.max(0, pagina), TAMANO_PAGINA, orden));
        List<Fila> filas = page.getContent().stream().map(this::fila).toList();
        return new Pagina(filas, page.getTotalElements(), page.getNumber(), page.hasNext());
    }

    private Specification<ResolucionEntity> especificacion(Filtro f, ZonedDateTime ahora) {
        String buscado = f.texto() == null ? "" : f.texto().strip();
        Set<String> codigos = buscado.isEmpty() ? Set.of() : operacionesQueDicen(buscado);
        Set<Integer> temasBuscados = buscado.isEmpty() ? Set.of() : temasQueDicen(buscado);
        String curso = f.curso() == null || f.curso().isBlank() || f.curso().equals(TODOS_LOS_CURSOS) ? null : f.curso();
        Instant desde = f.periodo() == null ? null : f.periodo().desde(ahora);
        return (root, consulta, cb) -> {
            Join<ResolucionEntity, EjercicioEntity> e = root.join("ejercicio");
            Join<ResolucionEntity, TemaEntity> t = root.join("tema", JoinType.LEFT);
            List<Predicate> y = new ArrayList<>();
            if (!buscado.isEmpty()) {
                String patron = "%" + escapar(sinEspacios(buscado).toLowerCase(Locale.ROOT)) + "%";
                List<Predicate> o = new ArrayList<>();
                for (Expression<String> campo : List.<Expression<String>>of(e.get("expresionEntrada"),
                        e.get("expresionNormalizada"), root.get("entradaTexto"))) {
                    Expression<String> limpio = cb.lower(cb.function("replace", String.class, campo, cb.literal(" "),
                            cb.literal("")));
                    o.add(cb.like(limpio, patron, '\\'));
                }
                if (!codigos.isEmpty()) {
                    o.add(root.get("operacionCodigo").in(codigos));
                }
                if (!temasBuscados.isEmpty()) {
                    o.add(t.get("id").in(temasBuscados));
                }
                y.add(cb.or(o.toArray(Predicate[]::new)));
            }
            if (curso != null) {
                y.add(cb.equal(t.join("curso", JoinType.LEFT).get("nombre"), curso));
            }
            if (desde != null) {
                y.add(cb.greaterThanOrEqualTo(root.<Instant>get("actualizadoEn"), desde));
            }
            if (f.soloFavoritos()) {
                y.add(cb.equal(e.get("esFavorito"), Boolean.TRUE));
            }
            return cb.and(y.toArray(Predicate[]::new));
        };
    }

    private Set<String> operacionesQueDicen(String buscado) {
        String b = plano(buscado);
        Set<String> r = new java.util.HashSet<>();
        for (OperacionEntity o : operaciones.findAll()) {
            String verbo = VERBOS.getOrDefault(o.getCodigo(), o.getNombre());
            if (plano(o.getNombre()).contains(b) || plano(verbo).contains(b) || plano(o.getCodigo()).contains(b)) {
                r.add(o.getCodigo());
            }
        }
        return r;
    }

    private Set<Integer> temasQueDicen(String buscado) {
        String b = plano(buscado);
        return temas.findAll().stream().filter(t -> plano(t.getNombre()).contains(b)).map(TemaEntity::getId)
                .collect(Collectors.toSet());
    }

    private Fila fila(ResolucionEntity r) {
        EjercicioEntity e = r.getEjercicio();
        TemaEntity t = r.getTema();
        return new Fila(r.getId(), e.getId(), latexDeFila(r), entradaDe(r), VERBOS.getOrDefault(r.getOperacionCodigo(),
                r.getOperacionCodigo()), t == null ? SIN_DATO : t.getNombre(),
                t == null ? SIN_DATO : t.getCurso().getNombre(), r.getCampo() == null ? SIN_DATO : r.getCampo(),
                r.getActualizadoEn(), Boolean.TRUE.equals(e.getEsFavorito()));
    }

    private static String latexDeFila(ResolucionEntity r) {
        if (r.getEntradaLatex() != null && !r.getEntradaLatex().isBlank()) {
            return r.getEntradaLatex();
        }
        if (r.getEntradaTexto() != null) {
            return latexDe(r.getEntradaTexto());
        }
        return r.getEjercicio().getExpresionLatex();
    }

    private static String entradaDe(ResolucionEntity r) {
        return r.getEntradaTexto() != null ? r.getEntradaTexto() : r.getEjercicio().getExpresionEntrada();
    }

    public static String fecha(Instant instante, ZonedDateTime ahora) {
        ZonedDateTime f = instante.atZone(ahora.getZone());
        LocalDate dia = f.toLocalDate();
        LocalDate hoy = ahora.toLocalDate();
        String hora = HORA.format(f);
        if (dia.equals(hoy)) {
            return "Hoy, " + hora;
        }
        if (dia.equals(hoy.minusDays(1))) {
            return "Ayer, " + hora;
        }
        String d = dia.getDayOfMonth() + " " + MESES[dia.getMonthValue() - 1];
        return (dia.getYear() == hoy.getYear() ? d : d + " " + dia.getYear()) + ", " + hora;
    }

    public String fecha(Instant instante) {
        return fecha(instante, ZonedDateTime.now(zona));
    }

    public long ejerciciosGuardados() {
        return resoluciones.contarEjercicios();
    }

    @Transactional
    public int borrarTodo() {
        resoluciones.borrarTodas();
        return resoluciones.borrarEjercicios();
    }

    @Transactional
    public boolean alternarFavorito(int resolucionId) {
        ResolucionEntity r = resoluciones.findById(resolucionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la resolución " + resolucionId));
        EjercicioEntity e = r.getEjercicio();
        boolean ahora = !Boolean.TRUE.equals(e.getEsFavorito());
        e.setEsFavorito(ahora);
        resoluciones.flush();
        return ahora;
    }

    @Transactional
    public void eliminar(int resolucionId) {
        Optional<ResolucionEntity> r = resoluciones.findById(resolucionId);
        if (r.isEmpty()) {
            return;
        }
        Integer ejercicio = r.get().getEjercicio().getId();
        resoluciones.borrar(resolucionId);
        resoluciones.borrarEjercicioSinResoluciones(ejercicio);
    }

    public Hoja hoja(int resolucionId) {
        Hoja h = lectura.execute(s -> leerHoja(resolucionId, false));
        if (h == null) {
            throw new IllegalArgumentException("No existe la resolución " + resolucionId);
        }
        if (h.versionMotor().equals(base.versionMotor())) {
            return h;
        }
        try {
            ResolverService r = resolver.getIfAvailable();
            if (r != null) {
                r.resolverYGuardar(h.entradaTexto(), h.campo() == null ? Campo.Q : Campo.valueOf(h.campo()),
                        h.metodoForzado());
                Hoja nueva = lectura.execute(s -> leerHoja(resolucionId, true));
                if (nueva != null) {
                    return nueva;
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudo recalcular la resolución {} de la versión {}", resolucionId, h.versionMotor(), e);
        }
        return h;
    }

    private Hoja leerHoja(int id, boolean recalculada) {
        ResolucionEntity r = resoluciones.findById(id).orElse(null);
        if (r == null) {
            return null;
        }
        Map<Integer, Integer> niveles = new HashMap<>();
        Map<Integer, String> numeros = new HashMap<>();
        Map<Integer, Integer> hijos = new HashMap<>();
        int arriba = 0;
        List<PasoHoja> pasos = new ArrayList<>();
        List<Descartado> descartados = new ArrayList<>();
        for (PasoEntity p : r.getPasos()) {
            String titulo = nombreTecnica(p.getTecnicaCodigo());
            if (Boolean.TRUE.equals(p.getEsDescartado())) {
                descartados.add(new Descartado(p.getTecnicaCodigo(), titulo, p.getAntesLatex(), p.getMotivoDescarte()));
                continue;
            }
            Integer padre = p.getPadre() == null ? null : p.getPadre().getId();
            String numero;
            int nivel;
            if (padre == null || !numeros.containsKey(padre)) {
                numero = String.valueOf(++arriba);
                nivel = 0;
            } else {
                int k = hijos.merge(padre, 1, Integer::sum);
                numero = numeros.get(padre) + "." + k;
                nivel = niveles.get(padre) + 1;
            }
            numeros.put(p.getId(), numero);
            niveles.put(p.getId(), nivel);
            pasos.add(new PasoHoja(numero, nivel, p.getTecnicaCodigo(), titulo, formula(p), p.getJustificacion()));
        }
        List<String> restricciones = r.getRestricciones().stream().map(RestriccionDominioEntity::getTexto).toList();
        return new Hoja(r.getId(), VERBOS.getOrDefault(r.getOperacionCodigo(), r.getOperacionCodigo()), latexDeFila(r),
                entradaDe(r), r.getCampo(), metodo(r), pasos, descartados, restricciones, r.getResultadoLatex(),
                r.getResultadoTexto(), r.getVersionMotor(), recalculada);
    }

    private static String metodo(ResolucionEntity r) {
        if (r.getTecnicaForzada() != null) {
            return r.getTecnicaForzada();
        }
        String clave = r.getClaveParametros();
        int i = clave == null ? -1 : clave.indexOf("metodo=");
        if (i < 0) {
            return null;
        }
        int fin = clave.indexOf(';', i);
        return clave.substring(i + "metodo=".length(), fin < 0 ? clave.length() : fin);
    }

    static String formula(PasoEntity p) {
        String antes = p.getAntesLatex();
        String despues = p.getDespuesLatex();
        if (despues == null || despues.isBlank()) {
            return antes == null ? "" : antes;
        }
        if (antes == null || antes.isBlank() || despues.contains("=") || antes.contains("=")
                || despues.contains("\\\\") || despues.startsWith("\\text")) {
            return despues;
        }
        return antes + " = " + despues;
    }

    private String nombreTecnica(String codigo) {
        Map<String, String> n = nombresTecnicas;
        if (n == null) {
            n = tecnicas.findAll().stream().collect(Collectors.toUnmodifiableMap(TecnicaEntity::getCodigo,
                    TecnicaEntity::getNombre));
            nombresTecnicas = n;
        }
        return n.getOrDefault(codigo, Tecnicas.nombre(codigo));
    }

    static String latexDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        try {
            return ImpresoraLatex.imprimir(Parser.parse(texto));
        } catch (RuntimeException e) {
            return textoLatex(texto);
        }
    }

    static String textoLatex(String texto) {
        return "\\text{" + texto.replace("\\", "").replace("{", "").replace("}", "")
                .replace("$", "").replace("%", "\\%").replace("&", "\\&").replace("#", "\\#") + "}";
    }

    static String latexRestriccion(String texto) {
        return texto.replace("≠", "\\neq ").replace("≥", "\\geq ").replace("≤", "\\leq ").replace("π", "\\pi ")
                .replace("·", "\\cdot ").replace("∈", "\\in ").replace("ℤ", "\\mathbb{Z}");
    }

    static String plano(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replace('_', ' ').strip();
    }

    private static String sinEspacios(String s) {
        return s.replace(" ", "");
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
