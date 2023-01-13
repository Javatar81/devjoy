package io.devjoy.operator.repository.domain;

import java.util.List;

public interface UserService {

	void createUser(String username, String email, String token, String baseUri);
	List<String> getAllUsernames(String token, String baseUri);
}
