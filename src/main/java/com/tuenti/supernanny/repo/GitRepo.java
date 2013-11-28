package com.tuenti.supernanny.repo;

import com.google.inject.Inject;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.strategy.DvcsStrategy;
import com.tuenti.supernanny.strategy.GitStrategy;

public class GitRepo extends DVCSRepo implements Repository {
	@Override
	public RepositoryType getRepoType() {
		return RepositoryType.GIT;
	}

	@Inject
	private GitStrategy strategy;

	public GitRepo(String uri, DependencyParser dparser) {
		super(uri, dparser);
	}

	protected DvcsStrategy getStrategy() {
		return strategy;
	}
}
