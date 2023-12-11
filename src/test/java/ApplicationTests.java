import com.antalex.db.service.ShardEntityManager;
import com.antalex.db.service.impl.ShardDatabaseManager;
import com.antalex.domain.persistence.repository.AdditionalParameterRepository;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
import liquibase.precondition.core.PreconditionContainer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
    private ShardDatabaseManager databaseManager;


	@Test
	public void ins() {
        profiler.start("Тест");

		List<AdditionalParameterEntity> additionalParameterEntities
				= additionalParameterService.generate(10000, "TEST", "С_CODE2", "VALUE2");

        AdditionalParameterEntity entity = additionalParameterEntities.get(1);

        System.out.println("AAA ID 0 = " + entity.getId());

        additionalParameterService.saveJPA(additionalParameterEntities);

        System.out.println("AAA ID 1 = " + entity.getId());

		entity.setValue("VAL_TEST");

		additionalParameterRepository.save(entity);

//		entityManeger.save(additionalParameterEntities);

		profiler.stop();

//		String hexStr = Integer.toString(63,32);

		System.out.println(profiler.printTimeCounter());
	}

}
