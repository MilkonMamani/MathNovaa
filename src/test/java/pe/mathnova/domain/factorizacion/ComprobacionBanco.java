package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.parser.Parser;

/**
 * Cómo se comprueba cada fila del banco, según su método principal (Sprints 2 a 7):
 * <ul>
 *   <li>F01–F20 y M01–M07 en Q: los mismos factores (en cualquier orden, ComparadorFactorizacion) y el
 *       mismo polinomio; en M01–M07 además el primer paso es el del banco y los factores finales quedan
 *       certificados (A6);</li>
 *   <li>F21–F30 en R y C: por estructura en el campo, con la verificación de la Parte E;</li>
 *   <li>A4: un solo factor, certificado; A5: las mismas raíces aproximadas; P01: la misma fracción y las
 *       mismas restricciones; T01–T05: con cada función como una letra.</li>
 * </ul>
 * Lo usan BancoCompletoTest y BancoFactorizacionTest.
 */
public final class ComprobacionBanco {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizadorFunciones FUNCIONES = new FactorizadorFunciones(MOTOR);

    private ComprobacionBanco() {
    }

    public static void comprobar(BancoCsv.Fila f) {
        String m = f.metodo();
        if (m.startsWith("M")) {
            varias(f);
        } else if (m.equals("A4")) {
            irreducible(f);
        } else if (m.equals("A5")) {
            aproximada(f);
        } else if (m.equals("P01")) {
            fraccion(f);
        } else if (m.startsWith("T")) {
            funciones(f);
        } else if (f.campo().equals("Q")) {
            enQ(f);
        } else {
            enCampo(f);
        }
    }

    private static void enQ(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        assertEquals(ComparadorFactorizacion.leer(f.resultadoSinNota()), ComparadorFactorizacion.leer(obtenido),
                () -> f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido);
        assertEquals(Polinomio.desde(Parser.parse(f.entrada())), Polinomio.desde(Parser.parse(obtenido)), f.id());
    }

    private static void varias(BancoCsv.Fila f) {
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), Campo.Q);
        String obtenido = r.texto();
        assertAll(f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido + " (banco: " + f.resultadoSinNota() + ")",
                () -> assertEquals(ComparadorFactorizacion.leer(f.resultadoSinNota()), ComparadorFactorizacion.leer(obtenido)),
                () -> assertEquals(Polinomio.desde(Parser.parse(f.entrada())), Polinomio.desde(Parser.parse(obtenido))),
                () -> assertEquals(f.metodo(), r.pasos().getFirst().codigoTecnica(), "primer paso"),
                () -> assertTrue(r.irreducibles().stream().allMatch(Irreducibilidad::certificada),
                        () -> "factores finales sin certificar: " + r.irreducibles()));
    }

    private static void enCampo(BancoCsv.Fila f) {
        Campo campo = Campo.valueOf(f.campo());
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), campo);
        String obtenido = r.texto();
        assertAll(f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido + " (banco: " + f.resultadoSinNota() + ")",
                () -> assertTrue(ComparadorFactorizacion.igualesEnCampo(
                        ComparadorFactorizacion.leerEnCampo(f.resultadoSinNota()),
                        ComparadorFactorizacion.leerEnCampo(obtenido)), "estructura"),
                () -> assertTrue(VerificacionCampo.de(r).coincide(), "verificación (Parte E)"),
                () -> assertEquals(campo == Campo.R ? "PR" : "PC", r.extension().pasos().getFirst().codigoTecnica(),
                        "primero Q, luego pasar al campo"));
    }

    private static void irreducible(BancoCsv.Fila f) {
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), Campo.Q);
        assertAll(f.id() + " " + f.entrada(),
                () -> assertTrue(r.pasos().isEmpty()),
                () -> assertEquals(1, r.irreducibles().size()),
                () -> assertTrue(r.irreducibles().getFirst().certificada()));
    }

    private static void aproximada(BancoCsv.Fila f) {
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), Campo.valueOf(f.campo()));
        String obtenido = r.texto();
        assertAll(f.id() + " " + f.entrada() + " → " + obtenido,
                () -> assertTrue(obtenido.startsWith("Irreducible en Q; raíces reales ≈ ")),
                () -> assertEquals(raices(f.resultadoEsperado()), raices(obtenido)),
                () -> assertTrue(r.extension().aproximada()),
                () -> assertTrue(r.extension().pasos().stream().anyMatch(p -> p.codigoTecnica().equals("A5"))));
    }

    private static List<BigDecimal> raices(String texto) {
        String lista = texto.substring(texto.indexOf('≈') + 1);
        return Arrays.stream(lista.split(",")).map(String::trim).map(BigDecimal::new).toList();
    }

    private static void fraccion(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        String[] esperado = f.resultadoEsperado().split(";", 2);
        String[] partes = obtenido.split(";", 2);
        assertAll(f.id() + " " + f.entrada() + " → " + obtenido,
                () -> assertEquals(ComparadorFactorizacion.leerFraccion(esperado[0].trim()),
                        ComparadorFactorizacion.leerFraccion(partes[0].trim())),
                () -> assertEquals(restricciones(esperado[1]), restricciones(partes.length > 1 ? partes[1] : "")));
    }

    private static Set<String> restricciones(String texto) {
        return Arrays.stream(texto.split(","))
                .map(s -> s.replace('−', '-').replaceAll("\\s+", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static void funciones(BancoCsv.Fila f) {
        ResolucionFunciones r = FUNCIONES.resolver(Parser.parse(f.entrada()));
        String obtenido = r.formula();
        var letras = new HashMap<String, Character>();
        assertAll(f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido + " (banco: " + f.resultadoSinNota() + ")",
                () -> assertTrue(r.factorizable(), () -> r.mensaje()),
                () -> assertEquals(ComparadorFactorizacion.leer(SustitucionFunciones.aLetras(Parser.parse(f.resultadoSinNota()), letras)),
                        ComparadorFactorizacion.leer(SustitucionFunciones.aLetras(Parser.parse(obtenido), letras))),
                () -> assertEquals(f.metodo(), r.codigo()));
    }
}
