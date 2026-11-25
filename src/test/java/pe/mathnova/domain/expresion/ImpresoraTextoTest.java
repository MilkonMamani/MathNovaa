package pe.mathnova.domain.expresion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.parser.Parser;

class ImpresoraTextoTest {

    private static final Set<String> ESTILO_DISTINTO = Set.of(
            "BP013 resultado", "BP014 resultado", "BP015 resultado", "BP018 resultado", "BP020 resultado",
            "BP219 resultado", "BP220 resultado");

    static List<Arguments> expresiones() {
        List<Arguments> lista = new ArrayList<>();
        for (BancoCsv.Fila f : BancoCsv.filas()) {
            lista.add(Arguments.of(f.id() + " entrada", f.entrada()));
            String esperado = f.resultadoSinNota();
            if (!esperado.startsWith("Irreducible")) {
                lista.add(Arguments.of(f.id() + " resultado", esperado));
            }
        }
        return lista;
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("expresiones")
    void idaYVuelta(String id, String texto) {
        Expresion arbol = Parser.parse(texto);
        String impreso = ImpresoraTexto.imprimir(arbol);
        assertEquals(arbol, Parser.parse(impreso), () -> id + " se imprimió como " + impreso);
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("expresiones")
    void formatoDelBanco(String id, String texto) {
        assumeFalse(ESTILO_DISTINTO.contains(id));
        assertEquals(texto, ImpresoraTexto.imprimir(Parser.parse(texto)), id);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x²+3x          | x^2 + 3x",
            "2*x*y          | 2xy",
            "sen 2x         | sen(2x)",
            "sen^2(x)       | sen(x)^2",
            "log x          | log(x)",
            "log_2 x        | log_2(x)",
            "x^-1           | x^-1",
            "-x^2           | -x^2",
            "-(x + 1)       | -(x + 1)",
            "x - (y - z)    | x - (y - z)",
            "2^3^2          | 2^3^2",
            "(x^2)^3        | (x^2)^3",
            "x/(2y)         | x/(2y)",
            "√(x + 1)       | √(x + 1)"
    })
    void normalizaLaEscritura(String entrada, String esperado) {
        assertEquals(esperado, ImpresoraTexto.imprimir(Parser.parse(entrada)));
    }
}
