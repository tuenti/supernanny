package repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.tuenti.supernanny.dependencies.RepositoryType;
import com.tuenti.supernanny.repo.artifacts.ArchiveArtifact;
import com.tuenti.supernanny.repo.artifacts.ReqType;
import com.tuenti.supernanny.repo.artifacts.Requirement;
import com.tuenti.supernanny.repo.index.IndexReader;
import com.tuenti.supernanny.util.Version;

public class IndexTest {
	@Test
	public void testIndex() throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new FileReader(new File("./testData/index1.txt")));
		IndexReader ir = new IndexReader(bufferedReader);
		List<ArchiveArtifact> artifacts = ir.parse();

		Assert.assertEquals(4, artifacts.size());
		
		ArchiveArtifact a = artifacts.get(0);
		Assert.assertEquals("libphonenumber", a.getName());
		Assert.assertEquals(new Version("1.2.0"), a.getVersion());
		Assert.assertEquals("libphonenumber-1.2.0.tar.bz2", a.getFilename());

		List<Requirement> expectedRequirements = new LinkedList<Requirement>();
		expectedRequirements.add(new Requirement("tuenti-common", ReqType.LT, "7.8", RepositoryType.TARBZ2, "http://artifacts.tuenti.int/"));
		expectedRequirements.add(new Requirement("tfw-lib", ReqType.GT, "1.*", RepositoryType.TARBZ2, "http://artifacts.tuenti.int/"));
		Assert.assertEquals(expectedRequirements, a.getRequirements());

		a = artifacts.get(1);
		Assert.assertEquals("supervisor-common", a.getName());
		Assert.assertEquals(new Version("0.33"), a.getVersion());
		Assert.assertEquals("supervisor/supervisor-common-0.33.tar.bz2", a.getFilename());
		Assert.assertEquals(new LinkedList<Requirement>(), a.getRequirements());

		a = artifacts.get(2);
		Assert.assertEquals("fefw-fbi", a.getName());
		Assert.assertEquals(new Version("2.1"), a.getVersion());
		Assert.assertEquals("fefw-fbi-2.1.tar.bz2", a.getFilename());

		expectedRequirements = new LinkedList<Requirement>();
		expectedRequirements.add(new Requirement("tuenti-build", ReqType.LE, "16.8", RepositoryType.TARBZ2, "http://artifacts.tuenti.int/"));
		expectedRequirements.add(new Requirement("befw", ReqType.GE, "4.*", RepositoryType.TARBZ2, "http://artifacts.tuenti.int/"));
		expectedRequirements.add(new Requirement("otro", ReqType.SW, "5.*", RepositoryType.TARBZ2, "http://artifacts.tuenti.int/"));
		//expect the default GE
		expectedRequirements.add(new Requirement("squeeze", ReqType.GE, "6.*", RepositoryType.TARBZ2, "http://artifacts.tuenti.int/"));
		Assert.assertEquals(expectedRequirements, a.getRequirements());

		a = artifacts.get(3);
		Assert.assertEquals("supervisor-common", a.getName());
		Assert.assertEquals(new Version("1.0.1"), a.getVersion());
		Assert.assertEquals("supervisor-common-1.0.1.tar.bz2", a.getFilename());
		Assert.assertEquals(new LinkedList<Requirement>(), a.getRequirements());
	}
}
