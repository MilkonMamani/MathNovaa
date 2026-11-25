package pe.mathnova.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pe.mathnova.application.pdf.DocumentoPdf;
import pe.mathnova.application.pdf.ExportadorPdf;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.numeros.EvaluacionNumerica;

/**
 * Exportar una resolución a PDF (Sprint 9, parte C). Resolver y la hoja del Historial sacan el mismo PDF: los
 * pasos, el resultado y las restricciones son los de la resolución guardada (la hoja del Historial), y la
 * comprobación se vuelve a pedir al motor, igual que en la Pizarra. El archivo lo escribe el puerto
 * {@link ExportadorPdf}.
 */
@Service
public class ExportacionPdfService {

    private static final Logger LOG = LoggerFactory.getLogger(ExportacionPdfService.class);
    private static final Locale ES = Locale.of("es", "PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);

    private final HistorialService historial;
    private final ResolverService resolver;
    private final FactorizacionService factorizacion;
    private final ExportadorPdf exportador;
    private final ConfiguracionService configuracion;

    public ExportacionPdfService(HistorialService historial, ResolverService resolver,
                                 FactorizacionService factorizacion, ExportadorPdf exportador,
                                 ConfiguracionService configuracion) {
        this.configuracion = configuracion;
        this.historial = historial;
        this.resolver = resolver;
        this.factorizacion = factorizacion;
        this.exportador = exportador;
    }

    /**
     * El PDF de lo que está en la Pizarra: se resuelve otra vez (es instantáneo), se guarda (RN25: la misma fila
     * del historial, solo cambia la fecha) y se arma con la hoja guardada.
     *
     * @param campo   el campo de la pantalla; null es Q
     * @param forzado el método con que se empezó, o null
     * @throws RuntimeException si el texto no se resuelve o no se pudo guardar
     */
    public DocumentoPdf desdeResolver(String texto, Campo campo, String forzado, LocalDate hoy) {
        Object r = resolver.resolver(texto, campo, forzado);
        int id = resolver.guardar(texto, r, forzado, resolver.sugerencias(r))
                .orElseThrow(() -> new IllegalStateException("Esta resolución no se pudo guardar: " + texto));
        return documento(historial.hoja(id), comprobacion(r), hoy).conEscala(escala());
    }

    /** El PDF de una fila del Historial. @throws IllegalArgumentException si la fila ya no existe */
    public DocumentoPdf desdeHistorial(int resolucionId, LocalDate hoy) {
        HistorialService.Hoja h = historial.hoja(resolucionId);
        DocumentoPdf.Comprobacion c = null;
        if (h.entradaTexto() != null && !h.entradaTexto().isBlank()) {
            try {
                c = comprobacion(resolver.resolver(h.entradaTexto(),
                        h.campo() == null ? null : Campo.valueOf(h.campo()), h.metodoForzado()));
            } catch (RuntimeException e) {
                LOG.warn("Sin comprobación para el PDF de {}", h.entradaTexto(), e);
            }
        }
        return documento(h, c, hoy).conEscala(escala());
    }

    /** Sprint 9, parte E: el tamaño de letra de Configuración también vale para el PDF. */
    private double escala() {
        return configuracion == null ? 1 : configuracion.tamanoLetra().factor();
    }

    public void exportar(DocumentoPdf documento, Path destino) throws IOException {
        exportador.exportar(documento, destino);
    }

    /** "mathnova-factorizar-2026-11-24.pdf", "mathnova-calcular-limite-2026-11-24.pdf". */
    public static String nombreArchivo(String verbo, LocalDate fecha) {
        String operacion = HistorialService.plano(verbo).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return "mathnova-" + (operacion.isEmpty() ? "resolucion" : operacion) + "-" + fecha + ".pdf";
    }

    /** "24 de noviembre de 2026". */
    static String fecha(LocalDate d) {
        return FECHA.format(d);
    }

    static DocumentoPdf documento(HistorialService.Hoja h, DocumentoPdf.Comprobacion c, LocalDate hoy) {
        List<DocumentoPdf.Paso> pasos = h.pasos().stream()
                .map(p -> new DocumentoPdf.Paso(p.numero(), p.nivel(), p.titulo(), p.latex(), p.nota()))
                .toList();
        return new DocumentoPdf(h.verbo(), h.latexEntrada(), h.entradaTexto(), fecha(hoy), pasos, h.resultadoLatex(),
                h.resultadoTexto(), h.restricciones(), c, null);
    }

    // ------------------------------------------------------------------ comprobación (HU18)

    /** La comprobación del motor, como la de la Pizarra; null si esta operación no se comprueba. */
    DocumentoPdf.Comprobacion comprobacion(Object r) {
        try {
            return switch (r) {
                case Resolucion x -> {
                    boolean nada = x.pasos().isEmpty() && (x.extension() == null || x.extension().pasos().isEmpty());
                    yield nada ? null : deFactorizacion(factorizacion.verificar(x), "Coincide con la entrada",
                            "No coincide con la entrada");
                }
                case ResolucionFunciones x -> x.factorizable() ? deFactorizacion(factorizacion.verificar(x),
                        "Coincide con la entrada", "No coincide con la entrada") : null;
                case Simplificacion x -> {
                    Verificacion v = factorizacion.verificar(x);
                    yield new DocumentoPdf.Comprobacion("Por productos cruzados", lineas(v.productos()), v.coincide(),
                            v.coincide() ? "Coincide con la fracción original" : "No coincide con la fracción original");
                }
                case ResolucionIntegral x -> deIntegral(x);
                default -> null;
            };
        } catch (RuntimeException e) {
            LOG.warn("No se pudo armar la comprobación del PDF", e);
            return null;
        }
    }

    private static DocumentoPdf.Comprobacion deFactorizacion(Verificacion v, String bien, String mal) {
        if (v == null) {
            return null;
        }
        String metodo = v.numerica() ? "Evaluando con " + EvaluacionNumerica.CIFRAS + " cifras" : "Por expansión";
        List<String> lineas = new ArrayList<>();
        if (!v.numerica()) {
            lineas.addAll(lineas(v.productos()));
            if (v.resultado() != null && v.expandido() != null) {
                lineas.add(HistorialService.latexDe(v.resultado()) + " = " + HistorialService.latexDe(v.expandido()));
            }
        }
        return new DocumentoPdf.Comprobacion(metodo, lineas, v.coincide(), v.coincide() ? bien : mal);
    }

    private static DocumentoPdf.Comprobacion deIntegral(ResolucionIntegral r) {
        ResolucionIntegral.Verificacion v = r.verificacion();
        if (v == null || !r.resuelta() || r.antiderivada() == null) {
            return null;
        }
        char x = r.variable();
        String linea = "\\frac{d}{d" + x + "}\\left(" + ImpresoraLatex.imprimir(r.antiderivada()) + "\\right) = "
                + ImpresoraLatex.imprimir(v.derivada());
        String mensaje = !v.coincide() ? "La derivada no coincide con el integrando"
                : v.exacta() ? "La derivada coincide con el integrando"
                : "La derivada coincide con el integrando en cinco puntos";
        return new DocumentoPdf.Comprobacion("Derivando el resultado", List.of(linea), v.coincide(), mensaje);
    }

    /** "(x - 1)^2 = x^2 - 2x + 1" → LaTeX. */
    private static List<String> lineas(List<String> textos) {
        return textos.stream().map(HistorialService::latexDe).toList();
    }
}
