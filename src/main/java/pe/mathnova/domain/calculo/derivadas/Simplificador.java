package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.numeros.Radical;

/**
 * Simplificación mínima de un árbol (Sprint 5), la que necesitan el derivador y el motor de límites:
 * <ul>
 *   <li>sumas: se aplanan, se reúnen los términos semejantes (3x + 2x = 5x) y se quitan los ceros</li>
 *   <li>productos: se aplanan, se multiplican las constantes, 0·f = 0, 1·f = f, y las potencias de
 *       una misma base se juntan (x·x² = x³); si hay fracciones, todo queda como una sola fracción</li>
 *   <li>fracciones: se simplifican las constantes y se cancelan las bases iguales arriba y abajo</li>
 *   <li>potencias: x¹ = x, x⁰ = 1, 1ⁿ = 1, (xᵐ)ⁿ = xᵐⁿ, (√u)² = u</li>
 * </ul>
 * La forma es canónica en un sentido débil: el signo menos va siempre afuera (−2x es Negacion(2x)),
 * las constantes van primero en un producto, y un coeficiente fraccionario se escribe como
 * fracción (x/2, no ½x). Así dos expresiones iguales escritas de la misma manera quedan iguales.
 */
public final class Simplificador {

    private static final Constante CERO = Constante.de(0);
    private static final Constante UNO = Constante.de(1);
    private static final int VUELTAS = 12;

    private Simplificador() {
    }

    public static Expresion simplificar(Expresion e) {
        Expresion actual = e;
        for (int i = 0; i < VUELTAS; i++) {
            Expresion siguiente = paso(actual);
            if (siguiente.equals(actual)) {
                return siguiente;
            }
            actual = siguiente;
        }
        return actual;
    }

    private static Expresion paso(Expresion e) {
        return switch (e) {
            case Negacion n -> negar(paso(n.argumento()));
            case Suma s -> suma(s.terminos().stream().map(Simplificador::paso).toList());
            case Producto p -> producto(p.factores().stream().map(Simplificador::paso).toList());
            case Cociente c -> cociente(paso(c.numerador()), paso(c.denominador()));
            case Potencia p -> potencia(paso(p.base()), paso(p.exponente()));
            case Raiz r -> raiz(r.indice(), paso(r.radicando()));
            case Funcion f -> funcion(f.tipo(), paso(f.argumento()), f.base());
            default -> e;
        };
    }

    // ------------------------------------------------------------------ constructores ya simplificados

    /** −e con el signo afuera: −(−x) = x, −(3) = −3. */
    public static Expresion negar(Expresion e) {
        return switch (e) {
            case Constante c -> new Constante(c.valor().negar());
            case Negacion n -> n.argumento();
            default -> new Negacion(e);
        };
    }

    public static Expresion suma(List<Expresion> terminos) {
        List<Expresion> planos = new ArrayList<>();
        for (Expresion t : terminos) {
            aplanarSuma(t, false, planos);
        }
        // Términos semejantes: resto → coeficiente, en el orden en que aparecen. La clave CERO es la constante.
        Map<Expresion, Racional> grupos = new LinkedHashMap<>();
        for (Expresion t : planos) {
            Termino d = descomponer(t);
            grupos.merge(d.resto == null ? CERO : d.resto, d.coeficiente, Racional::sumar);
        }
        List<Expresion> r = new ArrayList<>();
        grupos.forEach((resto, c) -> {
            if (!c.esCero()) {
                r.add(componer(c, resto == CERO ? null : resto));
            }
        });
        if (r.isEmpty()) {
            return CERO;
        }
        return r.size() == 1 ? r.getFirst() : new Suma(r);
    }

    private static void aplanarSuma(Expresion t, boolean negativo, List<Expresion> destino) {
        if (t instanceof Suma s) {
            s.terminos().forEach(x -> aplanarSuma(x, negativo, destino));
        } else if (t instanceof Negacion n && n.argumento() instanceof Suma s) {
            s.terminos().forEach(x -> aplanarSuma(x, !negativo, destino));
        } else {
            destino.add(negativo ? negar(t) : t);
        }
    }

    /** Un término como coeficiente × resto; resto null es una constante. */
    private record Termino(Racional coeficiente, Expresion resto) {
    }

