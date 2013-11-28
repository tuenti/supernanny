package com.tuenti.supernanny.repo;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.dependencies.DependencyParser;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.Requirement;

public class RepoProvider {
	@Inject
	Injector injector;
	@Inject
	CliParser p;
	private Map<String, Repository> repositories = new HashMap<String, Repository>();
	private DependencyParser dependencyParser;

	/**
	 * Do the repo init in parallel using the given executor service for the
	 * given list of requirements
	 * 
	 * @param reqs
	 *            List of requirements for which to init repos
	 * @param service
	 *            Executor service
	 * @throws Exception 
	 */
	public void warmUp(List<Requirement> reqs, ExecutorService service) throws Exception {
		for (Requirement req : reqs) {
			if (!hasRepoForTypeAndUri(req.getRepoType(), req.getRepo())) {
				makeRepo(req.getRepoType(), req.getRepo(), false);
			}
		}
		List<Future<Void>> futures = new LinkedList<Future<Void>>();
		for (Entry<String, Repository> entry : repositories.entrySet()) {
			final Repository repo = entry.getValue();
			futures.add(service.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					repo.init();
					return null;
				}
			}));
		}
		for (Future<Void> future : futures) {
			future.get();
		}
	}

	private Repository makeRepo(RepositoryType type, String uri) {
		return makeRepo(type, uri, true);
	}

	private Repository makeRepo(RepositoryType type, String uri, boolean initRepo) {
		int timeout = Integer.parseInt(p.verifyTimeout);
		boolean verifyArtifact = !p.skipVerifyArtifacts;
		
		Repository repo;
		uri = normalizeUri(uri);
		switch (type) {
		case TARBZ2:
		case TARGZ:
		case TARXZ:
		case ARCHIVE:
			// all different types refer to the same archive repo
			repo = new ArchiveRepo(uri, verifyArtifact, timeout);
			break;
		case GIT:
			repo = new GitRepo(uri, dependencyParser);
			break;
		case MERCURIAL:
			repo = new MercurialRepo(uri, dependencyParser);
			break;
		case SYMLINK:
			repo = new SymlinkRepo(dependencyParser);
			break;
		default:
			throw new RuntimeException("Unsupported repo type " + type);
		}
		injector.injectMembers(repo);
		this.addRepo(type, uri, repo);
		if (initRepo) {
			repo.init();
		}
		return repo;
	}

	/**
	 * Strig tags at the end of uris for associating uris with repos
	 * 
	 * @param uri
	 * @return
	 */
	private String normalizeUri(String uri) {
		// strip # tags at the end
		uri = uri.replaceAll("#.*", "");
		if (!uri.endsWith("/")) {
			uri += "/";
		}
		return uri;
	}

	public void addRepo(RepositoryType type, String uri, Repository repo) {
		uri = normalizeUri(uri);
		repositories.put(getKeyFor(type, uri), repo);
	}

	/**
	 * Repos are identified by the combination of type and uri (stripped of the
	 * hashtag)
	 * 
	 * @param type
	 * @param uri
	 * @return
	 */
	private String getKeyFor(RepositoryType type, String uri) {
		String t;
		switch (type) {
		case TARBZ2:
		case TARGZ:
		case TARXZ:
		case ARCHIVE:
			t = RepositoryType.ARCHIVE.toString();
			break;
		default:
			t = type.toString();
		}
		// strip path for keys
		String key = t + "_" + uri;
		return key;
	}

	private Repository getRepoForTypeAndUri(RepositoryType type, String uri) {
		uri = normalizeUri(uri);

		Repository repo = repositories.get(getKeyFor(type, uri));

		switch (type) {
		case TARBZ2:		
		case TARXZ:
		case TARGZ:
			// wrap the archive with to override the default type
			DefaultFormatArchiveWrapper wrapper = new DefaultFormatArchiveWrapper((ArchiveRepo)repo, type);
			injector.injectMembers(wrapper);
			return wrapper;
		default:
			return repo;
		}
	}

	private boolean hasRepoForTypeAndUri(RepositoryType type, String uri) {
		uri = normalizeUri(uri);
		return repositories.get(getKeyFor(type, uri)) != null;
	}

	public Repository getRepo(RepositoryType type, String uri) {
		if (!hasRepoForTypeAndUri(type, uri)) {
			makeRepo(type, uri);
		}
		return getRepoForTypeAndUri(type, uri);
	}

	public DependencyParser getDependencyParser() {
		return dependencyParser;
	}

	public void setDependencyParser(DependencyParser dependencyParser) {
		this.dependencyParser = dependencyParser;
	}
}
