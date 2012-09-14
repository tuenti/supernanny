/**
 * Ant task for SuperNanny dependency fetching.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.ant;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tuenti.supernanny.SuperNannyUtil;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.ProjectStatus;
import com.tuenti.supernanny.cli.handlers.CliParser;

/**
 * Ant task for SuperNanny.
 * 
 * Ant task takes a project root folder as input. The resolver will try and
 * fetch all dependencies for the desired project.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNannyStatus extends Task {
	private String root = ".";
	private String depFile = "";

	public void setRoot(String root) {
		this.root = root;
	}

	public void setDepFile(String depFile) {
		this.depFile = depFile;
	}

	@Override
	public void init() throws BuildException {
		super.init();

		/*
		 * Configure the logger.
		 */
		Logger.getLogger("").setLevel(Level.OFF);
	}

	@Override
	public void execute() throws BuildException {
		Injector injector = Guice.createInjector(new AbstractModule() {

			@Override
			protected void configure() {
				CliParser p = new CliParser();
				p.depfile = depFile;
				Util util = new SuperNannyUtil();
				util.setRoot(new File(root));

				bind(CliParser.class).toInstance(p);
				bind(Util.class).toInstance(util);
			}
		});
		
		System.out.println(injector.getInstance(ProjectStatus.class));
	}
}
