package application;
	
import java.awt.Color;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.text.Font;


public class Main extends Application {
	
	private static boolean DEBUG = false;
	public static Image studioImage;
	
	private static String emailTo = "";
	private static String emailFrom = "";
	private static String universeId = "";
	private static String blacklists = "";
	public static int importX = -1;
	public static int importY = -1;
	public static int errorX = -1;
	public static int errorY = -1;
	private static int maxHangTime = 60;
	private static String studioFilePath;
	private static String saveFolderPath;
	public static Dimension screenSize;
	private long startTime = 0;
	
	public static boolean isListening = false;
	public static boolean dataUpdate = false;
	private static boolean isPublishing = false;
	public static GlobalKeyListener keyListener;
	
	
	public static int colorCode = 0;
	private static int listeningTo = -1;
	
	private Logger logger;
	private boolean didInitializeLogging = false;
	
	private void startLogger() {
		if (saveFolderPath==null || didInitializeLogging) { return; }
		try {
			Handler handler = new FileHandler(saveFolderPath + "\\LatestLog.log");
	        Logger.getLogger("Output").addHandler(handler);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        handler.setFormatter(formatter); 
		}catch(Exception e) {
			e.printStackTrace();
		}
		didInitializeLogging = true;
	}
	
	public static void updateImportPosition(int x, int y) {
		try {
			Color c = new Robot().getPixelColor(x, y);
			colorCode = c.getRGB();
		}catch(Exception e) {
			e.printStackTrace();
		}
		importX = x;
		importY = y;
		
	}
	
	public static void updateErrorPosition(int x, int y) {
		errorX = x;
		errorY = y;
	}
	
