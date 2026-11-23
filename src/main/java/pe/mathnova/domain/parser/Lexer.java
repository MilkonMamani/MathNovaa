package pe.mathnova.domain.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.expresion.Letras;

/**
 * Convierte el texto en tokens.
 * <ul>
 *   <li>Antes de leer normaliza: "−" a "-", "×" a "*", "²" a "^2", "³" a "^3", "⁺" a "+" y "⁻" a "-".
 *       Ignora los espacios.</li>
 *   <li>Las operaciones se reconocen antes que las letras sueltas: "lim", "int", "inf", "d/dx",
 *       "d2/dx2" (RN47: "d/dx" siempre es derivada) y "∂/∂x".</li>
 *   <li>Después las letras buscan el nombre de función más largo (arcsen antes que sen; RN45: "ln"
 *       es logaritmo, para l·n se escribe "l n"); si ninguno calza, toman una sola letra: e, i y π
 *       son constantes (RN42) y las demás latinas y griegas minúsculas son variables (RF57).
 *       Por eso "2xy" es 2·x·y.</li>
 *   <li>Σ, ∏, ∮ y |x| se rechazan (RF62); una casilla □ sin llenar también.</li>
 *   <li>El decimal va solo con punto.</li>
 * </ul>
 */
public final class Lexer {

    static final String FUERA_DE_ALCANCE = "Las sumatorias, productorias, integrales de línea y el valor "
            + "absoluto están fuera del alcance de MathNova";

    /** Nombre escrito → nombre canónico. */
    private static final Map<String, String> NOMBRES = Map.ofEntries(
            Map.entry("arcsen", "arcsen"), Map.entry("arcsin", "arcsen"),
            Map.entry("arccos", "arccos"), Map.entry("arcsec", "arcsec"),
            Map.entry("arctan", "arctan"), Map.entry("arctg", "arctan"),
            Map.entry("sen", "sen"), Map.entry("sin", "sen"),
            Map.entry("cos", "cos"),
            Map.entry("tan", "tan"), Map.entry("tg", "tan"),
            Map.entry("cot", "cot"), Map.entry("ctg", "cot"),
            Map.entry("sec", "sec"),
            Map.entry("csc", "csc"), Map.entry("cosec", "csc"),
            Map.entry("ln", "ln"), Map.entry("log", "log"),
            Map.entry("sqrt", "sqrt"), Map.entry("exp", "exp"),
            Map.entry("pi", "π"),
            Map.entry("lim", "lim"), Map.entry("int", "∫"), Map.entry("inf", "∞"),
            Map.entry("grad", "grad"), Map.entry("Du", "Du"),
            // Sprint 8
            Map.entry("extremos", "extremos"), Map.entry("coord", "coord"),
            Map.entry("polares", "polares"), Map.entry("cilindricas", "cilindricas"),
            Map.entry("cilíndricas", "cilindricas"), Map.entry("esfericas", "esfericas"),
            Map.entry("esféricas", "esfericas"));

