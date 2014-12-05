package application;

import static application.Constants.COVER_HEIGHT;
import static application.Constants.COVER_WIDTH;
import static application.Constants.HOME;
import static application.Constants.ICON_PAUSE_OVER_VIDEO;
import static application.Constants.ICON_PLAY_OVER_VIDEO;
import static application.Constants.PRESERVE_RATIO;
import static application.Constants.SMOOTH;
import static application.Constants.STYLE_AUDIOBOOKS_HEADER;
import static application.Constants.STYLE_MENU;
import static application.Constants.STYLE_MENU_ITEM;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import model.Audiobook;
import model.AudiobookManager;
import model.Bookmark;
import model.BookmarkManager;
import model.DriveFile;
import model.Track;
import support.ConflictResolver;
import support.Monitor;
import support.Time;

public class Main extends Application {
	private Text player_author, player_album, player_track, player_time;
	private ImageView player_cover, player_overCover;
	private FlowPane player_track_flow, player_bookmark_flow;
	private File homefolder;
	private Audiobook currentAudiobook = null;
	private Track currentTrack = null;
	private int currentTrackno = -1;
	private static MediaPlayer mp = null;
	private static Monitor progress_monitor = null;
	private static Monitor bookmark_monitor = null;
	private VBox dialog_base, dialog_content;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		AudiobookManager.getInstance().loadAudiobooks();
		ArrayList<Bookmark> bookmarks = BookmarkManager.getInstance().loadBookmarks();
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("miniplayer.fxml"));
			StackPane root = loader.load();

			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent t) {
					System.exit(0);
				}
			});


			player_author = (Text) loader.getNamespace().get("player_author");
			player_album = (Text) loader.getNamespace().get("player_album");
			player_cover = (ImageView) loader.getNamespace().get("player_cover");
			player_track = (Text) loader.getNamespace().get("player_track");
			player_track_flow = (FlowPane) loader.getNamespace().get("player_track_flow");
			player_overCover = (ImageView) loader.getNamespace().get("player_overCover");
			player_time = (Text) loader.getNamespace().get("player_time");
			Button btn_next = (Button) loader.getNamespace().get("btn_next");
			btn_next.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					next();
				}
			});
			Button btn_prev = (Button) loader.getNamespace().get("btn_prev");
			btn_prev.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					prev();
				}
			});			
			Button play_pause = (Button) loader.getNamespace().get("btn_play_pause");
			play_pause.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					play_pause();
				}
			});
			player_bookmark_flow = (FlowPane) loader.getNamespace().get("player_bookmark_flow");
			Button sync_bookmarks = (Button) loader.getNamespace().get("sync_bookmarks");
			sync_bookmarks.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					sync_bookmarks();
				}
			});	
			//Audiobooks
			ScrollPane scroller = (ScrollPane) loader.getNamespace().get("audiobooks_scroller");
			scroller.setStyle("-fx-background: rgb(0,0,0);");

			//flow
			FlowPane audiobooks_flowpane = (FlowPane) loader.getNamespace().get("flow");
			displayAudiobooks(audiobooks_flowpane);
			if(currentAudiobook == null && bookmarks != null && !bookmarks.isEmpty()){
				Bookmark bookmark = bookmarks.get(0);
				if(bookmark != null){
					Audiobook audiobook = AudiobookManager.getInstance().getAudiobook(bookmark);
					if(audiobook != null){
						int trackNo = bookmark.getTrackno();
						int progress = bookmark.getProgress();
						selectAudiobook(audiobook, trackNo, progress);
					}
				}
			}

			Button reload_btn = (Button) loader.getNamespace().get("audiobooks_reload");
			reload_btn.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					autoDetectAudiobooks();
					displayAudiobooks(audiobooks_flowpane);
				}
			});



			FXMLLoader dialog_loader = new FXMLLoader(getClass().getResource("dialog.fxml"));
			dialog_base = dialog_loader.load();
			root.getChildren().add(dialog_base);
			Button dialog_exit = (Button) dialog_loader.getNamespace().get("dialog_exit");
			dialog_exit.setOnAction(new EventHandler<ActionEvent>(){

				@Override
				public void handle(ActionEvent event) {
					dialog_base.setVisible(false);					
				}

			});
			dialog_base.setVisible(false);
			dialog_content = (VBox) dialog_loader.getNamespace().get("dialog_content");

			primaryStage.show();

		} catch(Exception e) {
			e.printStackTrace();
		}

		showBookmarks();
		if(bookmark_monitor == null){
			bookmark_monitor = new Bookmark_monitor();
			bookmark_monitor.start();
		}
	}
	private void displayAudiobooks(FlowPane audiobooks_flowpane) {
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
								//								audiobooksStage.close();
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

	
	private void sync_bookmarks(){
		String refreshToken = getProperties().getProperty("key_refreshToken");
//		refreshToken = "";
		
		if(refreshToken == null || refreshToken.isEmpty()){
			getAccessCode(); //Will eventually lead to onRefreshTokenFetched(..)
		}else{
			onRefreshTokenFetched(refreshToken);
		}
	}
	public void getAccessCode(){
		try {
			FXMLLoader dialog_access_code_loader = new FXMLLoader(getClass().getResource("access_code.fxml"));
			VBox content = dialog_access_code_loader.load();
			Text msg = (Text) dialog_access_code_loader.getNamespace().get("access_code_msg");
			String url = DriveHandler.getRefreshUrl();
			msg.setText("Copy the URL below and paste it in a browser. Google will guide you through the steps and give you an access code. Enter the access code in the bottom field and then press OK");
			TextField output = (TextField) dialog_access_code_loader.getNamespace().get("access_code_output");
			output.setText(url);
			output.focusedProperty().addListener(new ChangeListener<Boolean>() {
		        @SuppressWarnings("rawtypes")
				@Override
		        public void changed(ObservableValue ov, Boolean t, Boolean t1) {

		            Platform.runLater(new Runnable() {
		                @Override
		                public void run() {
		                    if (output.isFocused() && !output.getText().isEmpty()) {
		                        output.selectAll();
		                    }
		                }
		            });
		        }
		    });
			dialog_content.getChildren().clear();
			dialog_content.getChildren().add(content);
			Button btn_ok = (Button) dialog_access_code_loader.getNamespace().get("btn_ok");
			btn_ok.setOnAction(new EventHandler<ActionEvent>(){

				@Override
				public void handle(ActionEvent event) {
					TextField input = (TextField) dialog_access_code_loader.getNamespace().get("access_code_input");
					String code = input.getText();
					onAccessCodeEntered(code);
				}

			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		dialog_base.setVisible(true);
	}
	private void onAccessCodeEntered(String code){
		String refreshToken = DriveHandler.getRefreshToken(code);
		Properties props = getProperties();
		props.setProperty("key_refreshToken", refreshToken);
		setProperties(props);
		onRefreshTokenFetched(refreshToken);
	}
	private void onRefreshTokenFetched(String refreshToken){
		DriveHandler handler = new DriveHandler(refreshToken);
		
		FXMLLoader dialog_content_loader = new FXMLLoader(getClass().getResource("up_download.fxml"));
		try {
			HBox up_download = dialog_content_loader.load();
			dialog_content.getChildren().clear();
			dialog_content.getChildren().add(up_download);
			Button btn_download = (Button) dialog_content_loader.getNamespace().get("btn_download");
			btn_download.setOnAction(new EventHandler<ActionEvent>(){

				@Override
				public void handle(ActionEvent event) {
					download(handler);
					dialog_base.setVisible(false);
				}

			});
			Button btn_upload = (Button) dialog_content_loader.getNamespace().get("btn_upload");
			btn_upload.setOnAction(new EventHandler<ActionEvent>(){

				@Override
				public void handle(ActionEvent event) {
					upload(handler);
					dialog_base.setVisible(false);
				}

			});
			dialog_base.setVisible(true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}


	
	//Download
	private void download(DriveHandler handler){
		String content = downloadAsJSON(handler);
		if(content == null) return;
		ArrayList<Bookmark> bookmarks = BookmarkManager.getInstance().convertToBookmarks(content);
		mergeBookmarks(bookmarks);
		showBookmarks();
	}
	private String downloadAsJSON(DriveHandler handler){
		DriveFile file = getBookmarksFile(handler);
		if(file == null) return null;
		String content = handler.getContent(file);
		return content;
	}
	private void mergeBookmarks(ArrayList<Bookmark> fetchedBookmarks){
		BookmarkManager bm = BookmarkManager.getInstance();
		AudiobookManager am = AudiobookManager.getInstance();
		for(Bookmark fetched : fetchedBookmarks){
			System.out.println("Fetched: "+fetched);
			Audiobook fetchedAudiobook = am.getAudiobook(fetched);
			if(bm.hasBookmark(fetched)){
				Bookmark exisisting = bm.getBookmark(fetched.getAuthor(), fetched.getAlbum());
				Bookmark bookmark = resolveConflicts(exisisting, fetched);
				bm.createOrUpdateBookmark(bookmark, false);
			} else if(fetchedAudiobook != null){
				bm.createOrUpdateBookmark(fetched, false);
			}
			bm.saveBookmarks();
		}
		
	}
	private Bookmark resolveConflicts(Bookmark oldData, Bookmark newData){
		ConflictResolver resolver = new ConflictResolver();
		Bookmark bookmark = resolver.resolveConflicts(oldData, newData);
		return bookmark;
	}
	
	//Upload
	private void upload(DriveHandler handler){
		String data = BookmarkManager.getInstance().constructData();
		DriveFile file = getBookmarksFile(handler);
		if(file == null){
			System.out.println("Create new file");
			file = createBookmarksFileAndFolder(handler, data);
		} else {
			System.out.println("update file");
			file = handler.setContent(file, data);			
		}
	}
	private DriveFile createBookmarksFileAndFolder(DriveHandler handler, String data){
		DriveFile folder = getDAPFolder(handler);
		if(folder == null){
			folder = handler.createDAPFolder();
		}
		
		DriveFile file = handler.createBookmarksFile(data);
		file.setParent(folder);
		return file;
	}
	
	//Common (download + upload)
	private DriveFile getBookmarksFile(DriveHandler handler){
		for(DriveFile file : handler.getFilelist().getItems()){
			if("bookmarks.dap".equals(file.getTitle())){
				return file;
			}
		}
		return null;
	}
	private DriveFile getDAPFolder(DriveHandler handler){
		for(DriveFile file : handler.getFilelist().getItems()){
			if("DAP".equals(file.getTitle())
					&& "application/vnd.google-apps.folder".equals(file.getMimiType())){
				System.out.println(file.getTitle());
				return file;
			}
		}
		return null;
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
	private void play_pause() {
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
	private void prev(){
		if(currentAudiobook.getPlaylist().getFirst().equals(currentTrack)) return;

		int nextTrackNo = currentTrackno - 1;

		setTrack(nextTrackNo, 0);
	}
	private void next(){
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

	//Audiobooks
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
				homefolder = selectFolder("Select homefolder");
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
		homefolder = selectFolder("Select homefolder");
	}
	public File selectFolder(String title){
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle(title);
		File defaultFolder = HOME;
		chooser.setInitialDirectory(defaultFolder);
		File folder = chooser.showDialog(new Stage());
		return folder;
	}
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

	//Bookmarks
	public void showBookmarks(){
		if(player_bookmark_flow == null) return;
		player_bookmark_flow.getChildren().clear();

		for(int i = 0; i < BookmarkManager.getBookmarks().size(); i++){
			Bookmark bookmark = BookmarkManager.getBookmarks().get(i);
			System.out.println("(Main.646) Show: "+bookmark);
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
					switch(event.getButton()){
					case PRIMARY: 
						selectAudiobook(audiobook, bookmark.getTrackno(), bookmark.getProgress());
						break;
					case SECONDARY:
						System.out.println("RIGHT CLICK");
						break;
					default: /* Do nothing */
					}
				}
			});
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	//Support
	public Properties getProperties(){
		File file = new File("dap.properties");
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

		try {
			FileInputStream in = new FileInputStream(file);
			Properties props = new Properties();
			props.load(in);
			return props;
		} catch (FileNotFoundException e) {
			System.out.println("Properties file doesn't exist");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
	public void setProperties(Properties props){
		FileOutputStream out;
		try {
			out = new FileOutputStream(new File("dap.properties"));
			String comments = "Time stamp: " + Time.getTimestamp().toString(Time.TimeStamp.FORMAT_DAY_TIME_VERY_EXACT);
			props.store(out, comments);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
					//					System.out.println("Progress monitor: "+progress + " / " + duration);
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
