package support;

import model.Bookmark;

public class ConflictResolver implements iConflictResolver<Bookmark> {

	@Override
	public Bookmark resolveConflicts(Bookmark oldData, Bookmark newData) {
		//FIXME The conflict isn't really resolved
		return newData;
	}
	
	public String resolveConflicts(String oldData, String newData) {
		//FIXME The conflict isn't really resolved
		return newData;
	}

}