    private static final List<String> PALABRAS = NOMBRES.keySet().stream()
            .sorted(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()))
            .toList();

    private static final String OPERADORES = "+-*/^()·√∛_,[]→=;";
    /** RF62: sumatoria, productoria, integral de línea y valor absoluto. */
    private static final String FUERA = "Σ∑∏Π∮|";
    private static final char CASILLA = '□';

    private Lexer() {
    }

    public static List<Token> tokenizar(String original) {
        // Texto normalizado y, para cada carácter, su posición (base 1) en el original.
        StringBuilder s = new StringBuilder();
        List<Integer> origen = new ArrayList<>();
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            String reemplazo = switch (c) {
                case '−', '⁻' -> "-";
                case '⁺' -> "+";
                case '×' -> "*";
                case '²' -> "^2";
                case '³' -> "^3";
                default -> String.valueOf(c);
            };
            for (int k = 0; k < reemplazo.length(); k++) {
                s.append(reemplazo.charAt(k));
                origen.add(i + 1);
            }
        }
        return new Lectura(s.toString(), origen, original.length()).leer();
    }

    /** Recorrido del texto normalizado. */
    private static final class Lectura {

        private final String s;
        private final List<Integer> origen;
        private final int largoOriginal;
        private final List<Token> tokens = new ArrayList<>();
        private int i;

        Lectura(String s, List<Integer> origen, int largoOriginal) {
            this.s = s;
            this.origen = origen;
            this.largoOriginal = largoOriginal;
        }

        List<Token> leer() {
            while (i < s.length()) {
                char c = s.charAt(i);
                int pos = origen.get(i);
                if (Character.isWhitespace(c)) {
                    i++;
                } else if (c == CASILLA) {
                    throw new ErrorSintaxis("Completa la casilla vacía en la posición " + pos, pos);
                } else if (FUERA.indexOf(c) >= 0) {
                    throw new ErrorSintaxis(FUERA_DE_ALCANCE, pos);
                } else if (esDigito(c)) {
                    numero(pos);
                } else if (c == 'π') {
                    agregar(Token.Tipo.CONSTANTE, "π", pos, 1);
                } else if (c == '∞') {
                    agregar(Token.Tipo.INFINITO, "∞", pos, 1);
                } else if (c == '∫' || c == '∬' || c == '∭') {
                    agregar(Token.Tipo.INTEGRAL, String.valueOf(c), pos, 1);
                } else if (c == '∂') {
                    parcial(pos);
                } else if (c == '-' && i + 1 < s.length() && s.charAt(i + 1) == '>') {
                    agregar(Token.Tipo.OPERADOR, "→", pos, 2);
                } else if (c == 'd' && esDerivada()) {
                    derivada(pos);
                } else if (Character.isLetter(c)) {
                    letra(c, pos);
                } else if (OPERADORES.indexOf(c) >= 0) {
                    agregar(Token.Tipo.OPERADOR, String.valueOf(c), pos, 1);
                } else {
                    throw new ErrorSintaxis("Carácter '" + c + "' no reconocido en la posición " + pos, pos);
                }
            }
            tokens.add(new Token(Token.Tipo.FIN, "", largoOriginal + 1));
            return tokens;
        }

        private void agregar(Token.Tipo tipo, String texto, int pos, int largo) {
            tokens.add(new Token(tipo, texto, pos));
            i += largo;
        }

        private void numero(int pos) {
            int fin = digitos(i);
            if (fin + 1 < s.length() && s.charAt(fin) == '.' && esDigito(s.charAt(fin + 1))) {
                fin = digitos(fin + 1);
            }
            agregar(Token.Tipo.NUMERO, s.substring(i, fin), pos, fin - i);
        }

        private void letra(char c, int pos) {
            String palabra = palabraEn(i);
            if (palabra != null) {
                String canonico = NOMBRES.get(palabra);
                Token.Tipo tipo = switch (canonico) {
                    case "π" -> Token.Tipo.CONSTANTE;
                    case "lim" -> Token.Tipo.LIMITE;
                    case "∫" -> Token.Tipo.INTEGRAL;
                    case "∞" -> Token.Tipo.INFINITO;
                    case "grad" -> Token.Tipo.GRADIENTE;
                    case "Du" -> Token.Tipo.DIRECCIONAL;
                    case "extremos" -> Token.Tipo.EXTREMOS;
                    case "coord", "polares", "cilindricas", "esfericas" -> Token.Tipo.COORDENADAS;
                    default -> Token.Tipo.FUNCION;
                };
                agregar(tipo, canonico, pos, palabra.length());
            } else if (Letras.esConstante(c)) {
                agregar(Token.Tipo.CONSTANTE, String.valueOf(c), pos, 1);
            } else if (Letras.esVariable(c)) {
                agregar(Token.Tipo.VARIABLE, String.valueOf(c), pos, 1);
            } else {
                throw new ErrorSintaxis("'" + c + "' en la posición " + pos + " no es una letra permitida", pos);
            }
        }

        /** En i hay "d" [ ["^"] entero ] "/d" seguido de una letra. */
        private boolean esDerivada() {
            int j = exponenteOpcional(i + 1);
            return j + 2 < s.length() && s.charAt(j) == '/' && s.charAt(j + 1) == 'd'
                    && Character.isLetter(s.charAt(j + 2));
        }

        /** "d" [ ["^"] entero ] "/d" letra [ ["^"] entero ]; el parser valida la letra y los órdenes. */
        private void derivada(int pos) {
            int j = exponenteOpcional(i + 1);
            String orden = sinCircunflejo(i + 1, j);
            char variable = s.charAt(j + 2);
            int fin = exponenteOpcional(j + 3);
            agregar(Token.Tipo.DERIVADA, "d" + orden + "/d" + variable + sinCircunflejo(j + 3, fin), pos, fin - i);
        }

        /** "∂" [ ["^"] entero ] "/" ( "∂" letra [ ["^"] entero ] )+ ; el parser valida letras y órdenes. */
        private void parcial(int pos) {
            int j = exponenteOpcional(i + 1);
            StringBuilder texto = new StringBuilder("∂").append(sinCircunflejo(i + 1, j));
            if (j >= s.length() || s.charAt(j) != '/') {
                throw new ErrorSintaxis("Se esperaba una derivada parcial como ∂/∂x en la posición " + pos, pos);
            }
            texto.append('/');
            j++;
            int variables = 0;
            while (j + 1 < s.length() && s.charAt(j) == '∂' && Character.isLetter(s.charAt(j + 1))) {
                int fin = exponenteOpcional(j + 2);
                texto.append('∂').append(s.charAt(j + 1)).append(sinCircunflejo(j + 2, fin));
                j = fin;
                variables++;
            }
            if (variables == 0) {
                throw new ErrorSintaxis("Se esperaba una derivada parcial como ∂/∂x en la posición " + pos, pos);
            }
            agregar(Token.Tipo.PARCIAL, texto.toString(), pos, j - i);
        }

        /** Salta un exponente opcional "2" o "^2" (de "²") y devuelve dónde termina. */
        private int exponenteOpcional(int desde) {
            int j = desde;
            if (j + 1 < s.length() && s.charAt(j) == '^' && esDigito(s.charAt(j + 1))) {
                j++;
            }
            int fin = digitos(j);
            return fin > j ? fin : desde;
        }

        private String sinCircunflejo(int desde, int hasta) {
            return s.substring(desde, hasta).replace("^", "");
        }

        private String palabraEn(int desde) {
            for (String p : PALABRAS) {
                if (s.startsWith(p, desde)) {
                    return p;
                }
            }
            return null;
        }

        private int digitos(int desde) {
            int j = desde;
            while (j < s.length() && esDigito(s.charAt(j))) {
                j++;
            }
            return j;
        }
    }

    private static boolean esDigito(char c) {
        return c >= '0' && c <= '9';
    }
}
