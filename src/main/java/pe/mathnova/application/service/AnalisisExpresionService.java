package pe.mathnova.application.service;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pe.mathnova.application.service.AnalisisExpresion.Tipo;
import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Rango;
import pe.mathnova.domain.expresion.VariablePrincipal;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

/**
 * Puente entre la pantalla de entrada y el dominio: lee, clasifica y prepara la vista previa.
 * Las operaciones (HU34) solo se reconocen y se describen; no se resuelven.
 */
@Service
public class AnalisisExpresionService {

    public AnalisisExpresion analizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return new AnalisisExpresion.Vacia();
        }
        Expresion e;
        try {
            e = Parser.parse(texto);
        } catch (ErrorSintaxis error) {
            return new AnalisisExpresion.Invalida(error.getMessage(), error.getPosicion());
        }

        String latex = ImpresoraLatex.imprimir(e);
        VariablePrincipal vp = VariablePrincipal.de(e);
        Optional<String> operacion = describirOperacion(e);
        Expresion cuerpo = cuerpo(e);
        if (Expresiones.esTrascendente(cuerpo)) {
            return new AnalisisExpresion.Valida(latex, vp.variables(), vp.constantes(), Tipo.TRASCENDENTE,
                    OptionalInt.empty(), operacion);
        }
        try {
            Polinomio p = Polinomio.desde(cuerpo);
            OptionalInt grado = p.esCero() ? OptionalInt.empty() : OptionalInt.of(p.grado());
            return new AnalisisExpresion.Valida(latex, vp.variables(), vp.constantes(), Tipo.POLINOMIO, grado,
                    operacion);
        } catch (NoEsPolinomioException noEs) {
            return new AnalisisExpresion.Valida(latex, vp.variables(), vp.constantes(), Tipo.ALGEBRAICA,
                    OptionalInt.empty(), operacion);
        } catch (ArithmeticException divisionEntreCero) {
            return new AnalisisExpresion.Invalida("La expresión divide entre cero", 0);
        }
    }

    /** En una operación, la expresión sobre la que actúa; si no, la expresión misma. */
    private static Expresion cuerpo(Expresion e) {
        return switch (e) {
            case Limite l -> l.cuerpo();
            case Derivada d -> d.cuerpo();
            case DerivadaParcial d -> d.cuerpo();
            case Integral i -> i.cuerpo();
            case IntegralMultiple i -> i.cuerpo();
            case pe.mathnova.domain.expresion.LimiteDoble l -> l.cuerpo();
            case pe.mathnova.domain.expresion.Extremos x -> x.funcion();
            default -> e;
        };
    }

    /** "límite cuando x → 2⁺", "derivada de orden 2 respecto de x", "integral definida de 0 a 1 respecto de x"… */
    private static Optional<String> describirOperacion(Expresion e) {
        return Optional.ofNullable(switch (e) {
            case Limite l -> {
                String punto = switch (l.punto()) {
                    case Limite.Finito f -> ImpresoraTexto.imprimir(f.valor());
                    case Limite.Infinito inf -> inf.positivo() ? "∞" : "-∞";
                };
                String lado = switch (l.lado()) {
                    case AMBOS -> "";
                    case DERECHA -> "⁺";
                    case IZQUIERDA -> "⁻";
                };
                yield "límite cuando " + l.variable() + " → " + punto + lado;
            }
            case Derivada d -> "derivada " + orden(d.orden()) + "respecto de " + d.variable();
            case DerivadaParcial d -> "derivada parcial " + orden(d.orden()) + "respecto de " + lista(d.variables());
            case Integral i -> i.esDefinida()
                    ? "integral definida de " + ImpresoraTexto.imprimir(i.inferior()) + " a "
                            + ImpresoraTexto.imprimir(i.superior()) + " respecto de " + i.variable()
                    : "integral indefinida respecto de " + i.variable();
            case IntegralMultiple i -> (i.rangos().size() == 2 ? "integral doble" : "integral triple")
                    + " respecto de " + lista(i.rangos().stream().map(Rango::variable).toList());
            case pe.mathnova.domain.expresion.LimiteDoble l -> "límite doble cuando (" + l.x() + ", " + l.y() + ") → ("
                    + ImpresoraTexto.imprimir(l.a()) + ", " + ImpresoraTexto.imprimir(l.b()) + ")";
            case pe.mathnova.domain.expresion.Extremos x -> x.conRestriccion()
                    ? "extremos con la restricción " + ImpresoraTexto.imprimir(x.restriccion())
                    : "puntos críticos y extremos";
            default -> null;
        });
    }

    private static String orden(int orden) {
        return orden == 1 ? "" : "de orden " + orden + " ";
    }

    private static String lista(List<Character> letras) {
        return letras.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
