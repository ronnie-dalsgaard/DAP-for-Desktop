package application;

import static application.Constants.COVER_HEIGHT;
import static application.Constants.COVER_WIDTH;
import static application.Constants.HOME;
import static application.Constants.PRESERVE_RATIO;
import static application.Constants.SMOOTH;
import static application.Constants.STYLE_AUDIOBOOKS_HEADER;
import static application.Constants.STYLE_MENU;
import static application.Constants.STYLE_MENU_ITEM;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import model.Audiobook;
import model.AudiobookManager;
import model.BookmarkManager;

public abstract class Audiobooks {
	private File homefolder;

	@FXML
	private void show_audiobooks(ActionEvent event){
		System.out.println("show_audiobooks");

		try{
			FXMLLoader fxmlAudiobooksLoader = new FXMLLoader(getClass().getResource("audiobooks.fxml"));
			VBox root = fxmlAudiobooksLoader.load();

			ScrollPane scroller = (ScrollPane) fxmlAudiobooksLoader.getNamespace().get("audiobooks_scroller");
			scroller.setStyle("-fx-background: rgb(0,0,0);");

			Scene scene = new Scene(root);
			Stage audiobooksStage = new Stage();
			audiobooksStage.setScene(scene);
			audiobooksStage.centerOnScreen();

			//flow
			FlowPane audiobooks_flowpane = (FlowPane) fxmlAudiobooksLoader.getNamespace().get("flow");
			displayAudiobooks(audiobooks_flowpane, audiobooksStage);

			Button reload_btn = (Button) fxmlAudiobooksLoader.getNamespace().get("audiobooks_reload");
			reload_btn.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					autoDetectAudiobooks();
					displayAudiobooks(audiobooks_flowpane, audiobooksStage);
				}
			});

			audiobooksStage.show();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	private void displayAudiobooks(FlowPane audiobooks_flowpane, Stage audiobooksStage) {
		try{
			audiobooks_flowpane.getChildren().clear();
			ArrayList<Audiobook> list = AudiobookManager.getInstance().loadAudiobooks();

			if(list.isEmpty()){
				autoDetectAudiobooks();
			}

			Collections.sort(list);

			String author = "";
			for(Audiobook audiobook : list){
				if(!author.equals(audiobook.getAuthor())){
					author = audiobook.getAuthor();
					//Add author header
					Text header = new Text(audiobook.getAuthor());
					header.setStyle(STYLE_AUDIOBOOKS_HEADER);
					Rectangle rect = new Rectangle(300, 25);
					StackPane p = new StackPane(rect, header);
					p.setAlignment(Pos.CENTER_LEFT);
					audiobooks_flowpane.getChildren().add(p);
				}
				//Cover
				File cover_file = new File(audiobook.getCover());
				if(cover_file != null){
					FileInputStream cover_inputstream = new FileInputStream(cover_file);
					Image image = new Image(cover_inputstream, COVER_WIDTH, COVER_HEIGHT, PRESERVE_RATIO, SMOOTH);
					ImageView cover_iv = new ImageView(image);
					audiobooks_flowpane.getChildren().add(cover_iv);

					cover_iv.setOnMouseClicked(new EventHandler<MouseEvent>() {

						@Override
						public void handle(MouseEvent event) {
							if(event.getButton() == MouseButton.PRIMARY){
								audiobooksStage.close();
								selectAudiobook(audiobook, 0, 0);
								String author = audiobook.getAuthor();
								String album = audiobook.getAlbum();
								int trackno = 0;
								int progress = 0;
								boolean force = true;
								BookmarkManager.getInstance().createOrUpdateBookmark(author, album, trackno, progress, force);
								showBookmarks();
								//When an audiobook is selected it is always at trackNo = 0, progress = 0.
							} else if(event.getButton() == MouseButton.SECONDARY){

								ContextMenu cm = new ContextMenu();
								cm.setStyle(STYLE_MENU);
								MenuItem itemDelete = new MenuItem("Delete");
								itemDelete.setStyle(STYLE_MENU_ITEM);
								itemDelete.setOnAction(new EventHandler<ActionEvent>() {

									@Override
									public void handle(ActionEvent event) {
										System.out.println("Delete menu item selected");
									}
								});
								MenuItem itemEdit = new MenuItem("Edit");
								itemEdit.setStyle(STYLE_MENU_ITEM);
								itemEdit.setOnAction(new EventHandler<ActionEvent>() {

									@Override
									public void handle(ActionEvent event) {
										System.out.println("Edit menu item selected");
									}
								});

								cm.getItems().add(itemDelete);
								cm.getItems().add(itemEdit);

								cm.show(cover_iv, event.getScreenX(), event.getScreenY());
							}
						}




					});
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void autoDetectAudiobooks(){
		File folder = getHomeFolder();
		AudiobookManager am = AudiobookManager.getInstance();
		ArrayList<Audiobook> list = am.autodetect(folder);
		am.addAllAudiobooks(list);
	}
	public File getHomeFolder(){
		if(homefolder == null){
			Properties props = getProperties();
			String _homefolder = props.getProperty("homefolder");
			if(_homefolder == null){
				homefolder = selectFolder();
				props.setProperty(homefolder.getAbsolutePath(), "homefolder");
				setProperties(props);
			} else {
				homefolder = new File(_homefolder);
			}
		}
		return homefolder;
	}
	@FXML 
	private void audiobooks_select_homefolder(ActionEvent event){
		homefolder = selectFolder();
	}

	public File selectFolder(){
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("New Audiobook");
		File defaultFolder = HOME;
		chooser.setInitialDirectory(defaultFolder);
		File folder = chooser.showDialog(new Stage());
		return folder;
	}

	public abstract void selectAudiobook(Audiobook audiobook, int trackno, int progress);
	public abstract void showBookmarks();
	public abstract Properties getProperties();
	public abstract void setProperties(Properties props);
}
