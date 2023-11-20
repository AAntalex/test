package com.antalex.prifiler.aspect;

import com.antalex.prifiler.service.ProfilerService;
import com.antalex.prifiler.service.impl.ProfilerServiceImpl;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Aspect
@Component
public class AOPController {
    @Autowired
    private ProfilerService profiler;

    //@Pointcut("!within(org.springframework..*) && !within(com.sun..*) && execution(public * *.*(..))")



//    @Pointcut("execution(public * com..*(..)) && !within(is(FinalType))")

//    @Pointcut("(execution(public * com..*(..)) || execution(public * org..*(..))) && !within(is(FinalType))")


/*
    @Pointcut("!within(is(FinalType)) " +
            "&& (!within(org.springframework..*) " +
            "|| within(org.springframework.orm..*) " +
            "|| within(org.springframework.jdbc..*))" +
            "&& (!within(com.sun..*) " +
            "&& (!within(com.antalex.prifiler..*) " +
            "&& (!within(com.antalex.domain.persistence..*) ")
*/


//    @Pointcut("!within(is(FinalType)) && !within(org.springframework..*) && !within(com.antalex.service..*Profiler*)")

//    @Pointcut("!within(org.springframework..*) && !within(com.antalex.service..*Profiler*)")

//    @Pointcut("within(org.springframework..*) && !within(org.springframework.aop..*) && !within(org.springframework.boot..*) && !within(org.springframework.beans..*) && !within(org.springframework.context..*) && !within(is(FinalType))")

    //@Pointcut("(!within(org.springframework..*) || within(org.springframework.data..*) && !within(org.springframework.data..*config*..*) || within(org.springframework.core..*)  || within(org.springframework.lang..*)  || within(org.springframework.boot..*)|| within(org.springframework.dao..*) || within(org.springframework.cache..*) || within(org.springframework.jdbc..*) || within(org.springframework.orm..*) ) && !within(is(FinalType)) && !within(com.antalex.service..*Profiler*)")

//    @Pointcut("within(oracle.jdbc..*)")


//    @Pointcut("!within(org.springframework..*aop*..*) && !within(org.springframework..*config*..*) && !within(is(FinalType)) && !within(com.antalex.service..*Profiler*)")

//    @Pointcut("execution(public * *(..)) && !within(com.antalex.service..*Profiler*)")


//    @Pointcut("!within(is(FinalType)) && (!within(org.springframework..*) || within(org.springframework.orm..*) || within(org.springframework.jdbc..*)) && !within(com.sun..*) && !within(com.antalex.prifiler..*) && !within(com.antalex.domain.persistence..*)")

    @Pointcut("!within(is(FinalType)) && !within(org.springframework..*) && !within(com.sun..*) && !within(com.antalex.prifiler..*)")

//    @Pointcut("within(com.antalex.service..*)")

//    @Pointcut("!within(org.springframework.orm.jpa..*) && !within(org.springframework.aop..*) && !within(is(FinalType))")

//    @Pointcut("execution(public * OptimizerApplication.*(..))")
    public void callProfiler() { }


    /*
    @After("callProfiler()")
    public void beforeProfiler(JoinPoint call) {
        try {
            System.out.println("AAAAAAAAAAA PROF " + call.getSignature());
        } catch (Exception err) {
            System.out.println("AAAAAAAAAAA ERR " + call.getSignature());
        }
    }
 */


    @Before("callProfiler()")
    public void beforeProfiler(JoinPoint call) {
        System.out.println("AAAAAAAAAAA PROF " + call.getSignature() + " profiler = " + profiler);

        if (Optional.ofNullable(profiler).map(ProfilerService::isStarted).orElse(false)) {
            profiler.startTimeCounter(call.getSignature().getName(), call.getSignature().toLongString());
        }
    }

    @After("callProfiler()")
    public void afterProfiler(JoinPoint call) {
        if (Optional.ofNullable(profiler).map(ProfilerService::isStarted).orElse(false))  {
            profiler.fixTimeCounter();
        }
    }

/*
    @Around("callProfiler()")
    public Object aroundProfiler(ProceedingJoinPoint joinPoint) throws Throwable {
            try {
                if (Optional.ofNullable(profiler).map(ProfilerService::isStarted).orElse(false)) {
                    System.out.println("AAAAAAAAAAA PROF " + joinPoint.getSignature());
                    profiler.startTimeCounter(
                            joinPoint.getSignature().getName(),
                            joinPoint.getSignature().toLongString()
                    );
                }
                return joinPoint.proceed();
            } catch (Exception err) {
                System.out.println("AAAAAAAAAAA err " + err.getMessage());
                return null;
            } finally {
                if (Optional.ofNullable(profiler).map(ProfilerService::isStarted).orElse(false)) {
                    profiler.fixTimeCounter();
                }
            }
    }
*/
}
