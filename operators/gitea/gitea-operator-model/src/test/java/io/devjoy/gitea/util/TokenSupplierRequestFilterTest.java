package io.devjoy.gitea.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.net.URISyntaxException;
import java.util.Base64;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.gitea_json.api.UserApi;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkiverse.wiremock.devservice.WireMockConfigKey;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
@ConnectWireMock
class TokenSupplierRequestFilterTest {
	
	WireMock wiremock;
	@ConfigProperty(name = WireMockConfigKey.PORT)
    Integer port;
	
	
	@Test
	void authAccessTokenSuccess() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
        wiremock.register(get(urlPathEqualTo("/api/v1/users/myuser"))
        		.withHeader("Authorization", matching("token adsfagag"))
        		.willReturn(aResponse()
        				.withHeader("content-type", "application/json")
        				.withStatus(200)
        				.withBody("{}")));
        RestClientBuilder.newBuilder()
			.baseUri(new URIBuilder("http://localhost:" + port).setPath("/api/v1").build())
			.register(AuthorizationRequestFilter.accessToken("adsfagag"))
			.build(UserApi.class).userGet("myuser");
	}
	
	@Test
	void authBasicAuthSuccess() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
        String username = "test";
        String password = "password";
		String encoded = Base64.getEncoder()
				.encodeToString(String.format("%s:%s", username, password).getBytes());
		
		wiremock.register(get(urlPathEqualTo("/api/v1/users/myuser"))
        		.withHeader("Authorization", matching("Basic " + encoded))
        		.willReturn(aResponse()
        				.withHeader("content-type", "application/json")
        				.withStatus(200)
        				.withBody("{}")));
        RestClientBuilder.newBuilder()
			.baseUri(new URIBuilder("http://localhost:" + port).setPath("/api/v1").build())
			.register(AuthorizationRequestFilter.basicAuth(username, password))
			.build(UserApi.class).userGet("myuser");
	}
}
