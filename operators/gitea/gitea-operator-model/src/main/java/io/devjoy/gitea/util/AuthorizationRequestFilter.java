package io.devjoy.gitea.util;

import java.io.IOException;
import java.util.Base64;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

@Priority(Priorities.AUTHENTICATION)
public class AuthorizationRequestFilter implements ClientRequestFilter {
    
	Supplier<String> valueSupplier;
	
	public AuthorizationRequestFilter(Supplier<String> valueSupplier) {
		this.valueSupplier = valueSupplier;
	}
	
	@Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, getValue());
    }

    private String getValue() {
        return valueSupplier.get();
    }
    
    public static AuthorizationRequestFilter accessToken(String token) {
    	return new AuthorizationRequestFilter(() -> "token " + token);
    }
    
    public static AuthorizationRequestFilter basicAuth(String username, String password) {
    	return new AuthorizationRequestFilter(() -> "Basic "+ Base64.getEncoder()
    			.encodeToString(String.format("%s:%s", username, password).getBytes()));
    }
}
