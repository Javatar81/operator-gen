package org.acme;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.Config;

public class Configuration {
	
	private final Config config;
	private final Map<String, String> pathParamMappings = new HashMap<>();
	
	public Configuration(Config config, List<String> pathParamMappings) {
		this.config = config;
		pathParamMappings.stream()
			.map(s -> s.split("="))
			.filter(a -> a.length > 1)
			.forEach(a -> this.pathParamMappings.put(a[0].trim(), a[1].trim()));
	}

	public String getCrdVersion() {
		return config.getValue("quarkus.operator-sdk.operator-gen.crd.version", String.class);
	}
	
	public String getCrdPackage() {
		return config.getValue("quarkus.operator-sdk.operator-gen.crd.package", String.class);
	}
	
	public List<String> getResponses() {
		return config.getValues("quarkus.operator-sdk.operator-gen.api.responses", String.class);
	}
	
	public Map<String, String> getPathParamMappings() {
		return Collections.unmodifiableMap(this.pathParamMappings);
	}

	public Config getConfig() {
		return config;
	}

}
