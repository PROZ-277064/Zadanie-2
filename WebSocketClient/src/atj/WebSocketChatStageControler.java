package atj;

import java.io.IOException;
import java.net.URI;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class WebSocketChatStageControler {
	@FXML TextField userTextField;
	@FXML TextArea chatTextArea;
	@FXML TextField messageTextField;
	@FXML Button btnSet;
	@FXML Button btnSend;
	private String user;
	private WebSocketClient webSocketClient;
	@FXML private void initialize() {
		System.out.println("initialize()");
		webSocketClient = new WebSocketClient();
		//user = userTextField.getText();
	}
	@FXML private void btnSet_Click() {
		System.out.println("btnSet_Click()");
		if (userTextField.getText().isEmpty()) { return; }
		user = userTextField.getText();
	}
	
	@FXML private void btnSend_Click() {
		System.out.println("btnSend_Click()");
		webSocketClient.sendMessage(messageTextField.getText());
	}
	
	public void closeSession(CloseReason closeReason) {
		System.out.println("closeSession()");
		try {
			webSocketClient.session.close(closeReason);
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@ClientEndpoint
	public class WebSocketClient {
		private Session session;
		public WebSocketClient() { 
			System.out.println("Konstruktor. connectToWebSocket();"); 
			connnectToWebSocket();
			System.out.println("Konstruktor. connectToWebSocket() end;");
			
		}
		@OnOpen public void onOpen(Session session) {
			System.out.println("onOpen()");
			System.out.println("Connection is opened.");
			this.session = session;
		}
		@OnClose public void onClose(CloseReason closeReason) {
			System.out.println("onClose()");
			System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
		}
		@OnError public void onError(Throwable throwable) {
			System.out.println("Error occured");
			throwable.printStackTrace();
		}
		@OnMessage public void onMessage(String message, Session session) {
			System.out.println("Message was received");
			chatTextArea.setText(chatTextArea.getText() + message + "\n");
		}
		
		private void connnectToWebSocket() {
			System.out.println("connectToWebSocket();");
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebSocketEndpoint/websocketendpoint");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) { e.printStackTrace(); }
		}
		
		public void sendMessage(String message) {
			try {
				System.out.println("Message was sent: " + message);
				session.getBasicRemote().sendText(user + ": " + message);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	} // public class WebSocketClient
} // public class WebSocketChatStageControler
