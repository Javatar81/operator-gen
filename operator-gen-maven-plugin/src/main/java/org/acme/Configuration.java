package org.acme;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.microprofile.config.Config;

public class Configuration {
	
	private final Config config;
	private final Map<String, String> pathParamMappings = new HashMap<>();
	private final Map<String, String> pathResponseMappings = new HashMap<>();
	private final Properties crdCustomizations;
	
	public Configuration(Config config, List<String> pathParamMappings, List<String> pathResponseMappings, Properties crdCustomizations) {
		this.config = config;
		this.crdCustomizations = crdCustomizations;
		pathParamMappings.stream()
			.map(s -> s.split("="))
			.filter(a -> a.length > 1)
			.forEach(a -> this.pathParamMappings.put(a[0].trim(), a[1].trim()));
		pathResponseMappings.stream()
			.map(s -> s.split("="))
			.filter(a -> a.length > 1)
			.forEach(a -> this.pathResponseMappings.put(a[0].trim(), a[1].trim()));
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
	
	public Map<String, String> getPathResponseMappings() {
		return Collections.unmodifiableMap(this.pathResponseMappings);
	}

	public Config getConfig() {
		return config;
	}
	
	public List<String> getIgnoreProps() {
		if (crdCustomizations.get("ignoreProps") != null) {
			String ignoreProps = crdCustomizations.get("ignoreProps").toString();
			return Arrays.asList(ignoreProps.split(","));
		} else {
			return Collections.emptyList();
		}
	}

}
