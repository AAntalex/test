import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.domain.TestADomain;
import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.domain.TestCDomain;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.domain.persistence.entity.shard.*;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.domain.persistence.repository.TestARepository;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
import com.antalex.service.TestDomainService;
import com.antalex.service.TestService;
import com.antalex.service.TestShardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private TestDomainService domainService;
	@Autowired
	private DomainEntityManager domainEntityManager;

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
	public void findShard() {
		profiler.start("findShard");
		TestBShardEntity b = entityManager.find(TestBShardEntity.class, 768795301L);
		System.out.println("FIND B");

		TestAShardEntity a = b.getA();
		System.out.println("FIND A");
		System.out.println("FIND A value " + a.getValue());

		TestBShardEntity b2 = entityManager.find(TestBShardEntity.class, 769431601L);
		TestBShardEntity b3 = entityManager.find(TestBShardEntity.class, 770067901L);
		System.out.println("b3.getCList().size() " + b3.getCList().size());
		TestBShardEntity b4 = entityManager.find(TestBShardEntity.class, 770067901L);
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

//    @Test
    public void findMBatis() {
        profiler.start("findMBatis");
        TestBEntity b = testService.findBByIdMBatis(768795301L);
        System.out.println("FIND B");


        TestBEntity b2 = testService.findBByIdMBatis(769431601L);
        TestBEntity b3 = testService.findBByIdMBatis(770067901L);
        System.out.println("b3.getCList().size() " + b3.getCList().size());
        TestBEntity b4 = testService.findBByIdMBatis(770067901L);
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

//    @Test
    public void findStatement() {
        profiler.start("findStatement");
        TestBEntity b = testService.findBByIdStatement(768795301L);
        System.out.println("FIND B");


        TestBEntity b2 = testService.findBByIdStatement(769431601L);
        TestBEntity b3 = testService.findBByIdStatement(770067901L);
        System.out.println("b3.getCList().size() " + b3.getCList().size());
        TestBEntity b4 = testService.findBByIdStatement(770067901L);
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

//    @Test
	public void findJPA() {
		profiler.start("findJPA");
		TestBEntity b = testBRepository.findById(768795301L).orElse(null);
		System.out.println("FIND B");

		/*
		TestAEntity a = b.getA();
		System.out.println("FIND A");
		System.out.println("FIND A value " + a.getValue());
*/

		TestBEntity b2 = testBRepository.findById(769431601L).orElse(null);
		TestBEntity b3 = testBRepository.findById(770067901L).orElse(null);
		System.out.println("b3.getCList().size() " + b3.getCList().size());
		TestBEntity b4 = testBRepository.findById(770067901L).orElse(null);
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
	public void findAllJPA() {
		profiler.start("findAllJPA");
		List<TestBEntity> bList = testBRepository.findAllByValueLike("JPA%");
		System.out.println("FIND B bList.size() = " + bList.size());
/*
		bList.forEach(b -> {
			int cSize = b.getCList().size();
		});
		System.out.println("b.getCList().size() = " + bList.get(0).getCList().size());
*/
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

//	@Test
	public void findAllShard() {
		profiler.start("findAllShard");
		List<TestBShardEntity> bList = entityManager.findAll(
				TestBShardEntity.class,
				"x0.C_VALUE like ?",
				"Shard%");
		System.out.println("FIND B bList.size() = " + bList.size());
/*
		bList.forEach(b -> {
			int cSize = b.getCList().size();
		});
		System.out.println("b.getCList().size() = " + bList.get(0).getCList().size());
*/
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

//	@Test
	public void findAllMBatis() {
		profiler.start("findAllMBatis");
		List<TestBEntity> bList = testService.findAllByValueLikeMBatis("MyBatis%");
		System.out.println("FIND B bList.size() = " + bList.size());

		/*
		bList.forEach(b -> {
			int cSize = b.getCList().size();
		});
		System.out.println("b.getCList().size() = " + bList.get(0).getCList().size());
*/
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

//	@Test
	public void findAllStatement() {
		profiler.start("findAllStatement");
		List<TestBEntity> bList = testService.findAllBByValueLikeStatement("Statement%");
		System.out.println("FIND B bList.size() = " + bList.size());
/*
		bList.forEach(b -> {
			int cSize = b.getCList().size();
		});
		System.out.println("b.getCList().size() = " + bList.get(0).getCList().size());
*/
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
	public void saveOther() {
		profiler.start("saveOther");
		entityManager.saveAll(testShardService.generateOther(100));
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

//	@Test
	public void findAllOther() {
		profiler.start("findAllOther");
		List<TestOtherShardEntity> entities = entityManager.findAll(TestOtherShardEntity.class);
		System.out.println("entities.size() = " + entities.size());
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}


	@Test
	public void saveJPA() {
        databaseManager.sequenceNextVal();
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "JPA5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveTransactionalJPA");
		testService.saveTransactionalJPA(testBEntities);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBEntities.size = " + testBEntities.size());
	}

	@Test
	public void saveMyBatis() {
        databaseManager.sequenceNextVal();
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

	@Test
	public void saveStatement() {
        databaseManager.sequenceNextVal();
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

	@Test
	public void saveShard() {
        databaseManager.sequenceNextVal();
		profiler.start("testShardService.generate");
		List<TestBShardEntity>  testBEntities2 = testShardService.generate(1000, 100, "Shard5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testShardService.save");

		entityManager.updateAll(testBEntities2);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBEntities2.size = " + testBEntities2.size());

/*
		TestBShardEntity b = testBEntities2.get(0);
		TestAShardEntity a = b.getA();
		b.setNewValue("ShardNewVal_B!!!");
		System.out.println("AAA!!!!!!!!!!!!!! b = " + b.getId());
		System.out.println("AAA!!!!!!!!!!!!!! a.getValue = " + a.getValue());
		TestCShardEntity c = b.getCList().get(10);
		c.setNewValue("ShardNewVal_C!!! c = " + c.getId());

		b = testBEntities2.get(7);
		b.setValue(b.getValue());
		System.out.println("AAA!!!!!!!!!!!!!! b = " + b.getId());

		b = testBEntities2.get(9);
		System.out.println("AAA!!!!!!!!!!!!!! BEFORE b = " + b.getId() +
				" shardMap = " + b.getStorageContext().getShardMap() +
				" a.ID = " + a.getId() +
				" a.shardMap = " + a.getStorageContext().getShardMap() +
				" b.shard = " + b.getStorageContext().getShard().getName()
		);
		b.setA(a);

		profiler.start("ADD testShardService.save");

		entityManager.updateAll(testBEntities2);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("AAA!!!!!!!!!!!!!! AFTER b = " + b.getId() +
				" shardMap = " + b.getStorageContext().getShardMap() +
				" a.ID = " + a.getId() +
				" a.shardMap = " + a.getStorageContext().getShardMap() +
				" b.shard = " + b.getStorageContext().getShard().getName()
		);

		System.out.println("STOP");
		*/
	}

	@Test
	public void saveDomain() {
		databaseManager.sequenceNextVal();
		profiler.start("saveDomain.generate");
		List<TestBDomain> testBDomains = domainService.generate(1000, 100, "Domain");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("saveDomain.save");

		domainEntityManager.updateAll(testBDomains);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBDomains.size = " + testBDomains.size());

/*
		TestBDomain b = testBDomains.get(0);
		TestADomain a = b.getTestA();

		b.setNewValue("DomainNewVal_B!!!");
		b.setValue("DomainVal_B!!!");
		System.out.println("AAA!!!!!!!!!!!!!! b = " + b.getEntity().getId());
		System.out.println("AAA!!!!!!!!!!!!!! a.getValue = " + a.getValue());
		TestCDomain c = b.getTestList().get(10);
		c.setValue("DomainNewVal_C!!! c = " + c.getEntity().getId());

		b = testBDomains.get(7);
		b.setValue(b.getValue());
		System.out.println("AAA!!!!!!!!!!!!!! b = " + b.getEntity().getId());

		b = testBDomains.get(9);
		System.out.println("AAA!!!!!!!!!!!!!! BEFORE b = " + b.getEntity().getId() +
				" a.ID = " + a.getEntity().getId()
		);
		b.setTestA(a);
		a.setValue("DomainVal_A!!!");

		profiler.start("ADD testShardService.save");

		domainEntityManager.updateAll(testBDomains);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("AAA!!!!!!!!!!!!!! AFTER b = " + b.getEntity().getId() +
				" a.ID = " + a.getEntity().getId()
		);

		System.out.println("STOP");
		*/
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

//	@Test
	public void testJson() {
		profiler.start("testJson");
		List<TestBEntity> testBEntities = testService.generate(2, 10, null, "Json");

		try {
            String jSonText;
            TestAEntity a = new TestAEntity();
            a.setId(2345L);
            a.setValue("AValue");

            TestBEntity b = testBEntities.get(0);

            ObjectNode root = objectMapper.createObjectNode();

            root.putPOJO("id", 1111L);
            root.putPOJO("executeTime", b.getExecuteTime());

            root.putPOJO("a", a);
            jSonText = root.toString();


            System.out.println("jSonText 0 = " + jSonText);

/*
			jSonText = objectMapper.writeValueAsString(testBEntities.get(0));
			System.out.println("jSonText 1 = " + jSonText);
*/

            testBEntities.get(0).setId(123L);


//            List<TestBEntity> bList = Arrays.asList(objectMapper.readValue(jSonText, TestBEntity[].class));
            b = objectMapper.readValue(jSonText, TestBEntity.class);

            root = (ObjectNode) objectMapper.readTree(jSonText);

            root.put("a", objectMapper.writeValueAsString(a));
            jSonText = root.asText();
            System.out.println("jSonText = " + jSonText);





		} catch (JsonProcessingException err) {
			throw new RuntimeException(err);
		}

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

}
