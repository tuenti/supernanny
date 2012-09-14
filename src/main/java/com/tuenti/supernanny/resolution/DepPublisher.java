/**
 * Dependency publishing for SuperNanny.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.resolution;

import java.io.IOException;
import java.util.Collection;

import com.tuenti.supernanny.dependencies.Dependency;

/**
 * Dependency publisher for SuperNanny.
 * 
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class DepPublisher {
	/**
	 * Publish dependencies.
	 * 
	 * @param dependencies
	 *            collection of dependencies to publish.
	 * @throws IOException
	 *             if reading export files fails.
	 */
	public void resolve(Collection<Dependency> dependencies) throws IOException {
		for (Dependency dependency : dependencies) {
			dependency.publish();
		}
	}
}