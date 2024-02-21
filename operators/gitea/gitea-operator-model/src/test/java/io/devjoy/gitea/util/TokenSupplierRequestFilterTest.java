package io.devjoy.gitea.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;

import java.net.URISyntaxException;

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
	void authSuccess() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
        wiremock.register(get(urlPathEqualTo("/api/v1/users/myuser"))
        		.withHeader("Authorization", matching("token adsfagag"))
        		.willReturn(aResponse()
        				.withHeader("content-type", "application/json")
        				.withStatus(200)
        				.withBody("{}")));
        RestClientBuilder.newBuilder()
			.baseUri(new URIBuilder("http://localhost:" + port).setPath("/api/v1").build())
			.register(new TokenSupplierRequestFilter(() -> "adsfagag"))
			.build(UserApi.class).userGet("myuser");
	}
}
