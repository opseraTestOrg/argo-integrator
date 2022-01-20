package com.opsera.integrator.argo.log;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.opsera.integrator.argo.config.IServiceFactory;

/**
 * The Class LoggingAspect.
 */
@Aspect
@Component
public class LoggingAspect {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    /** The Constant CORRELATION_ID_HEADER_NAME. */
    private static final String CORRELATION_ID_HEADER_NAME = "X-Correlation-Id";

    /** The Constant CORRELATION_ID_LOG_VAR_NAME. */
    private static final String CORRELATION_ID_LOG_VAR_NAME = "correlation_id";

    /** The environment. */
    @Autowired
    private Environment environment;

    /** The service factory. */
    @Autowired
    IServiceFactory serviceFactory;

    /**
     * Profile all methods.
     *
     * @param proceedingJoinPoint the proceeding join point
     * @return the object
     * @throws Throwable the throwable
     */
    // AOP expression for which methods shall be intercepted
    @Around("execution(* com.opsera.integrator.argo.controller..*(..)))")
    @AfterThrowing(pointcut = "execution(* com.opsera.integrator.argo.controller..*(..)))", throwing = "ex")
    public Object profileAllMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        final String correlationId = getCorrelationIdFromHeader(request);
        MDC.put(CORRELATION_ID_LOG_VAR_NAME, correlationId);
        MDC.put("request_uri", request.getRequestURI());
        MDC.put("component", "Microservice");
        String payload = getPayload(proceedingJoinPoint);
        MDC.put("service", environment.getProperty("spring.application.name"));
        MDC.put("request", payload);
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();
        MDC.put("function", className.concat(".").concat(methodName));
        final StopWatch stopWatch = new StopWatch();

        // Measure method execution time
        stopWatch.start();
        Object result = proceedingJoinPoint.proceed();
        MDC.put("response", serviceFactory.gson().toJson(result).toString());
        stopWatch.stop();

        // Log method execution time
        LOGGER.info("Execution time of {}.{} :: {} ms", className, methodName, stopWatch.getTotalTimeMillis());

        return result;
    }

    /**
     * Log after throwing all methods.
     *
     * @param ex the ex
     * @throws Throwable the throwable
     */
    @AfterThrowing(pointcut = "execution(* com.opsera.integrator.argo.controller..*(..)))", throwing = "ex")
    public void logAfterThrowingAllMethods(Exception ex) throws Throwable {
        MDC.put("exception", ex.toString());
    }

    /**
     * Gets the correlation id from header.
     *
     * @param request the request
     * @return the correlation id from header
     */
    private String getCorrelationIdFromHeader(final HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER_NAME);
        if (StringUtils.isBlank(correlationId)) {
            correlationId = generateUniqueCorrelationId();
            LOGGER.info("No correlationId found in Header. Generated : " + correlationId);
        } else {
            LOGGER.info("Found correlationId in Header : " + correlationId);
        }
        return correlationId;
    }

    /**
     * Generate unique correlation id.
     *
     * @return the string
     */
    private String generateUniqueCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets the payload.
     *
     * @param joinPoint the join point
     * @return the payload
     */
    private String getPayload(JoinPoint joinPoint) {
        CodeSignature signature = (CodeSignature) joinPoint.getSignature();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < joinPoint.getArgs().length; i++) {
            String parameterName = signature.getParameterNames()[i];
            builder.append(parameterName);
            builder.append(": ");
            builder.append(joinPoint.getArgs()[i].toString());
            builder.append(", ");
        }
        return builder.toString();
    }

}
