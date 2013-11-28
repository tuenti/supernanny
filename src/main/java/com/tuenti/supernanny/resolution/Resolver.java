package com.tuenti.supernanny.resolution;

import java.util.List;
import java.util.Set;

import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;

public interface Resolver {

	/**
	 * Resolve the set of artifacts to get for the list of initial
	 * requirements.
	 * 
	 * @param initialRequirements
	 * @return Set of artifacts that meet all the given requirements
	 * @throws ResolutionException In case the requirements can't be met
	 */
	public abstract Set<Artifact> resolve(List<Requirement> initialRequirements)
			throws ResolutionException;

}