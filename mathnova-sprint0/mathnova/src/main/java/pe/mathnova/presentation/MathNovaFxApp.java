package pe.mathnova.presentation;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.mathnova.MathNovaApplication;

/** Une JavaFX con Spring: Spring crea los controladores de cada FXML. */
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
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/entrada.fxml"));
        loader.setControllerFactory(contexto::getBean);
        Parent raiz = loader.load();
        stage.setTitle("MathNova");
        stage.setScene(new Scene(raiz, 1280, 800));
        stage.show();
    }

    @Override
    public void stop() {
        contexto.close();
        Platform.exit();
    }
}
