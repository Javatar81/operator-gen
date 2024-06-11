package org.operator.gen.v1alpha1;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@Dependent
public class AuthHeaderClientProvider implements WebClientProvider {
	@Inject()
	Vertx vertx;

	@ConfigProperty(name = "gitea.api.token")
	private String token;
	

	@Override
	public WebClient provide() {
		WebClientSession webClientSession = WebClientSession.create(WebClient.create(vertx));
		webClientSession.addHeader("Authorization", "token " + token);
		return webClientSession;
	}

}
