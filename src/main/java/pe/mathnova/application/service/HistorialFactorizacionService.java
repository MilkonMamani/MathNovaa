package pe.mathnova.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.VariablePrincipal;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.factorizacion.Tecnicas;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.infrastructure.persistence.EjercicioEntity;
import pe.mathnova.infrastructure.persistence.EjercicioRepository;
import pe.mathnova.infrastructure.persistence.PasoEntity;
import pe.mathnova.infrastructure.persistence.PasoMapper;
import pe.mathnova.infrastructure.persistence.PasoRepository;
import pe.mathnova.infrastructure.persistence.ResolucionEntity;
import pe.mathnova.infrastructure.persistence.ResolucionRepository;
import pe.mathnova.infrastructure.persistence.SugerenciaEntity;
import pe.mathnova.infrastructure.persistence.SugerenciaRepository;
import pe.mathnova.infrastructure.persistence.TecnicaEntity;
import pe.mathnova.infrastructure.persistence.TecnicaRepository;

/**
 * Guarda cada resolución en el historial (RN24): factorizaciones, fracciones simplificadas (P01) y
 * límites (Sprint 5), con sus sugerencias (HU10). RN25: si ya hay una resolución del mismo ejercicio,
 * operación y clave, solo actualiza la fecha (misma versión del motor) o reemplaza sus pasos y
 * sugerencias (otra versión).
 *
 * <p>Un límite se guarda sobre el ejercicio de su función: lim(x→2, (x² − 4)/(x − 2)) y la fracción
 * (x² − 4)/(x − 2) son el mismo ejercicio, con operaciones LIMITE y SIMPLIFICAR.
 *
 * <p>Desde el Sprint 9 lo llama {@link HistorialService}, que guarda también las demás operaciones con
 * estas mismas piezas ({@link #ejercicio}, {@link #guardarResolucion}, {@link #agregarPasoLimite}).
 */
@Service
public class HistorialFactorizacionService {

    static final String OPERACION = "FACTORIZAR";
    static final String SIMPLIFICAR = "SIMPLIFICAR";
    static final String LIMITE = "LIMITE";
    static final String CLAVE_Q = "campo=Q";

    private final EjercicioRepository ejercicios;
    private final ResolucionRepository resoluciones;
    private final PasoRepository pasos;
    private final SugerenciaRepository sugerencias;
    private final TecnicaRepository tecnicas;
    private final String versionMotor;
    /** Los códigos de la tabla tecnica (V2 y V3): paso.tecnica_codigo es clave foránea. */
    private volatile java.util.Set<String> catalogo;

    public HistorialFactorizacionService(EjercicioRepository ejercicios, ResolucionRepository resoluciones,
                                         PasoRepository pasos, SugerenciaRepository sugerencias,
                                         TecnicaRepository tecnicas,
                                         @Value("${mathnova.version-motor}") String versionMotor) {
        this.ejercicios = ejercicios;
        this.resoluciones = resoluciones;
        this.pasos = pasos;
        this.sugerencias = sugerencias;
        this.tecnicas = tecnicas;
        this.versionMotor = versionMotor;
    }

    public String versionMotor() {
        return versionMotor;
    }

    /** Si el código está en la tabla tecnica. */
    boolean enCatalogo(String codigo) {
        java.util.Set<String> c = catalogo;
        if (c == null) {
            c = tecnicas.findAll().stream().map(TecnicaEntity::getCodigo).collect(Collectors.toUnmodifiableSet());
            catalogo = c;
        }
        return codigo != null && c.contains(codigo);
    }

    // ------------------------------------------------------------------ factorizar

    @Transactional
    public Optional<ResolucionEntity> guardar(String entrada, Resolucion r) {
        return guardar(entrada, r, List.of(), Instant.now(), versionMotor);
    }

    @Transactional
    public Optional<ResolucionEntity> guardar(String entrada, Resolucion r, List<Sugerencia> sugeridas) {
        return guardar(entrada, r, sugeridas, Instant.now(), versionMotor);
    }

    /**
     * @param ahora   fecha de la factorización (se guarda al segundo)
     * @param version versión del motor que la produjo
     * @return vacío si la entrada no tiene letras (una constante no se guarda: V1 exige variables)
     */
    @Transactional
    public Optional<ResolucionEntity> guardar(String entrada, Resolucion r, Instant ahora, String version) {
        return guardar(entrada, r, List.of(), ahora, version);
    }

