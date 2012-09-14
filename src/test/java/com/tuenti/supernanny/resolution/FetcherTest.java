/**
 * Unit tests for SuperNanny.
 *
 * @package Tests
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tuenti.supernanny.FakeModule;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.Dependency;

/**
 * Tests for fetcher.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class FetcherTest {
	@Test
	public void fetchNoDeps() throws IOException {
		Injector injector = Guice.createInjector(new FakeModule());
		Util util = injector.getInstance(Util.class);

		DepFetcher fetcher = injector.getInstance(DepFetcher.class);

		// mocked dependencies
		LinkedList<Dependency> deps = new LinkedList<Dependency>();

		// mocked dependencies
		File f = mock(File.class);
		File depFile = mock(File.class);

		when(util.getFile(eq(f), eq(Util.DEP_FILE))).thenReturn(depFile);
		when(depFile.exists()).thenReturn(Boolean.FALSE);

		when(util.parseDepsFile(eq(depFile))).thenReturn(deps);
		when(util.getFile(any(File.class), anyString())).thenReturn(depFile);
		when(depFile.listFiles()).thenReturn(new File[] {});

		fetcher.resolve(f, null);
	}

	public void fetch() throws IOException {
		Injector injector = Guice.createInjector(new FakeModule());
		Util util = injector.getInstance(Util.class);

		DepFetcher fetcher = injector.getInstance(DepFetcher.class);

		// mocked dependencies
		LinkedList<Dependency> deps = new LinkedList<Dependency>();
		Dependency d1 = mock(Dependency.class);
		Dependency d2 = mock(Dependency.class);
		File f = mock(File.class);
		File depFile = mock(File.class);

		deps.add(d1);
		deps.add(d2);

		when(d1.getName()).thenReturn("d1");
		when(d2.getName()).thenReturn("d2");

		when(util.getFile(eq(f), eq(Util.DEP_FILE))).thenReturn(depFile);
		when(depFile.exists()).thenReturn(Boolean.TRUE);

		when(util.parseDepsFile(eq(depFile))).thenReturn(deps);
		when(util.getFile(any(File.class), anyString())).thenReturn(depFile);
		when(depFile.listFiles()).thenReturn(new File[] {});

		CliParser fakeParser = new CliParser();
		fakeParser.setPretend(true);
		util.setRoot(new File("."));
		fetcher.resolve(f, fakeParser);

		// verify the fetch will occur during publish
		verify(d1, atLeastOnce()).getName();
		verify(d2, atLeastOnce()).getName();
		verify(d1).getVersion();
		verify(d2).getVersion();

		Mockito.verifyNoMoreInteractions(d1, d2);
	}
}