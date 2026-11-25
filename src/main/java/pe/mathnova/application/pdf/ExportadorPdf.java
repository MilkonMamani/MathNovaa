package pe.mathnova.application.pdf;

import java.io.IOException;
import java.nio.file.Path;

public interface ExportadorPdf {

    void exportar(DocumentoPdf documento, Path destino) throws IOException;
}
