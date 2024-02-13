import com.antalex.db.model.Shard;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntityExt;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.domain.persistence.repository.TestARepository;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
import com.antalex.service.TestService;
import com.antalex.service.TestShardService;
import com.antalex.service.impl.TestBShardEntityRepository;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OptimizerApplication.class)
public class ApplicationTests {
	@Autowired
	private ProfilerService profiler;

	@Autowired
	private AdditionalParameterService additionalParameterService;

	@Autowired
	private AdditionalParameterRepository additionalParameterRepository;

	@Autowired
	private ShardEntityManager entityManager;
    @Autowired
    private ShardDataBaseManager databaseManager;
	@Autowired
	private TestARepository testARepository;
	@Autowired
	private TestBRepository testBRepository;
	@Autowired
	private TestService testService;
	@Autowired
	private TestShardService testShardService;


//	@Test
	public void dialect() {
		try {
			DialectResolver dialectResolver = new StandardDialectResolver();
			Connection connection = databaseManager.getCluster(ShardUtils.DEFAULT_CLUSTER_NAME).getMainShard().getDataSource().getConnection();

			Dialect dialect = dialectResolver.resolveDialect(
					new DatabaseMetaDataDialectResolutionInfoAdapter(connection.getMetaData())
			);
			String sql = dialect.getSequenceNextValString("$$$.SEQ_ID");
			System.out.println("AAA sql = " + sql);
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
	}

//	@Test
	public void sequence() {
		/*
		SequenceGenerator sequenceGenerator = new ApplicationSequenceGenerator(
				"SEQ_ID",
				databaseManager.getCluster(ShardUtils.DEFAULT_CLUSTER_NAME).getMainShard());
		((ApplicationSequenceGenerator) sequenceGenerator).setCacheSize(10000);
		((ApplicationSequenceGenerator) sequenceGenerator).setProfiler(profiler);
		*/

		profiler.start("Тест0");

//		System.out.println("AAA START! SEQ APP =  " + sequenceGenerator.nextValue());


		long seq;
		for (long i = 1L; i <= 100000L; i++) {
			seq = databaseManager.sequenceNextVal();
/*
			if (seq != i) {
				System.out.println("AAA i =  " + i + " seq = " + seq);
			}
*/
		}

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

/*
		sequenceGenerator = new TestSequenceGenerator(
				"SEQ_ID",
				databaseManager.getCluster(ShardUtils.DEFAULT_CLUSTER_NAME).getMainShard());
		((TestSequenceGenerator) sequenceGenerator).setCacheSize(1000);
		((TestSequenceGenerator) sequenceGenerator).setProfiler(profiler);

		profiler.start("Тест1");
		for (long i = 1L; i < 100000L; i++) {
			seq = sequenceGenerator.nextValue();
		}
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
*/
	}

//	@Test
	public void saveJPA() {
/*
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(100, 10, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveJPA");
		testService.saveJPA(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
*/
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(100, 10, null, "JPA");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveTransactionalJPA");
		testService.saveTransactionalJPA(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("ADD testService.saveTransactionalJPA");
		System.out.println("AAA!!!!!!!!!!!!!!");
		testBEntities.get(0).setNewValue("newVal!!!");
		testService.saveTransactionalJPA(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

//	@Test
	public void saveStatement() {
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(100, 10, null, "Statement");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.save");
		testService.save(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	@Test
	public void saveShard() {

		profiler.start("testShardService.generate 0");
		List<TestBShardEntity> testBEntities = testShardService.generate(100000, 0, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());


		profiler.start("testShardService.generate 1");
		testBEntities = testShardService.generate(100000, 0, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());


		profiler.start("testShardService.generate 2");
		testBEntities = testShardService.generate(100000, 0, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());


		profiler.start("testShardService.generate");
		testBEntities = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.saveLocal");
		testShardService.saveLocal(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());


		profiler.start("ADD testShardService.save");
		testBEntities.get(0).setNewValue("newVal!!!");
		testShardService.save(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());


		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities3 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.saveLocal2");
		testShardService.saveLocal(testBEntities3);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities2 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.save");
		testShardService.save(testBEntities2);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities5 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.saveLocal3");
		testShardService.saveLocal(testBEntities5);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities4 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.save2");
		testShardService.save(testBEntities4);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities7 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.saveLocal4");
		testShardService.saveLocal(testBEntities7);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities6 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.save3");
		testShardService.save(testBEntities6);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities8 = testShardService.generate(1000, 100, null);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.save4");
		testShardService.save(testBEntities8);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		List<TestBShardEntity>  testBEntities9 = testShardService.generate(1000, 100, null);
		profiler.start("testShardService.saveLocal5");
		testShardService.saveLocal(testBEntities9);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		List<TestBShardEntity>  testBEntities10 = testShardService.generate(1000, 100, null);
		profiler.start("testShardService.save5");
		testShardService.save(testBEntities10);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("STOP");
	}

	//	@Test
	public void ins() {

		/*
		TestAEntity a = new TestAEntity();
		a.setValue("A1");
		a.setValue("A2");
		TestBEntity b = new TestBEntity();
		b.setA(a);
		testARepository.save(a);
		b.setShardValue(1L);
		b.setValue("B2");
		b.setNewValue("B2");
		testBRepository.save(b);

		b.setNewValue("B3");
		testBRepository.save(b);

		testBRepository.save(b);
*/


		profiler.start("Тест");

/*
		List<AdditionalParameterEntity> additionalParameterEntities
				= additionalParameterService.generate(10000, "TEST", "С_CODE2", "VALUE2");

        AdditionalParameterEntity entity = additionalParameterEntities.get(2);

        System.out.println("AAA ID 0 = " + entity.getId());

        additionalParameterService.saveJPA(additionalParameterEntities);

        System.out.println("AAA ID 2 = " + entity.getId());

		entity.setValue("VAL_TEST");

		additionalParameterRepository.save(entity);
*/
//		entityManager.save(additionalParameterEntities);

		profiler.stop();

//		String hexStr = Integer.toString(63,32);

		System.out.println(profiler.printTimeCounter());
	}

}
