package atj;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("WebSocketChatStage.fxml"));
			AnchorPane root = fxmlLoader.load();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("JavaFX Web Socket Client");
			primaryStage.sizeToScene();
			primaryStage.setOnHiding(e -> primaryStage_Hiding(e, fxmlLoader));
			primaryStage.show();

			// Setting minimal size of the window
			primaryStage.setMinWidth(primaryStage.getWidth());
			primaryStage.setMinHeight(primaryStage.getHeight());

			// Requesting focus on the message text field (it has to be after the scene is set)
			((WebSocketChatStageControler) (fxmlLoader.getController())).messageTextField.requestFocus();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void primaryStage_Hiding(WindowEvent e, FXMLLoader fxmlLoader) {
		((WebSocketChatStageControler) fxmlLoader.getController())
				.closeSession(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Stage is hiding"));
	}

	public static void main(String[] args) {
		launch(args);
	}
}
