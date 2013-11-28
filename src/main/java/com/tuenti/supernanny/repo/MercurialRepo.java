package com.tuenti.supernanny.repo;

import com.google.inject.Inject;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.strategy.DvcsStrategy;
import com.tuenti.supernanny.strategy.HgStrategy;

public class MercurialRepo extends DVCSRepo implements Repository {
	@Inject
	private HgStrategy strategy;
	public MercurialRepo(String uri, DependencyParser dparser) {
		super(uri, dparser);
	}

	@Override
	public RepositoryType getRepoType() {
		return RepositoryType.MERCURIAL;
	}

	@Override
	protected DvcsStrategy getStrategy() {
		return strategy;
	}
}
