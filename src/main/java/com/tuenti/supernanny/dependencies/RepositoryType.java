package com.tuenti.supernanny.dependencies;

/**
 * Dependency type definition.
 * 
 * Each dependency type is tied to it's strategy.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public enum RepositoryType {
	GIT(0), MERCURIAL(1), TARGZ(2), TARBZ2(3), TARXZ(4), SYMLINK(5), ARCHIVE(6);

	private final int type;

	RepositoryType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}