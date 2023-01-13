package io.devjoy.operator.environment.k8s.domain;

import java.util.List;
import java.util.Optional;

import io.devjoy.operator.repository.domain.Token;

public interface TokenService {
	
	Optional<Token> createTokenForUser(String user, String password, String tokenName, String baseUri);

	void deleteTokenForUser(String user, String password, String tokenName, String baseUri);

	List<Token> getTokensByUser(String user, String password, String baseUri);
}
