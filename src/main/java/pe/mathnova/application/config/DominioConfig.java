package pe.mathnova.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;

@Configuration
public class DominioConfig {

    @Bean
    public OrquestadorFactorizacion factorizador() {
        return new OrquestadorFactorizacion();
    }

    @Bean
    public ResolvedorEcuaciones resolvedorEcuaciones(OrquestadorFactorizacion orquestador) {
        return new ResolvedorEcuaciones(orquestador);
    }
}
