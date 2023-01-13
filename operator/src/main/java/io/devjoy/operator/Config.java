package io.devjoy.operator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;

/**
 * 
 * Central configuration bean
 *
 */
@ApplicationScoped
public class Config {

	private static final String TOKEN_NAME = "devjoy";
	private static final String USER_DEFAULT_PASSWORD = "devjoy";

	@Produces
    public TektonClient tektonClient(){
        return new DefaultTektonClient();
    }
	
	public String getDefaultUserPassword(String userName) {
		return USER_DEFAULT_PASSWORD;
	}
	
	public String getUserTokenName(String userName) {
		return TOKEN_NAME;
	}

}
