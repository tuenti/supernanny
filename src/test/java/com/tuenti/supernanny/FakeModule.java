/**
 * Dependency injection.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny;

import java.util.concurrent.ExecutorService;

import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.tuenti.supernanny.strategy.GitStrategy;

/**
 * Guice module for SuperNanny tests.
 * 
 * Defines all bindings for the dependency graph.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class FakeModule extends AbstractModule {
	Util util = Mockito.mock(Util.class);
	ExecutorService executor = Mockito.mock(ExecutorService.class);
	GitStrategy strategy = Mockito.mock(GitStrategy.class);

	@Override
	protected void configure() {
		bind(Util.class).toInstance(util);
		bind(GitStrategy.class).toInstance(strategy);
	}
	
	@Provides
	ExecutorService provideExecutor() {
		return executor;
	}
}