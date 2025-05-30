package io.devjoy.operator;


import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

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
