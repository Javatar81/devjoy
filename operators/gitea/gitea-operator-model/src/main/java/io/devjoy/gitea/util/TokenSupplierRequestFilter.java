package io.devjoy.gitea.util;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

@Priority(Priorities.AUTHENTICATION)
public class TokenSupplierRequestFilter implements ClientRequestFilter {
    
	Supplier<String> tokenSupplier;
	
	public TokenSupplierRequestFilter(Supplier<String> tokenSupplier) {
		this.tokenSupplier = tokenSupplier;
	}
	
	@Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, getAccessToken());
    }

    private String getAccessToken() {
        return "token " + tokenSupplier.get();
    }
}
