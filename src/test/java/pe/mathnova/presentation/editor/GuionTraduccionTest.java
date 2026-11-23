package pe.mathnova.presentation.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;

/**
 * El guion de demostración sin pantalla: el LaTeX guardado de cada caso (el que arman las teclas,
 * capturado en PruebaHumoTest) se traduce al texto esperado, y ese texto tiene un plan que se puede
 * resolver o, si el caso es de error, uno inválido.
 */
class GuionTraduccionTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final ResolverService RESOLVER = new ResolverService(new FactorizacionService(MOTOR, null), MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    static Stream<Arguments> guion() {
        return Guion.casos().stream().map(Arguments::of);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("guion")
    void elLatexDelEditorSeTraduceAlTextoDelGuion(Guion.Caso c) {
        String latex = c.latex();
        assertNotNull(latex, c.id() + ": falta el fixture");
        String texto = TraductorLatex.traducir(latex);
        assertEquals(c.texto(), texto, latex);
        assertFalse(texto.contains(TraductorLatex.CASILLA), texto);
        ResolverService.Plan plan = RESOLVER.decidir(texto);
        if (c.aviso() != null) {
            ResolverService.Plan.Invalido inv = assertInstanceOf(ResolverService.Plan.Invalido.class, plan);
            assertEquals(true, inv.mensaje().contains(c.aviso()), inv.mensaje());
        } else {
            assertEquals(true, plan.resoluble(), texto + " → " + plan);
        }
    }
}
