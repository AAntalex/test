import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.QueryType;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.api.DataWrapper;
import com.antalex.db.service.api.DataWrapperFactory;
import com.antalex.db.service.impl.managers.ShardDatabaseManagerImpl;
import com.antalex.domain.persistence.domain.TestBDomain;
import com.antalex.domain.persistence.entity.hiber.TestAEntity;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.hiber.TestCEntity;
import com.antalex.domain.persistence.entity.shard.*;
import com.antalex.domain.persistence.entity.shard.app.MainDocum;
import com.antalex.domain.persistence.repository.TestARepository;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.GenerateService;
import com.antalex.service.TestDomainService;
import com.antalex.service.TestService;
import com.antalex.service.TestShardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.sun.tools.attach.VirtualMachine;
import jakarta.persistence.EntityManagerFactory;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.CharSet;
import org.apache.logging.log4j.util.Chars;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.xml.stream.events.Characters;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = OptimizerApplication.class)
public class ApplicationTests {
	private final ThreadLocal<String> testLocal = new ThreadLocal<>();

	@Autowired
	private ProfilerService profiler;

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
	@Autowired
	private DataWrapperFactory dataWrapperFactory;
	@Autowired
	private ShardDatabaseManagerImpl shardDatabaseManagerImpl;
	@Autowired
	private GenerateService<MainDocum> mainDocumGenerateService;

