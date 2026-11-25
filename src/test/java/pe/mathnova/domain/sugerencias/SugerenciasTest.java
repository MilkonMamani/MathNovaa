package pe.mathnova.domain.sugerencias;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.calculo.limites.MotorLimites;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parte E del Sprint 5 (HU10, RN15–RN20). */
class SugerenciasTest {

    private static final OrquestadorFactorizacion ORQUESTADOR = new OrquestadorFactorizacion();
    private static final MotorSugerencias MOTOR = new MotorSugerencias();

    private static List<Sugerencia> deFraccion(String texto) {
        var s = new SimplificadorFracciones(ORQUESTADOR).simplificar(Parser.parse(texto));
        return MOTOR.sugerir(EjercicioResuelto.de(s).orElseThrow());
    }

    private static List<Sugerencia> dePolinomio(String texto) {
        var r = ORQUESTADOR.resolver(Parser.parse(texto), Campo.Q);
        return MOTOR.sugerir(EjercicioResuelto.de(r).orElseThrow());
    }

    private static List<String> reglas(List<Sugerencia> s) {
        return s.stream().map(Sugerencia::regla).toList();
    }

    @Test
    void ceroSobreCeroSugiereElLimite() {
        List<Sugerencia> s = deFraccion("(x^2 - 4)/(x - 2)");
        Sugerencia limite = s.getFirst();
        assertEquals("RN15", limite.regla());
        assertEquals("LIMITE", limite.operacion());
        assertEquals("Límite cuando x → 2", limite.titulo());
        assertTrue(limite.justificacion().contains("indeterminación 0/0"), limite.justificacion());
        assertEquals("lim(x→2, (x^2 - 4)/(x - 2))", limite.plantilla());
        assertTrue(limite.disponible());
        // La plantilla se resuelve con la cascada y da 4.
        var r = new MotorLimites(ORQUESTADOR).resolver((Limite) Parser.parse(limite.plantilla()));
        assertEquals("4", r.resultado().texto());
        // Al simplificarse queda x + 2: no hay asíntotas que sugerir.
        assertFalse(reglas(s).contains("RN19"));
    }

    /** x = 1 se cancela (límite activo) y x = 2 queda abajo (asíntota vertical). */
    @Test
    void ceroSobreCeroYAsintotaVertical() {
        String texto = "(x^2 - 1)/(x^2 - 3x + 2)";
        List<Sugerencia> s = deFraccion(texto);
        List<Sugerencia> limites = s.stream().filter(x -> x.regla().equals("RN15")).toList();
        assertEquals(1, limites.size(), "Solo x − 1 se cancela");
        Sugerencia limite = limites.getFirst();
        assertEquals("Límite cuando x → 1", limite.titulo());
        assertEquals("lim(x→1, " + texto + ")", limite.plantilla());
        assertTrue(limite.disponible());
        var r = new MotorLimites(ORQUESTADOR).resolver((Limite) Parser.parse(limite.plantilla()));
        assertEquals("-2", r.resultado().texto());

        Sugerencia asintotas = s.stream().filter(x -> x.regla().equals("RN19")).findFirst().orElseThrow();
        assertEquals("ASINTOTAS", asintotas.operacion());
        assertTrue(asintotas.disponible());
        assertEquals(texto, asintotas.plantilla());
        // Lo que abre la pestaña Análisis: la asíntota vertical en x = 2 (x = 1 es un hueco, no asíntota).
        var simp = new SimplificadorFracciones(ORQUESTADOR).simplificar(Parser.parse(asintotas.plantilla()));
        var a = pe.mathnova.domain.calculo.limites.Asintotas.de(simp).orElseThrow();
        assertEquals(List.of("x = 2"), a.textoVerticales());
    }

    /** Si se cancelan varios factores, una tarjeta de límite por cada raíz, de menor a mayor. */
    @Test
    void unaTarjetaDeLimitePorCadaFactorCancelado() {
        List<Sugerencia> limites = deFraccion("((x - 1)(x + 3)(x - 5))/((x - 1)(x + 3))").stream()
                .filter(x -> x.regla().equals("RN15")).toList();
        assertEquals(List.of("Límite cuando x → -3", "Límite cuando x → 1"),
                limites.stream().map(Sugerencia::titulo).toList());
        assertTrue(limites.stream().allMatch(Sugerencia::disponible));
        assertTrue(limites.stream().allMatch(x -> x.justificacion().contains("indeterminación 0/0")));
    }

    @Test
    void fraccionPropiaSugiereFraccionesParcialesActiva() {
        List<Sugerencia> s = deFraccion("(3x + 5)/((x - 1)(x + 2))");
        Sugerencia parciales = s.stream().filter(x -> x.regla().equals("RN16")).findFirst().orElseThrow();
        assertEquals("Integral por fracciones parciales", parciales.titulo());
        assertEquals("INTEGRAR", parciales.operacion());
        assertTrue(parciales.disponible()); // Sprint 6: ya se resuelve
        assertFalse(reglas(s).contains("RN15"), "Nada se cancela: no hay 0/0");
        assertFalse(reglas(s).contains("RN17"));
        Sugerencia asintotas = s.stream().filter(x -> x.regla().equals("RN19")).findFirst().orElseThrow();
        assertTrue(asintotas.disponible());
    }

