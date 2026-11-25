package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pe.mathnova.domain.calculo.derivadas.MotorDerivadas;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.latex.TextoLatex;
import pe.mathnova.presentation.pizarra.Pizarra;

final class RenglonesPaso {

    enum Operacion { GENERAL, LIMITE, DERIVADA, INTEGRAL }

    record Marco(Operacion operacion, String limite, char variable, String punto, String derivada) {

        static final Marco GENERAL = new Marco(Operacion.GENERAL, null, (char) 0, null, null);

        private static final Pattern LIMITE = Pattern.compile("^\\\\lim_\\{\\s*([a-zA-Z])\\s*\\\\to\\s*(.+?)\\s*}");
        private static final Pattern DERIVADA = Pattern.compile("^\\\\frac\\{d}\\{d([a-zA-Z])}");
        private static final Pattern INTEGRAL = Pattern.compile("^\\\\int(_\\{[^}]*}\\^\\{[^}]*})?");
        private static final Pattern DIFERENCIAL = Pattern.compile("d([a-zA-Z])\\s*$");

        static Marco de(String rotulo, String latexEntrada) {
            if (rotulo == null) {
                return GENERAL;
            }
            String e = latexEntrada == null ? "" : latexEntrada.strip();
            if (rotulo.contains("LÍMITE")) {
                String prefijo = prefijoLimite(e);
                Matcher m = prefijo == null ? null : LIMITE.matcher(prefijo);
                if (m != null && m.find()) {
                    return new Marco(Operacion.LIMITE, prefijo, m.group(1).charAt(0), m.group(2), null);
                }
                return GENERAL;
            }
            if (rotulo.equals("DERIVA")) {
                Matcher m = DERIVADA.matcher(e);
                boolean hay = m.find();
                return new Marco(Operacion.DERIVADA, null, hay ? m.group(1).charAt(0) : 'x', null,
                        hay ? m.group() : "\\frac{d}{dx}");
            }
            if (rotulo.equals("INTEGRA")) {
                Matcher op = INTEGRAL.matcher(e);
                Matcher dif = DIFERENCIAL.matcher(e);
                return new Marco(Operacion.INTEGRAL, op.find() ? op.group() : "\\int", dif.find() ? dif.group(1).charAt(0)
                        : 'x', null, null);
            }
            return GENERAL;
        }

        static String prefijoLimite(String latex) {
            String inicio = "\\lim_{";
            if (!latex.startsWith(inicio)) {
                return null;
            }
            int nivel = 1;
            for (int i = inicio.length(); i < latex.length(); i++) {
                char c = latex.charAt(i);
                if (c == '{') {
                    nivel++;
                } else if (c == '}' && --nivel == 0) {
                    return latex.substring(0, i + 1);
                }
            }
            return null;
        }
    }

    static final String CANCELAR = "L02";

    private RenglonesPaso() {
    }

    static List<Igualdades.Renglon> renglones(VistaPaso p, Marco marco, boolean principal) {
        Marco m = efectivo(marco, p);
        if (ReorganizarPasos.SUSTITUIR.equals(p.codigo())) {
            return sustituir(p, m);
        }
        List<String> lineas = Pizarra.lineas(p.trabajo());
        if (lineas == null) {
            return null;
        }
        String antesTexto = p.antes() == null ? null : ImpresoraPizarra.imprimir(p.antes());
        String antesLatex = p.antes() == null ? null : ImpresoraLatex.imprimir(p.antes());
        List<Igualdades.Renglon> r = new ArrayList<>();
        for (int indice = 0; indice < lineas.size(); indice++) {
            String l = lineas.get(indice);
            String linea = l;
            String cabeza = null;
            if (m.operacion() == Operacion.DERIVADA && antesTexto != null && indice > 0) {
                String regla = "(" + antesTexto + ")' = ";
                String regla2 = "(" + antesTexto + ")′ = ";
                if (linea.startsWith(regla) || linea.startsWith(regla2)) {
                    cabeza = derivadaDe(antesLatex, m, principal);
                    linea = "= " + linea.substring(regla.length());
                }
            }
            for (Igualdades.Renglon x : Igualdades.Renglon.partidos(linea)) {
                if (cabeza != null && x.latex() != null) {
                    x = Igualdades.Renglon.deLatex(cabeza + " " + x.latex());
                    cabeza = null;
                }
                r.add(x);
            }
        }
        if (m.operacion() == Operacion.LIMITE) {
            return conLimite(r, p, antesLatex, m);
        }
        if (m.operacion() == Operacion.INTEGRAL) {
            r.replaceAll(x -> x.latex() == null ? x
                    : new Igualdades.Renglon(TextoLatex.conDiferenciales(x.latex()), x.texto(), x.marca()));
        }
        if (m.operacion() == Operacion.DERIVADA && MotorDerivadas.SIMPLIFICAR.equals(p.codigo()) && principal
                && !r.isEmpty() && r.getFirst().latex() != null
                && TextoLatex.igualDeAfuera(r.getFirst().latex()) < 0) {
            r.set(0, Igualdades.Renglon.deLatex("y' = " + r.getFirst().latex()));
        }
        return r;
    }

