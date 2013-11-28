package com.tuenti.supernanny.repo.artifacts;

import java.util.Comparator;

import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.util.Version;
import com.tuenti.supernanny.util.Versions;

public class Requirement {
	private String name;
	private ReqType type;
	private RepositoryType repoType;
	private Version version;
	private String repo;
	private Version versionWithoutWildcards;

	public String getRepo() {
		return repo;
	}

	public RepositoryType getRepoType() {
		return repoType;
	}

	public void setRepoType(RepositoryType repoType) {
		this.repoType = repoType;
	}

	public Version getVersion() {
		return version;
	}

	public ReqType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Requirement(String name, ReqType type, String version, RepositoryType repoType,
			String repo) {
		super();
		this.name = name;
		this.version = new Version(version);
		this.type = type;
		this.repoType = repoType;
		this.repo = repo;

		// get a version for wildcard comparisons
		if (version.contains("*")) {
			switch (type) {
			case SW:
			case GE:
			case GT:
				this.versionWithoutWildcards = new Version(version.replace("*", ""));
				break;
			default:
				throw new SuperNannyError("Don't use wildcards with types other than SW,GT,GE in "
						+ this);
			}
		}

	}

	@Override
	public String toString() {
		return "(" + name + " " + type + " " + version
				+ (repo != null ? " from " + repoType + " " + repo : "") + ")";
	}

	public boolean matches(String aName, Version aVersion) {
		if (aName.equals(name)) {
			return matches(aVersion);
		} else {
			return false;
		}
	}

	public boolean matches(Version artifactVersion) {
		// special artifact version that is matched by any requirement
		if (artifactVersion.getVersionString().equals("*")){
			return true;
		}
		
		// use the version without wildcards if there was a wildcard in the
		// requirement
		if (this.versionWithoutWildcards != null) {
			switch (type) {
			case SW:
				return Versions.startsWith(artifactVersion, this.versionWithoutWildcards);
			case GE:
			case GT:
				return compareVersions(artifactVersion, this.versionWithoutWildcards);
			default:
				throw new SuperNannyError("Don't use wildcards with types other than SW,GT,GE");
			}
		}

		if (type == ReqType.SW) {
			return Versions.startsWith(artifactVersion, version);
		}

		return compareVersions(artifactVersion, version);
	}

	private boolean compareVersions(Version artifactVersion, Version thisVersion) {
		Comparator<Version> cmp = Versions.getVersionComparator(false);
		int outcome = cmp.compare(artifactVersion, thisVersion);
		if (outcome < 0) {
			return type == ReqType.LT || type == ReqType.LE;
		} else if (outcome > 0) {
			return type == ReqType.GT || type == ReqType.GE;
		} else {
			return type == ReqType.GE || type == ReqType.LE || type == ReqType.EQ;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((repo == null) ? 0 : repo.hashCode());
		result = prime * result + ((repoType == null) ? 0 : repoType.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Requirement other = (Requirement) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (repo == null) {
			if (other.repo != null)
				return false;
		} else if (!repo.equals(other.repo))
			return false;
		if (repoType != other.repoType)
			return false;
		if (type != other.type)
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}
}
