import com.antalex.db.model.enums.QueryStrategy;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.domain.persistence.entity.shard.TestAShardEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.entity.shard.TestCShardEntity;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.domain.persistence.repository.TestARepository;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
import com.antalex.service.TestService;
import com.antalex.service.TestShardService;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
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
	@Autowired
	private EntityManagerFactory entityManagerFactory;

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

	@Test
	public void findShard() {
		profiler.start("findShard");
		TestBShardEntity b = entityManager.find(TestBShardEntity.class, 643891562L);
		System.out.println("FIND B");

		TestAShardEntity a = b.getA();
		System.out.println("FIND A");
		System.out.println("FIND A value " + a.getValue());

		TestBShardEntity b2 = entityManager.find(TestBShardEntity.class, 650304962L);
		TestBShardEntity b3 = entityManager.find(TestBShardEntity.class, 656667962L);
		System.out.println("b3.getCList().size() " + b3.getCList().size());
		TestBShardEntity b4 = entityManager.find(TestBShardEntity.class, 656667962L);
		System.out.println("b4.getCList().size() " + b3.getCList().size());
		System.out.println("b4.getCList().size() 2 " + b4.getCList().size());

		List<TestCShardEntity> cList =  b.getCList();
		System.out.println("b.getCList()");
		int size = cList.size();
		System.out.println("cList.size() " + size);
		TestCShardEntity c = b.getCList().get(0);
		System.out.println("FIND C value " + c.getValue());
//		System.out.println("c.getB().getValue() " + c.getB().getValue());


		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	@Test
	public void findJPA() {
		profiler.start("findJPA");
		TestBEntity b = testBRepository.findById(643891562L).orElse(null);
		System.out.println("FIND B");

		TestAEntity a = b.getA();

		System.out.println("FIND A");
		System.out.println("FIND A value " + a.getValue());

		TestBEntity b2 = testBRepository.findById(650304962L).orElse(null);
		TestBEntity b3 = testBRepository.findById(656667962L).orElse(null);
		System.out.println("b3.getCList().size() " + b3.getCList().size());
		TestBEntity b4 = testBRepository.findById(656667962L).orElse(null);
		System.out.println("b4.getCList().size() " + b4.getCList().size());
		System.out.println("b4.getCList().size() 2 " + b4.getCList().size());

		List<TestCEntity> cList =  b.getCList();
		System.out.println("b.getCList()");
		int size = cList.size();
		System.out.println("cList.size() " + size);
		TestCEntity c = b.getCList().get(0);
		System.out.println("FIND C value " + c.getValue());
//		System.out.println("c.getB().getValue() " + c.getB().getValue());

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
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
		List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "JPA5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveTransactionalJPA");
		testService.saveTransactionalJPA(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBEntities.size = " + testBEntities.size());
		/*
		TestBEntity b = testBEntities.get(0);
		b.setNewValue("JPAnewVal_B!!!");
		System.out.println("AAA!!!!!!!!!!!!!!");
		TestCEntity c = b.getCList().get(10);
		c.setNewValue("JPAnewVal_C!!!");


		profiler.start("ADD testService.saveTransactionalJPA");
		testService.saveTransactionalJPA(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
*/
	}

//	@Test
	public void saveMyBatis() {
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "MyBatis5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveMyBatis");
		testService.saveMyBatis(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
		System.out.println("testBEntities.size = " + testBEntities.size());
	}

//	@Test
	public void saveStatement() {
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "Statement5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.save");
		testService.save(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
		System.out.println("testBEntities.size = " + testBEntities.size());
	}

//	@Test
	public void saveShard() {
		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities2 = testShardService.generate(1000, 100, "Shard5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.save");
		testShardService.save(testBEntities2);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBEntities2.size = " + testBEntities2.size());

		TestBShardEntity b = testBEntities2.get(0);
		b.setNewValue("ShardNewVal_B!!!");
		System.out.println("AAA!!!!!!!!!!!!!!");
		TestCShardEntity c = b.getCList().get(10);
		c.setNewValue("ShardNewVal_C!!!");


		profiler.start("ADD testShardService.save");
		testShardService.update(testBEntities2);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
		System.out.println("STOP");
	}

	//	@Test
	public void bind() {
		profiler.start("BIND.Query");
		for (int i = 0; i < 100000; i++) {
			entityManager
					.createQuery(
							TestBShardEntity.class,
							"SELECT ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,? WHERE x0.ID=?",
							QueryType.SELECT
					)
					.bind(1).bind(2).bind(3).bind(4).bind(5).bind(6).bind(7).bind(8).bind(9).bind(10)
					.bind(11).bind(12).bind(13).bind(14).bind(15).bind(16).bind(17).bind(18).bind(19).bind(20);
		}
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("BIND.Queries");
		for (int i = 0; i < 100000; i++) {
			entityManager
					.createQueries(
							TestBShardEntity.class,
							"SELECT ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,? WHERE x0.ID=?",
							QueryType.SELECT
					)
					.forEach(query ->
							query
									.bind(1).bind(2).bind(3).bind(4).bind(5).bind(6).bind(7).bind(8).bind(9).bind(10)
									.bind(11).bind(12).bind(13).bind(14).bind(15).bind(16).bind(17).bind(18).bind(19).bind(20)
					);
		}
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
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
		b.setShardMap(1L);
		b.setValue("B2");
		b.setNewValue("B2");
		testBRepository.save(b);

		b.setNewValue("B3");
		testBRepository.save(b);

		testBRepository.save(b);
*/


		profiler.start("Тест");


		List<AdditionalParameterEntity> additionalParameterEntities
				= additionalParameterService.generate(10000, "TEST", "С_CODE2", "VALUE2");

        AdditionalParameterEntity entity = additionalParameterEntities.get(2);

        System.out.println("AAA ID 0 = " + entity.getId());

        additionalParameterService.saveJPA(additionalParameterEntities);

        System.out.println("AAA ID 2 = " + entity.getId());

		entity.setValue("VAL_TEST");

		additionalParameterRepository.save(entity);

//		entityManager.save(additionalParameterEntities);

		profiler.stop();

//		String hexStr = Integer.toString(63,32);

		System.out.println(profiler.printTimeCounter());
	}

}
