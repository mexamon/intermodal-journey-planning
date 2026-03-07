package com.thy.cloud.base.core.localization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.List;

/**
 * LocaleResolverInterceptor for Rest Request Language Header
 *
 * @author Engin Mahmut
 */
@Configuration
public class LocaleResolverInterceptor implements WebMvcConfigurer {

    /**
     * SessionLocaleResolver with a default locale as US
     * @return Default locale
     */
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
/*        sessionLocaleResolver.setDefaultLocale(Locale.US);*/
        return sessionLocaleResolver;
    }

   /* @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
        cookieLocaleResolver.setCookieName("NG_TRANSLATE_LANG_KEY");
        return cookieLocaleResolver;
    }*/

    /**
     * Interceptor configuration
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        return localeChangeInterceptor;
    }

    /**
     * Add LocaleChangeInterceptor registry
     */
/*    @Bean
    public RequestMappingHandlerMapping requestMappingHandlerMapping() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
        Object[] interceptors = {localeChangeInterceptor()};
        requestMappingHandlerMapping.setInterceptors(interceptors);
        return requestMappingHandlerMapping;
    }*/

    /**
     * Add LocaleChangeInterceptor registry
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor()).addPathPatterns("/**");
    }

   /* @Bean
    public AcceptHeaderLocaleResolver localeResolver(WebMvcProperties mvcProperties) {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver() {
            @Override
            public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
                LocaleContextHolder.setLocale(locale);
            }
        };

        localeResolver.setDefaultLocale(mvcProperties.getLocale());
        return localeResolver;
    }*/

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add( new PageableHandlerMethodArgumentResolver());
    }
}
