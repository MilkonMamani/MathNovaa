package pe.mathnova.domain.sugerencias;

import java.util.List;

import pe.mathnova.domain.calculo.parciales.MotorParciales;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;

/**
 * RN41 (Sprint 7): una expresión con dos o más variables sugiere sus derivadas parciales y su gradiente. Desde el
 * Sprint 8, con dos variables también los puntos críticos y extremos (el Hessiano) y la integral doble sobre
 * [0, 1] × [0, 1]; con tres, los extremos quedan desactivados (el criterio del Hessiano es para dos variables).
 */
final class ParcialesDeVariasVariables implements ReglaSugerencia {

    static final String CODIGO = "RN41";

    @Override
    public String codigo() {
        return CODIGO;
    }

    @Override
    public boolean variasVariables() {
        return true;
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        return de(r.entrada());
    }

    /** Las cuatro tarjetas para f; vacío si f tiene menos de dos letras. */
    static List<Sugerencia> de(Expresion f) {
        List<Character> letras = MotorParciales.variables(f, 0);
        if (letras.size() < 2) {
            return List.of();
        }
        String texto = ImpresoraTexto.imprimir(f);
        char x = letras.getFirst();
        String parcial = "∂/∂" + x + "(" + texto + ")";
        String grad = "grad(" + texto + ")";
        String extremos = "extremos(" + texto + ")";
        boolean dos = letras.size() == 2;
        String doble = "∬(" + texto + ", " + letras.getFirst() + ", 0, 1, " + letras.get(1) + ", 0, 1)";
        String nombres = String.join(", ", letras.stream().map(String::valueOf).toList());
        return List.of(
                new Sugerencia(CODIGO, "DERIVADA_PARCIAL", "Derivadas parciales",
                        "Tiene " + letras.size() + " variables (" + nombres + "): se deriva respecto de cada una "
                                + "tratando las demás como constantes.", parcial, Reglas.latex(parcial), true),
                new Sugerencia(CODIGO, "GRADIENTE", "Gradiente",
                        "∇f reúne las " + letras.size() + " derivadas parciales en un vector: apunta hacia donde f "
                                + "crece más rápido.", grad, Reglas.latex(grad), true),
                new Sugerencia(CODIGO, "EXTREMOS", "Puntos críticos y extremos",
                        "∇f = 0 da los puntos críticos y la matriz hessiana los clasifica en máximos, mínimos o "
                                + "puntos de silla.", extremos, Reglas.latex(extremos), dos),
                new Sugerencia(CODIGO, "INTEGRAL_MULTIPLE", "Integral doble",
                        "Se integra f sobre una región del plano, una variable a la vez (integral iterada).", doble,
                        Reglas.latex(doble), dos));
    }
}
