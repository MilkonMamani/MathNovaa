package pe.mathnova.domain.algebra;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import pe.mathnova.domain.expresion.Letras;

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

    public static Monomio de(char variable, int exponente) {
        validar(variable);
        return new Monomio(new TreeMap<>(Map.of(variable, exponente)));
    }

    private static void validar(char letra) {
        if (!Letras.esVariable(letra)) {
            throw new IllegalArgumentException("Variable no permitida: " + letra);
        }
    }

    public int exponente(char variable) {
        validar(variable);
        return exponentes.getOrDefault(variable, 0);
    }

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

    public Monomio potencia(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Exponente negativo: " + n);
        }
        TreeMap<Character, Integer> r = new TreeMap<>();
        exponentes.forEach((letra, exponente) -> r.put(letra, Math.multiplyExact(exponente, n)));
        return new Monomio(r);
    }

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
