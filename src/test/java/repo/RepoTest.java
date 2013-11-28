package repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.tuenti.supernanny.Util;
import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.ArchiveRepo;
import com.tuenti.supernanny.repo.RepoProvider;
import com.tuenti.supernanny.repo.artifacts.ArchiveArtifact;
import com.tuenti.supernanny.repo.artifacts.Artifact;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.exceptions.ResolutionException;
import com.tuenti.supernanny.repo.index.IndexReader;
import com.tuenti.supernanny.resolution.EagerResolver;
import com.tuenti.supernanny.resolution.Resolver;
import com.tuenti.supernanny.util.Version;

public class RepoTest {

	public class ArtifactData {
		public String name;
		public Version version;
		public ArtifactData(String name, String version) {
			this.name = name;
			this.version = new Version(version);
		}
		public ArtifactData(String name, Version version) {
			this.name = name;
			this.version = version;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
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
			ArtifactData other = (ArtifactData) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (version == null) {
				if (other.version != null)
					return false;
			} else if (!version.equals(other.version))
				return false;
			return true;
		}
		private RepoTest getOuterType() {
			return RepoTest.this;
		}
	}

	private static Injector injector;
	private static Util util;

	@BeforeClass
	public static void init() {
		util = Mockito.mock(Util.class);
		injector = Guice.createInjector(new AbstractModule() {
			
			@Override
			protected void configure() {
				bind(RepoProvider.class).in(Scopes.SINGLETON);
				bind(Util.class).toInstance(util);
			}
		});
	}

	public void expectArtifacts(ArtifactData[] expectedArray, Set<Artifact> actual) {
		Set<ArtifactData> adata = new HashSet<ArtifactData>();
		for (Artifact artifact : actual) {
			adata.add(new ArtifactData(artifact.getName(), artifact.getVersion()));
		}
		Set<ArtifactData> expected = new HashSet<ArtifactData>(Arrays.asList(expectedArray));
		Assert.assertEquals(expected, adata);
	}

	@Test
	public void testResolutionOk() throws Exception {
		prepareRepo();

		Resolver resolver = injector.getInstance(EagerResolver.class);
		List<Requirement> initialRequirements = new ArrayList<Requirement>();
		initialRequirements.add(new Requirement("befw", ReqType.EQ, "1.0.0", RepositoryType.TARBZ2,
				"http://localhost"));
		Set<Artifact> artifacts = resolver.resolve(initialRequirements);

		ArtifactData[] expected = { new ArtifactData("tuenti-common", "1.7.0"),
				new ArtifactData("futi", "3.0"), new ArtifactData("tuenti-build", "9.0"),
				new ArtifactData("befw", "1.0.0") };
		expectArtifacts(expected, artifacts);
	}


	@Test
	public void testResolutionLibNotFound() throws Exception {
		prepareRepo();

		Resolver resolver = injector.getInstance(EagerResolver.class);
		List<Requirement> initialRequirements = new ArrayList<Requirement>();
		initialRequirements.add(new Requirement("something-weird", ReqType.EQ, "1.0",
				RepositoryType.TARBZ2, "http://localhost"));
		try {
			resolver.resolve(initialRequirements);
			Assert.fail();
		} catch (ResolutionException e) {
			String msg = e.getMessage();
			Assert.assertTrue(msg.contains("No artifact found") && msg.contains("tuenti-build"));
		}
	}

	@Test
	public void testConflict() throws Exception {
		prepareRepo();

		Resolver resolver = injector.getInstance(EagerResolver.class);
		List<Requirement> initialRequirements = new ArrayList<Requirement>();
		initialRequirements.add(new Requirement("conflicting", ReqType.EQ, "1.0",
				RepositoryType.TARBZ2, "http://localhost"));
		try {
			resolver.resolve(initialRequirements);
			Assert.fail();
		} catch (ResolutionException e) {
			String msg = e.getMessage();
			Assert.assertTrue(msg.contains("tuenti-common") && msg.contains("tuenti-build")
					&& msg.contains("9.0") && msg.contains("8.0"));
		}
	}

	@Test
	public void testChooseCorrectMajor() throws Exception {
		prepareRepo();

		Resolver resolver = injector.getInstance(EagerResolver.class);
		List<Requirement> initialRequirements = new ArrayList<Requirement>();
		initialRequirements.add(new Requirement("tuenti-build", ReqType.SW, "8.*",
				RepositoryType.TARBZ2, "http://localhost"));
		Set<Artifact> artifacts = resolver.resolve(initialRequirements);
		expectArtifacts(new ArtifactData[] { new ArtifactData("tuenti-build", "8.0") }, artifacts);
	}

	@Test
	public void testCircular() throws Exception {
		prepareRepo();

		Resolver resolver = injector.getInstance(EagerResolver.class);
		List<Requirement> initialRequirements = new ArrayList<Requirement>();
		initialRequirements.add(new Requirement("flik", ReqType.SW, "1.*",
				RepositoryType.TARBZ2, "http://localhost"));
		Set<Artifact> artifacts = resolver.resolve(initialRequirements);
		expectArtifacts(new ArtifactData[] { new ArtifactData("flik", "1.0"), new ArtifactData("flak", "1.0"), new ArtifactData("flok", "1.0") }, artifacts);
	}
	
	private void prepareRepo() throws FileNotFoundException, IOException {
		ArchiveRepo repo = new ArchiveRepo("http://localhost", false, 10) {
			@Override
			public RepositoryType getRepoType() {
				return RepositoryType.TARBZ2;
			}
		};
		BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(
				"testData/index2.txt")));
		IndexReader ir = new IndexReader(bufferedReader);
		List<ArchiveArtifact> artifacts = ir.parse();
		repo.setArtifacts(artifacts);
		RepoProvider provider = injector.getInstance(RepoProvider.class);
		provider.addRepo(RepositoryType.TARBZ2, "http://localhost", repo);
	}
}
