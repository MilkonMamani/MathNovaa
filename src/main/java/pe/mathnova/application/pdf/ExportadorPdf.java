package pe.mathnova.application.pdf;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Puerto de salida (Sprint 9, parte C): escribe un {@link DocumentoPdf} en un archivo. La implementación vive en
 * infraestructura (PDFBox); el dominio no sabe que existe un PDF.
 */
public interface ExportadorPdf {

    /**
     * A4 vertical, en blanco y negro con azul; las fórmulas en vectores; un paso nunca se corta entre dos páginas.
     *
     * @throws IOException si no se puede escribir el archivo
     */
    void exportar(DocumentoPdf documento, Path destino) throws IOException;
}
