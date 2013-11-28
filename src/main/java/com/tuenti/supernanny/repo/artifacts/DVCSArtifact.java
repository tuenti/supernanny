package com.tuenti.supernanny.repo.artifacts;

import java.util.List;

import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.util.Version;

/**
 * Artifacts coming from a dvcs repo, include the concrete changeset and a possible subdir
 */
public class DVCSArtifact extends Artifact {
	protected String changeset;
	protected String subDir;

	public String getChangeset() {
		return changeset;
	}

	public DVCSArtifact(String name, Version version, Repository origin, String changeset,
			String subDir, List<Requirement> requirements) {
		super(name, version, origin, requirements);
		this.changeset = changeset;
		this.subDir = subDir;
	}

	public String getOriginUrl() {
		String s = origin.getUri();
		if (subDir != null) {
			s += "#" + subDir;
		}
		return s;
	}

	public String getSubDir() {
		return subDir;
	}

	@Override
	public String toString() {
		String s = super.toString();
		if (subDir != null) {
			s += " #" + subDir;
		}
		return s;
	}
}
