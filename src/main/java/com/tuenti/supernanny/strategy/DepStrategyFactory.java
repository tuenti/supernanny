package com.tuenti.supernanny.strategy;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.tuenti.supernanny.dependencies.Dependency.DepType;
import com.tuenti.supernanny.strategy.common.DepStrategy;

@Singleton
public class DepStrategyFactory {
	@Inject
	Injector injector;

	/**
	 * Gets the strategy for the given type.
	 * 
	 * @return instance of DepStragety that handles fetching of deps.
	 * @throws DepStrategyException
	 *             if the type is not supported (implementation error).
	 */
	public DepStrategy getStrategy(DepType type) throws DepStrategyException {
		switch (type) {
		case GIT:
			return injector.getInstance(GitStrategy.class);
		case MERCURIAL:
			return injector.getInstance(HgStrategy.class);
		case TARGZ:
			return injector.getInstance(TarGzStrategy.class);
		case TARBZ2:
			return injector.getInstance(TarBz2Strategy.class);
		default:
			throw new DepStrategyException(
					"No stragety registered for this type.");
		}
	}
}