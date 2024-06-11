package org.operator.gen.v1alpha1;

import io.quarkus.arc.DefaultBean;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@DefaultBean
@Dependent
public class DefaultWebClientProvider implements WebClientProvider{
	@Inject()
	Vertx vertx;
	
	@Override
	public WebClient provide() {
		return WebClient.create(vertx);
	}

}
