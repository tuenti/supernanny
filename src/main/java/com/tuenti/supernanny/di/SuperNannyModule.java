/**
 * Dependency injection.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.di;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.tuenti.supernanny.SuperNannyUtil;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.repo.RepoProvider;
import com.tuenti.supernanny.resolution.EagerResolver;
import com.tuenti.supernanny.resolution.Resolver;

/**
 * Guice module for SuperNanny.
 * 
 * Defines all bindings for the dependency graph.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNannyModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(Util.class).to(SuperNannyUtil.class).in(Scopes.SINGLETON);
		bind(CliParser.class).asEagerSingleton();
		bind(Integer.class).annotatedWith(Names.named("poolSize")).toInstance(
				Util.EXECUTOR_POOL_SIZE);
		bind(RepoProvider.class).in(Scopes.SINGLETON);
		bind(Resolver.class).to(EagerResolver.class);
	}

	@Provides
	ExecutorService provideExecutor(@Named("poolSize") Integer poolSize) {
		return Executors.newFixedThreadPool(poolSize);
	}
}