    private static Termino descomponer(Expresion t) {
        return switch (t) {
            case Constante c -> new Termino(c.valor(), null);
            case Negacion n -> {
                Termino d = descomponer(n.argumento());
                yield new Termino(d.coeficiente.negar(), d.resto);
            }
            case Producto p when p.factores().getFirst() instanceof Constante c -> {
                List<Expresion> resto = p.factores().subList(1, p.factores().size());
                yield new Termino(c.valor(), resto.size() == 1 ? resto.getFirst() : new Producto(resto));
            }
            case Cociente q when q.denominador() instanceof Constante d && !d.valor().esCero() -> {
                Termino arriba = descomponer(q.numerador());
                yield new Termino(arriba.coeficiente.dividir(d.valor()), arriba.resto);
            }
            default -> new Termino(Racional.UNO, t);
        };
    }

    private static Expresion componer(Racional c, Expresion resto) {
        if (resto == null) {
            return new Constante(c);
        }
        if (c.signo() < 0) {
            return negar(componer(c.negar(), resto));
        }
        if (c.equals(Racional.UNO)) {
            return resto;
        }
        return producto(List.of(new Constante(c), resto));
    }

    public static Expresion producto(List<Expresion> factores) {
        List<Expresion> planos = new ArrayList<>();
        boolean[] negativo = {false};
        for (Expresion f : factores) {
            aplanarProducto(f, negativo, planos);
        }
        // Si hay fracciones, todo el producto queda como una sola fracción.
        if (planos.stream().anyMatch(f -> f instanceof Cociente)) {
            List<Expresion> arriba = new ArrayList<>();
            List<Expresion> abajo = new ArrayList<>();
            for (Expresion f : planos) {
                if (f instanceof Cociente c) {
                    arriba.add(c.numerador());
                    abajo.add(c.denominador());
                } else {
                    arriba.add(f);
                }
            }
            Expresion q = cociente(producto(arriba), producto(abajo));
            return negativo[0] ? negar(q) : q;
        }
        Factores f = new Factores();
        planos.forEach(f::agregar);
        if (f.coeficiente.esCero()) {
            return CERO;
        }
        Racional coeficiente = negativo[0] ? f.coeficiente.negar() : f.coeficiente;
        return conCoeficiente(coeficiente, f.lista());
    }

    /** c × factores, con el signo afuera y un coeficiente fraccionario como fracción. */
    private static Expresion conCoeficiente(Racional c, List<Expresion> factores) {
        if (c.signo() < 0) {
            return negar(conCoeficiente(c.negar(), factores));
        }
        if (factores.isEmpty()) {
            return new Constante(c);
        }
        if (!c.esEntero()) {
            Expresion arriba = conCoeficiente(Racional.de(c.num()), factores);
            return new Cociente(arriba, new Constante(Racional.de(c.den())));
        }
        List<Expresion> r = new ArrayList<>();
        if (!c.equals(Racional.UNO)) {
            r.add(new Constante(c));
        }
        r.addAll(factores);
        return r.size() == 1 ? r.getFirst() : new Producto(r);
    }

    private static void aplanarProducto(Expresion f, boolean[] negativo, List<Expresion> destino) {
        switch (f) {
            case Producto p -> p.factores().forEach(x -> aplanarProducto(x, negativo, destino));
            case Negacion n -> {
                negativo[0] = !negativo[0];
                aplanarProducto(n.argumento(), negativo, destino);
            }
            case Constante c when c.valor().signo() < 0 -> {
                negativo[0] = !negativo[0];
                destino.add(new Constante(c.valor().negar()));
            }
            case Constante c when c.valor().equals(Racional.UNO) -> {
            }
            default -> destino.add(f);
        }
    }

