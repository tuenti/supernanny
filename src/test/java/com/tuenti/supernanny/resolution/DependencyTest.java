/**
 * Unit tests for SuperNanny.
 *
 * @package Tests
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tuenti.supernanny.FakeModule;
import com.tuenti.supernanny.SuperNannyUtil;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.dependencies.Dependency.DepType;
import com.tuenti.supernanny.dependencies.NoOpDependency;
import com.tuenti.supernanny.strategy.GitStrategy;
import com.tuenti.supernanny.strategy.common.DepStrategy;

/**
 * Tests for dependency.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class DependencyTest {
	@Test
	public void fetch() throws IOException, NoOpDependency {
		Injector injector = Guice.createInjector(new FakeModule());
		Util util = injector.getInstance(Util.class);

		Dependency dep = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl", "depVer");

		DepStrategy depStrategy = injector.getInstance(GitStrategy.class);
		
		when(depStrategy.fetch(any(File.class), eq("depUrl"), eq("depVer"))).thenReturn("depVer");
		
		dep.fetch();
		
		verify(util).stampProject(any(File.class), eq("depUrl"), eq("depVer"),
				eq(DepType.GIT));
		verify(util).getDepsFolder();
		verify(util).deleteDir(any(File.class));
		verify(depStrategy).init(any(File.class), eq("depUrl"));
	}

	@Test
	public void publish() throws IOException, NoOpDependency {
		Injector injector = Guice.createInjector(new FakeModule());
		Util util = injector.getInstance(Util.class);

		// set up the init of dependency
		when(util.getDepsFolder()).thenReturn(".deps");

		Dependency dep = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl", "depVer");

		DepStrategy depStrategy = injector.getInstance(GitStrategy.class);

		dep.publish();

		verify(util).getDepsFolder();
		verify(depStrategy).publish(eq("depName"), any(File.class),
				eq("depUrl"), eq("depVer"));

		Mockito.verifyNoMoreInteractions(util, depStrategy);
	}

	@Test
	public void compare() throws IOException, NoOpDependency {
		Injector injector = Guice.createInjector(new FakeModule());

		Dependency d1 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl", "depVer");

		Dependency d2 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl", "depVer");

		Dependency d3 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl2", "depVer");

		Dependency d4 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl", "depVer2");

		Dependency d5 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl2", "depVer3");

		Dependency d6 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", null, "depVer3");

		Dependency d7 = injector.getInstance(Dependency.class).init(
				DepType.GIT, "depName", "depUrl2", null);

		assertEquals(d1, d1);
		assertEquals(d1, d2);
		assertFalse(d2.equals(d3));
		assertFalse(d3.equals(d2));
		assertFalse(d3.equals(d4));
		assertFalse(d4.equals(d5));

		assertFalse(d5.equals(d6));
		assertFalse(d5.equals(d7));
		assertFalse(d6.equals(d5));
		assertFalse(d7.equals(d5));
		assertFalse(d5.equals(null));

		assertFalse(d4.equals(new String()));

		d6.setVersion("3");
		assertEquals("3", d6.getVersion());
		assertEquals(d1.toString(), d2.toString());
		assertFalse(d1.toString().equals((d3.toString())));
		assertEquals(d1.toPublishString(), d2.toPublishString());
		assertEquals(d2.toPublishString(), d4.toPublishString()); // version

		// contains
		List<Dependency> deps = new LinkedList<Dependency>();
		deps.add(d1);
		deps.add(d2);

		assertTrue(deps.contains(d1));
		assertTrue(deps.contains(d2));
		assertFalse(deps.contains(d4));
		assertFalse(d2.hashCode() == d4.hashCode()); // version
	}

	@Test
	public void nextVersionTest() {
		Util u = new SuperNannyUtil();

		assertEquals("10.2.0", u.getNextVersion("x.+", "10.1.7"));
		assertEquals("1.4.5-3", u.getNextVersion("x.x.x-+", "1.4.5-2"));
		assertEquals("befw-2.0.0", u.getNextVersion("x-+", "befw-1.3.1"));
		assertEquals("befw-1.5.0", u.getNextVersion("x-x.+", "befw-1.4.1"));
		assertEquals("befw-1.3.3", u.getNextVersion("x-x.x.+", "befw-1.3.2"));
		assertEquals("13.0.0", u.getNextVersion("+", "12.7.1985"));
		assertEquals("1.10.0", u.getNextVersion("x.+", "1.9.0"));
		assertEquals("4.0.0", u.getNextVersion("+", "3.19.0"));
	}
}
