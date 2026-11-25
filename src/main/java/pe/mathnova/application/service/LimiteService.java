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

@Service
public class LimiteService {

    private final OrquestadorFactorizacion orquestador;
    private final MotorLimites motor;

    public LimiteService(OrquestadorFactorizacion orquestador) {
        this.orquestador = orquestador;
        this.motor = new MotorLimites(orquestador);
    }

    public ResolucionLimite resolver(String texto, Consumer<FaseFactorizacion> avance) {
        return resolver(texto, null, avance);
    }

    public ResolucionLimite resolver(String texto, String forzado, Consumer<FaseFactorizacion> avance) {
        avance.accept(FaseFactorizacion.LECTURA);
        Limite l = limite(texto);
        avance.accept(FaseFactorizacion.METODOS);
        return motor.resolver(l, forzado);
    }

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

    public List<CaminosLimite.Camino> caminos(ResolucionLimite r) {
        return new CaminosLimite(motor).de(r);
    }

    public TablaValores tabla(ResolucionLimite r) {
        return TablaValores.de(r.entrada());
    }

    public Optional<Asintotas> asintotas(ResolucionLimite r) {
        return fraccion(r).flatMap(Asintotas::de);
    }

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
