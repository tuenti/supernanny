package com.tuenti.supernanny.repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.ArchiveArtifact;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.repo.index.IndexReader;
import com.tuenti.supernanny.strategy.ArchiveStrategy;
import com.tuenti.supernanny.util.Version;

public class ArchiveRepo implements Repository {
	@Inject
	private Util util;
	@Inject
	private ArchiveStrategy strategy;
	private String uri;
	private Map<String, List<Artifact>> repository = new HashMap<String, List<Artifact>>();
	private boolean verifyPublishedArtifact;
	private int verificationTimeout;

	public ArchiveRepo(String uri, boolean verifyArtifacts, int verifyTimeout) {
		this.uri = uri;
		verifyPublishedArtifact = verifyArtifacts;
		verificationTimeout = verifyTimeout;
	}

	public void setArtifacts(Collection<ArchiveArtifact> artifacts) {
		repository.clear();
		for (Artifact artifact : artifacts) {
			artifact.setOrigin(this);
			String name = artifact.getName();
			if (!repository.containsKey(name)) {
				repository.put(name, new ArrayList<Artifact>());
			}
			repository.get(name).add(artifact);
		}

		// sort all lists
		Set<Entry<String, List<Artifact>>> lists = repository.entrySet();
		for (Entry<String, List<Artifact>> entry : lists) {
			List<Artifact> l = entry.getValue();
			Comparator<Artifact> comparator = Artifact.getArtifactComparator(true);
			Collections.sort(l, comparator);
		}
	}

	@Override
	public List<Artifact> getPossibleArtifactsFor(Requirement req) throws ResolutionException {
		return repository.get(req.getName());
	}

	@Override
	public void init() {
		try {
			URL urlgz = new URL(uri + "index.gz");
			URL urlplain = new URL(uri + "index");
			BufferedReader in;
			try {
				in = new BufferedReader(new InputStreamReader(new GZIPInputStream(
						urlgz.openStream())));
			} catch (FileNotFoundException e) {
				try {
					in = new BufferedReader(new InputStreamReader(urlplain.openStream()));
				} catch (FileNotFoundException f) {
					throw new SuperNannyError("Can't find index file " + urlgz + " or " + urlplain);
				}
			}

			IndexReader indexReader = new IndexReader(in);
			setArtifacts(indexReader.parse());
			in.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String fetch(Artifact artifact, File destination) throws IOException {
		if (artifact instanceof ArchiveArtifact) {
			ArchiveArtifact arch = (ArchiveArtifact) artifact;
			strategy.fetch(uri, arch.getFilename(), destination);
			String resolvedVersion = artifact.getVersion().toString();
			util.stampProject(artifact.getName(), destination, artifact.getOriginUrl(),
					resolvedVersion, getRepoType());
			return resolvedVersion;
		} else {
			throw new RuntimeException("Given an artifact that wasn't from this repo");
		}
	}

	@Override
	public Version getLatestVersion(String name) {
		List<Artifact> artifacts = repository.get(name);
		if (artifacts != null) {
			return artifacts.get(0).getVersion();
		}
		return null;
	}

	public boolean existsVersion(String name, String version) {
		List<Artifact> artifacts = repository.get(name);
		if (artifacts != null) {
			for (Artifact artifact : artifacts) {
				if (version.equals(artifact.getVersion().getVersionString())) {
					return true;
				}
			}
		}

		return false;
	}

	private boolean verifyArtifact(String name, String version) {
		int maxWait = verificationTimeout * 1000;
		int interval = 500;
		long maxTime = System.currentTimeMillis() + maxWait;
		while (System.currentTimeMillis() < maxTime) {
			// reread the index
			init();
			if (existsVersion(name, version)) {
				return true;
			}

			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
			}
		}
		return false;
	}

	public void publish(Export export, String version, String prefix, String suffix,
			String extension) throws IOException {
		String nameToPublish = prefix + export.getName();

		// publish the dependency
		strategy.publish(export.getFolder(), uri, nameToPublish, version, suffix, extension);

		boolean published = true;

		if (verifyPublishedArtifact) {
			System.out.println("Artifact published - waiting for index to update... (max "
					+ verificationTimeout + " seconds)");
			published = verifyArtifact(nameToPublish, version);
		}

		if (published) {
			System.out.print(MessageFormat.format("  Published {0}@{1}", nameToPublish, version));
			if (!suffix.equals("")) {
				System.out.print(MessageFormat.format(" (suffix: {0})", suffix));
			}
			System.out.println();
		} else {
			System.out
					.println("ERROR! Publish check timed out... can't assure that your artifact has been published");
		}
	}

	@Override
	public boolean isUpdated(Artifact a, File dep) throws IOException {
		Requirement existing = util.getProjectInfo(dep);

		Requirement dummyReq = new Requirement(a.getName(), ReqType.EQ, a.getVersion().toString(),
				a.getOrigin().getRepoType(), a.getOriginUrl());
		return dummyReq.equals(existing);
	}

	@Override
	public String getTmpDir() {
		return null;
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public String toString() {
		return getRepoType() + " " + getUri();
	}

	@Override
	public RepositoryType getRepoType() {
		return RepositoryType.ARCHIVE;
	}

	@Override
	public void publish(Export export, String version, String prefix, String suffix)
			throws IOException {
		publish(export, version, prefix, suffix, ArchiveStrategy.TAR_XZ_EXT);
	}
}
