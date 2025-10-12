package com.smwu.matchalot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.reactive.ReactiveMailSender;
import org.springframework.mail.reactive.ReactiveMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    public ReactiveMailSender reactiveMailSender(JavaMailSender javaMailSender) {
        ReactiveMailSenderImpl mailSender = new ReactiveMailSenderImpl();
        mailSender.setJavaMailSender(javaMailSender);
        return mailSender;
    }
}