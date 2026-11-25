package pe.mathnova;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.factorizacion.ComprobacionBanco;

class BancoFactorizacionTest {

    @ParameterizedTest(name = "{0} {1} [{3}]: {6}")
    @CsvFileSource(resources = "/banco_factorizacion.csv", numLinesToSkip = 1,
            encoding = "UTF-8", maxCharsPerColumn = 8192)
    void factoriza(String id, String metodo, String pasos, String campo, String curso,
                   String dificultad, String entrada, String esperado, String nota) {
        ComprobacionBanco.comprobar(new BancoCsv.Fila(id, metodo, pasos, campo, curso, dificultad, entrada,
                esperado, nota == null ? "" : nota));
    }
}
