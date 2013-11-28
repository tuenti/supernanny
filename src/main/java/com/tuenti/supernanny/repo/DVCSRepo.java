package com.tuenti.supernanny.repo;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Hex;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.DVCSArtifact;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.strategy.DvcsStrategy;
import com.tuenti.supernanny.util.Version;
import com.tuenti.supernanny.util.Versions;

public abstract class DVCSRepo implements Repository {

	public class ResolveInfo {
		public Version reqVersion;
		public String refname;
		public String changeset;

		public ResolveInfo(Version reqVersion, String refname, String changeset) {
			this.reqVersion = reqVersion;
			this.refname = refname;
			this.changeset = changeset;
		}
	}

	@Inject
	protected Util util;

	protected String uri;
	protected DependencyParser dparser;
	protected String tmpname;

	protected abstract DvcsStrategy getStrategy();

	public DVCSRepo(String uri, DependencyParser dparsers) {
		this.uri = uri;
		this.dparser = dparsers;
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(uri.getBytes());
			this.tmpname = ".repo_" + new String(Hex.encodeHex(digest));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void publish(Export export, String version, String prefix, String suffix)
			throws IOException {
		String nameToPublish = prefix + export.getName();

		String tagName = nameToPublish + Util.ARCHIVE_VERSION_DELIMITER + version;

		getStrategy().makeTag(export.getFolder(), uri, tagName);

		System.out.print(MessageFormat.format("  published {0}@{1}", nameToPublish, version));
		if (!suffix.equals("")) {
			System.out.print(MessageFormat.format(" (suffix: {0})", suffix));
		}
		System.out.println();
	}

	protected File getRepoFolder() {
		return new File(util.getDepsFolder(), tmpname);
	}

	private ResolveInfo getMatchingVersionFor(Requirement req) throws IOException, ResolutionException {
		ResolveInfo ri = new ResolveInfo(req.getVersion(), req.getVersion().toString(), null);
		if (!ri.refname.contains("*")) {
			ri.changeset = getStrategy().resolveReference(uri, ri.refname);
			if (ri.changeset != null){
				return ri;
			}
		}

		// try to match the version
		Version[] versions = Versions.sort(getAvailableVersions(req.getName()));
		for (Version version : versions) {
			if (req.matches(version)) {
				ri.reqVersion = version;
				ri.refname = req.getName() + Util.ARCHIVE_VERSION_DELIMITER
						+ ri.reqVersion.toString();
				ri.changeset = getStrategy().resolveReference(uri, ri.refname);
				return ri;
			}
		}
		
		throw new ResolutionException("Can't find artifact for " + req);
	}
	
	@Override
	public List<Artifact> getPossibleArtifactsFor(Requirement req) throws ResolutionException {
		try {
			ResolveInfo ri = getMatchingVersionFor(req);

			// update repo
			getStrategy().checkoutVersion(uri, req.getName(), ri.refname, ri.changeset,
					getRepoFolder());
			// extract the subpath if there is any
			File sourceFolder = getRepoFolder();
			String subDir = null;
			if (req.getRepo().contains("#")) {
				subDir = req.getRepo().replaceAll(".*#", "");
				sourceFolder = new File(sourceFolder, subDir);
			}
			File depFile = new File(sourceFolder, ".DEP");
			List<Requirement> reqs = new LinkedList<Requirement>();
			if (depFile.exists()) {
				reqs = dparser.parseDepsFile(depFile);
			}
			List<Artifact> artifacts = new ArrayList<Artifact>();
			artifacts.add(new DVCSArtifact(req.getName(), ri.reqVersion, this, ri.changeset, subDir, reqs));
			return artifacts;
		} catch (Exception e) {
			throw new ResolutionException(e);
		}
	}

	@Override
	public void init() {
		File clone = getRepoFolder();
		if (clone.exists())
			return;

		try {
			getStrategy().init(clone, uri);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String fetch(Artifact artifact, File destination) throws IOException {
		File clone = getRepoFolder();
		String version = artifact.getVersion().toString();
		String subDir = null;
		String changeset = version;
		if (artifact instanceof DVCSArtifact) {
			DVCSArtifact a = (DVCSArtifact) artifact;
			subDir = a.getSubDir();
			changeset = a.getChangeset();
		}
		String resolvedVersion = getStrategy().fetch(uri, artifact.getName(), version, changeset, subDir,
				destination, clone);
		
		util.stampProject(artifact.getName(), destination, artifact.getOriginUrl(),
				resolvedVersion, getRepoType());
		return resolvedVersion;
	}

	@Override
	public Version getLatestVersion(String name) throws SuperNannyError, IOException {
		Version[] vs = getAvailableVersions(name);
		return Versions.getLatestVersion(vs);
	}

	private Version[] getAvailableVersions(String name) throws SuperNannyError, IOException {
		String[] tags = getStrategy().getTags(uri, name);
		Version[] vs = new Version[tags.length];
		int prefixLength = (name + Util.ARCHIVE_VERSION_DELIMITER).length();
		for (int i = 0; i < tags.length; i++) {
			vs[i] = new Version(tags[i].substring(prefixLength));
		}
		return vs;
	}

	@Override
	public boolean isUpdated(Artifact a, File dep) throws IOException {
		Requirement existing = util.getProjectInfo(dep);
		String expectedVersion = a.getVersion().toString();
		if (a instanceof DVCSArtifact) {
			DVCSArtifact da = (DVCSArtifact) a;
			expectedVersion = da.getChangeset();
		}

		Requirement dummyReq = new Requirement(a.getName(), ReqType.EQ, expectedVersion, a
				.getOrigin().getRepoType(), a.getOriginUrl());
		return dummyReq.equals(existing);
	}

	@Override
	public String getTmpDir() {
		return tmpname;
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public String toString() {
		return getRepoType() + " " + getUri();
	}
}