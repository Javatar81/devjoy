package io.devjoy.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;

@ApplicationScoped
public class Config {

	@Produces
    public TektonClient tektonClient(){
        return new DefaultTektonClient();
    }

}
