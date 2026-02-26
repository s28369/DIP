package org.example.fleetmanagement;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "org.example.fleetmanagement")
public class FleetManagementApplication extends Application {

    private static ConfigurableApplicationContext springContext;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        springContext = new SpringApplicationBuilder(FleetManagementApplication.class)
                .headless(false)
                .run();

        primaryStage = stage;
        showLoginScreen();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static void showLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                FleetManagementApplication.class.getResource("/fxml/login-view.fxml")
        );
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("System Zarządzania Flotą – Logowanie");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void showMainScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                FleetManagementApplication.class.getResource("/fxml/main-view.fxml")
        );
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 700);

        primaryStage.setTitle("System Zarządzania Flotą");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
