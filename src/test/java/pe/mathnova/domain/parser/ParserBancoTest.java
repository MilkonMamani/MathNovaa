package pe.mathnova.domain.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.domain.BancoCsv;

/**
 * Definición de terminado 1: el parser lee las 590 expresiones del banco (columnas entrada y
 * resultado_esperado, salvo las que empiezan con "Irreducible"; se ignora lo que va después de ";").
 */
class ParserBancoTest {

    static List<Arguments> expresiones() {
        List<Arguments> lista = new ArrayList<>();
        for (BancoCsv.Fila f : BancoCsv.filas()) {
            lista.add(Arguments.of(f.id(), "entrada", f.entrada()));
            String esperado = f.resultadoSinNota();
            if (!esperado.startsWith("Irreducible")) {
                lista.add(Arguments.of(f.id(), "resultado_esperado", esperado));
            }
        }
        return lista;
    }

    @ParameterizedTest(name = "{0} {1}: {2}")
    @MethodSource("expresiones")
    void leeLaExpresion(String id, String columna, String texto) {
        assertDoesNotThrow(() -> Parser.parse(texto), () -> id + " " + columna);
    }

    @Test
    void sonQuinientasNoventa() {
        assertEquals(590, expresiones().size());
    }
}
