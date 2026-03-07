package com.thy.cloud.base.core.localization;

import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Custom Localer Resolver
 *
 * @author Engin Mahmut
 */
public class CustomLocaleResolver extends AcceptHeaderLocaleResolver {


    List<Locale> locales = Arrays.asList(new Locale("en"),new Locale("tr"));


    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String headerLang = request.getHeader("Accept-Language");
        return headerLang == null || headerLang.isEmpty()
                ? Locale.getDefault()
                : Locale.lookup(Locale.LanguageRange.parse(headerLang), locales);
    }
}

    /* USAGE */

    /*@Bean
    public LocaleResolver sessionLocaleResolver() {
        return new CustomLocaleResolver();
    }*/
