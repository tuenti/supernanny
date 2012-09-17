Supernanny
=============

SuperNanny is a dependency management tool developed in-house, for managing dependencies between repositories 
and artifacts. It supports resolving and fetching defined dependencies and exporting them. 

Since we mostly used PHP we implemented a system similar to Ivy to manage our libraries, like Ivy, supernanny is language agnostic so 
it can be used to retrieve dependencies in any language.

Dependencies can be expresed as artifacts or as source control repos. Artifacts follow a versioning scheme.


1. Dependency definition
--------------------------

Dependencies are defined in .DEP file in the root of the project. Each line defines a single dependency. Comment lines begin with #. Dependencies are of format:

<name> <type> <uri> <version>

Type of the dependency currently must be one of the following: GIT, MERCURIAL, TARGZ, TARBZ2, e.g.

### my project dependencies

```
tu-memcache GIT git://github.com/tuenti/memcached-tuenti-multiport.git stable-1.3
pypy MERCURIAL https://bitbucket.org/pypy/pypy default
portage TARBZ2 http://gentoo.inode.at/snapshots/ latest
 ```

This project defines 3 dependencies, each at their own versions, specifying a URI where to find them.

In detail:
 
   ```common TARBZ2 http://artifacts.server.int/ 2.*  ```

This pulls an artifact with major version 2 and any minor version. The artifacts is really a TAR file
downloaded from the artifacts server
  
   ```backend-framework TARBZ2 http://artifacts.server.int/ 13.7 ```

This pulls version 13.7 of the backend-framework artifact
  
  ``` config MERCURIAL http://pull.code.some.repo/config default ```
      
This pulls a dependency as a version control depot from mercurial, you can substitute
mercurial by git and it also works.
  
  
2. Fetching dependencies
---------------------------
Once the project defines the .DEP file, fetching dependencies is trivial:
 
 ```
$ supernanny fetch
	# fetched lib/tu-memcache@stable-1.3
	# fetched lib/pypy@default
	# fetched lib/portage@latest
 ```
3. Getting dependency status
-----------------------------
Sometimes, you will want to know what is the status of your dependencies. Maybe you added, removed of changed some of them, and you want to see the status. You can do that using the status command:

```
$ supernanny status
# dependencies

	# befw @ 1.3 from ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
	# tu-memcache @ stable-1.3 from git://github.com/tuenti/memcached-tuenti-multiport.git (GIT)

# removed:
	 befw @ 1.3 from ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
# new:
	 class-loader @ 1.* from ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
	 portage @ latest from http://gentoo.inode.at/snapshots/ (TARBZ2)

inconsistent - consider refetching dependencies

```
Status will warn you of removed dependencies that are still present, and newly added dependencies that are not yet fetched. It will also suggest fetching them again, to solve everything. After the fetch, it should look like this:

```
$ supernanny status
# dependencies

	# class-loader @ 1.* from ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
	# portage @ latest from http://gentoo.inode.at/snapshots/ (TARBZ2)
	# tu-memcache @ stable-1.3 from git://github.com/tuenti/memcached-tuenti-multiport.git (GIT)
```

4. Exported library definition
------------------------------
Every project can export some libraries, maybe the whole project as a library. The definition of exports is done in .EXPORTS file in the project's root, like this:

### exporting befw as a git repo and as an archive

```
befw GIT ssh://code.somewhere.com:/srv/git/supernanny
befw TARBZ2 ssh://code.somewhere.com:/srv/supernanny build/
```

Note that tar strategies take a folder to publish as the 4th parameter. If omitted, the working directory is published.
To publish these artifacts, execute the publish command. The publish command will query for versions you want to export these artifacts under:

```
$ supernanny publish
Please input the version for befw for GIT: 1.3
Please input the version for befw for TARGZ: 1.4

	# published befw@1.3
	# published befw@1.4
```
To see what libraries a project exports, execute the exports command:

```
$ supernanny exports
# project's export targets

	# befw to ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
	# befw to ssh://code.somewhere.com:/srv/supernanny/ (TARGZ)
```

5. Pretend
----------