    static String conclusion(VistaPaso p, Marco marco, boolean principal, boolean hayRenglones,
                             Igualdades.Renglon ultimo) {
        if (ReorganizarPasos.SUSTITUIR.equals(p.codigo())) {
            return null;
        }
        Marco m = efectivo(marco, p);
        if (m.operacion() == Operacion.DERIVADA && m != marco) {
            principal = false;
        }
        if (p.despues() == null) {
            return null;
        }
        String antes = p.antes() == null ? null : ImpresoraLatex.imprimir(p.antes());
        String despues = ImpresoraLatex.imprimir(p.despues());
        return switch (m.operacion()) {
            case LIMITE -> {
                if (hayRenglones) {
                    yield null;
                }
                yield antes == null ? despues : conLimite(antes, m) + " = " + conLimite(despues, m);
            }
            case DERIVADA -> {
                if (MotorDerivadas.SIMPLIFICAR.equals(p.codigo())) {
                    yield null;
                }
                if (p.codigo().matches("D\\d\\d") && antes != null) {
                    boolean sigue = ultimo != null && ultimo.latex() != null
                            && ultimo.latex().startsWith(derivadaDe(antes, m, principal));
                    yield (sigue ? "=" : derivadaDe(antes, m, principal) + " =") + " " + despues;
                }
                if (ultimo != null && ultimo.latex() != null && ultimo.latex().matches("^[fy]'.*")) {
                    yield null;
                }
                yield antes == null ? despues : antes + " = " + despues;
            }
            case INTEGRAL -> {
                if (ultimo != null && ultimo.latex() != null && ultimo.latex().strip().startsWith("\\int")
                        && TextoLatex.igualDeAfuera(ultimo.latex()) > 0) {
                    yield null;
                }
                yield antes == null ? despues : antes + " = " + despues;
            }
            default -> antes == null ? despues : antes + " = " + despues;
        };
    }

    static Marco efectivo(Marco m, VistaPaso p) {
        if (m.operacion() != Operacion.LIMITE || p.codigo().startsWith("L")
                || ReorganizarPasos.SUSTITUIR.equals(p.codigo())) {
            return m;
        }
        if (p.codigo().matches("D\\d\\d")) {
            return new Marco(Operacion.DERIVADA, null, m.variable(), null, "\\frac{d}{d" + m.variable() + "}");
        }
        return Marco.GENERAL;
    }

    private static List<Igualdades.Renglon> sustituir(VistaPaso p, Marco m) {
        String antes = ImpresoraLatex.imprimir(p.antes());
        String despues = p.despues() == null ? null : ImpresoraLatex.imprimir(p.despues());
        List<Igualdades.Renglon> r = new ArrayList<>();
        if (m.operacion() == Operacion.LIMITE) {
            r.add(Igualdades.Renglon.deLatex(conLimite(antes, m)));
            if (despues != null) {
                r.add(Igualdades.Renglon.deLatex("= " + conLimite(despues, m) + " \\qquad (" + m.variable()
                        + " \\neq " + m.punto() + ")"));
            }
        } else if (m.operacion() == Operacion.INTEGRAL && despues != null) {
            String dx = " \\, d" + m.variable();
            String restriccion = restriccion(p);
            if (restriccion == null) {
                r.add(Igualdades.Renglon.deLatex(m.limite() + " " + antes + dx + " = " + m.limite() + " " + despues
                        + dx));
            } else {
                r.add(Igualdades.Renglon.deLatex(m.limite() + " " + antes + dx));
                r.add(Igualdades.Renglon.deLatex("= " + m.limite() + " " + cuerpoIntegral(despues) + dx
                        + " \\qquad (" + restriccion + ")"));
            }
        } else if (m.operacion() == Operacion.DERIVADA && despues != null) {
            String restriccion = restriccion(p);
            r.add(Igualdades.Renglon.deLatex(m.derivada() + "\\left(" + antes + "\\right)"));
            boolean potencia = p.despues() instanceof pe.mathnova.domain.expresion.Potencia pot
                    && despues.startsWith("\\left(") && !(pot.base() instanceof pe.mathnova.domain.expresion.Variable);
            r.add(Igualdades.Renglon.deLatex("= " + m.derivada() + (potencia ? " " + despues : "\\left(" + despues
                    + "\\right)") + (restriccion == null ? "" : " \\qquad (" + restriccion + ")")));
        } else {
            r.add(Igualdades.Renglon.deLatex(despues == null ? antes : antes + " = " + despues));
        }
        return r;
    }

