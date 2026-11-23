package pe.mathnova.domain.sugerencias;

import java.util.List;

import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.parser.Parser;

/**
 * Las reglas RN15 a RN19 (HU10) y RN41 (Sprint 7), cada una en su propia clase. RN20 la aplica {@link MotorSugerencias}.
 */
public final class Reglas {

    private Reglas() {
    }

    /** Las reglas en el orden en que se muestran sus tarjetas. */
    public static List<ReglaSugerencia> todas() {
        return List.of(new LimiteIndeterminado(), new FraccionesParciales(), new IntegralConDivision(),
                new DerivadaDePolinomio(), new IntegralDePolinomio(), new AsintotasDeRacional(),
                new DerivadaDeFunciones(), new ParcialesDeVariasVariables());
    }

    /** La plantilla en LaTeX, para la tarjeta. */
    static String latex(String plantilla) {
        return ImpresoraLatex.imprimir(Parser.parse(plantilla));
    }
}
