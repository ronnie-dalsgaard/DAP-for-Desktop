package support;

public interface iConflictResolver<T> {
	public T resolveConflicts(T oldData, T newData);
}
