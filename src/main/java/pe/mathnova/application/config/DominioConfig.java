package pe.mathnova.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;

/** El dominio no conoce Spring; aquí se registran sus clases como beans. */
@Configuration
public class DominioConfig {

    @Bean
    public OrquestadorFactorizacion factorizador() {
        return new OrquestadorFactorizacion();
    }

    /** HU38: ecuaciones polinómicas en Q, con el mismo orquestador. */
    @Bean
    public ResolvedorEcuaciones resolvedorEcuaciones(OrquestadorFactorizacion orquestador) {
        return new ResolvedorEcuaciones(orquestador);
    }
}
