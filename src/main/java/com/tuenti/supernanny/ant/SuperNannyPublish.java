/**
 * Ant task for SuperNanny dependency publishing.
 *
 * @package Build
 * @subpackage Dependencies
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
package com.tuenti.supernanny.ant;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.tuenti.supernanny.cli.handlers.CliParser;
import com.tuenti.supernanny.cli.handlers.CliPublishHandler;
import com.tuenti.supernanny.di.SuperNannyModule;

/**
 * Ant task for SuperNanny.
 * 
 * The publisher will try and publish all artifacts defined as exports for the
 * desired project.
 * 
 * @author Goran Petrovic <gpetrovic@tuenti.com>
 */
public class SuperNannyPublish extends Task {
	private String next = null;
	// enable force by default for ant tasks
	private boolean force = true;

	public void setNext(String next) {
		this.next = next;
	}
	
	public void setForce(boolean force){
		this.force = force;
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
						p.force = force;
					}
				}));

		injector.getInstance(CliPublishHandler.class).handle();
	}
}
