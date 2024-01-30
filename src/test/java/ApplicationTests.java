import com.antalex.db.service.SequenceGenerator;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.impl.ApplicationSequenceGenerator;
import com.antalex.db.utils.ShardUtils;
import com.antalex.domain.persistence.entity.TestAEntity;
import com.antalex.domain.persistence.entity.TestBEntity;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.domain.persistence.repository.TestARepository;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
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


	@Test
	public void ins() {

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
