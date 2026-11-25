package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.factorizacion.FactorizadorFunciones;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;

public final class CaminosLimite {

    public static final String TABLA = "TABLA";

    private static final List<String> FORZABLES = List.of("L02", "L03", "L04", "L06", "L07");

    public enum Estado { USADO, SIRVE, NO_APLICA }

    public record Camino(String codigo, Estado estado, String texto) {
        public Camino {
            Objects.requireNonNull(codigo, "codigo");
            Objects.requireNonNull(estado, "estado");
        }
    }

    private final MotorLimites motor;

    public CaminosLimite(MotorLimites motor) {
        this.motor = Objects.requireNonNull(motor, "motor");
    }

    public List<Camino> de(ResolucionLimite r) {
        Limite l = r.entrada();
        Set<String> usados = new LinkedHashSet<>();
        for (PasoLimite p : r.pasos()) {
            if (p.codigo().startsWith("L") && !(p.codigo().equals("L01") && r.indeterminacion() != null)) {
                usados.add(p.codigo());
            }
        }
        if (r.metodoFinal() != null) {
            usados.add(r.metodoFinal());
        }
        List<Camino> caminos = new ArrayList<>();
        usados.forEach(c -> caminos.add(new Camino(c, Estado.USADO, null)));

        List<Camino> noAplican = new ArrayList<>();
        boolean calculado = !(r.resultado() instanceof ResultadoLimite.NoCalculado);
        for (String c : FORZABLES) {
            if (usados.contains(c)) {
                continue;
            }
            Optional<String> vista = calculado ? vistaPrevia(c, l, r.resultado()) : Optional.empty();
            if (vista.isPresent()) {
                caminos.add(new Camino(c, Estado.SIRVE, vista.get()));
            } else {
                noAplican.add(new Camino(c, Estado.NO_APLICA, motivo(c, r)));
            }
        }
        caminos.add(new Camino(TABLA, Estado.SIRVE, tabla(r.resultado())));
        if (!usados.contains("L01")) {
            noAplican.addFirst(new Camino("L01", Estado.NO_APLICA, r.indeterminacion() == null
                    ? "al sustituir " + l.variable() + " no sale un número" : "al sustituir "
                    + punto(l) + " queda " + r.indeterminacion().texto()));
        }
        if (!usados.contains("L05")) {
            noAplican.add(new Camino("L05", Estado.NO_APLICA, "al sustituir no queda k/0"));
        }
        caminos.addAll(noAplican);
        return caminos;
    }

    private Optional<String> vistaPrevia(String codigo, Limite l, ResultadoLimite esperado) {
        ResolucionLimite rc;
        try {
            rc = motor.resolver(l, codigo);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        Optional<PasoLimite> paso = rc.pasos().stream().filter(p -> p.codigo().equals(codigo)).findFirst();
        if (paso.isEmpty() || !rc.resultado().texto().equals(esperado.texto())) {
            return Optional.empty();
        }
        String valor = " → " + esperado.texto();
        PasoLimite p = paso.get();
        return Optional.of(switch (codigo) {
            case "L07" -> lhopital(rc, l).orElse("derivar arriba y abajo") + valor;
            case "L02", "L03" -> "queda " + (p.despues() == null ? "otra expresión" : texto(p.despues())) + " "
                    + punto(l) + valor;
            case "L04" -> "comparar los grados de arriba y de abajo" + valor;
            case "L06" -> p.trabajo() instanceof pe.mathnova.domain.factorizacion.TrabajoIntermedio.Agrupacion a
                    && !a.grupos().isEmpty() ? a.grupos().getFirst()
                    : "límite notable" + valor;
            default -> valor.trim();
        });
    }

    private static Optional<String> lhopital(ResolucionLimite rc, Limite l) {
        return rc.pasos().stream().filter(p -> p.codigo().equals("L07") && p.antes() instanceof Cociente)
                .findFirst().map(p -> {
                    Cociente c = (Cociente) p.antes();
                    Expresion dn = Derivador.derivar(c.numerador(), l.variable()).derivada();
                    Expresion dd = Derivador.derivar(c.denominador(), l.variable()).derivada();
                    return "(" + texto(dn) + ")/" + par(dd) + " " + punto(l);
                });
    }

    private static String motivo(String codigo, ResolucionLimite r) {
        Indeterminacion t = r.indeterminacion();
        Expresion f = r.entrada().cuerpo();
        boolean alInfinito = r.entrada().punto() instanceof Limite.Infinito;
        String hay = t == null ? "no hay indeterminación" : "hay " + t.texto();
        return switch (codigo) {
            case "L02" -> t != Indeterminacion.CERO_SOBRE_CERO ? "solo con 0/0, y aquí " + hay
                    : !SimplificadorFracciones.esFraccion(f) ? "no es una fracción de polinomios"
                    : "el numerador y el denominador no tienen un factor común";
            case "L03" -> !MotorLimites.tieneRaiz(f) ? "no hay raíces que quitar" : "el conjugado no quita la indeterminación";
            case "L04" -> !alInfinito ? "solo cuando " + r.variable() + " → ±∞"
                    : "no es un cociente de potencias de " + r.variable();
            case "L06" -> !FactorizadorFunciones.tieneFunciones(f) ? "no hay sen, cos, e ni ln"
                    : "no tiene la forma de un límite notable";
            case "L07" -> t == null || !t.esCociente() ? "solo con 0/0 o ∞/∞, y aquí " + hay
                    : "derivando no se llega a un valor";
            default -> "no aplica";
        };
    }

    private static String tabla(ResultadoLimite r) {
        return switch (r) {
            case ResultadoLimite.Finito f -> "se acerca a " + f.texto() + " por ambos lados";
            case ResultadoLimite.Infinito i -> i.positivo() ? "crece sin cota" : "decrece sin cota";
            case ResultadoLimite.NoExiste n -> "por cada lado va a un lugar distinto";
            case ResultadoLimite.NoCalculado n -> "muestra hacia dónde van los valores";
        };
    }

    private static String punto(Limite l) {
        return switch (l.punto()) {
            case Limite.Finito f -> "en " + l.variable() + " = " + texto(f.valor());
            case Limite.Infinito i -> "cuando " + l.variable() + " → " + (i.positivo() ? "∞" : "−∞");
        };
    }

    private static String texto(Expresion e) {
        return ImpresoraTexto.imprimir(e);
    }

    private static String par(Expresion e) {
        return e instanceof Suma || e instanceof Negacion ? "(" + texto(e) + ")" : texto(e);
    }
}