To make a dry-run, add --pretend switch to fetch or publish commands, to get a summary of what would happen, but not actually do it:

```
$ supernanny publish --pretend
	# would publish befw to ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
	# would publish befw to ssh://code.somewhere.com:/srv/supernanny/ (TARGZ)
```
```
$ supernanny fetch --pretend
	# would fetch class-loader @ 1.* from ssh://code.somewhere.com:/srv/supernanny/ (TARBZ2)
	# would fetch tu-memcache @ stable-1.3 from git://github.com/tuenti/memcached-tuenti-multiport.git (GIT)
```

6. Integration with ant
------------------------

SuperNanny provides 2 ant targets, for fetching and publishing artifacts, that correspond to publish and fetch commands of supernanny. Here's how to use them.

To define targets from the Java archive:
```ant
	<target name="-init-supernanny">
		<taskdef name="supernanny-fetch" classname="com.tuenti.supernanny.ant.SuperNannyResolve" classpath="supernanny.jar"/>
		<taskdef name="supernanny-publish" classname="com.tuenti.supernanny.ant.SuperNannyPublish" classpath="supernanny.jar"/>
	</target>
```
Now to fetch dependencies, passing a root parameter (where the .DEP file resides):

```ant
	<target name="deps" depends="-init-supernanny">
		 <supernanny-fetch root="${root}"/>
	</target>
```
and to publish defined libraries:

```ant
	<target name="publish" depends="-init-supernanny">
		 <supernanny-publish/>
	</target>
```
The publish task takes versions parameter which define at which versions you want to publish the libraries. If the parameter is not present, ant task will query the user just as the publish command would. Versions are defined as a string of space-separated version tags, 1 per library. If the number of versions is different than the number of libraries, the task will fail with the proper exception. For the previous example of 2 exported libraries, this ant task is also appropriate:

```ant
	<target name="publish" depends="-init-supernanny">
		 <supernanny-publish versions="1.3    1.4" />
	</target>
```


7. Forcing specific versions
-----------------------------

If you don't want to change your .DEP file, and still override some entries, there are several ways:
More configuration files with --depfile: list of files that will be parsed, of same format as .DEP. First occurrence of a dependency entry is considered only.

```
supernanny fetch --depfile depOverride,lib/VERSIONS,.DEP

```
Using --force to set specific versions of dependencies. They must exist in one of the dependency files - the type defined there will be used:

```
supernanny fetch --force befw=1.3.2,tuenti-build=1.4
```

8. Prefixing
--------------

You can add any prefix to all publishes with --prefix:

```
supernanny publish --prefix=beta
```

9. Easy new version releases
------------------------------

Supernanny can recognize version formats and use it to release new versions.

Let's say that you have versions of format MAJOR.MINOR.BUILD, i.e. x.y.z, and that the latest version published is 1.3.7:

```
supernany publish --next x.x.+ // read x.x.+ as leave, leave, increase
```

This is semantically releasing a new hotfix and would produce a versions 1.3.8

```
supernany publish --next x.+ // read x.+ as leave, increase
```

This is semantically releasing a new minor and would produce a versions 1.4.0

```
supernany publish --next + // read + as increase
```

This is semantically releasing a new major and would produce a versions 2.0.0

In Tuenti build scripts, this is usually wrapped in ant targets publish-minor, publish-major, publish-hotfix.

11. Doing no action on a dependency
------------------------------------

If you remove a dependency from supernanny, it will delete the folder on the next run. In order to tell supernanny not to touch a folder because, e.g. you have modified some things and don't want it to kill them, you can add a NOOP dependency type, e.g.

```
befw NOOP git://github.com/tuenti/memcached-tuenti-multiport.git stable-1.3
```

11. Installing supernanny
--------------------------

SuperNanny is a Java application, basically a .jar file, and can be used as such:

```
$ java -jar supernanny.jar help
Usage: com.tuenti.supernanny.cli.handlers.CliParser
  help [flag] Shows help
  exports [flag] Shows help
  --pretend [flag] Don't actually execute the command, only show info.
  publish [flag] Publish a dependency
  fetch [flag] Fetch project dependencies
  status [flag] Shows dependency status
```






