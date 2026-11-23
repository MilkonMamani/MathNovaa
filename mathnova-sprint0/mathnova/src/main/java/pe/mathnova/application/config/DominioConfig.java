package pe.mathnova.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.mathnova.domain.factorizacion.Factorizador;
import pe.mathnova.domain.factorizacion.FactorizadorPendiente;

/** El dominio no conoce Spring; aquí se registran sus clases como beans. */
@Configuration
public class DominioConfig {

    @Bean
    public Factorizador factorizador() {
        return new FactorizadorPendiente();
    }
}
