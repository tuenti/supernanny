
Supernanny
==========

SuperNanny is a dependency management tool developed in-house, for managing dependencies between repositories and supports resolving and fetching defined dependencies and exporting them.

Since we mostly used php we implemented a system similar to ivy to manage our libraries.
Dependencies can be expresed as artifacts or as source control repos. Artifacts follow a versioning scheme.


Build and installation
----------------------

Build is done using maven:

    mvn assembly:assembly exec:exec install

That generates the binary files in the `target` directory, there you can find the jars you can directly use with `java -jar` or the debian file you can install in Debian or Ubuntu systems.

SuperNanny is a Java application, basically a .jar file, and can be used as such:

    Usage: com.tuenti.supernanny.cli.handlers.CliParser
      help [flag] Shows help
      exports [flag] List artifacts exported by the project.
      --pretend [flag] Don't actually execute the command, only show info.
      --next [String] Format of the next version of format a.a.a where a is one of x or +. X means keep the current max value, + means increase, e.g. if current latest version is 3.1.4, with next of x.x.+ one gets 3.1.5, with next of x.+ one gets 3.2.4 and with next of +, one gets 4.1.4. Useful for pushiung new minor/major/patch versions.
      --prefix [String] Prefix that will be used for all operations (e.g. --prefix beta).
      --suffix [String] Version suffix for published artifacts
      publish [flag] Publish a dependency
      delete [String] Delete a dependency
      clean [flag] Delete all dependencies
      fetch [flag] Fetch project dependencies
      status [flag] Shows dependency status
      -version (-v) [flag] Shows version
      -force (-f) [flag] Don't ask any questions - just do it
      --skipCleanup [flag] Don't delete any unreferenced libraries after fetching
      --depfile [String] Path to the dependency file (defaults to .DEP)
      --skipVerifyArtifacts [flag] Skip verification of published artifacts checking the index
      --verifyTimeout [String] Artifact verification timeout (30)

Additionally, supernanny is published as a Debian package, with a bootstrap and a man page, so it's easier to use.

To install:

    $ sudo apt-get install supernanny

After the install, try these:

    $ man supernanny
    $ supernanny help


Dependency definition
----------------------

Dependencies are defined in .DEP file in the root of the project. Each line defines a single dependency. Comment lines begin with #. Dependencies are of format:

    <name> <type> <uri> <version>

Type of the dependency currently must be one of the following: GIT, MERCURIAL and ARCHIVE (or to provide backwards compatibility TARGZ, TARBZ2, TARXZ).

    # my project dependencies
    tu-memcache GIT http://github.com/tuenti/memcached-tuenti-multiport.git stable-1.3
    pypy MERCURIAL https://bitbucket.org/pypy/pypy default
    libconfig ARCHIVE http://www.myartifacts.com/dev/ 1.3

This project defines 3 dependencies, each at their own versions, specifying a URI where to find them.

In detail:

    common ARCHIVE http://www.myartifacts.com/ 2.*

This pulls an artifact with major version 2 and any minor version. The artifacts is really a TAR file and can be in tar.gz, tar.bz2 or tar.xz formats, and will be
downloaded from the artifacts server. The artifact server needs to provide an index file with information on all the available artifacts.

    backend-framework ARCHIVE http://www.myartifacts.com/ 13.7

This pulls version 13.7 of the backend-framework artifact.

    config MERCURIAL http://pull.code.some.repo/config default

This pulls a dependency as a version control depot from mercurial (or git)


Fetching dependencies
---------------------
Once the project defines the .DEP file, fetching dependencies is trivia:

    $ supernanny fetch
    Init repos
    Resolve dependencies
    Fetch dependencies
      Ok  hstats-client           2.5      TARBZ2  http://www.myartifacts.com/
      Ok  tfw-lib                 3.1      TARBZ2  http://www.myartifacts.com/
      Ok  tuenti-common           9.11.0   TARBZ2  http://www.myartifacts.com/
    Cleanup

**How dependencies are stored locally**

Archive (TARGZ, TARBZ2, TARXZ) are extracted directly in the lib folder
DVCS dependencies create a "export" of the requested changeset in the lib folder. The .git/.hg files are stripped, so you can't do any commits from the repo dependencies.

**Symlink overrides**

If you need to develop a library and need to test it continously with your main project you can establish a manual override.
Simply create a symlink in lib/ with the name of the library and point it to your regular development environment.
SuperNanny will ignore all symlink dependencies.

    ls -al lib
    lrwxrwxrwx 1 test test   35 nov 15 15:39 tu-build -> /home/test/branches/tu-build/

