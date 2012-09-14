/**
 * Ant task for SuperNanny dependency publishing.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.ant;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.cli.handlers.CliPublishHandler;
import com.tuenti.supernanny.dependencies.Dependency;
import com.tuenti.supernanny.di.SuperNannyModule;
import com.tuenti.supernanny.resolution.DepPublisher;

/**
 * Ant task for SuperNanny.
 * 
 * The publisher will try and publish all artifacts defined as exports for the
 * desired project.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNannyPublish extends Task {
	@Inject
	private Util util;

	private String versions = null;

	private String next = null;

	public void setNext(String next) {
		this.next = next;
	}

	public void setVersions(String versions) {
		this.versions = versions;
	}

	@Override
	public void init() throws BuildException {
		super.init();

		/*
		 * Configure the logger.
		 */
		Logger.getLogger("").setLevel(Level.OFF);

		Guice.createInjector(new SuperNannyModule()).injectMembers(this);
	}

	@Override
	public void execute() throws BuildException {
		Injector injector = Guice.createInjector(Modules.override(new SuperNannyModule()).with(
				new AbstractModule() {

					@Override
					protected void configure() {
						CliParser p = new CliParser();
						p.next = next;
						bind(CliParser.class).toInstance(p);

					}
				}));
		if (versions == null) {
			injector.getInstance(CliPublishHandler.class).handle();
			return;
		}

		Collection<Dependency> deps = util.parseExportsFile(new File(
				Util.EXPORT_FILE));

		String[] splitVersions = versions.split("\\s");
		if (splitVersions.length != deps.size()) {
			throw new BuildException(
					MessageFormat
							.format("Number of versions defined is wrong. Expected {0}, but got {1}",
									deps.size(), splitVersions.length));
		}

		int i = 0;
		for (Dependency d : deps) {
			d.setVersion(splitVersions[i++]);
		}

		try {
			injector.getInstance(DepPublisher.class).resolve(deps);
		} catch (IOException e) {
			log("Errors exporting targets of the project.");
			throw new BuildException(e);
		}
	}
}