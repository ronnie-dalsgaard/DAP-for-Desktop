package model;

import java.util.ArrayList;

public class TrackList extends ArrayList<Track> {
	private static final long serialVersionUID = 6456287610354604293L;
	
	public TrackList() { super(); }
	public TrackList(TrackList original){
		super();
		addAll(original);
	}

	public Track getLast(){
		return get(size()-1);
	}

	public Track getFirst(){
		return get(0);
	}
}
