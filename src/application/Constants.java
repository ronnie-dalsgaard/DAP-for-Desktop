package application;

import java.io.File;

public class Constants {
	
	public static File HOME = new File(System.getProperty("user.home"));
	public static File WORKINGFOLDER = new File(System.getProperty("user.dir"));
	
	public static final String STYLE_MENU = "-fx-background-color: #000000; -fx-text-fill: #FFFFFF;" +
			"-fx-border: 2px solid; -fx-border-color: white;"+
			"-fx-background-radius: 2.0; -fx-border-radius: 2.0";
	public static final String STYLE_MENU_ITEM = "-fx-text-fill: #FFFFFF;";
	public static final String STYLE_AUDIOBOOKS_HEADER = "-fx-fill: #FFFFFF; -fx-font: 24px Roboto;";
	
	public static final String SEP = System.getProperty("file.separator");
	public static final String GOOGLE_ICONS = WORKINGFOLDER + SEP + "bin" + SEP + "images" + SEP +
			"Google icons" + SEP + "drawable-xhdpi" + SEP;
	
	public static final String ICON_PLAY_OVER_VIDEO = GOOGLE_ICONS + "ic_action_play_over_video.png";
	public static final String ICON_PAUSE_OVER_VIDEO = GOOGLE_ICONS + "ic_action_pause_over_video.png";
	public static final String ICON_NEW = GOOGLE_ICONS + "ic_action_new.png";
	
	public static final int COVER_WIDTH = 96;
	public static final int COVER_HEIGHT = 144;
	public static final boolean PRESERVE_RATIO = true;
	public static final boolean SMOOTH = false;
	
	public static final String END = "/END";
	
}
