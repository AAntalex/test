import com.antalex.db.service.SequenceGenerator;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.impl.ApplicationSequenceGenerator;
import com.antalex.db.service.impl.ApplicationSequenceGenerator2;
import com.antalex.db.service.impl.ShardDatabaseManager;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import com.antalex.profiler.model.TimeCounter;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
import liquibase.precondition.core.PreconditionContainer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.Objects;

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
    private ShardDatabaseManager databaseManager;


	@Test
	public void ins() {


		SequenceGenerator sequenceGenerator = new ApplicationSequenceGenerator("SEQ_ID", databaseManager.getDataSource());
		((ApplicationSequenceGenerator) sequenceGenerator).setCacheSize(10000);

		profiler.start("Тест0");

		for (int i = 0; i < 100000; i++) {
			sequenceGenerator.nextValue();
		}

		profiler.stop();
		System.out.println(profiler.printTimeCounter());


		profiler.start("Тест");

		List<AdditionalParameterEntity> additionalParameterEntities
				= additionalParameterService.generate(10000, "TEST", "С_CODE2", "VALUE2");

        AdditionalParameterEntity entity = additionalParameterEntities.get(2);

        System.out.println("AAA ID 0 = " + entity.getId());

        additionalParameterService.saveJPA(additionalParameterEntities);

        System.out.println("AAA ID 2 = " + entity.getId());

		entity.setValue("VAL_TEST");

		additionalParameterRepository.save(entity);

//		entityManeger.save(additionalParameterEntities);

		profiler.stop();

//		String hexStr = Integer.toString(63,32);

		System.out.println(profiler.printTimeCounter());
	}

}