    @Test
    void polinomioSugiereLaDerivadaYNuncaElLimite() {
        List<Sugerencia> s = dePolinomio("x^3 - 2x^2 - x + 2");
        Sugerencia derivada = s.getFirst();
        assertEquals("RN18", derivada.regla());
        assertEquals("Derivada y puntos críticos", derivada.titulo());
        assertTrue(derivada.disponible());
        assertTrue(derivada.justificacion().contains("no hace falta factorizarlo"));
        assertTrue(s.stream().anyMatch(x -> x.titulo().equals("Integral término a término") && x.disponible()));
        assertTrue(s.stream().noneMatch(x -> x.operacion().equals("LIMITE")));
        assertTrue(s.stream().noneMatch(x -> x.operacion().equals("ASINTOTAS")));
    }

    @Test
    void fraccionImpropiaSugiereDividirPrimero() {
        List<String> r = reglas(deFraccion("(x^2 + 1)/(x - 1)"));
        assertTrue(r.contains("RN17"));
        assertTrue(r.contains("RN19"));
        assertFalse(r.contains("RN16"));
    }

    @Test
    void rn20NuncaOfreceComoDisponibleLoQueNoSeResuelve() {
        for (String t : List.of("(x^2 - 4)/(x - 2)", "(3x + 5)/((x - 1)(x + 2))", "(x^2 + 1)/(x - 1)")) {
            deFraccion(t).forEach(s -> assertEquals(MotorSugerencias.DISPONIBLES.contains(s.operacion()), s.disponible()));
        }
        dePolinomio("x^2 - 1").forEach(s -> assertEquals(MotorSugerencias.DISPONIBLES.contains(s.operacion()),
                s.disponible()));
    }

    // ------------------------------------------------------------------ R y C

    private static List<Sugerencia> enCampo(String texto, Campo campo) {
        var r = ORQUESTADOR.resolver(Parser.parse(texto), campo);
        assertEquals(campo, r.campo());
        return MOTOR.sugerir(EjercicioResuelto.de(r).orElseThrow());
    }

    /** Las reglas no dependen del campo: x² − 3 en R da las mismas sugerencias que en Q. */
    @Test
    void enRSugiereLoMismoQueEnQ() {
        List<Sugerencia> enR = enCampo("x^2 - 3", Campo.R);
        assertFalse(enR.isEmpty());
        assertEquals(dePolinomio("x^2 - 3"), enR);
        assertEquals(List.of("Derivada y puntos críticos", "Integral término a término"),
                enR.stream().map(Sugerencia::titulo).toList());
    }

    /** x² + 4 en C da las mismas sugerencias que en Q. */
    @Test
    void enCSugiereLoMismoQueEnQ() {
        List<Sugerencia> enC = enCampo("x^2 + 4", Campo.C);
        assertFalse(enC.isEmpty());
        assertEquals(dePolinomio("x^2 + 4"), enC);
        assertEquals(List.of("Derivada y puntos críticos", "Integral término a término"),
                enC.stream().map(Sugerencia::titulo).toList());
    }

    /** El servicio tampoco filtra por campo: "Qué más puedes hacer" sale en Q, R y C. */
    @Test
    void elServicioSugiereEnLosTresCampos() {
        var servicio = new pe.mathnova.application.service.FactorizacionService(ORQUESTADOR);
        for (String texto : List.of("x^2 - 3", "x^2 + 4")) {
            List<Sugerencia> enQ = servicio.sugerencias(ORQUESTADOR.resolver(Parser.parse(texto), Campo.Q));
            assertFalse(enQ.isEmpty(), texto);
            for (Campo campo : List.of(Campo.R, Campo.C)) {
                assertEquals(enQ, servicio.sugerencias(ORQUESTADOR.resolver(Parser.parse(texto), campo)),
                        texto + " en " + campo);
            }
        }
    }

    /** Después de factorizar una expresión con funciones también hay sugerencias. */
    @Test
    void funcionesTrascendentesTambienTienenSugerencias() {
        var funciones = new pe.mathnova.domain.factorizacion.FactorizadorFunciones(ORQUESTADOR);
        var r = funciones.resolver(Parser.parse("2sen^2(x) - sen(x) - 1"));
        List<Sugerencia> s = MOTOR.sugerir(EjercicioResuelto.de(r).orElseThrow());
        Sugerencia derivada = s.getFirst();
        assertEquals("RN18", derivada.regla());
        assertEquals("Derivada y puntos críticos", derivada.titulo());
        assertTrue(derivada.justificacion().contains("u = sen(x)"), derivada.justificacion());
        assertEquals(Parser.parse("d/dx(2sen^2(x) - sen(x) - 1)"), Parser.parse(derivada.plantilla()));
        assertTrue(derivada.disponible());
        // Las de fracciones y las de polinomios no se cuelan.
        assertTrue(s.stream().noneMatch(x -> Set.of("RN15", "RN16", "RN17", "RN19").contains(x.regla())));
        // Lo que no se factorizó no tiene sugerencias.
        assertTrue(EjercicioResuelto.de(funciones.resolver(Parser.parse("x + sen(x)"))).isEmpty());
    }
}