    public static Expresion cociente(Expresion numerador, Expresion denominador) {
        if (numerador instanceof Negacion n) {
            return negar(cociente(n.argumento(), denominador));
        }
        if (denominador instanceof Negacion n) {
            return negar(cociente(numerador, n.argumento()));
        }
        if (numerador instanceof Constante c && c.valor().esCero()) {
            return CERO;
        }
        if (denominador instanceof Constante d) {
            if (d.valor().esCero()) {
                return new Cociente(numerador, denominador);
            }
            if (d.valor().signo() < 0) {
                return negar(cociente(numerador, new Constante(d.valor().negar())));
            }
            if (d.valor().equals(Racional.UNO)) {
                return numerador;
            }
            if (numerador instanceof Constante c) {
                return new Constante(c.valor().dividir(d.valor()));
            }
        }
        if (numerador instanceof Cociente a) {
            return cociente(a.numerador(), producto(List.of(a.denominador(), denominador)));
        }
        if (denominador instanceof Cociente b) {
            return cociente(producto(List.of(numerador, b.denominador())), b.numerador());
        }
        // Constantes y bases iguales arriba y abajo.
        Factores arriba = factores(numerador);
        Factores abajo = factores(denominador);
        Racional c = arriba.coeficiente.dividir(abajo.coeficiente);
        boolean negativo = c.signo() < 0;
        if (negativo) {
            c = c.negar();
        }
        for (Expresion base : new ArrayList<>(arriba.potencias.keySet())) {
            Racional k = abajo.potencias.get(base);
            if (k == null) {
                continue;
            }
            Racional m = arriba.potencias.get(base);
            Racional comun = m.compareTo(k) <= 0 ? m : k;
            if (comun.signo() <= 0) {
                continue;
            }
            arriba.potencias.put(base, m.restar(comun));
            abajo.potencias.put(base, k.restar(comun));
        }
        List<Expresion> fa = arriba.lista();
        List<Expresion> fb = abajo.lista();
        Expresion num = conCoeficiente(Racional.de(c.num()), fa);
        Expresion den = conCoeficiente(Racional.de(c.den()), fb);
        Expresion r = den.equals(UNO) ? num : new Cociente(num, den);
        return negativo ? negar(r) : r;
    }

    /**
     * Un producto como coeficiente × Π baseᵏ (k racional constante), en el orden en que aparecen las
     * bases. Una potencia con exponente variable (eˣ) cuenta como base con exponente 1.
     */
    private static final class Factores {
        Racional coeficiente = Racional.UNO;
        final Map<Expresion, Racional> potencias = new LinkedHashMap<>();

        void agregar(Expresion x) {
            if (x instanceof Constante c) {
                coeficiente = coeficiente.multiplicar(c.valor());
            } else if (x instanceof Potencia p && p.exponente() instanceof Constante k) {
                potencias.merge(p.base(), k.valor(), Racional::sumar);
            } else {
                potencias.merge(x, Racional.UNO, Racional::sumar);
            }
        }

        List<Expresion> lista() {
            List<Expresion> r = new ArrayList<>();
            potencias.forEach((b, k) -> {
                if (!k.esCero()) {
                    r.add(potencia(b, new Constante(k)));
                }
            });
            return r;
        }
    }

    private static Factores factores(Expresion e) {
        Factores f = new Factores();
        (e instanceof Producto p ? p.factores() : List.of(e)).forEach(f::agregar);
        return f;
    }

    public static Expresion potencia(Expresion base, Expresion exponente) {
        if (exponente instanceof Constante k) {
            Racional n = k.valor();
            if (n.esCero()) {
                return UNO;
            }
            if (n.equals(Racional.UNO)) {
                return base;
            }
            if (base instanceof Constante b) {
                if (b.valor().equals(Racional.UNO)) {
                    return UNO;
                }
                if (n.esEntero() && n.num().bitLength() < 16 && !(b.valor().esCero() && n.signo() < 0)) {
                    return new Constante(b.valor().potencia(n.num().intValueExact()));
                }
                if (b.valor().esCero() && n.signo() > 0) {
                    return CERO;
                }
            }
            if (base instanceof Potencia p && p.exponente() instanceof Constante m) {
                return potencia(p.base(), new Constante(m.valor().multiplicar(n)));
            }
            if (base instanceof Raiz r && n.equals(Racional.de(r.indice()))) {
                return r.radicando();
            }
            if (base instanceof Negacion neg && n.esEntero()) {
                Expresion p = potencia(neg.argumento(), exponente);
                return n.num().testBit(0) ? negar(p) : p;
            }
            if (base instanceof Cociente q && n.esEntero()) {
                return cociente(potencia(q.numerador(), exponente), potencia(q.denominador(), exponente));
            }
        }
        if (base instanceof Constante b && b.valor().equals(Racional.UNO)) {
            return UNO;
        }
        return new Potencia(base, exponente);
    }

