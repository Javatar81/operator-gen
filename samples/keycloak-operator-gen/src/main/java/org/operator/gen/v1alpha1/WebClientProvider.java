package org.operator.gen.v1alpha1;

import io.vertx.ext.web.client.WebClient;

public interface WebClientProvider {
	
	WebClient provide();
	
}
