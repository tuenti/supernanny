/**
 * Dependency definition for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.dependencies;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.strategy.DepStrategyException;
import com.tuenti.supernanny.strategy.DepStrategyFactory;
import com.tuenti.supernanny.strategy.common.DepStrategy;

/**
 * Dependency definition for SuperNanny.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class Dependency {
	@Inject
	Logger l;
	@Inject
	Util util;
	@Inject
	DepStrategyFactory depStrategyFactory;

	/**
	 * Dependency type definition.
	 * 
	 * Each dependency type is tied to it's strategy.
	 * 
	 * @author Goran Petrovic <gpetrovic@tuenti.com>
	 */
	public enum DepType {
		GIT(0), MERCURIAL(1), TARGZ(2), TARBZ2(3), NOOP(4);

		private final int type;

		DepType(int type) {
			this.type = type;
		}

		public int getType() {
			return type;
		}
	}

	// definition of a dependency
	protected String name;
	protected DepType type;
	protected String uri;
	protected String version;
	protected File depFolder = null;
	protected DepStrategy strategy = null;
	protected File depDefinitionFile = null;

	/**
	 * Create a dependency.
	 * 
	 * The dependency knows how to fetch itself with the given information.
	 * 
	 * @param type
	 *            A type of the dependency (git, hg, tar, ...).
	 * @param name
	 *            A symbolic name of the dependency (e.g. logger, storage, ...).
	 * @param uri
	 *            An URI of the dependency, be it a git repo, hg repo over ssh,
	 *            a file or an URL.
	 * @param version
	 *            version of the dependency to fetch (e.g. git/hg branch,
	 *            tag/bookmark, ...).
	 * @param depDefinitionFile
	 *            where the depenency was defined.
	 * @throws NoOpDependency
	 *             if no-operation is specified.
	 */
	public Dependency init(DepType type, String name, String uri,
			String version, File depDefinitionFile) throws NoOpDependency {
		if (type == DepType.NOOP) {
			throw new NoOpDependency();
		}
		this.type = type;
		this.depDefinitionFile = depDefinitionFile;
		this.name = name;
		this.uri = uri;
		this.version = version;
		depFolder = new File(util.getDepsFolder(), this.name);

		try {
			this.strategy = depStrategyFactory.getStrategy(type);
		} catch (DepStrategyException e) {
			System.err
					.println("SuperNanny has died with the following error: \n"
							+ e.getMessage());
			System.exit(1);
		}

		return this;
	}

	/**
	 * Create a dependency.
	 * 
	 * @see #init(DepType, String, String, String, File)
	 */
	public Dependency init(DepType type, String name, String uri, String version)
			throws NoOpDependency {
		return init(type, name, uri, version, new File(Util.DEP_FILE));
	}

	/**
	 * Get the latest published version.
	 * 
	 * @return string with the latest currently published version.
	 */
	public String getLatestVersion() {
		return this.strategy.getLatestVersion(this.name, this.uri);
	}

	/**
	 * Gets whether the given dependency folder is using the same strategy as
	 * the new version of it, or if the requested version has changed.
	 * 
	 * E.g., if a GIT dependency changes to MERCURIAL dependency, the folder
	 * will already exist, but it still needs to be cleaned-up.
	 * 
	 * @param type
	 * 
	 * @return instance of DepStrategy that handles fetching of deps.
	 * @throws DepStrategyException
	 *             if the type is not supported (implementation error).
	 * @throws IOException
	 */
	boolean isDirty(DepType type, File depRoot) {

		try {
			return util.getProjectDepType(depRoot) != this.type
					|| !util.getProjectVersion(depRoot).equals(this.version);
		} catch (IOException e) {
			l.warning(MessageFormat
					.format("SuperNanny dependency status file not found in {0}; refetching {1}.",
							depRoot.getAbsolutePath(), depRoot.getName()));
		} catch (NoOpDependency e) {
			return false; // NOOP is never dirty
		}

		return false;
	}

	/**
	 * Fetch the dependency.
	 * 
	 * Fetching is done in 3 stages: i) creating a dep folder, if neccesary ii)
	 * fetching the data (repo/file) iii) getting the good version of data in
	 * the dep folder (checkout or unzip)
	 * 
	 * @throws IOException
	 */
	public File fetch() throws IOException {
		// check if the dependency exists; init the dep if not
		// if dep type has changed, also cleanup the dep folder
		if (!depFolder.exists() || isDirty(type, depFolder)) {
			// clean-up and init
			util.deleteDir(depFolder);
			strategy.init(depFolder, uri);

			// fetch the dependency
			version = strategy.fetch(depFolder, uri, version);

			// stamp the project with metadata
			util.stampProject(depFolder, uri, version, type);

			System.out.println(MessageFormat.format(
					"\t# fetched {0}@{1} ({2})", depFolder, version,
					type.toString()));
		} else {
			System.out.println(MessageFormat.format(
					"\t#{0} already up to date (@{1})", depFolder, version));
		}

		return depFolder;
	}

	/**
	 * Publish the dependency. Depending on the type, do proper publishing
	 * actions.
	 * 
	 * @throws IOException
	 */
	public void publish() throws IOException {
		// publish the dependency
		System.out.println(this.toPublishString());

		strategy.publish(name, depFolder, uri, version);

		System.out.println(MessageFormat.format("\t# published {0}@{1}", name,
				version));
	}

	@Override
	public String toString() {
		if (depDefinitionFile != null) {
			return MessageFormat.format(
					"# {0} @ {1} from {2} ({3}) (from {4})", name, version,
					uri, type.toString(), depDefinitionFile);
		} else {
			return MessageFormat.format(
					"# {0} @ {1} from {2} ({3}) (removed)", name, version,
					uri, type.toString());
		}
	}

	public String toPublishString() {
		return MessageFormat.format("# {0} to {1} ({2})", name, uri,
				type.toString());
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public DepType getType() {
		return type;
	}

	public String getUri() {
		return uri;
	}

	public void setDepDefinitionFile(File depDefinitionFile) {
		this.depDefinitionFile = depDefinitionFile;
	}

	public File getDepDefinitionFile() {
		return depDefinitionFile;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((depFolder == null) ? 0 : depFolder.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((strategy == null) ? 0 : strategy.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dependency other = (Dependency) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setDepFolder(File depFolder) {
		this.depFolder = depFolder;
	}

	public String matchVersion() {
		return this.strategy.matchVersion(this.depFolder, this.uri,
				this.version);
	}

	public void setDepType(DepType type) {
		this.type = type;
	}
}