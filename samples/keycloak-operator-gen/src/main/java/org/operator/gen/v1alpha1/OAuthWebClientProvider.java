package org.operator.gen.v1alpha1;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.Oauth2Credentials;
import io.vertx.ext.web.client.OAuth2WebClient;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class OAuthWebClientProvider implements WebClientProvider {
	@Inject
	Vertx vertx;

	String tokenPath = "/realms/master/protocol/openid-connect/token";

	@ConfigProperty(name = "keycloak.api.uri")
	String keycloakApiUri;

	@Override
	public WebClient provide() {
		OAuth2Options options = new OAuth2Options().setFlow(OAuth2FlowType.PASSWORD).setClientId("admin-cli")
						.setTokenPath(keycloakApiUri + tokenPath);
		OAuth2Auth oAuth2Auth = OAuth2Auth.create(vertx, options);
		Oauth2Credentials oauth2Credentials = new Oauth2Credentials();
		oauth2Credentials.setUsername("admin");
		oauth2Credentials.setPassword("admin");
		return OAuth2WebClient.create(WebClient.create(vertx), oAuth2Auth)
				.withCredentials(oauth2Credentials);
	}

}
