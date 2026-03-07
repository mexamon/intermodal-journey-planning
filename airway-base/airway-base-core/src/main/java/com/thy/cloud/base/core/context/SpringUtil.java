package com.thy.cloud.base.core.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Obtain the bean object in the Spring IOC container
 *
 * @author Engin Mahmut
 */
@Component
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@Nullable ApplicationContext applicationContext)  throws BeansException {
        SpringUtil.applicationContext = applicationContext;
    }

    /**
     * Obtain ApplicationContext
     *
     * @return ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        checkApplicationContext();
        return applicationContext;
    }

    /**
     * Get Bean by name
     *
     * @param <T> Bean type
     * @param name Bean name
     * @return Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        checkApplicationContext();
        return (T) applicationContext.getBean(name);
    }

    /**
     * Obtain Bean through class
     *
     * @param <T> Bean type
     * @param clazz Bean class
     * @return Bean object
     */
    public static <T> T getBean(Class<T> clazz) {
        checkApplicationContext();
        Map<String, T> beansMap = applicationContext.getBeansOfType(clazz);
        if(!beansMap.isEmpty()){
            return beansMap.values().iterator().next();
        }else {
            return null;
        }
    }

    /**
     * Return the specified Bean by name and Clazz
     *
     * @param <T> bean type
     * @param name Bean name
     * @param clazz bean type
     * @return Bean object
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return applicationContext.getBean(name, clazz);
    }

    /**
     *
     * @param clazz bean type
     * @param <T> bean type
     * @return Bean Provider
     */
    @Nullable
    public static <T> ObjectProvider<T> getBeanProvider(Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBeanProvider(clazz);
    }

    /**
     *
     * @param resolvableType ResolvableType
     * @param <T> bean type
     * @return Bean Provider
     */
    @Nullable
    public static <T> ObjectProvider<T> getBeanProvider(ResolvableType resolvableType) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBeanProvider(resolvableType);
    }

    /**
     * Check application context if null
     */
    private static void checkApplicationContext(){
        if(applicationContext==null){
            throw new IllegalStateException("applicationContext is null");
        }
    }

}
