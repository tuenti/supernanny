package com.tuenti.supernanny.repo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.util.Version;

public interface Repository {
	void init();

	/**
	 * Get the temp dir the repo is using (if any)
	 * 
	 * @return
	 */
	String getTmpDir();

	/**
	 * Get the repo's URI
	 * 
	 * @return
	 */
	String getUri();

	/**
	 * Get the repo type
	 * 
	 * @return
	 */
	RepositoryType getRepoType();

	/**
	 * Check if the artifact located in 'dep' is up to date
	 * 
	 * @param artifact
	 *            Expected artifact
	 * @param dep
	 *            Local checkout
	 * @return
	 * @throws IOException
	 */
	boolean isUpdated(Artifact artifact, File dep) throws IOException;

	/**
	 * Get a list of possible artifacts for this requirement.
	 * Usually just return all artifacts for this requirement's name and let the resolver chose the correct one.
	 * Some repositories that can't parse all possible version's requirement may return the best possible match.
	 * 
	 * @param req
	 * @return Artifact if found
	 * @throws ResolutionException
	 */
	List<Artifact> getPossibleArtifactsFor(Requirement req) throws ResolutionException;

	/**
	 * Fetch a given artifact and store it to destination. This should stamp the current version if applicable
	 * @see Util.stampProject
	 * 
	 * @param artifact
	 * @param destination
	 * @return
	 * @throws IOException
	 */
	String fetch(Artifact artifact, File destination) throws IOException;

	/**
	 * Get the latest version for the given artifact name
	 * 
	 * @param name
	 * @return
	 * @throws SuperNannyError
	 * @throws IOException
	 */
	Version getLatestVersion(String name) throws SuperNannyError, IOException;

	/**
	 * Publish the current library to with the specified version.
	 * 
	 * @param export
	 * @param version
	 * @param prefix
	 *            Prepend this to the artifact's name
	 * @param suffix
	 *            Append this to the artifact's name
	 * @throws IOException
	 */
	void publish(Export export, String version, String prefix, String suffix) throws IOException;
}
