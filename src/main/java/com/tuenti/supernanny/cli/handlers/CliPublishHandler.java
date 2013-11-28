/**
 * Command line handling.  
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 * @author Jesus Bravo Alvarez <suso@tuenti.com>
 */
package com.tuenti.supernanny.cli.handlers;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.tuenti.supernanny.SuperNannyError;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.repo.artifacts.Export;
import com.tuenti.supernanny.resolution.DepPublisher;
import com.tuenti.supernanny.util.Version;
import com.tuenti.supernanny.util.Versions;

/**
 * Handler for 'publish' command.
 * 
 * Publishes artifacts.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class CliPublishHandler implements CliHandler {
	@Inject
	Logger l;
	@Inject
	Util util;
	@Inject
	CliParser p;
	@Inject
	CliExportsHandler cliExportsHandler;

	@Override
	public String handle() {
		try {
			cliExportsHandler.handle();
			System.out.println();

			File exportFile = new File(Util.EXPORT_FILE);

			if (!exportFile.exists()) {
				l.info(("Project export definitions not found in: " + new File(".")
						.getAbsoluteFile().getName()));
				return null;
			}

			// export deps
			Collection<Export> exports = util.parseExportsFile(exportFile);

			// if version is not implied by next version format, request
			// input, otherwise calculate it
			Version version = null;
			if (p.next == null || p.next.equals("")) {
				version = new Version(
						util.readInput("No next version specified!\nPlease input the version for this export: "));
			} else {
				Map<Version, List<Export>> last_versions = new HashMap<Version, List<Export>>();
				// determine the last version of all deps
				for (Export d : exports) {
					Version last_version = d.getRepository().getLatestVersion(d.getName());
					if (!last_versions.containsKey(last_version)) {
						last_versions.put(last_version, new ArrayList<Export>());
					}
					last_versions.get(last_version).add(d);
				}

				// different versions are present
				if (last_versions.size() > 1) {
					System.out.println("The repos have different latest versions:");
					for (Entry<Version, List<Export>> entry : last_versions.entrySet()) {
						System.out.println(entry.getKey() + ": ");
						for (Export dependency : entry.getValue()) {
							System.out.println("    " + dependency.getRepository());
						}
					}
					Version[] versions = last_versions.keySet().toArray(new Version[] {});
					version = Versions.getLatestVersion(versions);
				} else {
					// take the first version
					version = last_versions.keySet().iterator().next();
				}
				version = Versions.getNextVersion(p.next, version);
			}

			if (p.pretend) {
				for (Export d : exports) {
					System.out.println(MessageFormat.format("Would publish version {0} to {1}",
							version, d.getRepository()));
				}
			} else {
				if (!util.confirmYN("Version " + version
						+ " will be published - do you want to proceed?")) {
					throw new SuperNannyError("User aborted");
				}
				new DepPublisher().resolve(exports, version.getVersionString(), p);
			}
		} catch (IOException e) {
			l.warning(e.getMessage());
			throw new SuperNannyError(e);
		}
		return null;
	}
}
