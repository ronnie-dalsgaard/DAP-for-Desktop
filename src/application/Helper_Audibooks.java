package application;

import static application.Constants.COVER_HEIGHT;
import static application.Constants.COVER_WIDTH;
import static application.Constants.ICON_NEW;
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

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import javafx.stage.Stage;
import model.Audiobook;
import model.AudiobookManager;

public class Helper_Audibooks {

	public void showAudiobooks(Controller controller){
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
			FlowPane flow = (FlowPane) fxmlAudiobooksLoader.getNamespace().get("flow");
			controller.updateAudiobooks(flow, audiobooksStage);
			
			Button audiobooks_add = (Button) fxmlAudiobooksLoader.getNamespace().get("audiobooks_add");
			audiobooks_add.setOnMouseClicked(new EventHandler<Event>() {
				@Override
				public void handle(Event event) {
					controller.new_audiobook();
					controller.updateAudiobooks(flow, audiobooksStage);
				}
			});
			
			
			audiobooksStage.show();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void updateAudiobooks(Controller controller, FlowPane flow, Stage audiobooksStage) {
		try{
			flow.getChildren().clear();
			ArrayList<Audiobook> list = AudiobookManager.getInstance().loadAudiobooks();
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
					flow.getChildren().add(p);
				}
				//Cover
				File cover_file = new File(audiobook.getCover());
				if(cover_file != null){
					FileInputStream cover_inputstream = new FileInputStream(cover_file);
					Image image = new Image(cover_inputstream, COVER_WIDTH, COVER_HEIGHT, PRESERVE_RATIO, SMOOTH);
					ImageView cover_iv = new ImageView(image);
					flow.getChildren().add(cover_iv);
					
					cover_iv.setOnMouseClicked(new EventHandler<MouseEvent>() {

						@Override
						public void handle(MouseEvent event) {
							if(event.getButton() == MouseButton.PRIMARY){
								audiobooksStage.close();
								controller.selectAudiobook(audiobook);
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
			
			if(list.isEmpty()){
				File file_icon_new = new File(ICON_NEW);
				FileInputStream inputstream = new FileInputStream(file_icon_new);
				Image image = new Image(inputstream);
				ImageView icon_iv = new ImageView(image);
				icon_iv.setFitWidth(25);
				icon_iv.setFitHeight(25);

				StackPane newAudiobookPane = new StackPane();
				Rectangle rect = new Rectangle(92, 140);
				newAudiobookPane.getChildren().add(rect);
				newAudiobookPane.prefWidth(96.0);
				newAudiobookPane.prefHeight(144.0);
				newAudiobookPane.setStyle("-fx-background-color: #000000; -fx-border-style: dashed;  -fx-border-color: white; -fx-border-radius: 2;");
				newAudiobookPane.getChildren().add(icon_iv);
				flow.getChildren().add(newAudiobookPane);

				newAudiobookPane.setOnMouseClicked(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						controller.new_audiobook();
						controller.updateAudiobooks(flow, audiobooksStage);
					}
				});
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
