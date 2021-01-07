/*
 * Author - Karan Sharma
 * Date - 5 January, 2021
 * Detail - A Javafx application that uses JDBC driver to connect to an 
 * 			image repository/database to search, add or delete images. 
 */

import javafx.application.Application;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class ImageRepository extends Application{
	private static final double WIDTH = 600;	//Width of the JavaFX window
	private static final double HEIGHT = 400;	//Height of the JavaFX window
	
	private static final String TITLE = "Image Repository";	//JavaFX application title
	
	private static final String DB_URL = "jdbc:mysql://localhost:3306/imagerepo"; //database URL
	
	private static final String DB_NAME = DB_URL.substring(28);
	
	private static final String COLUMN_NAMES[] = {"IMAGE_ID", "IMAGE", "IMAGE_CAPTION"};
	
	private static final String SQL_CREATE_TABLE = "CREATE TABLE " + DB_NAME + ".images(" + COLUMN_NAMES[0] 
													+ " INTEGER  PRIMARY KEY  AUTO_INCREMENT, " +
													COLUMN_NAMES[1] + " LONGBLOB, " +  COLUMN_NAMES[2] + " VARCHAR(100));";
	
	//SQL Query
	private static final String SQL_INSERT_IMAGE = "INSERT INTO " + DB_NAME + ".images(" + COLUMN_NAMES[1] + "," 
													+ COLUMN_NAMES[2] + ") VALUES(?, ?)";	
	
	//root component of the application
	private BorderPane root;
	
	//database connector
	private Connection connection;
	
	//JavaFX application status label
	private Label connectionStatus;
	
	@Override
	public void init() throws Exception{ 
		root = new BorderPane();
		root.setTop(createOptionsBar());
		root.setBottom(createStatusBar());
	}
	
	@Override
	public void start(Stage theStage) throws Exception {
		Scene theScene = new Scene(root, WIDTH, HEIGHT); // holds the JavaFX components that need to be displayed

		theStage.setScene(theScene);
		theStage.setTitle(TITLE);
		theStage.setResizable(true);

		theStage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent theEvent) -> {
			if (KeyCode.ESCAPE == theEvent.getCode()) {
				theStage.hide(); // closes the application when ESC key is pressed
			}
		});

		theStage.show(); // display the JavaFX application
	}

	@Override
	public void stop() throws Exception {	//closes the application as well as the connection with the database
		if (connection != null) {
			connection.close();
		}
	}
	
	private Region createStatusBar() {		//creates the bottom pane component which displays the Label component (connectionStatus)
		connectionStatus = new Label("Status: Not Connected");
		ToolBar tool = new ToolBar(connectionStatus);
		return tool;
	}

	private Button createButton(String name, EventHandler<ActionEvent> onClick) {	
		Button button = new Button(name);
		button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		button.setOnAction(onClick);
		return button;
	}

	private TextField createTextField(String value, String prompt) {	
		TextField txt = new TextField();
		txt.setText(value);
		txt.setPromptText(prompt);
		GridPane.setHgrow(txt, Priority.ALWAYS); // to make the textfield maximizable when we maximize the application window.
		return txt;
	}

	private PasswordField createPasswordField(String value, String prompt) {
		PasswordField txt = new PasswordField();
		txt.setText(value);
		txt.setPromptText(prompt);
		GridPane.setHgrow(txt, Priority.ALWAYS); // to make the textfield maximizable when we maximize the application
													// window.
		return txt;
	}
	
	private Region createOptionsBar() {								//This method provides funtionality to the buttons created as
		TextField urlDBText = createTextField(DB_URL, "DB URL");	//well as it sets the button, textfield and passwordfield 
		TextField userName = createTextField("", "Username");		//component on to the grid
		TextField password = createPasswordField("", "Password");
		TextField addText = createTextField("", "Add Image Path");
		TextField addAllText = createTextField("", "Add Folder Path");
		
		
		Button connectButton = createButton("Connect to DB", x->{
			connectionStatus.setText("Status: Connecting");
			try {
				connectTo(urlDBText.getText(), userName.getText(), password.getText());
				createTable();
				connectionStatus.setText("Status: Connected");
			}catch(SQLException e) {
				if(e.getMessage().equals("Table 'images' already exists")) {
					connectionStatus.setText("Status: "+ e.getMessage());
				}else {
				connectionStatus.setText("Status: Failed to connect!" + e.getMessage());
				}
			}
		});
		
		Button addButton = createButton("Add Image to DB", x->{
			connectionStatus.setText("Adding to the database");
			try {
				if(connection == null || connection.isClosed()) {
					connectionStatus.setText("Status: Must connect to the database first");
				}else {
					addImage(addText.getText().trim());
				}
			}catch(SQLException | IOException e) {
				connectionStatus.setText("Status: Failed to add image to the database" + e.getMessage());
			}
		});
		
		Button addAllButton = createButton("Add All Images To DB", x->{
			connectionStatus.setText("Adding to the database");
			try {
				if(connection == null || connection.isClosed()) {
					connectionStatus.setText("Status: Must connect to the database first");
				}else {
					addAllImages(addAllText.getText().trim());
				}
			}catch(SQLException | IOException e) {
				connectionStatus.setText("Failed to add images! " + e.getMessage());
			}
		});
		
		
		GridPane theGrid = new GridPane();
		theGrid.setHgap(3);
		theGrid.setVgap(3);
		theGrid.setPadding(new Insets(5, 5, 5, 5));
		
		theGrid.add(urlDBText, 0, 0, 2, 1);
		theGrid.add(userName, 0, 1, 1, 1);
		theGrid.add(password, 1, 1, 1, 1);
		theGrid.add(connectButton, 2, 0, 1, 2);
		theGrid.add(addText, 0, 2, 2, 1);
		theGrid.add(addButton, 2, 2, 1, 1);
		theGrid.add(addAllText, 0, 3, 2, 1);
		theGrid.add(addAllButton, 2, 3, 1, 1);
		
		return theGrid;
	}
	
	private void connectTo(String DBurl, String user, String pwd) throws SQLException {		//connects the application to the 
		if (connection == null || connection.isClosed()) {									//database using DriverManager class
			connection = DriverManager.getConnection(DBurl, user, pwd);
		}
	}
	
	private void createTable() throws SQLException {
		if(!(connection == null || connection.isClosed()) ) {
			try(PreparedStatement statement = connection.prepareStatement(SQL_CREATE_TABLE)){
				statement.execute();
			}
		}
	}
	
	private void addImage(String addTerm) throws SQLException, IOException {								//Adds the specific
		if (addTerm == null || addTerm.isEmpty()) {															//image provided in
			connectionStatus.setText("Status: Please type in the image path to add it to the database");	//the textfield to
			return;																							//database.
		} else {
			try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT_IMAGE)) {
				String fileName = addTerm.substring(39);
				FileInputStream input = new FileInputStream(addTerm);
				statement.setBinaryStream(1, input);
				statement.setString(2, fileName);
				statement.executeUpdate();
				connectionStatus.setText("Status: Image added to database");
			} catch (SQLException | IOException e) {
				connectionStatus.setText("Status: Failed to add image to the database" + e.getMessage());
			}
		}
	}

	private void addAllImages(String addTerm) throws SQLException, IOException {									//Adds all the
		if (addTerm == null || addTerm.isEmpty()) {																	//images to the
			connectionStatus.setText("Status: Please type in the folder path to add all images to the database");	//database from
			return;																									//the provided
		} else {																									//folder path.
			File ImageFolder = new File(addTerm);
			ArrayList<String> theList = new ArrayList<String>();
			for (File file : ImageFolder.listFiles()) {
				theList.add(file.getName());
			}

			for (String row : theList) {
				try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT_IMAGE)) {
					FileInputStream input = new FileInputStream(ImageFolder + "\\" + row);
					statement.setBinaryStream(1, input);
					statement.setString(2, row);
					statement.executeUpdate();
					connectionStatus.setText("Status: All images added to database");
				}
			}
		}
	}
	
	public static void main(String[] args) {	//launches the JavaFX application
		launch(args);
	}
}
