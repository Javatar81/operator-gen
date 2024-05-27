package org.acme.read.crud;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;

public class ResponseTypeMapper implements CrudMapper {

	private static final String DO_NOT_MATCH = "%%%%";
	private static final String MEDIATYPE_JSON = "application/json";
	private final Schema schema;
	private final OpenAPI api;
	private String modelName;

	public ResponseTypeMapper(OpenAPI api, String modelName) {
		this.api = api;
		this.modelName = modelName;
		this.schema = getSchema(modelName)
				.orElseThrow(() -> new IllegalArgumentException("No schema found named " + modelName));
	}

	/**
	 * Currently only JSON responses are taken into account
	 * 
	 * @return
	 */
	public String getResponseMediaType() {
		return MEDIATYPE_JSON;
	}
	
	@Override
	public Schema getByIdSchema() {
		return schema;
	}
	
	public boolean isArrayType() {
		return Optional.ofNullable(schema.getType())
			.filter(t -> "array".equalsIgnoreCase(t.name()))
			.isPresent();
	}

	private Optional<Schema> getSchema(String modelName) {
		return Optional.ofNullable(api.getComponents().getResponses().get(modelName).getContent()
				.getMediaType(getResponseMediaType()).getSchema());
	}

	@Override
	public Optional<Entry<String, PathItem>> getByIdPath() {
		return byIdPath(this::matchGetResponse);
	}
	
	@Override
	public Optional<Schema> getCreateSchema() {
		return createPath()
				.map(Entry::getValue)
				.map(PathItem::getPOST)
				.map(Operation::getRequestBody)
				.map(RequestBody::getContent)
				.map(c -> c.getMediaType(MEDIATYPE_JSON))
				.map(m -> m.getSchema());
	}
	
	@Override
	public Optional<Schema> getUpdateSchema() {
		return patchPath()
				.map(Entry::getValue)
				.map(PathItem::getPATCH)
				.map(Operation::getRequestBody)
				.map(RequestBody::getContent)
				.map(c -> c.getMediaType(MEDIATYPE_JSON))
				.map(m -> m.getSchema());
	}

	
	private Optional<Entry<String, PathItem>> byIdPath(Predicate<Entry<String, PathItem>> filter) {
		return byIdPaths(filter).stream().findAny();
	}

	private Collection<Entry<String, PathItem>> byIdPaths(Predicate<Entry<String, PathItem>> filter) {
		List<Entry<String, PathItem>> potentialIdPaths = api.getPaths().getPathItems().entrySet().stream()
				.filter(filter).toList();
		if (potentialIdPaths.isEmpty()) {
			return Collections.emptyList();
		} else if (potentialIdPaths.size() == 1) {
			return potentialIdPaths;
		} else {
			return preferPathsContainingModelName(potentialIdPaths);
		}
	}

	private Collection<Entry<String, PathItem>> preferPathsContainingModelName(
			List<Entry<String, PathItem>> potentialIdPaths) {
		String pathSegmentContainingModelName = "/" + plural(modelName.toLowerCase());
		List<Entry<String, PathItem>> pathsContainingModelName = potentialIdPaths.stream()
				.filter(e -> e.getKey().contains(pathSegmentContainingModelName)).toList();
		if (pathsContainingModelName.isEmpty()) {
			return potentialIdPaths;
		} else {
			return pathsContainingModelName;
		}
	}

	private static String plural(String name) {
		if (!name.endsWith("s")) {
			return name + "s";
		} else {
			return name;
		}
	}

	@Override
	public Optional<Entry<String, PathItem>> deletePath() {
		return byIdPaths(this::matchGetResponse).stream().filter(e -> e.getValue().getDELETE() != null).findAny()
				.or(deriveDeleteFromCreatePath());

	}

