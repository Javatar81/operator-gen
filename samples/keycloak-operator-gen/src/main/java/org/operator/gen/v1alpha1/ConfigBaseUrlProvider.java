package org.operator.gen.v1alpha1;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.microsoft.kiota.RequestAdapter;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.Dependent;

@DefaultBean
@Dependent
public class ConfigBaseUrlProvider implements BaseUrlProvider{
	
	@ConfigProperty(name = "keycloak.api.uri")
	String keycloakApiUri;
	
	@Override
	public void provide(RequestAdapter requestAdapter) {
		requestAdapter.setBaseUrl(keycloakApiUri);
	}
}
