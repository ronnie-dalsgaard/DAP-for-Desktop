package application;

import static application.Constants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.GroupLayout.Alignment;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import model.Audiobook;
import model.AudiobookManager;
import model.Bookmark;
import model.BookmarkManager;
import model.Track;


public class Controller implements Initializable {
	@FXML private Label label;
	@FXML private FlowPane player_track_flow;
	@FXML private FlowPane player_bookmark_flow;
	@FXML private Button btn_prev;
	@FXML private Button btn_next;
	@FXML private ImageView player_cover;
	@FXML private ImageView player_overCover;
	@FXML private Text player_author;
	@FXML private Text player_album;
	@FXML private Text player_track;
	@FXML private Text player_time;

	private Audiobook currentAudiobook = null;
	private Track currentTrack = null;
	private int currentTrackno = 0;
	private static MediaPlayer mp = null;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		showBookmarks();
	}

	//Selected Audiobook
	public void selectAudiobook(Audiobook audiobook){
		currentAudiobook = audiobook;
		currentTrackno = 0;
		currentTrack = audiobook.getPlaylist().getFirst();
		showAudiobook();
		showTracks();
		showIsPaused();
		setTrack();
	}
	private void showAudiobook(){
		if(currentAudiobook == null) return;
		player_author.setText(currentAudiobook.getAuthor());
		player_album.setText(currentAudiobook.getAlbum());
		showCover();
		showTrack();
	}
	private void showTracks(){
		if(currentAudiobook == null) return;
		player_track_flow.getChildren().clear();
		for(int i = 0; i < currentAudiobook.getPlaylist().size(); i++){
			Label l = new Label(String.format("%02d", i+1));
			l.setTextFill(Color.WHITE);
			l.setPrefSize(30, 30);
			l.setAlignment(Pos.CENTER);

			if(i == currentTrackno){
				l.getStyleClass().add("bordered");
			} else {
				l.setStyle(null);
			}
			player_track_flow.getChildren().add(l);
		}
	}
	private void showTrack(){
		if(currentTrack == null) return;
		player_track.setText(currentTrack.getTitle());
	}
	private void showCover(){
		if(currentAudiobook == null) return;
		if(currentAudiobook.getCover() == null) return;
		try{
			File file = new File(currentAudiobook.getCover());
			FileInputStream inputstream = new FileInputStream(file);
			Image image = new Image(inputstream);
			player_cover.setImage(image);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	private void showIsPlaying(){
		try{
			File file = new File(ICON_PAUSE_OVER_VIDEO);
			FileInputStream inputstream = new FileInputStream(file);
			Image image = new Image(inputstream);
			player_overCover.setImage(image);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	private void showIsPaused(){
		try{
			File file = new File(ICON_PLAY_OVER_VIDEO);
			FileInputStream inputstream = new FileInputStream(file);
			Image image = new Image(inputstream);
			player_overCover.setImage(image);
		} catch(IOException e){
			e.printStackTrace();
		}
	}

	//Player functions
	@FXML
	private void play_pause(MouseEvent event) {
		if(currentTrack == null) return;
		if(mp != null && mp.getStatus() == MediaPlayer.Status.PLAYING){
			mp.pause();
			showIsPaused();
		} else if(mp != null && mp.getStatus() == MediaPlayer.Status.PAUSED){
			mp.play();
			showIsPlaying();
		} else { 
			setTrack();
			mp.play();
			showIsPlaying();
		}
	}
	@FXML
	private void prev(ActionEvent event){
		if(currentAudiobook.getPlaylist().getFirst().equals(currentTrack)) return;

		int nextTrackNo = currentTrackno - 1;
		Track nextTrack = currentAudiobook.getPlaylist().get(nextTrackNo);
		currentTrackno = nextTrackNo;
		currentTrack = nextTrack;
		showTracks();
		setTrack();
	}
	@FXML
	private void next(ActionEvent event){
		if(currentAudiobook == null) return;	
		if(currentAudiobook.getPlaylist().getLast().equals(currentTrack)) return;

		int nextTrackNo = currentTrackno + 1;
		Track nextTrack = currentAudiobook.getPlaylist().get(nextTrackNo);
		currentTrackno = nextTrackNo;
		currentTrack = nextTrack;
		showTracks();
		setTrack();
	}
	private void setTrack(){
		System.out.println(currentTrack);
		if(mp != null && mp.getStatus() == MediaPlayer.Status.PLAYING){
			mp.stop();
			showIsPaused();
		}
		File file = new File(currentTrack.getPath());
		Media media = new Media(file.toURI().toString());
		mp = new MediaPlayer(media);
		currentTrack.setDuration((int)mp.getTotalDuration().toMillis());
	}
	
	//Audiobooks
	@FXML
	private void show_audiobooks(ActionEvent event){
		new Helper_Audibooks().showAudiobooks(this);
	}	
	public void updateAudiobooks(FlowPane flow, Stage audiobooksStage) {
		new Helper_Audibooks().updateAudiobooks(this, flow, audiobooksStage);
	}
	@FXML 
	public void new_audiobook(){
		File folder = selectFolder();
		AudiobookManager am = AudiobookManager.getInstance();
		Audiobook audiobook = am.autoCreateAudiobook(folder, true);
		if(audiobook != null) {
			am.addAudiobook(audiobook);
			am.saveAudiobooks();
		}

		if(audiobook == null) return;

		//Display selected audiobook
		try {
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("audiobook.fxml"));
			ScrollPane root = fxmlLoader.load();
			root.setStyle("-fx-background: rgb(0,0,0);");

			//Author
			Text author_txt = (Text) fxmlLoader.getNamespace().get("audiobook_author_txt");
			author_txt.setText(audiobook.getAuthor());
			//Album
			Text album_txt = (Text) fxmlLoader.getNamespace().get("audiobook_album_txt");
			album_txt.setText(audiobook.getAlbum());
			//Cover
			ImageView cover_iv = (ImageView) fxmlLoader.getNamespace().get("audiobook_cover");

			File cover_file = new File(audiobook.getCover());
			if(cover_file != null){
				FileInputStream cover_inputstream = new FileInputStream(cover_file);
				Image image = new Image(cover_inputstream);
				cover_iv.setImage(image);
			}
			//Tracks
			VBox track_list = (VBox) fxmlLoader.getNamespace().get("audiobook_track_list");
			track_list.setPrefHeight(24*audiobook.getPlaylist().size());
			for(int i = 0; i < audiobook.getPlaylist().size(); i++){
				Track track = audiobook.getPlaylist().get(i);
				int pos = i+1;
				FXMLLoader fxmlTrackLoader = new FXMLLoader(getClass().getResource("track_item.fxml"));
				HBox item = fxmlTrackLoader.load();
				Text pos_txt = (Text) fxmlTrackLoader.getNamespace().get("track_item_pos");
				Text title_txt = (Text) fxmlTrackLoader.getNamespace().get("track_item_title");

				pos_txt.setText(String.format("%02d", pos));
				title_txt.setText(track.getTitle());
				track_list.getChildren().add(item);
			}

			Scene scene = new Scene(root);
			Stage stage = new Stage();
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	//Bookmarks
	private void showBookmarks(){
		if(player_bookmark_flow == null) return;
		BookmarkManager bm = BookmarkManager.getInstance();
		for(int i = 0; i < 2; i++){
			Bookmark b = new Bookmark("Rick Riordan", "album"+i, 2+i, 10000);
			bm.createOrUpdateBookmark(b, true);			
		}

		for(int i = 0; i < BookmarkManager.getBookmarks().size(); i++){
			Bookmark bookmark = BookmarkManager.getBookmarks().get(i);
			showBookmark(bookmark);

			//Add separator
			Label div = new Label();
			div.setPrefWidth(1);
			div.setPrefHeight(66);
			div.getStyleClass().add("v_div");
			player_bookmark_flow.getChildren().add(div);
		}
	}
	private void showBookmark(Bookmark bookmark){
		if(bookmark == null) return;
		try{
			FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("bookmark.fxml"));
			HBox bookmark_root = fxmlLoader.load();
			Text trackno = (Text) fxmlLoader.getNamespace().get("bookmark_trackno");
			trackno.setText(String.format("%02d", bookmark.getTrackno()));
			player_bookmark_flow.getChildren().add(bookmark_root);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	//Support
	private File selectFolder(){
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("New Audiobook");
		File defaultFolder = HOME;
		chooser.setInitialDirectory(defaultFolder);
		File folder = chooser.showDialog(new Stage());
		return folder;
	}

}
