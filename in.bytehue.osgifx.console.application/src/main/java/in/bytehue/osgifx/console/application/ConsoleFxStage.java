package in.bytehue.osgifx.console.application;

import static javafx.concurrent.Worker.State.SUCCEEDED;

import org.eclipse.fx.ui.workbench.fx.DefaultJFXApp;

import javafx.animation.FadeTransition;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class ConsoleFxStage extends DefaultJFXApp {

    private static final String SPLASH_IMAGE = "/graphic/images/splash.png";

    private Pane        splashLayout;
    private ProgressBar loadProgress;
    private Label       progressText;
    private Stage       initStage;

    private static final int SPLASH_WIDTH  = 695;
    private static final int SPLASH_HEIGHT = 227;

    @Override
    public void init() throws Exception {
        final ImageView splash = new ImageView(new Image(SPLASH_IMAGE));

        loadProgress = new ProgressBar();
        loadProgress.setPrefWidth(SPLASH_WIDTH - 20);
        progressText = new Label("");
        splashLayout = new VBox();

        splashLayout.getChildren().addAll(splash, loadProgress, progressText);
        progressText.setAlignment(Pos.CENTER);
        splashLayout.setStyle("-fx-padding: 5; -fx-background-color: cornsilk; -fx-border-width:5; -fx-border-color: "
                + "linear-gradient(to bottom, chocolate, derive(chocolate, 50%));");
        splashLayout.setEffect(new DropShadow());
    }

    @Override
    public void start(final Stage initStage) throws Exception {
        this.initStage = initStage;
        final Task<Void> friendTask = new Task<Void>() {
            @Override
            protected Void call() throws InterruptedException {
                updateMessage("Initializing Console . . .");
                for (int i = 0; i < 5; i++) {
                    Thread.sleep(400);
                    updateProgress(i + 1, 5);
                }
                Thread.sleep(400);
                updateMessage("Console Initialized.");
                return null;
            }
        };
        showSplash(initStage, friendTask, this::showFxConsoleStage);
        new Thread(friendTask).start();
    }

    private void showFxConsoleStage() {
        initialize();
        e4Application.jfxStart(e4Application.getApplicationContext(), this, initStage);
    }

    private void showSplash(final Stage initStage, final Task<?> task, final InitCompletionHandler initCompletionHandler) {
        progressText.textProperty().bind(task.messageProperty());
        loadProgress.progressProperty().bind(task.progressProperty());
        task.stateProperty().addListener((observableValue, oldState, newState) -> {
            if (newState == SUCCEEDED) {
                loadProgress.progressProperty().unbind();
                loadProgress.setProgress(1);
                initStage.toFront();
                final FadeTransition fadeSplash = new FadeTransition(Duration.seconds(1.2), splashLayout);
                fadeSplash.setFromValue(1.0);
                fadeSplash.setToValue(0.0);
                fadeSplash.setOnFinished(actionEvent -> initStage.hide());
                fadeSplash.play();

                initCompletionHandler.complete();
            }
        });

        final Scene       splashScene = new Scene(splashLayout, Color.TRANSPARENT);
        final Rectangle2D bounds      = Screen.getPrimary().getBounds();

        splashScene.getStylesheets().add(getClass().getResource("/css/default.css").toExternalForm());

        initStage.setScene(splashScene);
        initStage.setX(bounds.getMinX() + bounds.getWidth() / 2d - SPLASH_WIDTH / 2d);
        initStage.setY(bounds.getMinY() + bounds.getHeight() / 2d - SPLASH_HEIGHT / 2d);
        initStage.initStyle(StageStyle.TRANSPARENT);
        initStage.setAlwaysOnTop(true);
        initStage.show();
    }

    @FunctionalInterface
    public interface InitCompletionHandler {
        void complete();
    }

}