package atj;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

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

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

public class WebSocketChatStageControler {

	static Semaphore mutexSend = new Semaphore(1);

	@FXML
	TextField userTextField;
	@FXML
	TextArea chatTextArea;
	@FXML
	TextField messageTextField;

	@FXML
	ListView<String> fileListView;
	OutputStream userFileStream;

	@FXML
	Button btnSet;
	@FXML
	Button btnSend;
	@FXML
	Button btnUpload;

	private String user;
	private WebSocketClient webSocketClient;

	@FXML
	private void initialize() {
		webSocketClient = new WebSocketClient();

		user = userTextField.getText() + webSocketClient.session.getId().hashCode();
		userTextField.setText(user);

		(new File(webSocketClient.session.getId() + "/")).mkdirs();
	}

	@FXML
	private void btnSet_Click() {
		if (!userTextField.getText().isEmpty()) {
			user = userTextField.getText();
		}
	}

	@FXML
	private void btnSend_Click() {
		if (!messageTextField.getText().isEmpty()) {
			webSocketClient.sendMessage(messageTextField.getText());
			messageTextField.setText("");
		}
	}

	@FXML
	public void messageTextField_EnterPressed(KeyEvent ev) {
		if (ev.getCode() == KeyCode.ENTER) {
			btnSend_Click();
		}
	}

	@FXML
	public void userTextField_EnterPressed(KeyEvent ev) {
		if (ev.getCode() == KeyCode.ENTER) {
			btnSet_Click();
		}
	}

	@FXML
	public void btnUpload_Click() {
		File selectedFile = (new FileChooser()).showOpenDialog(null);

		if (selectedFile != null) {
			UploadThread uploadThread = new UploadThread(selectedFile);
			uploadThread.start();
		}
	}

	class UploadThread extends Thread {
		private File selectedFile;

		UploadThread(File selectedFile) {
			this.selectedFile = selectedFile;
		}

		public void run() {
			try {
				String filename = selectedFile.getName();
				System.out.println("File " + filename + " is being uploaded...");

				InputStream input = new FileInputStream(selectedFile);

				byte[] buffer = new byte[1024];
				byte[] tmp_buffer = new byte[1024 - 255];

				System.arraycopy(filename.getBytes(), 0, buffer, 0, filename.getBytes().length);
				OutputStream output = webSocketClient.session.getBasicRemote().getSendStream();
				int read;
				while ((read = input.read(tmp_buffer)) > 0) {
					System.arraycopy(tmp_buffer, 0, buffer, 255, read);

					mutexSend.acquire();
					
					output.write(buffer, 0, 255 + read);
					
					mutexSend.release();
				}
				output.close();
				input.close();

				System.out.println("File " + filename + " uploaded!");
				webSocketClient.sendFilename(filename);
			} catch (IOException e) {
				e.printStackTrace();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	public void fileListView_Click() {
		Integer fileIndex = fileListView.getSelectionModel().getSelectedIndex();

		if (fileIndex < 0)
			return;

		String filename = fileListView.getSelectionModel().getSelectedItem();
		
		fileListView.getSelectionModel().clearSelection();
		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialFileName(filename);
		File file = fileChooser.showSaveDialog(null);
		
		SaveFileThread saveFileThread = new SaveFileThread( file, filename );
		saveFileThread.start();
		
	}
	
	class SaveFileThread extends Thread {
		private File file;
		private String originalFilename;
		
		SaveFileThread( File file, String originalFilename ) {
			this.file = file;
			this.originalFilename = originalFilename;
		}
		
		public void run() {			
			
			if (file != null) {
				try {
					InputStream initialStream = new FileInputStream(webSocketClient.session.getId() + "/" + originalFilename);
									
					OutputStream outStream = new FileOutputStream(file);
					byte[] buffer = new byte[1024*1024];
					int read;
					while( (read = initialStream.read(buffer)) > 0 ) {
						outStream.write(buffer, 0, read);
					}
					outStream.close();
					initialStream.close();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			System.out.println("File " + originalFilename + " saved as " + file.getName());
		}
	}
	
	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		} catch (IOException e) {
			e.printStackTrace();
			Platform.exit();
		}
	}

	@ClientEndpoint
	public class WebSocketClient {

		private Session session;

		public WebSocketClient() {
			connnectToWebSocket();
		}

		@OnOpen
		public void onOpen(Session session) {
			System.out.println("Connection is opened.");
			this.session = session;
		}

		@OnClose
		public void onClose(CloseReason closeReason) {
			for (String filename : fileListView.getItems()) {
				try {
					userFileStream = new FileOutputStream(webSocketClient.session.getId() + "/" + filename, true);
					userFileStream.close();
				} catch (FileNotFoundException e) {
					System.out.println("Couldn't find " + filename + "!");
				} catch (IOException e) {
					e.printStackTrace();
				}

				boolean isDeleted = (new File(webSocketClient.session.getId() + "/" + filename)).delete();
				System.out.println("Deleted ("+filename+"): " + isDeleted);
			}
			(new File(webSocketClient.session.getId() + "/")).delete();

			System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
		}

		@OnError
		public void onError(Throwable throwable) {
			System.out.println("Error occured");
			throwable.printStackTrace();
		}

		@OnMessage
		public void onMessage(String message, Session session) {
			if (message.indexOf(':') >= 0) {
				System.out.println("Message was received.");
				chatTextArea.appendText(message + '\n');
				return;
			}
			System.out.println("Filename was received.");
			System.out.println("Filename:" + message);

			Platform.runLater(() -> fileListView.getItems().add(message));
		}

		@OnMessage
		public void onMessage(ByteBuffer buf, boolean last, Session session) {
			if (buf.remaining() != 0) {

				// Konwertowanie nazwy pliku
				String filename = new String(Arrays.copyOfRange(buf.array(), 0, 255), StandardCharsets.UTF_8);
				for (int i = 0; i < 255; ++i) {
					if (filename.charAt(i) == 0) {
						filename = filename.substring(0, i);
						break;
					}
				}

				//System.out.println(filename);
				try {
					userFileStream = new FileOutputStream(webSocketClient.session.getId() + "/" + filename, true);

					byte[] tmp = Arrays.copyOfRange(buf.array(), 255, buf.remaining());
					userFileStream.write(tmp);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			//System.out.println(buf.remaining());
		}

		public void sendFilename(String filename) {
			System.out.println("Sending filename: " + filename);
			try {
				mutexSend.acquire();
				session.getBasicRemote().sendText(filename);
				mutexSend.release();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		private void connnectToWebSocket() {
			WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
			try {
				URI uri = URI.create("ws://localhost:8080/WebSocketEndpoint/websocketendpoint");
				webSocketContainer.connectToServer(this, uri);
			} catch (DeploymentException | IOException e) {
				e.printStackTrace();
				Platform.exit();
			}
		}

		public void sendMessage(String message) {
			try {
				System.out.println("Message was sent: " + message);
				mutexSend.acquire();
				session.getBasicRemote().sendText(user + ": " + message);
				mutexSend.release();
			} catch (IOException ex) {
				ex.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	} // public class WebSocketClient
} // public class WebSocketChatStageControler
