package pe.mathnova.domain.factorizacion;

import java.util.Map;

/** Nombres de las técnicas de factorización (Q, R, C y funciones), iguales a los del catálogo de V2. */
public final class Tecnicas {

    private static final Map<String, String> NOMBRES = Map.ofEntries(
            Map.entry("F01", "Factor común monomio"),
            Map.entry("F02", "Factor común con coeficientes fraccionarios"),
            Map.entry("F03", "Factor común polinomio"),
            Map.entry("F04", "Agrupación de términos"),
            Map.entry("F05", "Diferencia de cuadrados"),
            Map.entry("F06", "Diferencia de cuadrados repetida"),
            Map.entry("F07", "Trinomio cuadrado perfecto"),
            Map.entry("F08", "Suma de cubos"),
            Map.entry("F09", "Diferencia de cubos"),
            Map.entry("F10", "Cubo perfecto de binomio"),
            Map.entry("F11", "Suma o diferencia de potencias enésimas"),
            Map.entry("F12", "Trinomio x² + bx + c"),
            Map.entry("F13", "Trinomio ax² + bx + c (aspa simple)"),
            Map.entry("F14", "Cambio de variable u = xⁿ"),
            Map.entry("F15", "Quita y pon"),
            Map.entry("F16", "Ruffini (divisores binómicos)"),
            Map.entry("F17", "Ruffini con raíces repetidas"),
            Map.entry("F18", "Polinomio recíproco"),
            Map.entry("F19", "Aspa doble especial"),
            Map.entry("F20", "Coeficientes indeterminados"),
            Map.entry("F21", "Diferencia de cuadrados con radicales"),
            Map.entry("F22", "Completar cuadrados"),
            Map.entry("F23", "Fórmula general"),
            Map.entry("F24", "Suma de cuadrados en C"),
            Map.entry("F25", "Fórmula general con Δ < 0"),
            Map.entry("F26", "Suma o diferencia de cubos con radicales"),
            Map.entry("F27", "Método de Cardano"),
            Map.entry("F28", "Método de Ferrari"),
            Map.entry("F29", "Raíces conjugadas en un cuadrático real"),
            Map.entry("F30", "Fórmula de De Moivre"),
            Map.entry("A5", "Aproximación numérica de raíces"),
            Map.entry("PR", "Pasar a R"),
            Map.entry("PC", "Pasar a C"),
            Map.entry("T01", "Factor común de una función"),
            Map.entry("T02", "Sustitución u = eˣ"),
            Map.entry("T03", "Sustitución trigonométrica"),
            Map.entry("T04", "Identidades trigonométricas"),
            Map.entry("T05", "Sustitución u = ln x"),
            Map.entry("A1", "Discriminante y completar cuadrados"),
            Map.entry("A4", "Verificación de irreducibilidad"),
            Map.entry("P01", "Simplificar fracción algebraica"),
            Map.entry("P02", "División de polinomios"),
            Map.entry("L01", "Evaluación directa"),
            Map.entry("L02", "Factorizar y cancelar"),
            Map.entry("L03", "Racionalizar"),
            Map.entry("L04", "Límite al infinito por grados"),
            Map.entry("L05", "Límites laterales"),
            Map.entry("L06", "Límite notable"),
            Map.entry("L07", "Regla de L'Hôpital"),
            Map.entry("D01", "Derivada de una constante"),
            Map.entry("D02", "Regla de la potencia"),
            Map.entry("D03", "Regla de la suma"),
            Map.entry("D04", "Regla del producto"),
            Map.entry("D05", "Regla del cociente"),
            Map.entry("D06", "Regla de la cadena"),
            Map.entry("D07", "Derivada de función trascendente"),
            Map.entry("D08", "Derivación implícita"),
            Map.entry("D09", "Derivación logarítmica"),
            Map.entry("SIMP", "Simplificar el resultado"),
            Map.entry("PCR", "Puntos críticos (f'(x) = 0)"),
            Map.entry("EC", "Resolver la ecuación"),
            Map.entry("EXP", "Expandir el polinomio"),
            Map.entry("PROD", "Cociente como producto f·g⁻¹"),
            Map.entry("EXPO", "Potencia como exponencial e^(g·ln f)"),
            Map.entry("K", "Constante fuera de la integral"),
            Map.entry("I01", "Integral inmediata"),
            Map.entry("I02", "Sustitución"),
            Map.entry("I03", "Por partes (ILATE)"),
            Map.entry("I04", "Integral trigonométrica"),
            Map.entry("I05", "Sustitución trigonométrica"),
            Map.entry("I06", "Fracciones parciales"),
            Map.entry("I07", "Regla de Barrow"),
            Map.entry("M01", "Factor común (varias variables)"),
            Map.entry("M02", "Agrupación (varias variables)"),
            Map.entry("M03", "Productos notables en dos variables"),
            Map.entry("M04", "Aspa simple homogénea"),
            Map.entry("M05", "Aspa doble"),
            Map.entry("M06", "Polinomio homogéneo"),
            Map.entry("M07", "Ruffini respecto de una variable"),
            Map.entry("M08", "Coeficientes indeterminados multivariable"),
            Map.entry("A6", "Factorización multivariable"),
            Map.entry("V01", "Derivada parcial"),
            Map.entry("V02", "Gradiente"),
            Map.entry("V03", "Derivada direccional"),
            Map.entry("V04", "Regla de la cadena multivariable"),
            Map.entry("V05", "Límite por trayectorias"),
            Map.entry("V06", "Límite en coordenadas polares"),
            Map.entry("V07", "Criterio del Hessiano"),
            Map.entry("V08", "Multiplicadores de Lagrange"),
            Map.entry("V09", "Integral iterada"),
            Map.entry("V10", "Cambio de coordenadas con jacobiano"));

