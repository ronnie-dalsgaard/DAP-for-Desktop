package model;
import java.util.ArrayList;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class DriveFileList {
	private FileList list;
	
	public DriveFileList(FileList list){
		this.list = list;
	}
	
	public ArrayList<DriveFile> getItems(){
		ArrayList<DriveFile> l = new ArrayList<DriveFile>();
		for(File file : list.getItems()){
			l.add(new DriveFile(file));
		}
		return l;
	}

}
