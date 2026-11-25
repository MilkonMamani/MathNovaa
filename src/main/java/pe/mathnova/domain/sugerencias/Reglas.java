package pe.mathnova.domain.sugerencias;

import java.util.List;

import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.parser.Parser;

public final class Reglas {

    private Reglas() {
    }

    public static List<ReglaSugerencia> todas() {
        return List.of(new LimiteIndeterminado(), new FraccionesParciales(), new IntegralConDivision(),
                new DerivadaDePolinomio(), new IntegralDePolinomio(), new AsintotasDeRacional(),
                new DerivadaDeFunciones(), new ParcialesDeVariasVariables());
    }

    static String latex(String plantilla) {
        return ImpresoraLatex.imprimir(Parser.parse(plantilla));
    }
}