    /**
     * Los valores exactos que necesita la regla de Barrow (Sprint 6): ln 1 = 0, ln e = 1, ln eᵏ = k,
     * |c| con c constante, sen 0 = 0, cos 0 = 1, tan 0 = 0, arctan 0 = 0 y arctan 1 = π/4.
     */
    public static Expresion funcion(TipoFuncion tipo, Expresion u, int base) {
        boolean cero = u instanceof Constante c && c.valor().esCero();
        boolean uno = u instanceof Constante c && c.valor().equals(Racional.UNO);
        switch (tipo) {
            case LN -> {
                if (uno) {
                    return CERO;
                }
                if (u instanceof ConstanteEspecial e && e.tipo() == ConstanteEspecial.Tipo.E) {
                    return UNO;
                }
                if (u instanceof Potencia p && p.base() instanceof ConstanteEspecial e
                        && e.tipo() == ConstanteEspecial.Tipo.E) {
                    return p.exponente();
                }
            }
            case ABS -> {
                if (u instanceof Constante c) {
                    return c.valor().signo() < 0 ? new Constante(c.valor().negar()) : c;
                }
                if (u instanceof ConstanteEspecial e && e.tipo() != ConstanteEspecial.Tipo.I) {
                    return e;
                }
                if (u instanceof Negacion n && n.argumento() instanceof ConstanteEspecial e
                        && e.tipo() != ConstanteEspecial.Tipo.I) {
                    return e;
                }
            }
            case SEN, TAN, ARCSEN -> {
                if (cero) {
                    return CERO;
                }
                Racional k = multiploDePi(u);
                if (k != null && tipo != TipoFuncion.ARCSEN) {
                    if (k.esEntero()) {
                        return CERO; // sen nπ = tan nπ = 0
                    }
                    if (tipo == TipoFuncion.SEN && k.den().intValue() == 2) {
                        // sen(π/2 + nπ) = (−1)ⁿ
                        int n = k.restar(Racional.de(1, 2)).num().intValue();
                        return Math.floorMod(n, 2) == 0 ? UNO : Constante.de(-1);
                    }
                }
            }
            case COS -> {
                if (cero) {
                    return UNO;
                }
                Racional k = multiploDePi(u);
                if (k != null && k.esEntero()) {
                    return k.num().testBit(0) ? Constante.de(-1) : UNO; // cos nπ = (−1)ⁿ
                }
                if (k != null && k.den().intValue() == 2) {
                    return CERO;
                }
            }
            case ARCTAN -> {
                if (cero) {
                    return CERO;
                }
                if (uno) {
                    return cociente(new ConstanteEspecial(ConstanteEspecial.Tipo.PI), Constante.de(4));
                }
            }
            case EXP -> {
                if (cero) {
                    return UNO;
                }
            }
            default -> {
            }
        }
        return new Funcion(tipo, u, base);
    }

    /** k si u es k·π (π, 2π, π/2, −π…); null si no. */
    private static Racional multiploDePi(Expresion u) {
        if (u instanceof ConstanteEspecial e && e.tipo() == ConstanteEspecial.Tipo.PI) {
            return Racional.UNO;
        }
        if (u instanceof Negacion n) {
            Racional k = multiploDePi(n.argumento());
            return k == null ? null : k.negar();
        }
        if (u instanceof Producto(List<Expresion> fs) && fs.size() == 2 && fs.get(0) instanceof Constante c
                && fs.get(1) instanceof ConstanteEspecial e && e.tipo() == ConstanteEspecial.Tipo.PI) {
            return c.valor();
        }
        if (u instanceof Cociente(Expresion n, Expresion d) && d instanceof Constante c && !c.valor().esCero()) {
            Racional k = multiploDePi(n);
            return k == null ? null : k.dividir(c.valor());
        }
        return null;
    }

    public static Expresion raiz(int indice, Expresion radicando) {
        if (radicando instanceof Constante c && (c.valor().esCero() || c.valor().equals(Racional.UNO))) {
            return radicando;
        }
        if (radicando instanceof Constante c && c.valor().signo() > 0) {
            Radical r = Radical.raiz(indice, c.valor());
            if (r.esRacional()) {
                return new Constante(r.coeficiente());
            }
        }
        if (radicando instanceof Potencia p && p.exponente() instanceof Constante k
                && k.valor().equals(Racional.de(indice)) && indice % 2 == 1) {
            return p.base();
        }
        return new Raiz(indice, radicando);
    }
}
