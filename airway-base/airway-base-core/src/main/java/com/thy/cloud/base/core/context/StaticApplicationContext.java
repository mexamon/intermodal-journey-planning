package com.thy.cloud.base.core.context;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @classname SpringContextHolder
 * @description Spring tool class
 *
 * @author Engin Mahmut
 *
 */

@Slf4j
@Component
@Service
@Lazy(false)
public class StaticApplicationContext implements ApplicationContextAware, DisposableBean {

    private static ApplicationContext context;

    /**
     * Implement the ApplicationContextAware interface and inject Context into static variables.
     */
    @Override
    public void setApplicationContext(@Nullable final ApplicationContext context) {
        StaticApplicationContext.context = context;
    }

    /**
     * Get the ApplicationContext stored in a static variable.
     */
    public static ApplicationContext getApplicationContext() {
        return context;
    }


    /**
     * Tries to autowire the specified instance of the class if one of the specified
     * beans which need to be autowired are null.
     *
     * @param classToAutowire        the instance of the class which holds @Autowire
     *                               annotations
     * @param beansToAutowireInClass the beans which have the @Autowire annotation
     *                               in the specified {#classToAutowire}
     */
    public static void autowire(Object classToAutowire, Object... beansToAutowireInClass) {
        for (Object bean : beansToAutowireInClass) {
            if (bean == null) {
                context.getAutowireCapableBeanFactory().autowireBean(classToAutowire);
            }
        }
    }

    /**
     * Clear ApplicationContext in SpringContextHolder to Null.
     */
    public static void clearHolder()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Clear ApplicationContext in SpringContextHolder:" + context);
        }
        context = null;
    }

    /**
     * Post event
     *
     * @param event
     */
    public static void publishEvent(ApplicationEvent event)
    {
        if (context == null)
        {
            return;
        }
        context.publishEvent(event);
    }

    /**
     * Implement the DisposableBean interface and clean up static variables when the Context is closed.
     */
    @Override
    @SneakyThrows
    public void destroy()
    {
        StaticApplicationContext.clearHolder();
    }

}