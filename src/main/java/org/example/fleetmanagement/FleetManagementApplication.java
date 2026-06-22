package org.example.fleetmanagement;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Application entry point that boots the Spring context and drives the JavaFX UI.
 */
@SpringBootApplication(scanBasePackages = "org.example.fleetmanagement")
public class FleetManagementApplication extends Application {

    private static ConfigurableApplicationContext springContext;
    private static Stage primaryStage;

    // JavaFX startup: initializes Spring, sets the window icon and shows the login screen.
    @Override
    public void start(Stage stage) throws Exception {
        springContext = new SpringApplicationBuilder(FleetManagementApplication.class)
                .headless(false)
                .run();

        primaryStage = stage;
        primaryStage.getIcons().add(new Image(
            FleetManagementApplication.class.getResourceAsStream("/images/logo.png")));
        showLoginScreen();
    }

    // JavaFX shutdown: closes the Spring context and exits the platform.
    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    // Standard Java main method that launches the JavaFX application.
    public static void main(String[] args) {
        launch(args);
    }

    // Loads and displays the login view, wiring controllers through Spring.
    public static void showLoginScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                FleetManagementApplication.class.getResource("/fxml/login-view.fxml")
        );
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 400, 300);

        primaryStage.setTitle("System zarządzania flotą — Logowanie");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Loads and displays the main application view, wiring controllers through Spring.
    public static void showMainScreen() throws Exception {
        FXMLLoader loader = new FXMLLoader(
                FleetManagementApplication.class.getResource("/fxml/main-view.fxml")
        );
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 700);

        primaryStage.setTitle("System zarządzania flotą");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