    /** Nombres cortos para los títulos; el largo queda como ayuda. */
    private static final Map<String, String> CORTOS = Map.ofEntries(
            Map.entry("F01", "Factor común"),
            Map.entry("F02", "Factor común con fracciones"),
            Map.entry("F03", "Factor común polinomio"),
            Map.entry("F04", "Agrupación"),
            Map.entry("F05", "Diferencia de cuadrados"),
            Map.entry("F06", "Diferencia de cuadrados"),
            Map.entry("F07", "Trinomio cuadrado perfecto"),
            Map.entry("F08", "Suma de cubos"),
            Map.entry("F09", "Diferencia de cubos"),
            Map.entry("F10", "Cubo perfecto"),
            Map.entry("F11", "Potencias enésimas"),
            Map.entry("F12", "Trinomio simple"),
            Map.entry("F13", "Aspa simple"),
            Map.entry("F14", "Cambio de variable"),
            Map.entry("F15", "Quita y pon"),
            Map.entry("F16", "Ruffini"),
            Map.entry("F17", "Ruffini con raíz repetida"),
            Map.entry("F18", "Polinomio recíproco"),
            Map.entry("F19", "Aspa doble especial"),
            Map.entry("F20", "Coeficientes indeterminados"),
            Map.entry("F21", "Diferencia de cuadrados con radicales"),
            Map.entry("F22", "Completar cuadrados"),
            Map.entry("F23", "Fórmula general"),
            Map.entry("F24", "Suma de cuadrados"),
            Map.entry("F25", "Fórmula general (Δ < 0)"),
            Map.entry("F26", "Cubos con radicales"),
            Map.entry("F27", "Cardano"),
            Map.entry("F28", "Ferrari"),
            Map.entry("F29", "Raíces conjugadas"),
            Map.entry("F30", "De Moivre"),
            Map.entry("A5", "Aproximación de raíces"),
            Map.entry("PR", "Pasar a R"),
            Map.entry("PC", "Pasar a C"),
            Map.entry("T01", "Factor común de una función"),
            Map.entry("T02", "Sustitución exponencial"),
            Map.entry("T03", "Sustitución trigonométrica"),
            Map.entry("T04", "Identidades trigonométricas"),
            Map.entry("T05", "Sustitución logarítmica"),
            Map.entry("A1", "Discriminante"),
            Map.entry("A4", "Prueba de irreducibilidad"),
            Map.entry("P01", "Simplificar fracción"),
            Map.entry("P02", "División de polinomios"),
            Map.entry("L04", "Comparar grados"),
            Map.entry("D07", "Derivada trascendente"),
            Map.entry("M01", "Factor común"),
            Map.entry("M02", "Agrupación"),
            Map.entry("M03", "Productos notables"),
            Map.entry("M04", "Aspa simple homogénea"),
            Map.entry("M05", "Aspa doble"),
            Map.entry("M06", "Homogéneo (t = x/y)"),
            Map.entry("M07", "Ruffini respecto de una letra"),
            Map.entry("M08", "Coeficientes indeterminados"),
            Map.entry("A6", "Reducción de Kronecker"));

    private Tecnicas() {
    }

    /** Nombre de la técnica; el código mismo si no está en la lista. */
    public static String nombre(String codigo) {
        return NOMBRES.getOrDefault(codigo, codigo);
    }

    /** "Ruffini", "Aspa simple", "Quita y pon"…; el nombre largo si no tiene uno corto. */
    public static String nombreCorto(String codigo) {
        return CORTOS.getOrDefault(codigo, nombre(codigo));
    }

