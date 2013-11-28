package com.tuenti.supernanny.resolution;

import org.junit.Assert;
import org.junit.Test;

import com.tuenti.supernanny.util.Version;
import com.tuenti.supernanny.util.Versions;

public class VersionTest {
	@Test
	public void getLatestAllNumbers() {
		Version[] versions = new Version[]{new Version("1.2.3"), new Version("1.2.4")};
		Assert.assertEquals("Wrong latest version", new Version("1.2.4"), Versions.getLatestVersion(versions));

		versions = new Version[]{new Version("1.3.3"), new Version("1.2.4")};
		Assert.assertEquals("Wrong latest version", new Version("1.3.3"), Versions.getLatestVersion(versions));

		// verify numbers are not compared lexicographically
		versions = new Version[]{new Version("10.0.0"), new Version("2.1.1")};
		Assert.assertEquals("Wrong latest version", new Version("10.0.0"), Versions.getLatestVersion(versions));
	}
	
	@Test
	public void getLatestSomeLetters() {
		Version[] versions = new Version[]{new Version("1.2.b"), new Version("1.2.a")};
		Assert.assertEquals("Wrong latest version", new Version("1.2.b"), Versions.getLatestVersion(versions));

		versions = new Version[]{new Version("a.2.2"), new Version("a.2.3")};
		Assert.assertEquals("Wrong latest version", new Version("a.2.3"), Versions.getLatestVersion(versions));

		versions = new Version[]{new Version("a.4.4"), new Version("b.2.3")};
		Assert.assertEquals("Wrong latest version", new Version("b.2.3"), Versions.getLatestVersion(versions));
	}
	
	@Test
	public void differentLength() {
		Version[] versions = new Version[]{new Version("1.2"), new Version("1.2.1")};
		Assert.assertEquals("Wrong latest version", new Version("1.2.1"), Versions.getLatestVersion(versions));

		versions = new Version[]{new Version("1.2"), new Version("1.3")};
		Assert.assertEquals("Wrong latest version", new Version("1.3"), Versions.getLatestVersion(versions));

		versions = new Version[]{new Version("1.2"), new Version("1.1.9")};
		Assert.assertEquals("Wrong latest version", new Version("1.2"), Versions.getLatestVersion(versions));
	}
	
	@Test
	public void mixedNumbersLetters() {
		Version[] versions = new Version[]{new Version("1.2"), new Version("1.a")};
		Assert.assertEquals("Wrong latest version", new Version("1.a"), Versions.getLatestVersion(versions));
	
		versions = new Version[]{new Version("a"), new Version("9")};
		Assert.assertEquals("Wrong latest version", new Version("a"), Versions.getLatestVersion(versions));
	}
	
	@Test
	public void empty() {
		Version[] versions = new Version[]{new Version(""), new Version("1.1")};
		Assert.assertEquals("Wrong latest version", new Version("1.1"), Versions.getLatestVersion(versions));
	}
}
