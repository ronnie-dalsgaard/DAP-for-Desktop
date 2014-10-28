package application;

import static application.Constants.HOME;
import static application.Constants.ICON_PAUSE_OVER_VIDEO;
import static application.Constants.ICON_PLAY_OVER_VIDEO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Audiobook;
import model.AudiobookManager;
import model.Bookmark;
import model.BookmarkManager;
import model.Track;
import support.Monitor;
import support.Time;


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
	private int currentTrackno = -1;
	private static MediaPlayer mp = null;
	private static Monitor progress_monitor = null;
	private static Monitor bookmark_monitor = null;
	private static boolean firstRun = true;

	@Override
 	public void initialize(URL url, ResourceBundle resourceBundle) {
		if(firstRun){
			firstRun = false;
			AudiobookManager.getInstance().loadAudiobooks();
			BookmarkManager.getInstance().loadBookmarks();
		}
		showBookmarks();
		if(bookmark_monitor == null){
			bookmark_monitor = new Bookmark_monitor();
			bookmark_monitor.start();
		}
	}

	//Selected Audiobook
	public void selectAudiobook(Audiobook audiobook, int trackNo, int progress){
		currentAudiobook = audiobook;
		currentTrackno = trackNo;
		currentTrack = audiobook.getPlaylist().getFirst();
		showAudiobook();
		showTracks();
		showIsPaused();
		setTrack(trackNo, progress);
		showTime(progress, -1);
	}
	
	//Display functions
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
				//Only set onclicklistener to tracks other than the one currently playing
				final int trackNo = i;
				l.setOnMouseClicked(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						setTrack(trackNo, 0);
					}
				});
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
	private void showTime(int progress, int duration){
		String _progress = Time.toString(progress);
		String _duration = Time.toString(duration);
		
		player_time.setText(_progress + (duration < 0 ? "" : " / " + _duration));
	}
	
	//Player functions
	@FXML
	private void play_pause(MouseEvent event) {
		if(currentTrack == null) return;
		if(mp != null && mp.getStatus() == MediaPlayer.Status.PLAYING){
			mp.pause();
			showIsPaused();
		} else if(mp != null && mp.getStatus() != MediaPlayer.Status.PLAYING){
			mp.play();
			showIsPlaying();
		} else { //mp is null
			setTrack(currentTrackno, 0); //set progress to 0
			mp.play();
			showIsPlaying();
		}
	}
	@FXML
	private void prev(ActionEvent event){
		if(currentAudiobook.getPlaylist().getFirst().equals(currentTrack)) return;

		int nextTrackNo = currentTrackno - 1;
		
		setTrack(nextTrackNo, 0);
	}
	@FXML
	private void next(ActionEvent event){
		if(currentAudiobook == null) return;	
		if(currentAudiobook.getPlaylist().getLast().equals(currentTrack)) return;

		int nextTrackNo = currentTrackno + 1;
		
		setTrack(nextTrackNo, 0);
	}
	private void setTrack(int trackNo, int progress){
		//Start progress monitor
		System.out.println("Starting progress monitor");
		if(progress_monitor != null) progress_monitor.kill();
		progress_monitor = new Progress_monitor();
		progress_monitor.start();
		
		//Stop current playback
		if(mp != null && mp.getStatus() == MediaPlayer.Status.PLAYING){
			mp.stop();
			showIsPaused();
		}
		
		//instantiate new data
		currentTrackno = trackNo;
		currentTrack = currentAudiobook.getPlaylist().get(trackNo);
		showTracks();
		
		File file = new File(currentTrack.getPath());
		Media media = new Media(file.toURI().toString());
		mp = new MediaPlayer(media);
		mp.setStartTime(new Duration(progress));
		currentTrack.setDuration((int)mp.getTotalDuration().toMillis());
	}
	
	//Audiobooks - has Helper
	@FXML
	private void show_audiobooks(ActionEvent event){
		new Helper_Audibooks().showAudiobooks(this);
	}	
	public void updateAudiobooks(FlowPane flow, Stage audiobooksStage) {
		new Helper_Audibooks().updateAudiobooks(this, flow, audiobooksStage);
	}
	@FXML 
	public void new_audiobook(){
		new Helper_Audibooks().new_audiobook(this);
	}

	//Bookmarks
	private void showBookmarks(){
		if(player_bookmark_flow == null) return;
		player_bookmark_flow.getChildren().clear();

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
			
			//Cover
			ImageView cover_iv = (ImageView) fxmlLoader.getNamespace().get("bookmark_cover");
			Audiobook audiobook = AudiobookManager.getInstance().getAudiobook(bookmark);
			if(audiobook != null && audiobook.getCover() != null){
				File file = new File(audiobook.getCover());
				FileInputStream inputstream = new FileInputStream(file);
				Image image = new Image(inputstream);
				cover_iv.setImage(image);
			}
			
			//Track number
			Text trackno = (Text) fxmlLoader.getNamespace().get("bookmark_trackno");
			trackno.setText(String.format("%02d", bookmark.getTrackno()+1));
			
			//Progress
			Text time = (Text) fxmlLoader.getNamespace().get("bookmark_time");
			time.setText(Time.toString(bookmark.getProgress()));
			
			player_bookmark_flow.getChildren().add(bookmark_root);
			
			bookmark_root.setOnMouseClicked(new EventHandler<MouseEvent>() {

				@Override
				public void handle(MouseEvent event) {
					selectAudiobook(audiobook, bookmark.getTrackno(), bookmark.getProgress());
				}
			});
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
		
	//Support
	public  File selectFolder(){
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("New Audiobook");
		File defaultFolder = HOME;
		chooser.setInitialDirectory(defaultFolder);
		File folder = chooser.showDialog(new Stage());
		return folder;
	}

	//Monitors
	class Progress_monitor extends Monitor{

		public Progress_monitor() {
			super(1, TimeUnit.SECONDS);
		}

		@Override
		public void execute() {
			if(mp == null) return;
			if(player_time == null) return;
			
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					int progress = (int) mp.getCurrentTime().toMillis();
					int duration = (int) mp.getTotalDuration().toMillis();
					System.out.println("Progress monitor: "+progress + " / " + duration);
					showTime(progress, duration);
				}
			});
			
		}
	}
	class Bookmark_monitor extends Monitor{
		private boolean go_again = true;
		
		public Bookmark_monitor() {
			super(5, TimeUnit.SECONDS);
		}

		@Override
		public void execute() {
			if(mp == null) return;
			if(!go_again && !(mp.getStatus() == MediaPlayer.Status.PLAYING)) return;
			if(currentAudiobook == null) return;
			if(currentTrack == null) return;
			if(currentTrackno < 0) return;
			
			BookmarkManager manager = BookmarkManager.getInstance();
			String author = currentAudiobook.getAuthor();
			String album = currentAudiobook.getAlbum();
			int trackno = currentTrackno;
			int progress = 0;
			Duration progress_duration = mp.getCurrentTime();
			if(progress_duration != null){				
				progress = (int) progress_duration.toMillis();
			}
			boolean force = false; //only update bookmark if progress is greater than previously recorded
			if(trackno > 0 || progress > 0){ //only update bookmark if playback has started
				Bookmark bookmark = manager.createOrUpdateBookmark(author, album, trackno, progress, force);
				System.out.println("Bookmark created or updated: "+bookmark);
				
				Platform.runLater(new Runnable() {
					
					@Override
					public void run() {
						showBookmarks();
					}
				});
			}
			
			go_again = mp.getStatus() == MediaPlayer.Status.PLAYING;
		}
		
	}
}


