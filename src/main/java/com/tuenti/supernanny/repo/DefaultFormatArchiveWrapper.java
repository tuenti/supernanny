package com.tuenti.supernanny.repo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.strategy.ArchiveStrategy;
import com.tuenti.supernanny.util.Version;

/**
 * Wrapper for ArchiveRepo
 * 
 * Sets the default format when publishing artifacts, delegates all calls to the
 * given ArchiveRepo.
 */
public class DefaultFormatArchiveWrapper implements Repository {
	private ArchiveRepo repo;
	private String extension;
	private RepositoryType type;
	@Inject
	private Util util;

	public DefaultFormatArchiveWrapper(ArchiveRepo repo, RepositoryType type) {
		super();
		this.repo = repo;
		this.type = type;

		switch (this.type) {
		case TARBZ2:
			extension = ArchiveStrategy.TAR_BZ2_EXT;
			break;
		case TARGZ:
			extension = ArchiveStrategy.TAR_GZ_EXT;
			break;
		case TARXZ:
			extension = ArchiveStrategy.TAR_XZ_EXT;
			break;
		default:
			throw new RuntimeException("Invalid format for this repo " + repo);
		}
	}

	@Override
	public void init() {
		repo.init();
	}

	@Override
	public String getTmpDir() {
		return repo.getTmpDir();
	}

	@Override
	public String getUri() {
		return repo.getUri();
	}

	@Override
	public RepositoryType getRepoType() {
		return type;
	}

	@Override
	public boolean isUpdated(Artifact artifact, File dep) throws IOException {
		return repo.isUpdated(artifact, dep);
	}

	@Override
	public List<Artifact> getPossibleArtifactsFor(Requirement req) throws ResolutionException {
		List<Artifact> artifacts = repo.getPossibleArtifactsFor(req);
		for (Artifact artifact : artifacts) {
			artifact.setOrigin(this);
		}
		return artifacts;
	}

	@Override
	public String fetch(Artifact artifact, File destination) throws IOException {
		String version = repo.fetch(artifact, destination);
		util.stampProject(artifact.getName(), destination, artifact.getOriginUrl(), version,
				getRepoType());
		return version;
	}

	@Override
	public Version getLatestVersion(String name) throws SuperNannyError, IOException {
		return repo.getLatestVersion(name);
	}

	@Override
	public void publish(Export export, String version, String prefix, String suffix)
			throws IOException {
		repo.publish(export, version, prefix, suffix, extension);
	}

	public String toString() {
		return getRepoType() + " " + getUri();
	}
}
