package com.thy.cloud.base.core.enums;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enables Autowiring for Localized Enums with Bean Factory Post Processor
 *
 * @author Engin Mahmut
 *
 */
public class IEnumI18nBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final List<Class<? extends IEnumI18n<?>>> enumClasses = new ArrayList<>();

    @SafeVarargs
    public IEnumI18nBeanFactoryPostProcessor(Class<? extends IEnumI18n<?>>... enumClasses) {
        Collections.addAll(this.enumClasses, enumClasses);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        enumClasses.forEach(enumClass -> Arrays.stream(enumClass.getEnumConstants()).forEach(enumVal -> {
            BeanDefinition def = new AnnotatedGenericBeanDefinition(enumClass);
            def.setBeanClassName(enumClass.getName());
            def.setFactoryMethodName("valueOf");
            def.getConstructorArgumentValues().addGenericArgumentValue(enumVal.name());
            ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition(enumClass.getName() + "." + enumVal.name(), def);
        }));

    }
}
