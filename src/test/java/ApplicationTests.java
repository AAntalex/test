import com.antalex.dao.ShardEntityManeger;
import com.antalex.optimizer.OptimizerApplication;
import com.antalex.domain.persistence.entity.AdditionalParameterEntity;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.AdditionalParameterService;
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
	private ShardEntityManeger entityManeger;

	@Test
	public void ins() {
		profiler.start("Тест");

		List<AdditionalParameterEntity> additionalParameterEntities
				= additionalParameterService.generate(10000, "TEST", "С_CODE2", "VALUE2");
		additionalParameterService.save(additionalParameterEntities);

//		entityManeger.save(additionalParameterEntities);

		profiler.stop();

//		String hexStr = Integer.toString(63,32);

		System.out.println(profiler.printTimeCounter());
	}

}
