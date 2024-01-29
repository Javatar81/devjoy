package io.devjoy.gitea.domain.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.model.CreateUserOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.Command;
import io.devjoy.gitea.domain.Option;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConditionType;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserService {
	private static final String OPTION_USERNAME = "username";
	private static final String OPTION_PASSWORD = "password";
	private static final String ARG_USER = "user";
	private static final String ARG_ADMIN = "admin";
	private static final String ADMIN_COMMAND = "/usr/bin/giteacmd";
	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
	private final GiteaPodExecService execService;

	public UserService(GiteaPodExecService execService) {
		this.execService = execService;
	}
	
	public boolean adminExists(String baseUri, String adminUser) {
		try {
			return getDynamicUrlClient(baseUri, AdminApi.class).adminGetAllUsers(null, null).stream()
				.anyMatch(u -> adminUser.equals(u.getLoginName()) && u.getIsAdmin());
		} catch (URISyntaxException e) {
			throw new ServiceException("Error creating admin via api", e, GiteaConditionType.GITEA_ADMIN_CREATED);
		}
	}
	
	public Optional<String> getUserId(Gitea gitea, String userName) {
		Command cmd = Command.builder()
				.withExecutable(ADMIN_COMMAND)
				.withArgs(List.of(ARG_ADMIN, ARG_USER, "list")).build();
			
		Pattern pattern = Pattern.compile(String.format("(\\d+)\\s+%s.*", userName));
		
		return execService.execOnDeployment(gitea, cmd)
				.map(pattern::matcher)
				.filter(Matcher::find)
				.map(m -> m.group(1));
	}
	
	public Optional<String> getAdminId(Gitea gitea) {
		return getUserId(gitea, gitea.getSpec().getAdminUser());
	}
	
	public void deleteAdminUserViaExec(Gitea gitea) {
		LOG.info("Waiting up to 180 seconds for replicas to become ready....");
		Command cmd = Command.builder()
			.withExecutable(ADMIN_COMMAND)
			.withArgs(List.of(ARG_ADMIN, ARG_USER, "delete"))
			.addOption(new Option(OPTION_USERNAME, gitea.getSpec().getAdminUser()))
			.build();
					
		execService.execOnDeployment(gitea, cmd);
	}

	public void changeUserPasswordViaExec(Gitea gitea, String user, String password) {
		LOG.info("Waiting up to 180 seconds for replicas to become ready....");
		Command cmd = Command.builder()
			.withExecutable(ADMIN_COMMAND)
			.withArgs(List.of(ARG_ADMIN, ARG_USER, "change-password"))
			.addOption(new Option(OPTION_USERNAME, user))
			.addOption(new Option(OPTION_PASSWORD, password))
			.build();
					
		execService.execOnDeployment(gitea, cmd);
	}
	
	public Optional<String> createUserViaExec(Gitea gitea, String userName) {
		LOG.info("Waiting up to {} seconds for replicas to become ready....", 180);
		Command cmd = Command.builder()
				.withExecutable(ADMIN_COMMAND)
				.withArgs(List.of(ARG_ADMIN, ARG_USER, "create"))
				.addOption(new Option(OPTION_USERNAME, userName))
				.addOption(new Option("password", "devjoy"))
				.addOption(new Option("email", userName + "@example.com"))
				// If the user must change password, token will be invalid, hence must change is set to false
				.addOption(new Option("must-change-password=false", ""))
				.build();
		return execService.execOnDeployment(gitea, cmd);
	}
	
	public void createAdminUserViaExec(Gitea gitea) {
		try {
			LOG.info("Waiting up to {} seconds for replicas to become ready....", 180);
			Command cmd = Command.builder()
					.withExecutable(ADMIN_COMMAND)
					.withArgs(List.of(ARG_ADMIN, ARG_USER, "create"))
					.addOption(new Option(OPTION_USERNAME, gitea.getSpec().getAdminUser()))
					.addOption(new Option("password", gitea.getSpec().getAdminPassword()))
					.addOption(new Option("email", gitea.getSpec().getAdminEmail()))
					.addOption(new Option("must-change-password=false", ""))
					.addOption(new Option(ARG_ADMIN, ""))
					.build();
			execService.execOnDeployment(gitea, cmd);
			gitea.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(gitea.getStatus().getObservedGeneration())
					.withType(GiteaConditionType.GITEA_ADMIN_CREATED.toString())
					.withMessage(String.format("Admin %s has been created", gitea.getSpec().getAdminUser()))
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(String.format("Expected admin %s did not exist", gitea.getSpec().getAdminUser()))
					.withStatus("True")
					.build()); 
		} catch (Exception e) {
			throw new ServiceException("Error creating admin user", e, GiteaConditionType.GITEA_ADMIN_CREATED);
		}
	}

	public void createAdminUser(String baseUri, String adminUser, String adminEmail, String adminPassword) {
		CreateUserOption body = new CreateUserOption();
		body.setUsername(adminUser);
		body.setEmail(adminEmail);
		body.setPassword(adminPassword);
		try {
			getDynamicUrlClient(baseUri, AdminApi.class).adminCreateUser(body);
		} catch (URISyntaxException e) {
			throw new ServiceException("Error creating admin via api", e, GiteaConditionType.GITEA_ADMIN_CREATED);
		}
	}
	
	private <T> T getDynamicUrlClient(String baseUri, Class<T> clazz) throws URISyntaxException {
		return RestClientBuilder.newBuilder()
				.baseUri(new URI(baseUri + "/api/v1"))
				.build(clazz);
	}
}
