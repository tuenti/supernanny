/**
 * Unit tests for SuperNanny.
 *
 * @package Tests
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.di.SuperNannyModule;
import com.tuenti.supernanny.resolution.DepPublisher;

/**
 * Tests for publisher.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class PublisherTest {
	@Test
	public void publish() throws IOException {
		Injector injector = Guice.createInjector(new SuperNannyModule());
		DepPublisher publisher = injector.getInstance(DepPublisher.class);

		// mocked dependencies
		List<Dependency> deps = new LinkedList<Dependency>();
		Dependency d1 = mock(Dependency.class);
		Dependency d2 = mock(Dependency.class);

		deps.add(d1);
		deps.add(d2);

		publisher.resolve(deps);

		verify(d1, times(1)).publish();
		verify(d2, times(1)).publish();
	}
}