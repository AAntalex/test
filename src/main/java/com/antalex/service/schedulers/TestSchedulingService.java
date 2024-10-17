package com.antalex.service.schedulers;

import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.service.TestService;
import com.antalex.service.TestShardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vtb.pmts.db.service.ShardEntityManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@DependsOn("TestTaskScheduler")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TestSchedulingService {
    private final TestService testService;
    private final TestShardService testShardService;
    private final ShardEntityManager entityManager;

    @Transactional
    private void processShard() {
        List<TestBShardEntity>  testBEntities = testShardService.generate(1000, 100, "Shard_Schedule");
        entityManager.updateAll(testBEntities);
    }

    @Transactional
    private void processHibernate() {
        List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "JPA_Schedule");
        testService.saveTransactionalJPA(testBEntities);
    }

    @Scheduled(initialDelay = 1, fixedDelayString = "${test.scheduler.intervalSec}", timeUnit = TimeUnit.SECONDS)
    public void jobTestProcess() {
        try {
            log.trace(
                    "AAA START тестового планировщика thread: {}, время запуска: {}",
                    Thread.currentThread().getName(),
                    LocalDateTime.now(ZoneId.of("UTC+3")));


//            processShard();
            processHibernate();

            log.trace(
                    "AAA STOP тестового планировщика thread: {}, время запуска: {}",
                    Thread.currentThread().getName(),
                    LocalDateTime.now(ZoneId.of("UTC+3")));
        } catch (Exception err) {
            log.error(
                    "Ошибка выполнения задания: {}, ERR: {}",
                    ExceptionUtils.getRootCause(err).getClass().getSimpleName(),
                    ExceptionUtils.getRootCause(err).getMessage());
        }
    }
}