    @Transactional
    public Optional<ResolucionEntity> guardar(String entrada, Resolucion r, List<Sugerencia> sugeridas, Instant ahora,
                                              String version) {
        String variables = letras(r.entrada());
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        Instant fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        EjercicioEntity ejercicio = ejercicio(entrada, r.entrada(), variables, fecha);
        // HU08: una resolución forzada se guarda aparte de la automática (RN25 compara la clave).
        String campo = "campo=" + r.campo().name();
        String clave = r.metodoForzado() == null ? campo : campo + ";metodo=" + r.metodoForzado();
        return Optional.of(guardarResolucion(ejercicio, OPERACION, clave, fecha, version, res -> {
            res.setCampo(r.campo().name());
            res.setTecnicaForzada(r.metodoForzado());
            res.setResultadoTexto(r.texto());
            res.setResultadoLatex(FactorizacionService.latex(r));
            res.setEsAproximado(r.extension() != null && r.extension().aproximada());
            int orden = 1;
            for (Paso p : r.pasos()) {
                res.agregarPaso(PasoMapper.aEntidad(p, orden++));
            }
            if (r.extension() != null) {
                // "Pasar a R/C" no es una técnica del catálogo (V2): se guardan F21–F30 y A5.
                for (var p : r.extension().pasos()) {
                    if (!p.codigoTecnica().startsWith("P")) {
                        res.agregarPaso(PasoMapper.aEntidad(p, orden++));
                    }
                }
            }
            for (Paso d : r.descartados()) {
                res.agregarPaso(PasoMapper.aEntidad(d, orden++));
            }
            agregarSugerencias(res, sugeridas);
        }));
    }

    // ------------------------------------------------------------------ simplificar (P01)

    @Transactional
    public Optional<ResolucionEntity> guardarSimplificacion(String entrada, Simplificacion s, List<Sugerencia> sugeridas) {
        return guardarSimplificacion(entrada, s, sugeridas, Instant.now(), versionMotor);
    }

    @Transactional
    public Optional<ResolucionEntity> guardarSimplificacion(String entrada, Simplificacion s, List<Sugerencia> sugeridas,
                                                            Instant ahora, String version) {
        String variables = letras(s.entrada());
        if (variables.isEmpty()) {
            return Optional.empty();
        }
        Instant fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        EjercicioEntity ejercicio = ejercicio(entrada, s.entrada(), variables, fecha);
        return Optional.of(guardarResolucion(ejercicio, SIMPLIFICAR, CLAVE_Q, fecha, version, res -> {
            res.setCampo("Q");
            res.setResultadoTexto(s.texto());
            res.setResultadoLatex(ImpresoraLatex.imprimir(s.aExpresion()));
            res.setEsAproximado(false);
            int orden = 1;
            for (Paso p : s.numerador().pasos()) {
                res.agregarPaso(PasoMapper.aEntidad(p, orden++));
            }
            for (Paso p : s.denominador().pasos()) {
                res.agregarPaso(PasoMapper.aEntidad(p, orden++));
            }
            res.agregarPaso(PasoMapper.aEntidad("P01", s.justificacion(), s.entrada(), s.aExpresion(), null, orden));
            agregarSugerencias(res, sugeridas);
        }));
    }

    // ------------------------------------------------------------------ límites (Sprint 5)

    @Transactional
    public Optional<ResolucionEntity> guardarLimite(String entrada, ResolucionLimite r) {
        return guardarLimite(entrada, r, Instant.now(), versionMotor);
    }

    /**
     * Un límite que no se pudo calcular no se guarda (RN24: solo lo resuelto). Los pasos anidados
     * (factorización en L02, derivadas en L07) van con paso_padre_id.
     */
    @Transactional
    public Optional<ResolucionEntity> guardarLimite(String entrada, ResolucionLimite r, Instant ahora, String version) {
        if (r.resultado() instanceof ResultadoLimite.NoCalculado) {
            return Optional.empty();
        }
        Limite l = r.entrada();
        Expresion f = l.cuerpo();
        String variables = String.join(",", VariablePrincipal.de(l).variables().stream().map(String::valueOf).toList());
        Instant fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        EjercicioEntity ejercicio = ejercicio(ImpresoraTexto.imprimir(f), f, variables, fecha);
        String punto = l.punto() instanceof Limite.Finito fin ? ImpresoraTexto.imprimir(fin.valor())
                : ((Limite.Infinito) l.punto()).positivo() ? "∞" : "-∞";
        String clave = "variable=" + l.variable() + ";punto=" + punto + ";lado=" + l.lado().name();
        return Optional.of(guardarResolucion(ejercicio, LIMITE, clave, fecha, version, res -> {
            res.setCampo(null);
            res.setResultadoTexto(r.resultado().texto());
            res.setResultadoLatex(r.resultado().latex());
            res.setEsAproximado(false);
            res.getParametros().put("variable", String.valueOf(l.variable()));
            res.getParametros().put("punto", punto);
            res.getParametros().put("lado", l.lado().name());
            int[] orden = {1};
            for (PasoLimite p : r.pasos()) {
                agregarPasoLimite(res, p, null, orden);
            }
        }));
    }

