package com.tuenti.supernanny.util;


public class Version {
	private String versionString;
	@SuppressWarnings("rawtypes")
	private Comparable[] parsedVersion;
	
	public Version(String versionString) {
		super();
		this.versionString = versionString;
		this.parsedVersion = Versions.parse(versionString);
	}

	public String getVersionString() {
		return versionString;
	}

	@SuppressWarnings("rawtypes")
	public Comparable[] getParsedVersion() {
		return parsedVersion;
	}

	@Override
	public String toString() {
		return versionString;
	}

	@Override
	public int hashCode() {
		return versionString.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Version other = (Version) obj;
		return versionString.equals(other.versionString);
	}
}
