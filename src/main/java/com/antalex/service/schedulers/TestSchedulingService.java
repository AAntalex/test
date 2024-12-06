package com.antalex.service.schedulers;

import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import com.antalex.domain.persistence.entity.hiber.TestBEntity;
import com.antalex.domain.persistence.entity.shard.TestBShardEntity;
import com.antalex.domain.persistence.repository.TestBRepository;
import com.antalex.profiler.service.ProfilerService;
import com.antalex.service.TestService;
import com.antalex.service.TestShardService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@DependsOn("TestTaskScheduler")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TestSchedulingService {
    private final TestService testService;
    private final TestShardService testShardService;
    private final ShardEntityManager entityManager;
    private final ShardDataBaseManager dataBaseManager;
    private final MeterRegistry meterRegistry;
    private final TestBRepository testBRepository;
    private final ProfilerService profiler;

    private AtomicLong counter;
    private AtomicLong counterSpeed;

    @Transactional
    protected void findAllShard() {
        List<TestBShardEntity> testBEntities = entityManager.findAll(
                TestBShardEntity.class,
                "${value} like ?",
                "Domain%");
        log.trace("AAA testBEntities.count: {}", testBEntities.size());
    }

    @Transactional
    protected void findAllHibernate() {
        List<TestBEntity> testBEntities = testBRepository.findAllByValueLike("JPA%");
        log.trace("AAA testBEntities.count: {}", testBEntities.size());
    }

    @Transactional
    protected void processShard() {
        List<TestBShardEntity>  testBEntities = testShardService.generate(1000, 100, "Shard_Schedule");
        entityManager.updateAll(testBEntities);
    }

    @Transactional
    protected void processHibernate() {
        List<TestBEntity> testBEntities = testService.generate(1000, 100, null, "JPA_Schedule");
        testService.saveTransactionalJPA(testBEntities);
    }

//    @Scheduled(initialDelay = 1, fixedDelayString = "${test.scheduler.intervalSec}", timeUnit = TimeUnit.SECONDS)
    @Timed(value = "test.aaa.process", description = "Time to process test job", percentiles = {0.5,0.9})
    public void jobTestProcess() {
        if (Objects.isNull(counter)) {
            counter = meterRegistry.gauge("aaa_used_sequence", new AtomicLong());
            counterSpeed = meterRegistry.gauge("aaa_sequence_speed", new AtomicLong());
        }
        long curVal = dataBaseManager.sequenceCurVal();
        long curTime = System.currentTimeMillis();

        try {
            log.trace(
                    "AAA START тестового планировщика thread: {}, время запуска: {}",
                    Thread.currentThread().getName(),
                    LocalDateTime.now(ZoneId.of("UTC+3")));


            profiler.start("process");

//            processShard();
//            processHibernate();

            findAllShard();
//            findAllHibernate();

            profiler.stop();
            System.out.println(profiler.printTimeCounter());

            log.trace(
                    "AAA STOP тестового планировщика thread: {}, время запуска: {}",
                    Thread.currentThread().getName(),
                    LocalDateTime.now(ZoneId.of("UTC+3")));
        } catch (Exception err) {
            log.error(
                    "Ошибка выполнения задания: {}, ERR: {}",
                    ExceptionUtils.getRootCause(err).getClass().getSimpleName(),
                    ExceptionUtils.getRootCause(err).getMessage());
        } finally {
            counter.set(dataBaseManager.sequenceCurVal() - curVal);
            counterSpeed.set(BigDecimal.valueOf(counter.get())
                    .divide(BigDecimal.valueOf(System.currentTimeMillis() - curTime), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(600000))
                    .longValue());
        }
    }
}
