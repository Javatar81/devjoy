package io.devjoy.operator.repository.domain;

import java.util.Optional;

public interface RepositoryService {

	public Repository create(Repository repository, String token);
	public void delete(Repository repository, String token);
	public Optional<Repository> getByUserAndName(String user, String name, String token);
}