    /**
     * Práctica (Sprint 9, parte D): la pista de las técnicas que no son un {@link MetodoFactorizacion} (las de
     * cálculo y F03). Orienta sin decir el nombre del método. Las de los métodos de factorización están en cada
     * método ({@link MetodoFactorizacion#pista()}).
     */
    private static final Map<String, String> PISTAS = Map.ofEntries(
            Map.entry("F03", "¿Hay un mismo paréntesis que se repite en todos los sumandos?"),
            Map.entry("L01", "Prueba lo más simple: reemplaza la variable por el valor al que tiende y mira qué sale."),
            Map.entry("L02", "Sale 0/0 en una fracción de polinomios: el valor al que tiende es raíz arriba y abajo, así que "
                    + "hay un factor que se repite en los dos."),
            Map.entry("L03", "Hay una raíz que estorba: multiplica arriba y abajo por la misma expresión con el signo del "
                    + "medio cambiado."),
            Map.entry("L04", "La variable tiende a infinito: compara qué término crece más rápido arriba y abajo."),
            Map.entry("L05", "El denominador se hace cero y el numerador no: mira qué pasa un poco a la izquierda y un poco "
                    + "a la derecha del punto."),
            Map.entry("L06", "¿La expresión se parece a una de las formas clásicas que ya conoces, como sen(u)/u cuando u "
                    + "tiende a 0?"),
            Map.entry("L07", "Con 0/0 o ∞/∞ en un cociente, puedes trabajar con las derivadas de arriba y de abajo."),
            Map.entry("D01", "Ese término no depende de la variable."),
            Map.entry("D02", "Es una potencia de la variable: baja el exponente y réstale uno."),
            Map.entry("D03", "Es una suma: trabaja cada término por separado."),
            Map.entry("D04", "Son dos funciones multiplicadas."),
            Map.entry("D05", "Es una función dividida entre otra."),
            Map.entry("D06", "Hay una función dentro de otra: trabaja de afuera hacia adentro."),
            Map.entry("D07", "Aparece una exponencial, un logaritmo o una función trigonométrica: recuerda su derivada."),
            Map.entry("D08", "La y no está despejada: deriva los dos lados pensando que y depende de x."),
            Map.entry("D09", "La variable está en la base y en el exponente: toma logaritmo antes de derivar."),
            Map.entry("I01", "¿Reconoces la primitiva directamente de la tabla?"),
            Map.entry("I02", "¿Aparece una función y, multiplicándola, algo parecido a su derivada?"),
            Map.entry("I03", "Es un producto de dos tipos de funciones distintos: elige cuál derivar y cuál integrar."),
            Map.entry("I04", "Son potencias de seno y coseno: usa una identidad para bajar los exponentes."),
            Map.entry("I05", "Aparece √(a² − x²), √(a² + x²) o √(x² − a²): piensa en un triángulo rectángulo."),
            Map.entry("I06", "Es una fracción de polinomios con el denominador factorizable: pártela en fracciones más simples."),
            Map.entry("I07", "Ya tienes la primitiva: solo falta evaluarla en los límites y restar."),
            Map.entry("V01", "Deriva respecto de una letra y trata las demás como números."),
            Map.entry("V02", "Junta en un vector las derivadas respecto de cada letra."),
            Map.entry("V03", "Combina el vector de derivadas con la dirección, hecha unitaria."),
            Map.entry("V04", "Las letras dependen de otra: suma cada camino por el que cambia la función."),
            Map.entry("V05", "Acércate al punto por distintos caminos y compara lo que sale."),
            Map.entry("V06", "Cambia a una distancia y un ángulo: mira si el ángulo deja de importar."),
            Map.entry("V07", "En cada punto crítico, mira el signo del determinante de las segundas derivadas."),
            Map.entry("V08", "Hay una restricción: los gradientes de la función y de la restricción deben ser paralelos."),
            Map.entry("V09", "Integra primero respecto de una letra, dejando la otra fija."),
            Map.entry("V10", "La región es más simple en otras coordenadas: no olvides el factor del cambio."),
            Map.entry("T01", "Todos los términos comparten la misma función."),
            Map.entry("T02", "Aparecen eˣ y potencias de eˣ: llama u a eˣ."),
            Map.entry("T03", "Aparece una función trigonométrica y su cuadrado: llámala u."),
            Map.entry("T04", "Una identidad trigonométrica puede dejarlo todo en una sola función."),
            Map.entry("T05", "Aparecen ln x y sus potencias: llama u a ln x."));

    /** La pista de una técnica sin clase propia; null si no tiene. */
    public static String pista(String codigo) {
        return PISTAS.get(codigo);
    }
}