	private String getUniverseIdFromPlace(String placeId) {
		
		String universe = null;
		
		String url = "https://api.roblox.com/universes/get-universe-containing-place?placeid=" + placeId;
		try {
			System.out.println("Checking url: " + url);
			InputStream response = new URL(url).openStream();
			@SuppressWarnings("unchecked")
			Map<String,Object> result = new ObjectMapper().readValue(response, HashMap.class);
			
			universe = result.get("UniverseId").toString();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return universe;
	}
	
	public void run(Label status) {
		ArrayList<String> blacklistedPlaces = new ArrayList<String>();
		Scanner in = new Scanner(blacklists);
		in.useDelimiter(Pattern.compile(",\\s*"));
		while (in.hasNext()) {
			String id = in.next();
			blacklistedPlaces.add(id);
			logger.info("Ignoring place ID " + id + "...");
		}
		in.close();
		try {
			if (!DEBUG) {
				Robot robot = new Robot();
				boolean timedOut = false;
				
				ArrayList<String> placeIds = new ArrayList<String>(20);
				ArrayList<String> placeNames = new ArrayList<String>(20);
				
				String url = "https://develop.roblox.com/v1/universes/" + universeId + "/places?sortOrder=Asc&limit=10";
				
				
				try {
					
					String nextPage;
					do {
						//System.out.println("Checking url: " + url);
						status.setText("Checking url: " + url);
						InputStream response = new URL(url).openStream();
						@SuppressWarnings("unchecked")
						Map<String,Object> result = new ObjectMapper().readValue(response, HashMap.class);
						nextPage = (String) result.get("nextPageCursor");
						@SuppressWarnings({ "unchecked", "rawtypes" })
						ArrayList<HashMap> data = (ArrayList<HashMap>) result.get("data");
						for (int i = 0; i < data.size(); i++) {
							String nextId = data.get(i).get("id").toString();
							String nextName = data.get(i).get("name").toString();
							if (blacklistedPlaces.contains(nextId)) {
								logger.info("Ignored " + nextName + " (id = " + nextId + ")");
								continue;
							}
							placeIds.add(nextId);
							placeNames.add(nextName);
							//System.out.println("Got " + nextName + " (id = " + nextId + ")");
							status.setText("Got " + nextName + " (id = " + nextId + ")");
							logger.info("Got " + nextName + " (id = " + nextId + ")");
						}
						url = "https://develop.roblox.com/v1/universes/" + universeId + "/places?sortOrder=Asc&limit=10&cursor=" + nextPage;
					}while (nextPage != null);
					
					
				}catch(Exception e) {
					e.printStackTrace();
				}
				for (int i = 0; i < placeNames.size(); i++) {
					System.out.println(i + ": \t" + placeNames.get(i));
				}
				
				
				
				for (int i = 0; i < placeIds.size(); i++) {
					
					
					//FileWriter writer = new FileWriter(batchFile,false);
					//writer.write(studioFilePath + " -task EditPlace -universeId " + universeId + " -placeId " + placeIds.get(i));
					//writer.flush();
					//writer.close();
					ProcessBuilder pb = new ProcessBuilder(studioFilePath, "-task", "EditPlace", "-universeId", universeId, "-placeId", placeIds.get(i));
					Process process = pb.start();
					status.setText("Opening place ID: " + placeIds.get(i) + " (" + placeNames.get(i) + ")...");
					logger.info("Opening place ID: " + placeIds.get(i) + " (" + placeNames.get(i) + ")...");
					robot.mouseMove(0, 0);
					//Thread.sleep(24000);
					Thread.sleep(300);
					int timeOut = 0;
					while (true) {
						Color c = robot.getPixelColor(importX, importY);
						if (c.getRGB() == colorCode)
							break;
						Thread.sleep(100);
						timeOut++;
						if (timeOut > maxHangTime*10) {//studio is taking too long
							timedOut = true;
							break;
						}
					}
					if (timedOut) {
						status.setText("ERROR: TIMED OUT");
						logger.severe("ERROR: TIMED OUT. Place did not load in time?");
						break;
					}
					Thread.sleep(30000);
					status.setText("Place loaded, publishing and exiting...");
					robot.keyPress(KeyEvent.VK_ALT);
					robot.keyPress(KeyEvent.VK_P);
					Thread.sleep(300);
					robot.keyRelease(KeyEvent.VK_ALT);
					robot.keyRelease(KeyEvent.VK_P);
					Thread.sleep(300);
					boolean failedToPublish = false;
					for (int j = 0; j < 10; j++) {
						Color c = robot.getPixelColor(errorX, errorY);
						if (c.getRGB() == -16735489) {
							failedToPublish = true;
							break;
						}
						Thread.sleep(300);
					}
					if (failedToPublish) {
						status.setText("ERROR: COULD NOT PUBLISH");
						logger.severe("ERROR: COULD NOT PUBLISH. Could there be an unpublished package?");
						break;
					}
					int x = screenSize.width;
					robot.mouseMove(x, 1);
					robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
					//robot.keyPress(KeyEvent.VK_ALT);
					//robot.keyPress(KeyEvent.VK_F4);
					Thread.sleep(300);
					robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					//robot.keyRelease(KeyEvent.VK_ALT);
					//robot.keyRelease(KeyEvent.VK_F4);
					status.setText("Waiting for publish to finish...");
					process.waitFor();
					
					status.setText("Closing");
					
					
					
					while (true) {
						Color c = robot.getPixelColor(importX, importY);
						//logger.info("c = " + c.getRGB());
						if (c.getRGB() != colorCode)
							break;
						Thread.sleep(100);
						timeOut++;
						if (timeOut > maxHangTime*10) {//studio is taking too long
							timedOut = true;
							break;
						}
					}
					if (timedOut) {
						status.setText("ERROR: TIMED OUT CLOSING");
						logger.severe("ERROR: TIMED OUT CLOSING. Place did not close in time?");
						break;
					}
					logger.info("Place \"" + placeNames.get(i) + "\" published and closed successfully!");
					Thread.sleep(2000);
					
					/*robot.keyPress(KeyEvent.VK_CONTROL);
					robot.keyPress(KeyEvent.VK_SHIFT);
					robot.keyPress(KeyEvent.VK_ESCAPE);
					Thread.sleep(300);
					robot.keyRelease(KeyEvent.VK_CONTROL);
					robot.keyRelease(KeyEvent.VK_SHIFT);
					robot.keyRelease(KeyEvent.VK_ESCAPE);
					Thread.sleep(1000);
					robot.keyPress(KeyEvent.VK_R);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_R);
					robot.keyPress(KeyEvent.VK_O);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_O);
					robot.keyPress(KeyEvent.VK_B);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_B);
					robot.keyPress(KeyEvent.VK_L);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_L);
					robot.keyPress(KeyEvent.VK_O);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_O);
					robot.keyPress(KeyEvent.VK_X);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_X);
					robot.keyPress(KeyEvent.VK_DELETE);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_DELETE);
					Thread.sleep(100);
					robot.keyPress(KeyEvent.VK_ALT);
					robot.keyPress(KeyEvent.VK_F4);
					Thread.sleep(300);
					robot.keyRelease(KeyEvent.VK_ALT);
					robot.keyRelease(KeyEvent.VK_F4);
					*/
				}
				
				logger.info("Entire universe published!");
				status.setText("Entire universe published!");
				
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public String getStatus() {
		if (saveFolderPath == null) 
			return "[CRITICAL] No save path selected";
		if (studioFilePath == null)
			return "[CRITICAL] Studio path not set";
		if (importX == -1 || importY == -1)
			return "[CRITICAL] Import button location not calibrated";
		if (universeId.equals(""))
			return "[CRITICAL] Universe ID is not set";
		else {
			if (!universeId.matches("roblox.com/games/%d+/")) {
				try {
					int n = Integer.parseInt(universeId);
					if (n <= 0)
						return "[CRITICAL] Universe ID must be a positive number";
				}catch(Exception e) {
					return "[CRITICAL] Universe ID must be a number";
				}
			}
		}
		if (errorX == -1 || errorY == -1)
			return "[WARNING] Error button location not calibrated; will cause hang delay";
		if (maxHangTime <= 0)
			return "[WARNING] Max hang time is too low; The program may not work";
		return "ok";
	}
	
	@Override
	public void start(Stage primaryStage) {
		
		// Clear previous logging configurations.
		//LogManager.getLogManager().reset();
		logger = Logger.getLogger("Output");
		logger.setLevel(Level.INFO);
		// Get the logger for "org.jnativehook" and set the level to off.
		Logger nativeLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		nativeLogger.setLevel(Level.OFF);
		//String test = "https://www.roblox.com/games/21532277/Notoriety?refPageId=23f5e10e-364f-4c6d-8220-e959e2978b46";
		//System.out.println(test.matches(".*\\.roblox\\.com/games/\\d+/.*"));
		
		try {
			
			//load settings:
			
			Preferences prefs = Preferences.userRoot().node(this.getClass().getName());
			saveFolderPath = prefs.get("SavePath", null);
			
			if (saveFolderPath!=null) {
			    ObjectMapper mapper = new ObjectMapper();
			    @SuppressWarnings("unchecked")
				Map<String,Object> result = mapper.readValue(new File(saveFolderPath + "\\Data.json"), HashMap.class);
			    emailTo = (String) result.get("emailto");
			    emailFrom = (String) result.get("emailfrom");
			    universeId = (String) result.get("universeid");
			    blacklists = (String) result.get("blacklists");
			    importX = (int) result.get("importx");
			    importY = (int) result.get("importy");
			    errorX = (int) result.get("errorx");
			    errorY = (int) result.get("errory");
			    maxHangTime = (int) result.get("maxhang");
			    studioFilePath = (String) result.get("studiopath");
			    colorCode = (int) result.get("importcolorcode");
			}else {
				System.out.println("SavePath is null!");
			}
			
			
			//graphical user interface:
			
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root,400,600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			primaryStage.setTitle("Roblox Universe Publisher");
			VBox titleLocation = new VBox();
			Label title = new Label("Roblox Universe Publisher");
			title.setFont(new Font("Arial", 24));
			titleLocation.setAlignment(Pos.CENTER);
			VBox.setMargin(title, new Insets(10,0,0,0));
			Label studioIcon = new Label("");
			//studioImage = loadImage( "resources/Images/ROBLOX_Studio.png" );
			studioImage = loadImage("/image/ROBLOX_Studio.png");
			ImageView view = new ImageView(studioImage);
			view.setFitHeight(80);
			view.setPreserveRatio(true);
			studioIcon.setGraphic(view);
			Label notificationTitle = new Label("Notifications (leave blank for no notifications):");
			VBox.setMargin(notificationTitle, new Insets(10,0,0,0));
			HBox notificationUserId = new HBox();
			VBox.setMargin(notificationUserId, new Insets(10,0,0,0));
			Label notificationEmailLabel = new Label("Sender Email: ");
			TextField notificationEmailField = new TextField(emailFrom);
			HBox.setMargin(notificationEmailLabel, new Insets(0,0,0,10));
			notificationEmailLabel.setPrefWidth(175);
			notificationUserId.getChildren().addAll(notificationEmailLabel, notificationEmailField);
			HBox notificationUserPwd = new HBox();
			Label notificationPasswordLabel = new Label("Sender Email Password: ");
			PasswordField notificationPasswordField = new PasswordField();
			HBox.setMargin(notificationPasswordLabel, new Insets(0,0,0,10));
			notificationPasswordLabel.setPrefWidth(175);
			notificationUserPwd.getChildren().addAll(notificationPasswordLabel, notificationPasswordField);
			HBox receiverEmail = new HBox();
			VBox.setMargin(receiverEmail, new Insets(10,0,0,0));
			Label receiverEmailLabel = new Label("Destination Email: ");
			TextField receiverEmailField = new TextField(emailTo);
			HBox.setMargin(receiverEmailLabel, new Insets(0,0,0,10));
			receiverEmailLabel.setPrefWidth(175);
			receiverEmail.getChildren().addAll(receiverEmailLabel, receiverEmailField);
			titleLocation.getChildren().addAll(title, studioIcon, notificationTitle, notificationUserId, notificationUserPwd, receiverEmail);
			root.setTop(titleLocation);
			VBox rowContainer = new VBox();
			rowContainer.setAlignment(Pos.CENTER);
			HBox row1 = new HBox();
			Label setUniverseLabel = new Label("Universe ID (or link to place): ");
			TextField setUniverseField = new TextField(universeId);
			HBox.setMargin(setUniverseLabel, new Insets(0,0,0,10));
			row1.getChildren().addAll(setUniverseLabel,setUniverseField);
			HBox blacklistRow = new HBox();
			Label setBlackListIdsLabel = new Label("Blacklist place IDs (separate by commas): ");
			TextField setBlackListIdsField = new TextField(blacklists);
			HBox.setMargin(setBlackListIdsLabel, new Insets(0,0,0,10));
			blacklistRow.getChildren().addAll(setBlackListIdsLabel, setBlackListIdsField);
			BorderPane row2 = new BorderPane();
			Label recalibrateImportLabel = new Label("Import button: ");
			recalibrateImportLabel.setPrefWidth(100);
			Button recalibrateImportButton = new Button("Recalibrate");
			Label recalibrateImportLocationLabel = new Label(importX + ", " + importY); //"49, 648"
			recalibrateImportLocationLabel.setPrefWidth(100);
			BorderPane.setMargin(recalibrateImportLabel, new Insets(0,0,0,10));
			BorderPane.setMargin(recalibrateImportLocationLabel, new Insets(0,10,0,0));
			row2.setLeft(recalibrateImportLabel);
			row2.setCenter(recalibrateImportButton);
			row2.setRight(recalibrateImportLocationLabel);
			BorderPane row3 = new BorderPane();
			Label recalibrateErrorLabel = new Label("Error button: ");
			recalibrateErrorLabel.setPrefWidth(100);
			Button recalibrateErrorButton = new Button("Recalibrate");
			Label recalibrateErrorLocationLabel = new Label(errorX + ", " + errorY); //"1900, 862"
			recalibrateErrorLocationLabel.setPrefWidth(100);
			BorderPane.setMargin(recalibrateErrorLabel, new Insets(0,0,0,10));
			BorderPane.setMargin(recalibrateErrorLocationLabel, new Insets(0,10,0,0));
			row3.setLeft(recalibrateErrorLabel);
			row3.setCenter(recalibrateErrorButton);
			row3.setRight(recalibrateErrorLocationLabel);
			HBox row4 = new HBox();
			Label setTimeoutLabel = new Label("Max wait time (in seconds): ");
			HBox.setMargin(setTimeoutLabel, new Insets(0,0,0,10));
			TextField setTimeoutField = new TextField(maxHangTime + "");
			row4.getChildren().addAll(setTimeoutLabel,setTimeoutField);
			VBox row5 = new VBox();
			Label fileLocationLabel = new Label("Studio location: " + (studioFilePath!=null ? studioFilePath : "---"));
			Button fileLocationButton = new Button("Change Studio Location");
			Label saveLocationLabel = new Label("Save Folder location: " + (saveFolderPath!=null ? saveFolderPath : "---"));
			Button saveLocationButton = new Button("Change Save Folder");
			row5.setAlignment(Pos.CENTER);
			row5.getChildren().addAll(fileLocationLabel,fileLocationButton,saveLocationLabel,saveLocationButton);
			BorderPane row6 = new BorderPane();
			Button publishButton = new Button("Publish!");
			row6.setCenter(publishButton);
			VBox infoBar = new VBox();
			Label help = new Label("HelpText");
			help.setWrapText(true);
			help.setPrefHeight(75);
			VBox.setMargin(help, new Insets(0,0,0,10));
			Label status = new Label("Status: ");
			infoBar.getChildren().addAll(help,status);
			VBox.setMargin(row2, new Insets(10,0,0,0));
			VBox.setMargin(row3, new Insets(10,0,0,0));
			VBox.setMargin(row4, new Insets(10,0,0,0));
			VBox.setMargin(row5, new Insets(10,0,0,0));
			VBox.setMargin(row6, new Insets(10,0,0,0));
			rowContainer.getChildren().addAll(row1,blacklistRow,row2,row3,row4,row5,row6);
			root.setCenter(rowContainer);
			root.setBottom(infoBar);
			//button presses
			EventHandler<ActionEvent> studioFileEvent = new EventHandler<ActionEvent>() { 
	            public void handle(ActionEvent e) 
	            { 
	            	FileChooser fileChooser = new FileChooser();
			    	fileChooser.setTitle("Choose Studio Location");
			    	
			    	if (studioFilePath==null || !(new File(studioFilePath).exists())) {
			    		File defaultDirectory = new File(System.getProperty("user.home") + "\\AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Roblox");
			    		fileChooser.setInitialDirectory(defaultDirectory);
			    	}else {
			    		File defaultDirectory = new File(studioFilePath).getParentFile();
			    		fileChooser.setInitialDirectory(defaultDirectory);
			    	}
			    	fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Executable", "*.exe"));
			    	File selected = fileChooser.showOpenDialog(primaryStage);
			    	if (selected!=null) {
			    		fileLocationLabel.setText("Studio Location: " + selected.getAbsolutePath());
			    		studioFilePath = selected.getAbsolutePath();
			    		dataUpdate = true;
			    	}
	            } 
	        }; 
			fileLocationButton.setOnAction(studioFileEvent);
			fileLocationButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set the location of RobloxStudioBeta.exe");
		         }
	        });
			
			EventHandler<ActionEvent> saveFileEvent = new EventHandler<ActionEvent>() { 
	            public void handle(ActionEvent e) 
	            { 
	            	DirectoryChooser chooser = new DirectoryChooser();
	            	chooser.setTitle("Choose Save Directory");
	            	if (saveFolderPath!=null) {
	            		File defaultDirectory = new File(saveFolderPath);
	            		chooser.setInitialDirectory(defaultDirectory);
	            	}
	            	File selectedDirectory = chooser.showDialog(primaryStage);
			    	if (selectedDirectory!=null) {
			    		saveLocationLabel.setText("Save Folder Location: " + selectedDirectory.getAbsolutePath());
			    		saveFolderPath = selectedDirectory.getAbsolutePath();
			    		dataUpdate = true;
			    		prefs.put("SavePath",saveFolderPath);
			    	}
	            } 
	        }; 
			saveLocationButton.setOnAction(saveFileEvent);
			saveLocationButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set the directory where the output files are kept");
		         }
	        });
			
