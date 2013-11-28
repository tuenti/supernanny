package com.tuenti.supernanny.repo.artifacts;

import java.util.Comparator;
import java.util.List;

import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.util.Version;
import com.tuenti.supernanny.util.Versions;

public class Artifact {
	protected final String name;
	protected final Version version;
	protected List<Requirement> requirements;
	protected Repository origin;

	public Artifact(String name, Version version, Repository origin, List<Requirement> requirements) {
		this.name = name;
		this.version = version;
		this.origin = origin;
		this.requirements = requirements;
	}

	public void setOrigin(Repository origin) {
		this.origin = origin;
	}

	public Repository getOrigin() {
		return origin;
	}

	public String getOriginUrl() {
		return origin.getUri();
	}
	
	public List<Requirement> getRequirements() {
		return requirements;
	}

	public String getName() {
		return name;
	}

	public Version getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return name + " " + version + " from " + getOriginUrl() + " deps=" + requirements;
	}
	
	public String toShortString() {
		return name + " " + version;
	}

	public static Comparator<Artifact> getArtifactComparator(boolean descending) {
		final Comparator<Version> versionComparator = Versions.getVersionComparator(descending);
		return new Comparator<Artifact>() {

			@Override
			public int compare(Artifact o1, Artifact o2) {
				return versionComparator.compare(o1.getVersion(), o2.getVersion());
			}
		};
	}
}
