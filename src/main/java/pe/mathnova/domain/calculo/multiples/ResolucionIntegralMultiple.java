package pe.mathnova.domain.calculo.multiples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.IntegralMultiple;

/**
 * Una integral doble o triple resuelta (HU31, RF56).
 *
 * @param entrada     lo que se escribió
 * @param efectiva    la que se integró de verdad: con el orden invertido o en otras coordenadas, sus rangos son
 *                    los de la región que se dibuja; su cuerpo es el de la entrada
 * @param integrando  lo que se integra paso a paso: en coordenadas, f ya cambiada y por el jacobiano
 * @param jacobiano   el jacobiano simplificado (r, ρ² sen φ); null en cartesianas
 * @param valor       el valor exacto; null si no se pudo
 * @param invertida   si se cambió el orden de integración
 * @param motivo      por qué no se resolvió, o null
 */
public record ResolucionIntegralMultiple(IntegralMultiple entrada, IntegralMultiple efectiva, Expresion integrando,
                                         Expresion jacobiano, Expresion valor, boolean invertida,
                                         List<PasoLimite> pasos, String motivo) {

    public ResolucionIntegralMultiple {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(efectiva, "efectiva");
        pasos = List.copyOf(pasos);
    }

    public boolean resuelta() {
        return valor != null;
    }

    public IntegralMultiple.Coordenadas coordenadas() {
        return efectiva.coordenadas();
    }

    /** "1/2", "π/2", o el motivo. */
    public String texto() {
        return resuelta() ? ImpresoraPizarra.imprimir(valor) : motivo;
    }

    public String latex() {
        String izq = ImpresoraLatex.imprimir(entrada);
        return resuelta() ? izq + " = " + ImpresoraLatex.imprimir(valor) : izq;
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }
}