			EventHandler<ActionEvent> recalibrateImportEvent = new EventHandler<ActionEvent>() { 
	            public void handle(ActionEvent e) 
	            { 
	            	if (listeningTo == 0) {
	            		isListening = false;
	            		dataUpdate = true;
	            		listeningTo = -1;
	            		GlobalScreen.removeNativeKeyListener(keyListener);
	    				try {
							GlobalScreen.unregisterNativeHook();
						} catch (NativeHookException e1) {
							e1.printStackTrace();
						}
	    				keyListener = null;
	    				return;
	            	}
	            	if (isListening) { return ; }
	            	recalibrateImportButton.setText("Cancel");
	            	recalibrateErrorButton.setDisable(true);
					setTimeoutField.setDisable(true);
					setUniverseField.setDisable(true);
					notificationEmailField.setDisable(true);
					notificationPasswordField.setDisable(true);
					receiverEmailField.setDisable(true);
					saveLocationButton.setDisable(true);
					fileLocationButton.setDisable(true);
	            	listeningTo = 0;
	            	GlobalKeyListener listener = new GlobalKeyListener();
	            	listener.type = "Import";
	            	keyListener = listener;
	            	try {
						GlobalScreen.registerNativeHook();
					} catch (NativeHookException e1) {
						e1.printStackTrace();
					}
	            	GlobalScreen.addNativeKeyListener(listener);
	            	isListening = true;
	            } 
	        }; 
	        recalibrateImportButton.setOnAction(recalibrateImportEvent);
	        recalibrateImportButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Find the location of the \"Import\" button in studio (in game explorer) and press SHIFT to mark position");
		         }
	        });
	        
	        EventHandler<ActionEvent> recalibrateErrorEvent = new EventHandler<ActionEvent>() { 
	            public void handle(ActionEvent e) 
	            { 
	            	if (listeningTo == 1) {
	            		isListening = false;
	            		dataUpdate = true;
	            		listeningTo = -1;
	            		GlobalScreen.removeNativeKeyListener(keyListener);
	    				try {
							GlobalScreen.unregisterNativeHook();
						} catch (NativeHookException e1) {
							e1.printStackTrace();
						}
	    				keyListener = null;
	    				return;
	            	}
	            	if (isListening) { return ; }
	            	recalibrateErrorButton.setText("Cancel");
	            	recalibrateImportButton.setDisable(true);
					setTimeoutField.setDisable(true);
					setUniverseField.setDisable(true);
					notificationEmailField.setDisable(true);
					notificationPasswordField.setDisable(true);
					receiverEmailField.setDisable(true);
					saveLocationButton.setDisable(true);
					fileLocationButton.setDisable(true);
	            	listeningTo = 1;
	            	GlobalKeyListener listener = new GlobalKeyListener();
	            	listener.type = "Error";
	            	keyListener = listener;
	            	try {
						GlobalScreen.registerNativeHook();
					} catch (NativeHookException e1) {
						e1.printStackTrace();
					}
	            	GlobalScreen.addNativeKeyListener(listener);
	            	isListening = true;
	            } 
	        }; 
	        recalibrateErrorButton.setOnAction(recalibrateErrorEvent);
	        recalibrateErrorButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Find the location of the \"Select All\" button in studio (in package unpublished error screen) and press SHIFT to mark position. This is optional depending on whether your game uses packages or not");
		         }
	        });
	        
	        setUniverseField.focusedProperty().addListener(new ChangeListener<Boolean>(){
	            @Override
	            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
	                if (!newPropertyValue){
	                	universeId = setUniverseField.getText();
	                	if (universeId.matches(".*\\.roblox\\.com/games/\\d+/.*")){
	                		
		                	Pattern p = Pattern.compile(".*\\.roblox\\.com/games/(\\d+)/.*");
		    				Matcher m = p.matcher(universeId);
		    				if (m.matches()) {
			    				String actualId = m.group(1);
			    				universeId = getUniverseIdFromPlace(actualId);
		    				}
	                	}
	                	dataUpdate = true;
	                }
	            }
	        });
	        setUniverseField.setOnKeyReleased(event -> {
	        	  if (event.getCode() == KeyCode.ENTER){
	        		universeId = setUniverseField.getText();
	        		if (universeId.matches(".*\\.roblox\\.com/games/\\d+/.*")){
	        			Pattern p = Pattern.compile(".*\\.roblox\\.com/games/(\\d+)/.*");
	    				Matcher m = p.matcher(universeId);
	    				if (m.matches()) {
		    				String actualId = m.group(1);
		    				universeId = getUniverseIdFromPlace(actualId);
	    				}
                	}
                	dataUpdate = true;
	        	  }
	        });
	        setUniverseField.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set the Universe ID, or link a place from the universe");
		         }
	        });
	        
	        setBlackListIdsField.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set which places in the game to blacklist, or not mass update");
		         }
	        });
	        setBlackListIdsField.focusedProperty().addListener(new ChangeListener<Boolean>(){
	            @Override
	            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
	                if (!newPropertyValue){
	                	blacklists = setBlackListIdsField.getText();
	                	dataUpdate = true;
	                }
	            }
	        });
	        setBlackListIdsField.setOnKeyReleased(event -> {
	        	  if (event.getCode() == KeyCode.ENTER){
	        		blacklists = setBlackListIdsField.getText();
                	dataUpdate = true;
	        	  }
	        });
	        
	        setTimeoutField.focusedProperty().addListener(new ChangeListener<Boolean>(){
	            @Override
	            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
	                if (!newPropertyValue){
	                	try {
	                		maxHangTime = Integer.parseInt(setTimeoutField.getText());
	                	}catch(Exception e){
	                		maxHangTime = -1;
	                	}
	                	dataUpdate = true;
	                }
	            }
	        });
	        setTimeoutField.setOnKeyReleased(event -> {
	        	  if (event.getCode() == KeyCode.ENTER){
	        		  try {
	        			  maxHangTime = Integer.parseInt(setTimeoutField.getText());
	        		  }catch(Exception e){
	        			  maxHangTime = -1;
	        		  }
	        		  dataUpdate = true;
	        	  }
	        });
	        setTimeoutField.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set how long you want the maximum hang time to be before we stop trying to publish");
		         }
	        });
	        
	        notificationEmailField.focusedProperty().addListener(new ChangeListener<Boolean>(){
	            @Override
	            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
	                if (!newPropertyValue){
	                	emailFrom = notificationEmailField.getText();
	                	dataUpdate = true;
	                }
	            }
	        });
	        notificationEmailField.setOnKeyReleased(event -> {
	        	  if (event.getCode() == KeyCode.ENTER){
	        		  emailFrom = notificationEmailField.getText();
	        		  dataUpdate = true;
	        	  }
	        });
	        notificationEmailField.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set the email you want to send from to get a notification. If blank, no notifications will be sent. Requires a password (which is not stored anywhere)");
		         }
	        });
	        
	        notificationPasswordField.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set the password to the email you want to send from (above). This data is not saved and you will have to enter it every time you wish to receive a notification");
		         }
	        });
	        
	        receiverEmailField.focusedProperty().addListener(new ChangeListener<Boolean>(){
	            @Override
	            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
	                if (!newPropertyValue){
	                	emailTo = receiverEmailField.getText();
	                	dataUpdate = true;
	                }
	            }
	        });
	        receiverEmailField.setOnKeyReleased(event -> {
	        	  if (event.getCode() == KeyCode.ENTER){
	        		  emailTo = receiverEmailField.getText();
	        		  dataUpdate = true;
	        	  }
	        });
	        receiverEmailField.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Set the email you want to receive the completed notification. This can be set to send SMS messages depending on your provider!");
		         }
	        });
	        
			EventHandler<ActionEvent> publishEvent = new EventHandler<ActionEvent>() { 
	            public void handle(ActionEvent e) 
	            { 
	            	if (isPublishing) { return; }
	            	isPublishing = true;
	            	startTime = System.currentTimeMillis();
	                publishButton.setText("Publishing!"); 
	                publishButton.setDisable(true);
	                recalibrateImportButton.setDisable(true);
	                recalibrateErrorButton.setDisable(true);
					setTimeoutField.setDisable(true);
					setUniverseField.setDisable(true);
					notificationEmailField.setDisable(true);
					notificationPasswordField.setDisable(true);
					receiverEmailField.setDisable(true);
					saveLocationButton.setDisable(true);
					fileLocationButton.setDisable(true);
	                //save routine
	                
	                
	                //convert to JSON
	                Map<String, Object> map = new HashMap<>();
	                map.put("emailto", emailTo);
	                map.put("emailfrom", emailFrom);
	                map.put("universeid", universeId);
	                map.put("blacklists", blacklists);
	                map.put("importx", importX);
	                map.put("importy", importY);
	                map.put("errorx", errorX);
	                map.put("errory", errorY);
	                map.put("maxhang", maxHangTime);
	                map.put("studiopath", studioFilePath);
	                map.put("importcolorcode", colorCode);
	                
	                File saveFile = null;
	                File logFile = null;
	                screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	                boolean isSuccess = false;
	                
	                try {
		                // create object mapper instance
		                ObjectMapper mapper = new ObjectMapper();
		                // convert map to JSON file
		                saveFile = new File(saveFolderPath + "\\Data.json");
		                logFile = new File(saveFolderPath + "\\LatestLog.log");
		                saveFile.createNewFile();
		                logFile.createNewFile();
		                mapper.writeValue(saveFile, map);
		                startLogger();
		                logger.info("Starting up publisher...");
		                if (saveFile!=null && logFile!=null) {
		                	run(status);
		                	isSuccess = true;
		                }else {
		                	if (saveFile == null) {
		                		logger.severe("ERROR: Save file does not exist!");
		                	}
		                	if (logFile == null) {
		                		logger.severe("ERROR: Log file does not exist!");
		                	}
		                }
	                }catch(Exception exception) {
	                	exception.printStackTrace();
	                }
	                
	                //email when done
	                
                	logger.info("Checking notification settings...");
                	if (!emailFrom.equals("") && !emailTo.equals("")) {
                		logger.info("Entries for notification email were found.");
                		long timeTaken = System.currentTimeMillis() - startTime;
                		logger.info("Took " + timeTaken/1000 + " seconds");
                		sendEmail(status, notificationPasswordField.getText(), isSuccess, timeTaken);
                	}else {
                		logger.info("No entries were provided for the notification email. Ignoring.");
                	}
	                
	                isPublishing = false;
	                publishButton.setDisable(false);
	                publishButton.setText("Publish!"); 
	                dataUpdate = true;
	            } 
	        }; 
			publishButton.setOnAction(publishEvent);
			publishButton.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
		         @Override
		         public void handle(MouseEvent e) {
		        	 help.setText("Publish the entire universe using the data provided. Be sure everything is set up correctly to make sure this goes smoothly! Check below for status information.");
		         }
	        });
			
			primaryStage.setOnCloseRequest(event -> {
			    try {
					GlobalScreen.unregisterNativeHook();
				} catch (NativeHookException e1) {
					e1.printStackTrace();
				}
			});
			dataUpdate = true;
			AnimationTimer t = new AnimationTimer() {
				@Override
				public void handle(long now) {
					if (dataUpdate) {
						dataUpdate = false;
						recalibrateImportLocationLabel.setText(importX + ", " + importY);
						recalibrateErrorLocationLabel.setText(errorX + ", " + errorY);
						recalibrateImportButton.setText("Recalibrate");
						recalibrateErrorButton.setText("Recalibrate");
						setTimeoutField.setText(maxHangTime + "");
						setUniverseField.setText(universeId);
						recalibrateImportButton.setDisable(false);
						recalibrateErrorButton.setDisable(false);
						setTimeoutField.setDisable(false);
						setUniverseField.setDisable(false);
						notificationEmailField.setDisable(false);
						notificationPasswordField.setDisable(false);
						receiverEmailField.setDisable(false);
						saveLocationButton.setDisable(false);
						fileLocationButton.setDisable(false);
						status.setText("Status: " + getStatus());
						if (status.getText().contains("[CRITICAL]")) {
							status.setTextFill(javafx.scene.paint.Color.DARKRED);
							publishButton.setDisable(true);
						}else if(status.getText().contains("[WARNING]")) {
							status.setTextFill(javafx.scene.paint.Color.ORANGE);
							publishButton.setDisable(true); 
						}else {
							status.setTextFill(javafx.scene.paint.Color.BLUE);
							publishButton.setDisable(false);
						}
					}
				}
			};
			t.start();
			
			try {
				GlobalScreen.registerNativeHook();
			}
			catch (NativeHookException ex) {
				System.err.println("There was a problem registering the native hook.");
				System.err.println(ex.getMessage());

				//System.exit(1);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public void sendEmail(Label status, String password, boolean success, long timeTakenMS) {
		  if (password.equals("")) {
			  logger.info("No password entered, not sending email");
			  return;
		  }
	      Properties properties = System.getProperties();
	      properties.put("mail.smtp.starttls.enable", "true");
	      Session session = Session.getDefaultInstance(properties);
	      
	      long seconds = timeTakenMS/1000;
	      long minutes = seconds/60;
	      seconds-=(minutes*60);
	      try {
		      Transport trans = session.getTransport("smtp");
		      trans.connect("smtp.live.com", 25, emailFrom, password);
	         MimeMessage message = new MimeMessage(session);
	         message.setFrom(new InternetAddress(emailFrom));
	         if (success) {
		         message.setSubject("Publish completed successfully!");
		         
		         
		         message.setText("Took " + minutes + " minutes and " + seconds + " seconds");
	         }else {
	        	 message.setSubject("Publish failed");
		         message.setText("An error occurred. Took " + minutes + " minutes and " + seconds + " seconds");
	         }
	         Address[] toList = {new InternetAddress(emailTo)};
	         trans.sendMessage(message, toList);
	         logger.info("Sent notification successfully");
	      } catch (MessagingException mex) {
	         mex.printStackTrace();
	         logger.severe("Failed to send notification. Perhaps password was entered incorrectly?");
	      }
	   }
	
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public Image loadImage( String path ){
		try {
			//return new Image( new FileInputStream( path ) );
			return new Image( getClass().getResourceAsStream(path));
		} catch (Exception e) {
			System.out.println( "Path to a texture file couldn't be found." );
			e.printStackTrace();
		}
		return null;
	}
}


