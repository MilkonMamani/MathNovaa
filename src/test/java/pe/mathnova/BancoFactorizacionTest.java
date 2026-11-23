package pe.mathnova;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.factorizacion.ComprobacionBanco;

/**
 * Los 300 casos del banco, leídos directo del CSV. Desde el Sprint 7 el banco pasa completo y esta prueba
 * corre con "mvn test" (ya no lleva @Tag("banco")). No compara el texto letra por letra: el orden de los
 * factores en R y C, los mensajes de A4 y las funciones (T01–T05) se comprueban como en BancoCompletoTest.
 */
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