	//	@Test
	public void dialect() {
		try {
			DialectResolver dialectResolver = new StandardDialectResolver();
			Connection connection = databaseManager.getCluster("").getMainShard().getDataSource().getConnection();


			Dialect dialect = dialectResolver.resolveDialect(
					new DatabaseMetaDataDialectResolutionInfoAdapter(connection.getMetaData())
			);

//			String sql = dialect.getSequenceNextValString("$$$.SEQ_ID");
//			System.out.println("AAA sql = " + sql);
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
	}

	//	@Test
	public void findDomain() {
		profiler.start("findDomain");
		TestBDomain b = domainEntityManager.find(TestBDomain.class, 6301297863L);
		System.out.println("FIND B b.getValue() = " + b.getValue());

		b.setNewValue("TEST1");
		domainEntityManager.update(b);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

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

		List<TestCShardEntity> cList = b.getCList();
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

		List<TestCEntity> cList = b.getCList();
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

		List<TestCEntity> cList = b.getCList();
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

		List<TestCEntity> cList = b.getCList();
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
		List<TestBEntity> bList = testBRepository.findAllByValueLikeAndShardMapGreaterThanEqual("JPA%", 0L);
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
	public void findAllDomain() {
		profiler.start("findAllDomain");

		testService.test();

		/*
		List<TestBDomain> bList = domainEntityManager.findAll(
				TestBDomain.class,
				"${value} like ? and ${x0.newValue} like ?",
				"Domain%", "Domain%");
		System.out.println("FIND B bList.size() = " + bList.size());
*/

/*
		Runnable target = () -> {
			System.out.println("START SQL 1!");
			List<TestBDomain> bList = domainEntityManager.findAll(
					TestBDomain.class,
					"${value} like ? and ${x0.newValue} like ?",
					"Domain%", "Domain%");
			System.out.println("FIND B bList.size() = " + bList.size());
		};
		target.run();
*/



		List<TestBDomain> bList = domainService.findAllB();

		Set<Long> ids = new HashSet<>();
		bList.forEach(it -> it.getTestList().forEach(c -> ids.add(c.getId())));

		System.out.println("All testList.size() = " + ids.size());


/*
		ExecutorService executorService = Executors.newFixedThreadPool(10);

		Future future = null;
		for (int i = 0; i < 10; i++) {
			future = executorService.submit(target);
		}

		try {
			future.get();
		} catch (Exception err) {
			throw new ShardDataBaseException(err);
		}
*/

/*
		bList.forEach(b -> {
			int cSize = b.getCList().size();
		});
		System.out.println("b.getCList().size() = " + bList.get(0).getCList().size());
*/

/*
		System.out.println("isLazy= " + bList.get(0).isLazy("TestBDomain"));

		System.out.println("b.note = " + bList.get(0).getNote());
		System.out.println("b.routing.name = " + bList.get(0).getRouting().getName());
		System.out.println("b.numDoc = " + bList.get(0).getNumDoc());
*/
/*
		TestBDomain b = bList.get(0);


		System.out.println("b.routing.name = " + b.getRouting().getName());

		b.getRouting().setName("TESt2");
		domainEntityManager.updateAll(bList);

		b.getRouting().setName("TESt2");
		domainEntityManager.updateAll(bList);

		b.getRouting().setName("TESt3");
		domainEntityManager.updateAll(bList);

		domainEntityManager.updateAll(bList);
*/

//		domainEntityManager.getTransaction().commit();


//		System.out.println("FIND C bList.get(0).getCList().size() = " + bList.get(0).getTestList().size());

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
/*
		TestBDomain b = bList.get(0);
		profiler.start("ADD findDomain");

		for (int i = 0; i < 100; i++) {
			b = domainEntityManager.find(TestBDomain.class, b.getId());
		}

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("FIND C b.getCList().size() = " + b.getTestList().size());
*/
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
		domainEntityManager.getTransaction().commit();
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
	public void skipLockedDomain() {
		profiler.start("skipLockedDomain");
		List<TestBDomain> bList = domainEntityManager.skipLocked(
				TestBDomain.class,
				"x0.C_VALUE like ?",
				"Domain%");
		System.out.println("FIND B bList.size() = " + bList.size());
		domainEntityManager.getTransaction().commit();
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	//	@Test
	public void deleteDomain() {
		List<TestBDomain> bList = domainEntityManager.findAll(
				TestBDomain.class,
				"x0.C_VALUE like ?",
				"Domain%");
		System.out.println("FIND B bList.size() = " + bList.size());

		TestBDomain b = bList.get(0);
		System.out.println("FIND B ID = " + b.getId());

		profiler.start("deleteDomain");
		domainEntityManager.deleteAll(bList);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		domainEntityManager.saveAll(bList);

		domainEntityManager.getTransaction().commit();
	}

	//	@Test
	public void updateOther() {
		profiler.start("saveOther");

		try {
			Connection connection = shardDatabaseManagerImpl.getConnection();
			PreparedStatement preparedStatement = connection.prepareStatement(
					"UPDATE segment_integr.TEST_OTHER SET SN=SN+1,C_STATUS=?,C_URL=? WHERE ID=?");
			preparedStatement.setString(1, "ST1");
			preparedStatement.setString(2, "URL1");
			preparedStatement.setLong(3, 20790000L);
			preparedStatement.addBatch();
			preparedStatement.addBatch();
			preparedStatement.setString(1, "ST12");
			preparedStatement.addBatch();
			preparedStatement.executeBatch();
		} catch (Exception err) {
			throw new RuntimeException(err);
		}
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	//	@Test
	public void saveOther() {
		profiler.start("saveOther");
		List<TestOtherShardEntity> list = testShardService.generateOther(100);
		entityManager.saveAll(list);

		list.get(0).setStatus(TestStatus.TIMEOUT);
		entityManager.updateAll(list);

		shardDatabaseManagerImpl.saveTransactionInfo();
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	//	@Test
	public void findAllOther() {
		profiler.start("findAllOther");
		List<TestOtherShardEntity> entities = entityManager.findAll(TestOtherShardEntity.class, "st=?", "3a21c0ed-dbc0-4fc1-a748-337bd7b9c01b");
		System.out.println("entities.size() = " + entities.size());
		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	@Transactional
	public void saveJPA(List<TestBEntity> testBEntities) {
		testBEntities.forEach(testBRepository::save);
		System.out.println("AAA testBEntities.size() = " + testBEntities.size());
	}

//	@Test
	public void saveJPA() {
		databaseManager.sequenceNextVal();
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(10000, 100, null, "JPA5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveTransactionalJPA");

		testService.saveTransactionalJPA(testBEntities);
//		testBEntities.forEach(it -> testBRepository.save(it));


		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBEntities.size = " + testBEntities.size());
	}

	//	@Test
	public void saveMyBatis() {
		databaseManager.sequenceNextVal();
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "MyBatis5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.saveMyBatis");

		testService.saveMyBatis(testBEntities);
//		testBEntities.forEach(it -> testService.saveMyBatis(it));

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
		System.out.println("testBEntities.size = " + testBEntities.size());
	}

	//	@Test
	public void saveStatement() {
		databaseManager.sequenceNextVal();
		profiler.start("testService.generate");
		List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "Statement5");
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("testService.save");

//		testBEntities.forEach(it -> testService.save(it));
		testService.save(testBEntities);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
		System.out.println("testBEntities.size = " + testBEntities.size());
	}

//	@Test
	public void saveShard() {
		databaseManager.sequenceNextVal();
		profiler.start("testShardService.generate");
		List<TestBShardEntity> testBEntities2 = testShardService.generate(10000, 100, "Shard");
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


	//	@Test
	public void deadLock() {
		databaseManager.sequenceNextVal();
		profiler.start("testShardService.generate");

		List<TestBShardEntity> bList = entityManager.findAll(
				TestBShardEntity.class,
				"id in (?, ?, ?)",
				699230133001L, 693000126000L, 2709012978002L);


		System.out.println("bList.size = " + bList.size());

		bList.get(0).setValue("Test11");
		bList.get(1).setValue("Test22");
		bList.get(2).setValue("Test33");

		entityManager.updateAll(bList);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}


//	@Test
	public void saveDomain() {
		databaseManager.sequenceNextVal();
		profiler.start("saveDomain.generate");
		List<TestBDomain> testBDomains = domainService.generate(10000, 100, "Domain");
		profiler.stop();

		profiler.start("saveDomain.save");

		domainEntityManager.updateAll(testBDomains);
//		testBDomains.forEach(it -> domainEntityManager.update(it));

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("testBDomains.size = " + testBDomains.size());
/*

		TestBDomain b = testBDomains.get(0);
		System.out.println("B ID = " + b.getId());

		b.setExecuteTime(OffsetDateTime.now());
		b.setExecuteTime(OffsetDateTime.now());
		b.setExecuteTime(OffsetDateTime.now());

		b.setSum(BigDecimal.valueOf(10.1));
		b.setSum(BigDecimal.valueOf(10.2));
		b.setSum(BigDecimal.valueOf(10.3));

		List<AttributeHistory> history = domainEntityManager.getAttributeHistory(b, "executeTime");
		System.out.println("1 history.size = " + history.size());

		domainEntityManager.updateAll(testBDomains);

		history = domainEntityManager.getAttributeHistory(b, "sum");
		System.out.println("2 history.size = " + history.size());
*/
/*
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
				" shardMap = " + b.getEntity().getStorageContext().getShardMap() +
				" a.ID = " + a.getEntity().getId() +
				" a.shardMap = " + a.getEntity().getStorageContext().getShardMap() +
				" b.shard = " + b.getEntity().getStorageContext().getShard().getName() +
				" b.note = " + b.getNote()
		);

		b.setTestA(a);
		a.setValue("DomainVal_A!!!");

		b.setNote("new Note");

		profiler.start("ADD testShardService.save");

		domainEntityManager.updateAll(testBDomains);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		System.out.println("AAA!!!!!!!!!!!!!! AFTER b = " + b.getEntity().getId() +
				" shardMap = " + b.getEntity().getStorageContext().getShardMap() +
				" a.ID = " + a.getEntity().getId() +
				" a.shardMap = " + a.getEntity().getStorageContext().getShardMap() +
				" b.shard = " + b.getEntity().getStorageContext().getShard().getName() +
				" b.note = " + b.getNote()
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

	//@Test
	public void testJson3() {
		DataWrapper dataWrapper = dataWrapperFactory.createDataWrapper(DataFormat.JSON);

		List<Object> list = new ArrayList<>();
		list.add(1L);
		list.add("test");
		list.add(2);
		list.add(OffsetDateTime.now());
		list.add(BigDecimal.valueOf(1.1));
		list.add(Date.valueOf(LocalDate.now()));

		try {
			dataWrapper.init(null);

			dataWrapper.put("test", list);
			System.out.println("AAA content = " + dataWrapper.getContent());

			DataWrapper dataWrapper2 = dataWrapperFactory.createDataWrapper(DataFormat.JSON);
			dataWrapper2.init(dataWrapper.getContent());

			List<Object> list2 = dataWrapper2.getList("test", Object.class);

			System.out.println("AAA list2.size = " + list2.size());

		} catch (Exception err) {
			throw new RuntimeException(err);
		}


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

	//	@Test
	public void testJson2() {
		profiler.start("testJson2");

		try {
			ObjectNode root = objectMapper.createObjectNode();

			root.putPOJO("note", "AAA");
//			root.putPOJO("executeTime", OffsetDateTime.now());

			root.put("executeTime", objectMapper.writeValueAsString(OffsetDateTime.now()));

			/*
			TestAEntity a = new TestAEntity();
			a.setId(2345L);
			a.setValue("AValue");
			a.setExecuteTime(LocalDateTime.now());

			root.putPOJO("a", a);
			*/

			System.out.println("jSonText 0 = " + root.toString());
		} catch (Exception err) {
			throw new RuntimeException(err);
		}

		profiler.stop();
		System.out.println(profiler.printTimeCounter());
	}

	//	@Test
	public void testJPAPersistenceContext() {
		testLocal.set("MainThread");

		System.out.println("MAIN testLocal = " + testLocal.get());
		Runnable target = () -> {
			try {
				System.out.println("START SQL! testLocal = " + testLocal.get());
				Thread.sleep(1000L);
				testLocal.set("Thread threadId = " + Thread.currentThread().getId());
//				TestBShardEntity b = entityManager.find(TestBShardEntity.class, 27090062L);
				TestBEntity b = testBRepository.findById(63000L).orElse(null);
				Thread.sleep(1000L);
				System.out.println("FIND B = " + b.hashCode() + " identityHashCode " + System.identityHashCode(b) + " threadId = " + Thread.currentThread().getId() + " testLocal = " + testLocal.get());
//				b = entityManager.find(TestBShardEntity.class, 27090062L);
				TestBEntity b2 = testBRepository.findById(63000L).orElse(null);
				Thread.sleep(1000L);
				System.out.println("FIND 2 B = " + b2.hashCode() + " identityHashCode " + System.identityHashCode(b2) + " threadId = " + Thread.currentThread().getId() + " testLocal = " + testLocal.get());
				if (b == b2) {
					System.out.println("B == B2 is TRUE");
				} else {
					System.out.println("B == B2 is FALSE");
				}
			} catch (Exception err) {
				System.out.println("ERR = " + err.getLocalizedMessage());
				throw new ShardDataBaseException(err);
			}
		};

		ExecutorService executorService = Executors.newFixedThreadPool(10);

		Future future = null;
		for (int i = 0; i < 2; i++) {
			future = executorService.submit(target);
		}

		try {
			future.get();
		} catch (Exception err) {
			throw new ShardDataBaseException(err);
		}

		TestBEntity b = testBRepository.findById(27090062L).orElse(null);
	}


//	@Test
	public void testSaveUpd() {
		TestBEntity b = testBRepository.findById(10999000001L).orElse(null);
		TestBEntity b2 = testBRepository.findById(10999000001L).orElse(null);
		if (b == b2) {
			System.out.println("B == B2 is TRUE");
		} else {
			System.out.println("B == B2 is FALSE");
		}
		b.setValue("JPAAAA");
		b2.setNewValue("JPAAAA");
		System.out.println("1 value = " + b.getValue() + " newValue = " + b.getNewValue());
		System.out.println("2 value = " + b2.getValue() + " newValue = " + b2.getNewValue());

		testBRepository.save(b);
		testBRepository.save(b2);
		b = testBRepository.findById(10999000001L).orElse(null);
		System.out.println("1 value = " + b.getValue() + " newValue = " + b.getNewValue());
	}

//	@Test
	public void testCriteria() {
		profiler.start("findCriteria");

/*

		TestBDomain b = domainEntityManager.findByCriteria(TestCriteria.class, "{a.value} = ?", "AAA");
		List<TestBDomain> bList = domainEntityManager.findAllByCriteria(TestCriteria.class, "a.C_VALUE = ?", "AAA");
		Stream<TestBDomain> bStream = domainEntityManager.getCache(TestCriteria.class, 1L, "TEST");

		System.out.println("FIND B b.getValue() = " + b.getValue());
*/


		profiler.stop();
		System.out.println(profiler.printTimeCounter());

	}


//	@Test
	public void testEqual() {
		TestClass t1 = new TestClass();
		TestClass t2 = new TestClass();

		UUID uuid = UUID.randomUUID();
		t1.setCondition("test");
		t1.setBinds(new Object[16]);

		t1.getBinds()[0] = true;
		t1.getBinds()[1] = (byte) 1;
		t1.getBinds()[2] = 2d;
		t1.getBinds()[3] = 3f;
		t1.getBinds()[4] = (short) 4;
		t1.getBinds()[5] = BigDecimal.valueOf(3.14);
		t1.getBinds()[6] = 6;
		t1.getBinds()[7] = 7;
		t1.getBinds()[8] = new java.util.Date();
		t1.getBinds()[9] = LocalDateTime.now();
		t1.getBinds()[10] = OffsetDateTime.now();
		t1.getBinds()[11] = new Time(System.currentTimeMillis());
		t1.getBinds()[12] = new Timestamp(System.currentTimeMillis());

		t1.getBinds()[13] = TestStatus.PROCESS;
		try {
			t1.getBinds()[14] = new URL("https://www.baeldung.com/java-url");
		} catch (MalformedURLException err) {
			throw new RuntimeException(err);
		}
		t1.getBinds()[15] = uuid;


		t2.setCondition("test");
		t2.setBinds(new Object[16]);

		t2.getBinds()[0] = true;
		t2.getBinds()[1] = (byte) 1;
		t2.getBinds()[2] = 2d;
		t2.getBinds()[3] = 3f;
		t2.getBinds()[4] = (short) 4;
		t2.getBinds()[5] = BigDecimal.valueOf(3.14);
		t2.getBinds()[6] = 6;
		t2.getBinds()[7] = 7;
		t2.getBinds()[8] = new java.util.Date();
		t2.getBinds()[9] = LocalDateTime.now();
		t2.getBinds()[10] = OffsetDateTime.now();
		t2.getBinds()[11] = new Time(System.currentTimeMillis());
		t2.getBinds()[12] = new Timestamp(System.currentTimeMillis());

		t2.getBinds()[13] = TestStatus.PROCESS;
		try {
			t2.getBinds()[14] = new URL("https://www.baeldung.com/java-url");
		} catch (MalformedURLException err) {
			throw new RuntimeException(err);
		}
		t2.getBinds()[15] = uuid;




		System.out.println("1 t1.hashCode() = " + t1.hashCode() + " t2.hashCode() = " + t2.hashCode()
				+ " t1.equals(t2) = " + t1.equals(t2)
						+ " t1.toString() = " + t1
				);

		System.out.println("AAA 1 t1.getBinds().hashCode() = " + Arrays.hashCode(t1.getBinds()) + " t2.getBinds().hashCode() = " + Arrays.hashCode(t2.getBinds())
				+ " t1.equals(t2) = " + Arrays.equals(t1.getBinds(), t2.getBinds())
				+ " t1.getBinds().toString() = " + Arrays.toString(t1.getBinds())
		);

	}

	@Data
	class TestClass {
		private String condition;
		private Object[] binds;
	}


//	@Test
	public void findOne() {
		WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080")
				.build();

		profiler.start("findOne");
		int cnt = 0;




		for (int i = 0; i < 10000; i++) {
			TestCShardEntity c = entityManager.find(TestCShardEntity.class, 1566180000L);
			if (c != null) cnt++;
		}



/*
		for (int i = 0; i < 10000; i++) {
			cnt = cnt +
					webClient.get().uri("/api/v1/test").retrieve().bodyToMono(Integer.class).block();;
		}
*/


		System.out.println("FIND cnt = " + cnt);

		profiler.stop();
		System.out.println(profiler.printTimeCounter());

	}

	//@Test
	public void saveMainDocum() {
		profiler.start("generateMainDocum");
		List<MainDocum> list = mainDocumGenerateService.generate("40702810X", 100000, 10000, 1000);
		profiler.stop();
		System.out.println(profiler.printTimeCounter());

		profiler.start("saveMainDocum");
		entityManager.saveAll(list);
		profiler.stop();

		shardDatabaseManagerImpl.saveTransactionInfo();

		System.out.println(profiler.printTimeCounter());
	}

	//@Test
	public void regExpTest() {
		String condition = "        a1.Id = ? \n" +
				"   and (\n" +
				"        (\n" +
				"            a2.C_DEST like 'A1.ID = ? AND (A2.C_DEST like ''AAA%'' or a3.C_DATE >= :date)%' or a2.C_DEST like 'AAA%'" +
				"         or a3.C_DATE >= ?\n" +
				"        ) \n" +
				"        OR (1=1)\n" +
				"       )\n";

//		Pattern pattern = Pattern.compile("(?<=[\\(\\{\\['\"]).+(?=[\\)\\}\\]'\"])");

		//Pattern pattern = Pattern.compile("'.+?'");

		Pattern pattern = Pattern.compile("\r");

		Matcher matcher = pattern.matcher(condition);


		System.out.println("Start RegExp");
		while (matcher.find()) {
			System.out.println(matcher.group());
		}

		System.out.println(condition.replaceAll("[  \n]", " "));
	}

	int getEndWord(char[] chars, int offset) {
		for (int i = offset; i < chars.length; i++) {
			if (
					i == chars.length - 1 ||
					!(chars[i+1] >= 'a' && chars[i+1] <= 'z' ||
							chars[i+1] >= 'A' && chars[i+1] <= 'Z' ||
							chars[i+1] >= '0' && chars[i+1] <= '9' ||
							chars[i+1] == '_' || chars[i+1] == '$'))
			{
				return i;
			}
		}
		return chars.length;
	}

	int getEndString(char[] chars, int offset) {
		char quote = chars[offset];
		if (quote != Chars.QUOTE && quote != Chars.DQUOTE) {
			return -1;
		}
		int quotesCount = 0;
		for (int i = offset+1; i < chars.length; i++) {
			if (chars[i] == quote) {
				if (++quotesCount % 2 == 1 && (i == chars.length - 1 || chars[i + 1] != quote)) return i;
			} else {
				quotesCount = 0;
			}
		}
		throw new RuntimeException(
				String.format(
						"Отсутсвует закрывающая кавычка %c в тексте; %s",
						quote,
						String.copyValueOf(chars, offset, chars.length - offset)
				)
		);
	}

	int getEndParenthesis(char[] chars, int offset) {
		char parenthesis = chars[offset];
		char endParenthesis;
		if (parenthesis == '(') {
			endParenthesis = ')';
		} else {
			return -1;
		}
		for (int i = offset+1; i < chars.length; i++) {
			if (chars[i] == endParenthesis) return i;
			if (chars[i] == parenthesis) {
				i = getEndParenthesis(chars, i);
				continue;
			}
			if (chars[i] == Chars.QUOTE) {
				i = getEndString(chars, i);
			}
		}
		throw new RuntimeException(
				String.format(
						"Отсутсвует закрывающая скобка %c в тексте; %s",
						endParenthesis,
						String.copyValueOf(chars, offset, chars.length - offset)
				)
		);
	}

	@Data
	@Accessors(chain = true, fluent = true)
	class BooleanExpression {
		private StringBuilder expression = new StringBuilder();
		private List<BooleanExpression> expressions = new ArrayList<>();
		private Set<String> aliases = new HashSet<>();
		boolean isAnd;
		boolean isNot;
	}

	String expressionToString(BooleanExpression expression) {
		if (expression.expressions().isEmpty()) {
			return (expression.isNot() ? "NOT " : "") + expression.expression();
		}
		return
				(!expression.isAnd() ? "(" : "") +
						expression
								.expressions()
								.stream()
								.map(this::expressionToString)
								.collect(Collectors.joining(expression.isAnd() ? " AND " : " OR ")) +
						(!expression.isAnd() ? ")" : "");
	}

	void cloneUpExpression(BooleanExpression source, boolean isAnd) {
		BooleanExpression child = new BooleanExpression();
		child
				.expression(source.expression())
				.expressions(source.expressions())
				.aliases(source.aliases())
				.isAnd(source.isAnd())
				.isNot(source.isNot());
		source.aliases(new HashSet<>());
		source.expression(new StringBuilder());
		source.expressions(new ArrayList<>());
		source.expressions().add(child);
		source.isAnd(isAnd);
		source.isNot(false);
		source.expression().append("p1");
	}

	void concatExpression(BooleanExpression left, BooleanExpression right, boolean isAnd, boolean isNot) {
		right.isNot(isNot && !right.isNot() || !isNot && right.isNot());
		if (left.expressions().isEmpty() ||
				!isNot && left.isAnd() && !isAnd ||
				isNot && !left.isAnd() && isAnd)
		{
			cloneUpExpression(left, isAnd);
		}
		if (!isNot && !left.isAnd() && isAnd || isNot && left.isAnd() && !isAnd) {
			concatExpression(left.expressions().get(left.expressions().size() - 1), right, isAnd, false);
		}
		if (left.isAnd() == isAnd) {
			left.expressions().add(right);
			left.expression()
					.append(isAnd ? " AND " : " OR ")
					.append("p")
					.append(left.expressions().size());
		}
	}

	void parseCondition(String condition, BooleanExpression expression) {
		Set<Character> escapeCharacters = Set.of(Chars.LF, Chars.CR, Chars.TAB, Chars.SPACE);
		BooleanExpression currentExpression = expression;
		boolean isNot = expression.isNot();
		String token = Strings.EMPTY;
		char[] chars = condition.toCharArray();
		char lastChar = 0;

		for (int i = 0; i < chars.length; i++) {
			char curChar = chars[i];
			if (escapeCharacters.contains(curChar)) continue;
			if (curChar == Chars.QUOTE) {
				int endPos = getEndString(chars, i);
				String currentString = String.copyValueOf(chars, i, endPos - i + 1);
				currentExpression
						.expression()
						.append(token.isEmpty() ? "" :  " ")
						.append(currentString);
				i = endPos;
				lastChar = chars[i];
				continue;
			}
			if (curChar == '(') {
				int endPos = getEndParenthesis(chars, i);
				boolean needParenthesis = false;
				if (!currentExpression.expression().isEmpty()) {
					currentExpression.expression().append("(");
					needParenthesis = true;
				}
				parseCondition(String.copyValueOf(chars, i + 1, endPos - i - 1), currentExpression);
				if (expression == currentExpression && !currentExpression.expressions.isEmpty()) {
					cloneUpExpression(expression, true);
				}
				if (needParenthesis) {
					currentExpression.expression().append(")");
				}
				i = endPos;
				lastChar = chars[i];
				continue;
			}
			String curToken = Strings.EMPTY;
			if (curChar == Chars.DQUOTE) {
				int endPos = getEndString(chars, i);
				curToken = String.copyValueOf(chars, i, endPos - i + 1);
				i = endPos;
			}
			if (curChar == '_' || curChar >= 'a' && curChar <= 'z' || curChar >= 'A' && curChar <= 'Z') {
				int endPos = getEndWord(chars, i);
				curToken = String.copyValueOf(chars, i, endPos - i + 1).toUpperCase();
				i = endPos;
			}
			if (!curToken.isEmpty()) {
				if ("OR".equals(curToken) || "AND".equals(curToken)) {
					currentExpression = new BooleanExpression();
					concatExpression(
							expression,
							currentExpression,
							isNot && "OR".equals(curToken) || !isNot && "AND".equals(curToken),
							isNot
					);
					token = Strings.EMPTY;
				} else if ("NOT".equals(curToken)) {
					currentExpression.isNot(!currentExpression.isNot());
					token = Strings.EMPTY;
				} else {
					currentExpression
							.expression()
							.append(lastChar == '.' ? "." : (token.isEmpty() ? "" :  " "))
							.append(curToken);
					token = curToken;
				}
				lastChar = chars[i];
				continue;
			}
			if (curChar == '.' && !token.isEmpty()) {
				System.out.println("ALIAS: " + token);
				currentExpression.aliases().add(token);
				lastChar = chars[i];
				continue;
			}
			token = Strings.EMPTY;
			currentExpression.expression().append(Character.toUpperCase(curChar));
			lastChar = chars[i];
		}
	}

	@Test
	public void regExpTest2() {
		String condition = "(a1.Id = ? or a1.C_COL = ?)\n" +
				"and\n" +
				"  (\n" +
				"    upper (a1.C_DEST) = ('ASD')\n" +
				"    and (\n" +
				"        \"a2\".C_DEST like 'A1.ID = ?  AND (A2.C_DEST like \t''AAA%'' or a3.C_DATE >= :date)%'\n" +
				"      or a2.C_DEST like 'Aaa%'\n" +
				"      or a3.C_DATE >= ?\n" +
				"    )\n" +
				"    AND NOT (\n" +
				"          b1.\"c_col\" =1\n" +
				"      and Not b2.C_COL2 = 2\n" +
				"      or b3.C_COL3= 3\n" +
				"    )\n" +
				"    AND NOT (\n" +
				"        c1.\"c_col\" = 1\n" +
				"     or c2.C_COL2 = 2\n" +
				"     AND Not c3.C_COL3= 3\n" +
				"    )\n" +
				"    AND (\n" +
				"          (\n" +
				"               D1.C_1 = 1\n" +
				"            OR C2.C_2 = 2\n" +
				"          )\n" +
				"      AND (D3.C_3 = 3 and D4.C_4 = 4)\n" +
				"    )\n" +
				"    OR Not (1=1)\n" +
				"  )";


		VirtualMachine vm = VirtualMachine.attach();

		BooleanExpression expression = new BooleanExpression();
		parseCondition(condition, expression);

		System.out.println("RES: " + expressionToString(expression));

	}

	//@Test
	public void testPID() {
		long pid = ProcessHandle.current().pid();
		try {
			VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
		} catch (Exception err) {
			err.printStackTrace();
		}
	}

}
