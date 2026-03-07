package com.thy.cloud.service.api;

import com.thy.cloud.data.jpa.constant.PersistenceConstants;
import com.thy.cloud.data.jpa.repository.factory.GenericRepositoryFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan("com.thy.cloud")
@EntityScan(basePackages = {
        "com.thy.cloud.service.dao.entity",
        "com.thy.cloud.data.jpa.entity"
})
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
        PersistenceConstants.BASE_REPOSITORY_PACKAGE,
        "com.thy.cloud.service.dao.repository"
}, repositoryFactoryBeanClass = GenericRepositoryFactoryBean.class)
public class AirwaySrvApiApp {

    public static void main(String[] args) {
        SpringApplication.run(AirwaySrvApiApp.class, args);
    }
}
