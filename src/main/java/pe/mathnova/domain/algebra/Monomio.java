package pe.mathnova.domain.algebra;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import pe.mathnova.domain.expresion.Letras;

/**
 * Parte literal a^p·b^q·…·x^r·…·α^s, sin coeficiente, en cualquier letra que pueda ser variable
 * (RF57, RN44). Solo guarda los exponentes mayores que cero, así que equals compara por contenido.
 *
 * <p>El orden natural es por grado total y luego lexicográfico, con las letras en orden alfabético
 * (a > b > … > x > y > z > α > … > ω): de dos monomios del mismo grado es mayor el que tiene más
 * exponente en la primera letra donde difieren. Para x, y, z es el mismo orden x > y > z de antes.
 *
 * @param exponentes letra → exponente, sin ceros; inmutable
 */
public record Monomio(SortedMap<Character, Integer> exponentes) implements Comparable<Monomio> {

    public static final Monomio UNO = new Monomio(new TreeMap<>());

    public Monomio {
        TreeMap<Character, Integer> copia = new TreeMap<>();
        exponentes.forEach((letra, exponente) -> {
            validar(letra);
            if (exponente < 0) {
                throw new IllegalArgumentException("Exponentes negativos en un monomio");
            }
            if (exponente > 0) {
                copia.put(letra, exponente);
            }
        });
        exponentes = Collections.unmodifiableSortedMap(copia);
    }

    /** variable^exponente. */
    public static Monomio de(char variable, int exponente) {
        validar(variable);
        return new Monomio(new TreeMap<>(Map.of(variable, exponente)));
    }

    private static void validar(char letra) {
        if (!Letras.esVariable(letra)) {
            throw new IllegalArgumentException("Variable no permitida: " + letra);
        }
    }

    /** Exponente de la letra; 0 si no aparece. */
    public int exponente(char variable) {
        validar(variable);
        return exponentes.getOrDefault(variable, 0);
    }

    /** Letras con exponente mayor que cero, en orden alfabético. */
    public SortedSet<Character> variables() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(exponentes.keySet()));
    }

    public int grado() {
        return exponentes.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Monomio multiplicar(Monomio o) {
        TreeMap<Character, Integer> suma = new TreeMap<>(exponentes);
        o.exponentes.forEach((letra, exponente) -> suma.merge(letra, exponente, Math::addExact));
        return new Monomio(suma);
    }

    /** this^n, con n ≥ 0. */
    public Monomio potencia(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Exponente negativo: " + n);
        }
        TreeMap<Character, Integer> r = new TreeMap<>();
        exponentes.forEach((letra, exponente) -> r.put(letra, Math.multiplyExact(exponente, n)));
        return new Monomio(r);
    }

    /** Raíz n-ésima exacta: existe solo si todos los exponentes son múltiplos de n. */
    public Optional<Monomio> raiz(int n) {
        TreeMap<Character, Integer> r = new TreeMap<>();
        for (Map.Entry<Character, Integer> e : exponentes.entrySet()) {
            if (e.getValue() % n != 0) {
                return Optional.empty();
            }
            r.put(e.getKey(), e.getValue() / n);
        }
        return Optional.of(new Monomio(r));
    }

    /** Máximo común divisor: cada letra con su menor exponente. */
    public Monomio mcd(Monomio o) {
        TreeMap<Character, Integer> r = new TreeMap<>();
        exponentes.forEach((letra, exponente) -> {
            int otro = o.exponentes.getOrDefault(letra, 0);
            if (otro > 0) {
                r.put(letra, Math.min(exponente, otro));
            }
        });
        return new Monomio(r);
    }

    /** this / o, que debe ser exacta (cada exponente de o es menor o igual). */
    public Monomio dividir(Monomio o) {
        TreeMap<Character, Integer> r = new TreeMap<>(exponentes);
        o.exponentes.forEach((letra, exponente) -> {
            int resto = r.getOrDefault(letra, 0) - exponente;
            if (resto < 0) {
                throw new ArithmeticException("La división de monomios no es exacta");
            }
            r.put(letra, resto);
        });
        return new Monomio(r);
    }

    @Override
    public int compareTo(Monomio o) {
        int porGrado = Integer.compare(grado(), o.grado());
        if (porGrado != 0) {
            return porGrado;
        }
        TreeSet<Character> letras = new TreeSet<>(exponentes.keySet());
        letras.addAll(o.exponentes.keySet());
        for (char letra : letras) {
            int c = Integer.compare(exponentes.getOrDefault(letra, 0), o.exponentes.getOrDefault(letra, 0));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /** "x^2y", "a^2b", "z", "" para UNO. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        exponentes.forEach((letra, exponente) -> {
            sb.append(letra);
            if (exponente > 1) {
                sb.append('^').append(exponente);
            }
        });
        return sb.toString();
    }
}