    static String restriccion(VistaPaso p) {
        if (!(p.trabajo() instanceof TrabajoIntermedio.Agrupacion a) || a.grupos().isEmpty()) {
            return null;
        }
        return restriccionLatex(String.join(", ", a.grupos()));
    }

    static String restriccionLatex(String texto) {
        return texto.replace("≠", "\\neq").replace("−", "-").replace(", ", ",\\ ");
    }

    private static String cuerpoIntegral(String latex) {
        return TextoLatex.igualDeAfuera(latex) < 0 && necesitaParentesis(latex) ? "\\left(" + latex + "\\right)" : latex;
    }

    private static String derivadaDe(String antesLatex, Marco m, boolean principal) {
        if (principal) {
            return "y'";
        }
        return m.derivada() + "\\left(" + antesLatex + "\\right)";
    }

    private static List<Igualdades.Renglon> conLimite(List<Igualdades.Renglon> renglones, VistaPaso p,
                                                     String antes, Marco m) {
        List<Igualdades.Renglon> r = new ArrayList<>();
        for (int i = 0; i < renglones.size(); i++) {
            Igualdades.Renglon x = renglones.get(i);
            if (x.latex() == null || x.latex().contains("\\text{")) {
                r.add(x);
                continue;
            }
            List<String> partes = partes(x.latex());
            List<String> nuevas = new ArrayList<>();
            for (String parte : partes) {
                nuevas.add(parte.isEmpty() ? parte : conLimite(parte, m));
            }
            String latex = String.join(" = ", nuevas).strip();
            if (i == 0 && antes != null && !partes.getFirst().isEmpty() && !depende(partes.getFirst(), m)) {
                latex = conLimite(antes, m) + " = " + latex;
            }
            r.add(new Igualdades.Renglon(latex, x.texto(), x.marca()));
        }
        if (CANCELAR.equals(p.codigo()) && r.size() >= 2) {
            int k = r.size() - 1;
            Igualdades.Renglon u = r.get(k);
            if (u.latex() != null) {
                r.set(k, new Igualdades.Renglon(u.latex() + " \\qquad (" + m.variable() + " \\neq " + m.punto() + ")",
                        u.texto(), u.marca()));
            }
        }
        return r;
    }

    static String conLimite(String latex, Marco m) {
        String s = latex.strip();
        if (s.startsWith("\\lim") || !depende(s, m)) {
            return s;
        }
        return m.limite() + " " + (necesitaParentesis(s) ? "\\left(" + s + "\\right)" : s);
    }

    static boolean depende(String latex, Marco m) {
        String sinComandos = latex.replaceAll("\\\\text\\{[^}]*}", " ").replaceAll("\\\\[a-zA-Z]+", " ");
        return sinComandos.indexOf(m.variable()) >= 0;
    }

    private static boolean necesitaParentesis(String latex) {
        int nivel = 0;
        for (int i = 0; i < latex.length(); i++) {
            char c = latex.charAt(i);
            if (c == '\\') {
                continue;
            }
            if (c == '{' || c == '(' || c == '[') {
                nivel++;
            } else if (c == '}' || c == ')' || c == ']') {
                nivel--;
            } else if ((c == '+' || c == '-') && nivel == 0 && i > 0) {
                return true;
            }
        }
        return false;
    }

    static List<String> partes(String latex) {
        List<String> r = new ArrayList<>();
        int nivel = 0;
        int desde = 0;
        for (int i = 0; i < latex.length(); i++) {
            char c = latex.charAt(i);
            if (c == '\\' && i + 1 < latex.length()) {
                i++;
                continue;
            }
            if (c == '{' || c == '(' || c == '[') {
                nivel++;
            } else if (c == '}' || c == ')' || c == ']') {
                nivel--;
            } else if (c == '=' && nivel == 0) {
                r.add(latex.substring(desde, i).strip());
                desde = i + 1;
            }
        }
        r.add(latex.substring(desde).strip());
        return r;
    }

    static boolean dibujoConcluye(TrabajoIntermedio t) {
        return t instanceof TrabajoIntermedio.TablaPartes || t instanceof TrabajoIntermedio.AspaSimple a
                && a.segunda() == 0;
    }
}
