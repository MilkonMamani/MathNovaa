package pe.mathnova.application.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.domain.calculo.limites.CaminosLimite;
import pe.mathnova.domain.calculo.limites.MotorLimites;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.TablaValores;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.parser.Parser;

/**
 * HU11 y HU24: resuelve lim(x→a, f) con la cascada de {@link MotorLimites} y lo guarda en el
 * historial (RN24). También da la tabla de valores y, si f es racional, sus asíntotas.
 */
@Service
public class LimiteService {

    private final OrquestadorFactorizacion orquestador;
    private final MotorLimites motor;

    /** Desde el Sprint 9 no guarda: lo hace {@link ResolverService#guardar}. */
    public LimiteService(OrquestadorFactorizacion orquestador) {
        this.orquestador = orquestador;
        this.motor = new MotorLimites(orquestador);
    }

    /** @throws IllegalArgumentException si el texto no es un límite */
    public ResolucionLimite resolver(String texto, Consumer<FaseFactorizacion> avance) {
        return resolver(texto, null, avance);
    }

    /**
     * "Probar" en Exploración: ante la indeterminación se prueba primero {@code forzado} ("L07"…).
     *
     * @param forzado null en automático
     */
    public ResolucionLimite resolver(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Limite l = limite(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return motor.resolver(l, forzado);
    }

    /** Sin guardar: para el resultado en vivo. */
    public ResolucionLimite calcular(String texto) {
        return motor.resolver(limite(texto));
    }

    private static Limite limite(String texto) {
        Expresion e = Parser.parse(texto);
        if (!(e instanceof Limite l)) {
            throw new IllegalArgumentException("No es un límite: " + texto);
        }
        return l;
    }

    /** Exploración: el método usado, los otros caminos que llegan al mismo valor y los que no aplican. */
    public List<CaminosLimite.Camino> caminos(ResolucionLimite r) {
        return new CaminosLimite(motor).de(r);
    }

    public TablaValores tabla(ResolucionLimite r) {
        return TablaValores.de(r.entrada());
    }

    /** RF26 para la función del límite, si es una fracción de polinomios. */
    public Optional<Asintotas> asintotas(ResolucionLimite r) {
        return fraccion(r).flatMap(Asintotas::de);
    }

    /** La función del límite simplificada, si es una fracción de polinomios (para la gráfica). */
    public Optional<Simplificacion> fraccion(ResolucionLimite r) {
        Expresion f = r.entrada().cuerpo();
        if (!SimplificadorFracciones.esFraccion(f)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SimplificadorFracciones(orquestador).simplificar(f));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
