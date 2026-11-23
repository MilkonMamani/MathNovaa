package pe.mathnova.domain.numeros;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;

/**
 * Complejo aproximado con BigDecimal. Solo lo usan la verificación numérica (Parte E) y la
 * aproximación de raíces de A5; el resto del dominio es exacto.
 */
public record ComplejoDecimal(BigDecimal re, BigDecimal im) {

    public static final ComplejoDecimal CERO = new ComplejoDecimal(BigDecimal.ZERO, BigDecimal.ZERO);
    public static final ComplejoDecimal UNO = new ComplejoDecimal(BigDecimal.ONE, BigDecimal.ZERO);
    public static final ComplejoDecimal I = new ComplejoDecimal(BigDecimal.ZERO, BigDecimal.ONE);

    public ComplejoDecimal {
        Objects.requireNonNull(re, "re");
        Objects.requireNonNull(im, "im");
    }

    public static ComplejoDecimal real(BigDecimal x) {
        return new ComplejoDecimal(x, BigDecimal.ZERO);
    }

    public static ComplejoDecimal de(Racional r, MathContext mc) {
        return real(new BigDecimal(r.num()).divide(new BigDecimal(r.den()), mc));
    }

    public ComplejoDecimal sumar(ComplejoDecimal o, MathContext mc) {
        return new ComplejoDecimal(re.add(o.re, mc), im.add(o.im, mc));
    }

    public ComplejoDecimal restar(ComplejoDecimal o, MathContext mc) {
        return new ComplejoDecimal(re.subtract(o.re, mc), im.subtract(o.im, mc));
    }

    public ComplejoDecimal negar() {
        return new ComplejoDecimal(re.negate(), im.negate());
    }

    public ComplejoDecimal conjugar() {
        return new ComplejoDecimal(re, im.negate());
    }

    public ComplejoDecimal multiplicar(ComplejoDecimal o, MathContext mc) {
        return new ComplejoDecimal(re.multiply(o.re, mc).subtract(im.multiply(o.im, mc), mc),
                re.multiply(o.im, mc).add(im.multiply(o.re, mc), mc));
    }

    public ComplejoDecimal multiplicar(BigDecimal k, MathContext mc) {
        return new ComplejoDecimal(re.multiply(k, mc), im.multiply(k, mc));
    }

    public ComplejoDecimal dividir(ComplejoDecimal o, MathContext mc) {
        BigDecimal d = o.re.multiply(o.re, mc).add(o.im.multiply(o.im, mc), mc);
        if (d.signum() == 0) {
            throw new ArithmeticException("División entre cero");
        }
        ComplejoDecimal n = multiplicar(o.conjugar(), mc);
        return new ComplejoDecimal(n.re.divide(d, mc), n.im.divide(d, mc));
    }

    public ComplejoDecimal potencia(int n, MathContext mc) {
        if (n < 0) {
            return UNO.dividir(potencia(-n, mc), mc);
        }
        ComplejoDecimal r = UNO;
        ComplejoDecimal b = this;
        int k = n;
        while (k > 0) {
            if ((k & 1) == 1) {
                r = r.multiplicar(b, mc);
            }
            k >>= 1;
            if (k > 0) {
                b = b.multiplicar(b, mc);
            }
        }
        return r;
    }

    public BigDecimal modulo(MathContext mc) {
        return re.multiply(re, mc).add(im.multiply(im, mc), mc).sqrt(mc);
    }

    /** Raíz cuadrada principal. */
    public ComplejoDecimal raizCuadrada(MathContext mc) {
        if (im.signum() == 0) {
            return re.signum() >= 0 ? real(re.sqrt(mc)) : new ComplejoDecimal(BigDecimal.ZERO, re.negate().sqrt(mc));
        }
        BigDecimal m = modulo(mc);
        BigDecimal dos = BigDecimal.valueOf(2);
        BigDecimal a = m.add(re, mc).divide(dos, mc).sqrt(mc);
        BigDecimal b = m.subtract(re, mc).divide(dos, mc).sqrt(mc);
        return new ComplejoDecimal(a, im.signum() < 0 ? b.negate() : b);
    }

    public boolean esReal(int digitos) {
        return im.abs().compareTo(BigDecimal.ONE.movePointLeft(digitos)) < 0;
    }

    /** |this − o| ≤ 10⁻ᵈ · max(1, |o|). */
    public boolean cercano(ComplejoDecimal o, int digitos, MathContext mc) {
        BigDecimal escala = o.modulo(mc).max(BigDecimal.ONE);
        BigDecimal tolerancia = escala.multiply(BigDecimal.ONE.movePointLeft(digitos), mc);
        return restar(o, mc).modulo(mc).compareTo(tolerancia) <= 0;
    }

    /** "1.1673039783", "-1 + 2i" con los decimales dados. */
    public String texto(int decimales) {
        String a = re.setScale(decimales, RoundingMode.HALF_EVEN).toPlainString();
        if (esReal(decimales + 2)) {
            return a;
        }
        BigDecimal b = im.setScale(decimales, RoundingMode.HALF_EVEN);
        return a + (b.signum() < 0 ? " - " : " + ") + b.abs().toPlainString() + "i";
    }
}