    /**
     * Un paso de los motores de cálculo con sus hijos (paso_padre_id). Un paso que no es técnica del
     * catálogo (E01…) no se guarda, pero sus hijos sí: cuelgan del paso de más arriba.
     */
    void agregarPasoLimite(ResolucionEntity res, PasoLimite p, PasoEntity padre, int[] orden) {
        PasoEntity e = padre;
        if (enCatalogo(p.codigo())) {
            String justificacion = p.justificacion() != null && !p.justificacion().isBlank() ? p.justificacion()
                    : p.titulo() != null ? p.titulo() : Tecnicas.nombre(p.codigo());
            e = PasoMapper.aEntidad(p.codigo(), justificacion, p.antes(), p.despues(), p.trabajo(), orden[0]++);
            e.setPadre(padre);
            res.agregarPaso(e);
        }
        for (PasoLimite h : p.hijos()) {
            agregarPasoLimite(res, h, e, orden);
        }
    }

    // ------------------------------------------------------------------ común

    /**
     * RN25: si ya existe con la misma versión, solo cambia la fecha; con otra versión, se borran los
     * pasos y las sugerencias viejos y se vuelven a llenar.
     */
    ResolucionEntity guardarResolucion(EjercicioEntity ejercicio, String operacion, String clave, Instant fecha,
                                               String version, Consumer<ResolucionEntity> llenar) {
        Optional<ResolucionEntity> existente =
                resoluciones.findByEjercicioAndOperacionCodigoAndClaveParametros(ejercicio, operacion, clave);
        if (existente.isPresent() && existente.get().getVersionMotor().equals(version)) {
            ResolucionEntity res = existente.get();
            res.setActualizadoEn(fecha);
            return resoluciones.saveAndFlush(res);
        }
        ResolucionEntity res;
        if (existente.isPresent()) {
            res = existente.get();
            // Se borran antes de insertar los nuevos: UNIQUE (resolucion_id, orden). Los anidados primero.
            List<PasoEntity> viejos = new ArrayList<>(res.getPasos());
            Collections.reverse(viejos);
            res.getPasos().clear();
            pasos.deleteAll(viejos);
            List<SugerenciaEntity> viejas = new ArrayList<>(res.getSugerencias());
            res.getSugerencias().clear();
            sugerencias.deleteAll(viejas);
            res.getParametros().clear();
            res.getRestricciones().clear();
            pasos.flush();
        } else {
            res = new ResolucionEntity();
            res.setEjercicio(ejercicio);
            res.setOperacionCodigo(operacion);
            res.setClaveParametros(clave);
            res.setCreadoEn(fecha);
        }
        res.setVersionMotor(version);
        res.setActualizadoEn(fecha);
        llenar.accept(res);
        return resoluciones.saveAndFlush(res);
    }

    static void agregarSugerencias(ResolucionEntity res, List<Sugerencia> sugeridas) {
        int orden = 1;
        for (Sugerencia s : sugeridas) {
            SugerenciaEntity e = new SugerenciaEntity();
            e.setOperacionCodigo(s.operacion());
            e.setReglaCodigo(s.regla());
            e.setJustificacion(s.justificacion());
            e.setParametrosJson("{\"titulo\":" + PasoMapper.texto(s.titulo()) + ",\"plantilla\":"
                    + PasoMapper.texto(s.plantilla()) + ",\"disponible\":" + s.disponible() + "}");
            e.setOrden(orden++);
            res.agregarSugerencia(e);
        }
    }

    static String letras(Expresion e) {
        return VariablePrincipal.de(e).variables().stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    EjercicioEntity ejercicio(String entrada, Expresion e, String variables, Instant fecha) {
        String normalizada = normalizada(e);
        return ejercicios.findByExpresionNormalizada(normalizada).orElseGet(() -> {
            EjercicioEntity x = new EjercicioEntity();
            x.setExpresionEntrada(entrada);
            x.setExpresionNormalizada(normalizada);
            x.setExpresionLatex(ImpresoraLatex.imprimir(e));
            x.setVariables(variables.isEmpty() ? "x" : variables);
            x.setTipo(tipo(e));
            x.setEsFavorito(false);
            x.setCreadoEn(fecha);
            return ejercicios.save(x);
        });
    }

    /**
     * La forma canónica de un ejercicio: un polinomio expandido ("x^2 - 4"), una fracción con sus dos
     * polinomios expandidos ("(x^2 - 4)/(x - 2)") o, si no, el texto de la expresión.
     */
    static String normalizada(Expresion e) {
        try {
            return Polinomio.desde(e).toString();
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            // sigue
        }
        if (SimplificadorFracciones.esFraccion(e)) {
            Expresion x = e;
            String signo = "";
            while (x instanceof Negacion n) {
                signo = signo.isEmpty() ? "-" : "";
                x = n.argumento();
            }
            Cociente c = (Cociente) x;
            return signo + "(" + Polinomio.desde(c.numerador()) + ")/(" + Polinomio.desde(c.denominador()) + ")";
        }
        return ImpresoraTexto.imprimir(e);
    }

    private static String tipo(Expresion e) {
        try {
            Polinomio.desde(e);
            return "POLINOMIO";
        } catch (NoEsPolinomioException | ArithmeticException noEs) {
            return SimplificadorFracciones.esFraccion(e) || !Expresiones.esTrascendente(e) ? "RACIONAL" : "TRASCENDENTE";
        }
    }
}
