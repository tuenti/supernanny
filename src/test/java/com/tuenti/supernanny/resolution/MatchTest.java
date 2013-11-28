package com.tuenti.supernanny.resolution;

import junit.framework.Assert;

import org.junit.Test;

import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.util.Version;

public class MatchTest {
	@Test
	public void testPrefixMatches() {
		Requirement r = new Requirement("test", ReqType.SW, "5.9", RepositoryType.GIT, "");
		Assert.assertFalse(r.matches(new Version("5.8.0")));
		Assert.assertFalse(r.matches(new Version("5.10.0")));
		Assert.assertTrue(r.matches(new Version("5.9")));
		Assert.assertTrue(r.matches(new Version("5.9.0")));
		Assert.assertTrue(r.matches(new Version("5.9.1")));
		r = new Requirement("test", ReqType.SW, "5.*", RepositoryType.GIT, "");
		Assert.assertTrue(r.matches(new Version("5.9.1")));
		Assert.assertTrue(r.matches(new Version("5.10.1")));
		Assert.assertTrue(r.matches(new Version("5")));
		Assert.assertFalse(r.matches(new Version("4.6.5")));
		Assert.assertFalse(r.matches(new Version("50.4.5")));
		r = new Requirement("test", ReqType.SW, "*", RepositoryType.GIT, "");
		Assert.assertTrue(r.matches(new Version("5.0.0")));
		Assert.assertTrue(r.matches(new Version("1")));
	}

	@Test
	public void testEQMatches() {
		Requirement r = new Requirement("test", ReqType.EQ, "5.9", RepositoryType.GIT, "");
		Assert.assertFalse(r.matches(new Version("5.8.0")));
		Assert.assertFalse(r.matches(new Version("5.10.0")));
		Assert.assertTrue(r.matches(new Version("5.9")));
		Assert.assertFalse(r.matches(new Version("5.9.0")));
		Assert.assertFalse(r.matches(new Version("5.9.1")));
		try {
			r = new Requirement("test", ReqType.EQ, "5.*", RepositoryType.GIT, "");
			Assert.fail("Should have thrown an exception");
		} catch (SuperNannyError e) {

		}
	}

	@Test
	public void testGEMatches() {
		Requirement r = new Requirement("test", ReqType.GE, "5.9", RepositoryType.GIT, "");
		Assert.assertFalse(r.matches(new Version("5.8.0")));
		Assert.assertTrue(r.matches(new Version("5.10.0")));
		Assert.assertTrue(r.matches(new Version("5.9")));
		Assert.assertTrue(r.matches(new Version("5.9.0")));
		Assert.assertFalse(r.matches(new Version("4.10.1")));
		r = new Requirement("test", ReqType.GE, "5.*", RepositoryType.GIT, "");
		Assert.assertTrue(r.matches(new Version("5.9.1")));
		Assert.assertTrue(r.matches(new Version("5.10.1")));
		Assert.assertTrue(r.matches(new Version("5")));
		Assert.assertFalse(r.matches(new Version("4.6.5")));
		Assert.assertTrue(r.matches(new Version("50.4.5")));
	}

}
