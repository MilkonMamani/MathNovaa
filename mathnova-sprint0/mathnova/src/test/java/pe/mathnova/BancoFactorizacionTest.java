package pe.mathnova;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Factorizador;
import pe.mathnova.domain.factorizacion.FactorizadorPendiente;

/**
 * Los 300 casos del banco. En el Sprint 0 fallan todos a propósito;
 * cada sprint pasa a verde su parte. Se corre con: mvn test -Pbanco
 */
@Tag("banco")
class BancoFactorizacionTest {

    private final Factorizador factorizador = new FactorizadorPendiente();

    @ParameterizedTest(name = "{0} {1} [{3}]: {6}")
    @CsvFileSource(resources = "/banco_factorizacion.csv", numLinesToSkip = 1,
            encoding = "UTF-8", maxCharsPerColumn = 8192)
    void factoriza(String id, String metodo, String pasos, String campo, String curso,
                   int dificultad, String entrada, String esperado, String nota) {
        String obtenido = factorizador.factorizar(entrada, Campo.valueOf(campo));
        assertEquals(esperado, obtenido, () -> id + " " + metodo + " · pasos esperados: " + pasos);
    }
}
