package pe.mathnova.presentation;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.mathnova.MathNovaApplication;

public class MathNovaFxApp extends Application {

    private ConfigurableApplicationContext contexto;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        contexto = new SpringApplicationBuilder(MathNovaApplication.class)
                .headless(false)
                .run(args);
    }

    @Override
    public void start(Stage stage) {
        contexto.getBean(Navegador.class).iniciar(stage);
        stage.show();
    }

    @Override
    public void stop() {
        contexto.close();
        Platform.exit();
    }
}