	/**
	 * Assumes that if there is a POST e.g. /admin/users then there is a
	 * corresponding DELETE e.g. /admin/users/{username}. The path for the delete is
	 * assumed to start with that of the post. If there is more than one result,
	 * e.g. /admin/users/{username}/orgs and /admin/users/{username} the shortest
	 * path is returned. This is not perfect, but only for sake of simplicity. Might lead to
	 * wrong results in edge cases.
	 * 
	 * @return
	 */
	private Supplier<Optional<? extends Entry<String, PathItem>>> deriveDeleteFromCreatePath() {
		return () -> api.getPaths().getPathItems().entrySet().stream()
				.filter(p -> p.getKey().startsWith(createPath().map(Entry::getKey).orElse(DO_NOT_MATCH)))
				.filter(e -> e.getValue().getDELETE() != null)
				.sorted((e1, e2) -> Integer.compare(e1.getKey().length(), e1.getKey().length())).findFirst();
	}

	@Override
	public Optional<Entry<String, PathItem>> createPath() {
		List<String> parentOfIdPaths = parentPaths(byIdPaths(this::matchGetResponse));
		return api.getPaths().getPathItems().entrySet().stream().filter(e -> parentOfIdPaths.contains(e.getKey()))
				.filter(e -> e.getValue().getPOST() != null).findFirst()
				.or(() -> {
					List<String> parentOfPatchPaths = parentPaths(byIdPaths(this::matchPatchResponse));
					return api.getPaths().getPathItems().entrySet().stream()
							.filter(e -> parentOfPatchPaths.contains(e.getKey())).filter(e -> e.getValue().getPOST() != null)
							.findAny();
				})
				.or(() -> 
					api.getPaths().getPathItems().entrySet().stream().filter(this::matchPostResponse).findAny()

				);
	}

	@Override
	public Optional<Entry<String, PathItem>> patchPath() {
		Optional<Entry<String, PathItem>> patchPath = api.getPaths().getPathItems().entrySet().stream().filter(this::matchGetResponse)
			.filter(e -> e.getValue().getPATCH() != null).findFirst();
		return patchPath.or(() -> deletePath().filter(p -> p.getValue().getPATCH() != null));		
	}

	private List<String> parentPaths(Collection<Entry<String, PathItem>> paths) {
		return paths.stream().map(Entry::getKey).map(this::dropLastPathSegment).toList();
	}

	private String dropLastPathSegment(String path) {
		String[] pathSegments = path.split("/");
		String lastSegement = pathSegments[pathSegments.length - 1];
		return path.replace("/" + lastSegement, "");
	}

	private boolean matchGetResponse(Entry<String, PathItem> e) {
		return e.getValue().getGET() != null && e.getValue().getGET().getResponses().getAPIResponse("200") != null
				&& e.getValue().getGET().getResponses().getAPIResponse("200").getContent()
						.getMediaType(getResponseMediaType()) != null
				&& schema.getRef() != null
				&& Objects.equals(schema.getRef(), e.getValue().getGET().getResponses().getAPIResponse("200")
						.getContent().getMediaType(getResponseMediaType()).getSchema().getRef());
	}

	private boolean matchPatchResponse(Entry<String, PathItem> e) {
		return e.getValue().getPATCH() != null && e.getValue().getPATCH().getResponses().getAPIResponse("200") != null
				&& e.getValue().getPATCH().getResponses().getAPIResponse("200").getContent()
						.getMediaType(getResponseMediaType()) != null
				&& schema.getRef() != null
				&& Objects.equals(schema.getRef(), e.getValue().getPATCH().getResponses().getAPIResponse("200")
						.getContent().getMediaType(getResponseMediaType()).getSchema().getRef());
	}
	
	private boolean matchPostResponse(Entry<String, PathItem> e) {
		return e.getValue().getPOST() != null && e.getValue().getPOST().getResponses().getAPIResponse("201") != null
				&& e.getValue().getPOST().getResponses().getAPIResponse("201").getContent()
						.getMediaType(getResponseMediaType()) != null
				&& schema.getRef() != null
				&& Objects.equals(schema.getRef(), e.getValue().getPOST().getResponses().getAPIResponse("201")
						.getContent().getMediaType(getResponseMediaType()).getSchema().getRef());
	}
	

}
