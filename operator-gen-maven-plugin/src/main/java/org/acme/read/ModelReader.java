package org.acme.read;

import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.models.OpenAPI;

public class ModelReader {
	
	private final OpenAPI api;

	public ModelReader(OpenAPI api) {
		super();
		this.api = api;
	}
	
	public Stream<String> getResponseTypeOrSchemaNames(Predicate<Entry<String, ?>> filter) {
		if (api.getComponents().getResponses() != null && !api.getComponents().getResponses().isEmpty()) {
			return getResponseTypeNames(filter);
		} else {
			return getSchemaNames(filter);
		}
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
