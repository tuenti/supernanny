package com.tuenti.supernanny.repo.artifacts;

import java.util.List;

import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.util.Version;

/**
 * For artifacts coming from an archive repo, includes the filename
 */
public class ArchiveArtifact extends Artifact {
	protected String filename;
	protected String md5;

	public ArchiveArtifact(String name, Version version, String filename, String md5, Repository origin, List<Requirement> requirements) {
		super(name, version, origin, requirements);
		this.filename = filename;
		this.md5 = md5;
	}

	public String getFilename() {
		return filename;
	}
}
