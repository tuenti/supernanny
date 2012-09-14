/**
 * Ant task for SuperNanny dependency fetching.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.ant;

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.di.SuperNannyModule;
import com.tuenti.supernanny.resolution.DepFetcher;

/**
 * Ant task for SuperNanny.
 * 
 * Ant task takes a project root folder as input. The resolver will try and
 * fetch all dependencies for the desired project.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNannyResolve extends Task {
	private String root = ".";
	private boolean skip = false;
	private String depFile = "";

	public void setRoot(String root) {
		this.root = root;
	}
	
	public void setSkip(boolean skip) {
		this.skip = skip;
	}
	
	public void setDepFile(String depFile) {
		this.depFile  = depFile;
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
		Injector injector = Guice.createInjector(new SuperNannyModule());
		Util util = injector.getInstance(Util.class);
		DepFetcher fetcher = injector.getInstance(DepFetcher.class);
		if (this.skip) {
			return;
		}
		try {
			CliParser p = new CliParser();
			p.depfile = this.depFile;
			util.setRoot(new File(root));
			fetcher.resolve(new File(root), p);
		} catch (IOException e) {
			log("Errors resolving dependencies for project in " + root);
			throw new BuildException(e);
		}
	}
}
