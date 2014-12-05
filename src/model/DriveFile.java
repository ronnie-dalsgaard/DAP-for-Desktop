package model;
import java.io.IOException;
import java.util.ArrayList;

import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.ParentReference;

public class DriveFile {
	private File f;
	
	public DriveFile(File file){
		f = file;
	}
	public DriveFile(){
		f = new File();
	}
	
	public File getFile() { return f; }
	public String getTitle() { return f.getTitle(); }
	public String getMimiType() { return f.getMimeType(); }
	public String toPrettyString(){
		try {
			return f.toPrettyString();
		} catch (IOException e) {
			e.printStackTrace();
			return "<< ERROR >>";
		}
	}
	public String getDownloadUrl() { return f.getDownloadUrl(); }
	public String getFileId() { return f.getId(); }
	public void setTitle(String title) { f.setTitle(title); }
	public void setMimeType(String mimetype) { f.setMimeType(mimetype); }
	public void setDescription(String desc) { f.setDescription(desc); }
	public void setParent(DriveFile folder){
		ParentReference parent = new ParentReference();
	    parent.setId(folder.getFileId());
		ArrayList<ParentReference> parents = new ArrayList<ParentReference>();
		f.setParents(parents);
	}
}
