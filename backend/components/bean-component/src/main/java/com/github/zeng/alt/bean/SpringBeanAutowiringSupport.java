package com.github.zeng.alt.bean;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

public class SpringBeanAutowiringSupport {


    private static AutowireCapableBeanFactory beanFactory;


    public SpringBeanAutowiringSupport(
            AutowireCapableBeanFactory factory
    ) {
        SpringBeanAutowiringSupport.beanFactory = factory;
    }


    public static void autowire(Object bean) {
        beanFactory.autowireBean(bean);
    }
}