package io.devjoy.operator.repository.domain;

import java.util.Optional;

public interface RepositoryService {

	Optional<Repository> getByUserAndName(String user, String name, String token, String baseUri);
	Repository create(Repository repository, String token, String baseUri);
	void delete(Repository repository, String token, String baseUri);
}
