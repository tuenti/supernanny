package com.tuenti.supernanny.repo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.util.Version;

public class SymlinkRepo implements Repository {
	@Inject
	private Util util;

	private DependencyParser dparser;

	public SymlinkRepo(DependencyParser dparser) {
		super();
		this.dparser = dparser;
	}

	@Override
	public List<Artifact> getPossibleArtifactsFor(Requirement req) throws ResolutionException {
		// just get the dependencies
		File depFile = new File(util.getDepsFolder() + File.separator + req.getName(), ".DEP");
		List<Requirement> reqs = new LinkedList<Requirement>();
		if (depFile.exists()) {
			try {
				reqs = dparser.parseDepsFile(depFile);
			} catch (Exception e) {
				throw new ResolutionException(e);
			}
		}
		List<Artifact> artifacts = new ArrayList<Artifact>();
		// return artifact with wildcard version so it matches everything
		artifacts.add(new Artifact(req.getName(), new Version("*"), this, reqs));
		return artifacts;
	}

	@Override
	public void init() {
		// do nothing
	}

	@Override
	public String fetch(Artifact artifact, File destination) throws IOException {
		// do nothing
		return null;
	}

	@Override
	public String getUri() {
		return "*Manual override*";
	}

	@Override
	public RepositoryType getRepoType() {
		return RepositoryType.SYMLINK;
	}

	@Override
	public Version getLatestVersion(String name) {
		throw new RuntimeException("Symlink repo doesn't implement getLatestVersion");
	}

	@Override
	public void publish(Export export, String version, String prefix, String suffix)
			throws IOException {
		throw new RuntimeException("Symlink repo doesn't implement publish");
	}

	@Override
	public boolean isUpdated(Artifact artifact, File dep) throws IOException {
		return true;
	}

	@Override
	public String getTmpDir() {
		return null;
	}
}
