package pe.mathnova.domain.calculo.parciales;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;

/**
 * Una derivada parcial, un gradiente, una derivada direccional o una regla de la cadena resuelta
 * (HU28, RF52, RF53).
 *
 * @param entrada    lo que se escribió (para la cadena, la derivada d/dt(f))
 * @param funcion    f
 * @param variables  las letras de f, en orden (x, y, z…)
 * @param resultado  el escalar: la parcial, D_u f(P) o df/dt; null en un gradiente
 * @param vector     el gradiente (ya evaluado si se pidió en un punto); vacío si no es un gradiente
 * @param pasos      V01–V04, con las reglas de derivación anidadas
 * @param verificada si coincide con la derivada numérica (cociente incremental con 60 cifras)
 * @param dominio    las restricciones de f (RF52): "x + y > 0"; vacío si no hay
 */
public record ResolucionParcial(Expresion entrada, Tipo tipo, Expresion funcion, List<Character> variables,
                                Expresion resultado, List<Expresion> vector, List<PasoLimite> pasos,
                                boolean verificada, List<String> dominio) {

    public enum Tipo {
        PARCIAL("V01"), GRADIENTE("V02"), DIRECCIONAL("V03"), CADENA("V04");

        private final String codigo;

        Tipo(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    public ResolucionParcial {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(funcion, "funcion");
        variables = List.copyOf(variables);
        vector = List.copyOf(vector);
        pasos = List.copyOf(pasos);
        dominio = List.copyOf(dominio);
    }

    /** "2xy + 3y²", "(2x + yz, xz, xy)", "22/5". */
    public String texto() {
        if (tipo == Tipo.GRADIENTE) {
            return "(" + String.join(", ", vector.stream().map(ImpresoraPizarra::imprimir).toList()) + ")";
        }
        return ImpresoraPizarra.imprimir(resultado);
    }

    /** "\frac{\partial}{\partial x}… = …". */
    public String latex() {
        String derecha = tipo == Tipo.GRADIENTE
                ? "\\left(" + String.join(", ", vector.stream().map(ImpresoraLatex::imprimir).toList()) + "\\right)"
                : ImpresoraLatex.imprimir(resultado);
        return ImpresoraLatex.imprimir(entrada) + " = " + derecha;
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
