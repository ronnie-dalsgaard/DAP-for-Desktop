package model;

import static application.Constants.HOME;
import static application.Constants.WORKINGFOLDER;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import support.AlbumFolderFilter;
import support.Mp3FileFilter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class AudiobookManager extends Data{
	private static AudiobookManager instance = new AudiobookManager();

	public static AudiobookManager getInstance(){
		return instance; //Eager singleton
	}
	
	private AudiobookManager(){}
	
	//CRUD Audiobook
	public void addAudiobook(Audiobook audiobook){
		if(Data.getAudiobooks().contains(audiobook)) return;
		audiobooks.add(audiobook);
		authors.add(audiobook.getAuthor());
		saveAudiobooks();
	}
	public Audiobook getAudiobook(String author, String album){
		if(author == null) return null;
		if(album == null) return null;
		for(Audiobook audiobook : audiobooks){
			if(author.equals(audiobook.getAuthor()) 
					&& album.equals(audiobook.getAlbum())){
				return audiobook;
			}
		}
		return null;
	}
	public Audiobook getAudiobook(Bookmark bookmark){
		String author = bookmark.getAuthor();
		String album = bookmark.getAlbum();
		return getAudiobook(author, album);
	}
	public ArrayList<Audiobook> getAudiobooks(Void v) { return audiobooks; }
	public void updateAudiobook(Audiobook audiobook, Audiobook original_audiobook) {
		for(Audiobook element : getAudiobooks()){
			if(element.equals(original_audiobook)){
				element.setAudiobook(audiobook);
				
				authors.remove(original_audiobook.getAuthor());
				authors.add(audiobook.getAuthor());
			}
		}
		saveAudiobooks();
	}
	public void removeAudiobook(Audiobook audiobook) { 
		audiobooks.remove(audiobook);
		authors.remove(audiobook.getAuthor());
		saveAudiobooks();
	}
	
	//Load and save
	public void saveAudiobooks(){
		Gson gson = new Gson();
		String json = gson.toJson(audiobooks);
		
		//create a file in internal storage
		File file = new File(WORKINGFOLDER, "audiobooks.dap"); //FIXME filename as constant
		try {
			FileWriter writer = new FileWriter(file, false);
			BufferedWriter out = new BufferedWriter(writer);
			out.write(json);
			out.close();
		} catch (IOException e) {
			System.out.println("Unable to save audiobooks");
			e.printStackTrace();
		}
		
		
		String authors_json = gson.toJson(authors);
		//Create a file for authors
		File authors_file = new File(WORKINGFOLDER, "authors.dap"); //FIXME filename as constant
		try {
			FileWriter writer = new FileWriter(authors_file, false);
			BufferedWriter out = new BufferedWriter(writer);
			out.write(authors_json);
			out.close();
		} catch (IOException e) {
			System.out.println("Unable to save audiobooks");
			e.printStackTrace();
		}
	}
	public ArrayList<Audiobook> loadAudiobooks(){
		//Audiobooks
		File file = new File(WORKINGFOLDER, "audiobooks.dap");
		try {
			FileInputStream stream = new FileInputStream(file);
			InputStreamReader reader = new InputStreamReader(stream);
			BufferedReader in = new BufferedReader(reader);
			Gson gson = new Gson();
			ArrayList<Audiobook> list = gson.fromJson(in, new TypeToken<ArrayList<Audiobook>>(){}.getType());
			audiobooks.clear();
			audiobooks.addAll(list);
			in.close();
		} catch (FileNotFoundException e) {
			System.out.println("Unable to load file doesn't exists!");
			return new ArrayList<Audiobook>();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Authors
		File authors_file = new File(WORKINGFOLDER, "authors.dap");
		try {
			FileInputStream stream = new FileInputStream(authors_file);
			InputStreamReader reader = new InputStreamReader(stream);
			BufferedReader in = new BufferedReader(reader);
			Gson gson = new Gson();
			HashSet<String> set = gson.fromJson(in, new TypeToken<HashSet<String>>(){}.getType());
			authors.clear();
			authors.addAll(set);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return audiobooks;
	}
	
	public Audiobook autoCreateAudiobook(File album_folder, boolean incl_subfolders){
		if(album_folder == null) return null;
		Audiobook audiobook = new Audiobook();
		audiobook.setAuthor(album_folder.getParentFile().getName());
		audiobook.setAlbum(album_folder.getName());

		String cover = null;
		for(File file : album_folder.listFiles()){
			if("albumart.jpg".equalsIgnoreCase(file.getName())){
				cover = file.getAbsolutePath();
				break;
			}
		}
		if(cover != null) { audiobook.setCover(cover); }
		
		ArrayList<File> filelist;
		if(incl_subfolders){
			filelist = collectFiles(new ArrayList<File>(), album_folder, new Mp3FileFilter());
		} else {
			filelist = new ArrayList<File>(Arrays.asList(album_folder.listFiles(new Mp3FileFilter())));			
		}

		
		TrackList playlist = new TrackList();
		//TODO sort by filename
		for(File file : filelist){
			Track track = new Track();
			track.setPath(file.getAbsolutePath());
			track.setTitle(file.getName().replace(".mp3", ""));
			if(cover != null) track.setCover(cover);
			
			playlist.add(track);
		}
		audiobook.setPlaylist(playlist);
		return audiobook;
	}
	public ArrayList<Audiobook> autodetect(){
		ArrayList<Audiobook> list = new ArrayList<Audiobook>();

		ArrayList<File> albums = collectFiles(new ArrayList<File>(), HOME, new AlbumFolderFilter());
		for(File album_folder : albums){
			Audiobook audiobook = autoCreateAudiobook(album_folder, true);
			if(audiobook != null) list.add(audiobook);
		}		
		return list;
	}
	private ArrayList<File> collectFiles(ArrayList<File> list, File folder, FileFilter filter){
		if(list == null) throw new IllegalArgumentException("Must have a list");
		if(folder == null) throw new IllegalArgumentException("Must have a root folder");
		if(folder.listFiles() == null) return list;
		for(File file : folder.listFiles()){
			if(filter == null || filter.accept(file.getAbsoluteFile())) list.add(file);
			if(file.isDirectory()) collectFiles(list, file, filter);
		}
		return list;
	}

}
