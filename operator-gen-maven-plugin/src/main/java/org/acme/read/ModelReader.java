package org.acme.read;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.OpenAPI;

public class ModelReader {
	
	private final OpenAPI api;
	private boolean readSchemas = true; 

	public ModelReader(OpenAPI api) {
		super();
		this.api = api;
	}
	
	public void setReadSchemas(boolean readSchemas) {
		this.readSchemas = readSchemas;
	}

	public Stream<String> getResponseTypeOrSchemaNames(Predicate<Entry<String, ?>> filter) {
		Set<String> union = new HashSet<>();
		Stream<String> schemaNames = getSchemaNames(filter);
		if (api.getComponents().getResponses() != null && !api.getComponents().getResponses().isEmpty()) {
			getResponseTypeNames(filter).forEach(union::add);
		} 
		if (readSchemas) {
			schemaNames.forEach(union::add);
		}
		return union.stream();
	}
	
	public Stream<String> getResponseTypeNames() {
		return getResponseTypeNames(e -> true);
	}
	
	public Stream<String> getSchemaNames() {
		return getSchemaNames(e -> true);
	}
	
	private Stream<String> getResponseTypeNames(Predicate<Entry<String, ?>> filter) {
		if (api.getComponents().getResponses() == null) {
			return Stream.empty();
		} else {
			return api.getComponents().getResponses().entrySet().stream().filter(filter).map(Entry::getKey);
		}
		
	}
	
	private Stream<String> getSchemaNames(Predicate<Entry<String, ?>> filter) {
		if (api.getComponents().getSchemas() == null) {
			return Stream.empty();
		} else {
			return api.getComponents().getSchemas().entrySet().stream().filter(filter).map(Entry::getKey);
		}
	}
	
}
