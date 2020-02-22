package org.springframework.samples.petclinic.customers;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.samples.petclinic.opencensus.TracingIncomingInterceptor;
import org.springframework.samples.petclinic.opencensus.TracingInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    TracingIncomingInterceptor tracingIncomingInterceptor =  new TracingIncomingInterceptor();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tracingIncomingInterceptor);
    }

    @Bean
    @LoadBalanced
    RestTemplate loadBalancedRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (CollectionUtils.isEmpty(interceptors)) {
            interceptors = new ArrayList<>();
        }
        interceptors.add(new TracingInterceptor());
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