When executing a supernanny fetch, you can see that SuperNanny recognizes the override, and ignores it.
It does read it's .DEP file and download any additional dependencies tuenti-build might need.

    $ supernanny fetch
    Init repos
    Resolve dependencies
    Fetch dependencies
      Ok  hstats-client           2.5     TARBZ2   http://www.myartifacts.com/
      Ok  tfw-lib                 3.1     TARBZ2   http://www.myartifacts.com/
      Ok  thrift-hbase            1.3.0   TARBZ2   http://www.myartifacts.com/
      Ok  tuenti-build            *       SYMLINK  *Manual override*
    Cleanup

Getting dependency status
-------------------------
Sometimes, you will want to know what the status of your dependencies is. The status command simply prints information on the currently checked out libraries.

    $ supernanny status
    Current libraries:

      hstats-client           2.5      TARBZ2  http://www.myartifacts.com/
      tfw-lib                 3.1      TARBZ2  http://www.myartifacts.com/
      thrift-hbase            1.3.0    TARBZ2  http://www.myartifacts.com/
      tuenti-build            30.13.0  TARBZ2  http://www.myartifacts.com/


Exported library definition
---------------------------
Every project can export some libraries, maybe the whole project as a library. The definition of exports is done in .EXPORT file in the project's root, like this:

    # exporting befw as a git repo and as an archive
    befw GIT http://push.mygitrepo.com/befw
    befw ARCHIVE http://www.myartifacts.com/ build/

Note that tar strategies take a folder to publish as the 4th parameter. If omitted, the working directory is published.
To publish these artifacts, execute the publish command. The publish command will query for versions you want to export these artifacts under:

    $ supernanny publish
    Project's export targets:
      befw  GIT     http://push.mygitrepo.com/befw/  .
      befw  ARCHIVE http://www.myartifacts.com/      build

    No next version specified!
    Please input the version for this export: 13.7.0
    Version 13.7.0 will be published - do you want to proceed? [y/n]y
      published befw@13.7.0
    [username:]...
    [password:]
    Artifact published - waiting for index to update... (max 30 seconds)
      Published befw@13.7.0

To see what libraries a project exports, execute the exports command:

    $ supernanny exports
    Project's export targets:
      befw  GIT     http://push.mygitrepo.com/befw/  .
      befw  ARCHIVE http://www.myartifacts.com/      build

**Archive formats**

You can either specify the tar formats, TARBZ2, TARGZ or TARXZ which will generate an artifact with that compression method, or choose ARCHIVE.
The default for ARCHIVE is TARXZ.

Pretend
-------

To make a dry-run, add --pretend switch to publish commands, to get a summary of what would happen, but not actually do it:

    $ supernanny publish --pretend
    Project's export targets:
      befw  GIT     http://push.mygitrepo.com/befw/  .
      befw  ARCHIVE http://www.myartifacts.com/      build

    No next version specified!
    Please input the version for this export: 7.0
    Would publish version 7.0 to GIT http://push.mygitrepo.com/befw/
    Would publish version 7.0 to ARCHIVE http://www.myartifacts.com/

Integration with ant
--------------------

SuperNanny provides ant targets for fetching, publishing and getting the status of artifacts, that correspond to fetch, publish and status commands of supernanny. Here's how to use them.

To define targets from the Java archive:

    <target name="-init-supernanny">
     <taskdef name="supernanny-fetch" classname="com.tuenti.supernanny.ant.SuperNannyResolve" classpath="supernanny.jar"/>
     <taskdef name="supernanny-publish" classname="com.tuenti.supernanny.ant.SuperNannyPublish" classpath="supernanny.jar"/>
    </target>

Now to fetch dependencies, passing a root parameter (where the .DEP file resides):

    <target name="deps" depends="-init-supernanny">
      <supernanny-fetch root="${root}"/>
    </target>

and to publish defined libraries:

    <target name="publish" depends="-init-supernanny">
      <supernanny-publish/>
    </target>

The publish task takes next parameter which define how to increment the version:

    <target name="publish-major" description="Publish a major version (x.y.z →[x+1].0.0)">
        <supernanny-publish next="+"/>
    </target>

    <target name="publish-minor" description="Publish a minor version (x.y.z →x.[y+1].0)">
        <supernanny-publish next="x.+"/>
    </target>

    <target name="publish-hotfix" description="Publish a hotfix version (x.y.z →x.y.[z+1])">
        <supernanny-publish next="x.x.+"/>
    </target>

Prefixing
---------

You can add any prefix to all publishes with --prefix:

    supernanny publish --prefix=beta
