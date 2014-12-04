package model;
import java.io.IOException;

import com.google.api.services.drive.model.File;

public class DriveFile {
	private File f;
	
	public DriveFile(File file){
		f = file;
	}
	
	public String getTitle(){
		return f.getTitle();
	}
	
	public String toPrettyString(){
		try {
			return f.toPrettyString();
		} catch (IOException e) {
			e.printStackTrace();
			return "<< ERROR >>";
		}
	}

}
