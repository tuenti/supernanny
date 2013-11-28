package com.tuenti.supernanny.resolution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.RepoProvider;
import com.tuenti.supernanny.repo.Repository;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.util.Versions;

public class EagerResolver implements Resolver {
	@Inject
	private RepoProvider repoProvider;
	@Inject
	private Util util;

	public EagerResolver() {
	}

	/**
	 * Resolve the list of artifacts to get for the list of initial
	 * requirements.
	 * 
	 * This is a eager resolution algorithm, that always chooses the most recent
	 * artifact that meets the given requirement.
	 * 
	 * 1. the artifacts for all initial requirements are chosen
	 * 
	 * 2. for all of the artifact's requirements, if they don't match the
	 * already chosen artifacts, an error is thrown. For requirements that don't
	 * have an artifact, the most recent artifact that matches is chosen.
	 */
	@Override
	public Set<Artifact> resolve(List<Requirement> initialRequirements) throws ResolutionException {
		Set<Artifact> chosenArtifacts = new HashSet<Artifact>();
		List<Artifact> unresolved = new ArrayList<Artifact>();
		// process the initial list of requirements first getting the most
		// recent artifact for all the requirements
		for (Requirement req : initialRequirements) {
			Artifact artifact = getArtifactFor(req);
			chosenArtifacts.add(artifact);
			unresolved.add(artifact);
		}

		// resolve all dependencies of the the artifacts chosen until now
		while (unresolved.size() > 0) {
			Artifact artifact = unresolved.remove(0);
			// resolve the requirements for each
			List<Artifact> newArtifacts = resolveReq(artifact, chosenArtifacts);
			unresolved.addAll(newArtifacts);
			chosenArtifacts.addAll(newArtifacts);
		}

		return chosenArtifacts;
	}

	/**
	 * Resolve the requirements of an artifact.
	 * Verify that chosen artifacts are valid and return a list of the new artifacts needed.
	 * 
	 * @param artifact Artifact to resolve
	 * @param chosenArtifacts The list of already chosen artifacts
	 * @return List of new artifacts needed
	 * @throws ResolutionException
	 */
	private List<Artifact> resolveReq(Artifact artifact, Set<Artifact> chosenArtifacts)
			throws ResolutionException {
		List<Artifact> newArtifacts = new ArrayList<Artifact>();
		for (Requirement requirement : artifact.getRequirements()) {
			boolean matched = false;
			for (Artifact a : chosenArtifacts) {
				if (requirement.getName().equals(a.getName())) {
					if (!requirement.matches(a.getName(), a.getVersion())) {
						throw new ResolutionException("Requirement conflict: "
								+ artifact.toShortString() + " depends on " + requirement + " but "
								+ a.toShortString() + " is already selected.");
					} else {
						// check if the major version is different to throw a
						// warning
						if (Versions.isDifferentMajor(a.getVersion(), requirement.getVersion())) {
							System.out.println("WARNING: Differing major versions for " + a.getName() + ": ["									
									+ artifact.toShortString() + "] requires " + requirement
									+ " and " + a.getVersion() + " is chosen.");
						}
						matched = true;
					}
				}
			}
			if (!matched) {
				Artifact newArtifact = getArtifactFor(requirement);
				newArtifacts.add(newArtifact);
			}
		}
		return newArtifacts;
	}

	/**
	 * Get the artifact for the given requirement
	 * 
	 * Overrides artifacts where directory is a symlink to use the symlink repo
	 * Gets the artifact from the corresponding repo and returns it.
	 * 
	 * @param req
	 * @return
	 * @throws ResolutionException
	 */
	private Artifact getArtifactFor(Requirement req) throws ResolutionException {
		// ingore dependencies that are a symlinks in libs folder
		try {
			File destination = new File(util.getDepsFolder(), req.getName());
			if (util.isSymlink(destination)) {
				req.setRepoType(RepositoryType.SYMLINK);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Repository repo = repoProvider.getRepo(req.getRepoType(), req.getRepo());

		List<Artifact> possibleArtifactsFor = repo.getPossibleArtifactsFor(req);
		if (possibleArtifactsFor != null) {
			Collections.sort(possibleArtifactsFor,  Artifact.getArtifactComparator(true));
			for (Artifact a: possibleArtifactsFor) {
				if (req.matches(a.getName(), a.getVersion())) {
					return a;
				}
			}
		}
		
		throw new ResolutionException("No artifact found for " + req);
	}
}
