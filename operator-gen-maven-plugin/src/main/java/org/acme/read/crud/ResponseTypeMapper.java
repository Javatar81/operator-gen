package org.acme.read.crud;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.util.StringUtil;

public class ResponseTypeMapper implements CrudMapper {

	private static final String DO_NOT_MATCH = "%%%%";
	private static final String MEDIATYPE_JSON = "application/json";
	private static final String MEDIATYPE_TEXT_HTML = "text/html";
	private static final String SCHEMA_PATH = "#/components/schemas/";
	private static final Logger LOG = LoggerFactory.getLogger(ResponseTypeMapper.class);
	
	private final Schema schema;
	private final String  schemaName;
	private final OpenAPI api;
	private String modelName;

	public ResponseTypeMapper(OpenAPI api, String modelName) {
		this.api = api;
		this.modelName = modelName;
		Optional<Schema> schemaFromResponses = getSchemaFromResponses(modelName);
		this.schemaName = schemaFromResponses.map(s -> resolveSchemaName(s)).orElse(modelName);
		this.schema =  schemaFromResponses.or(() -> Optional.ofNullable(api.getComponents().getSchemas())
				.map(r -> r.get(modelName))).orElseThrow(() -> new IllegalArgumentException("No schema found named " + modelName));
	}

	/**
	 * Currently only JSON responses are taken into account
	 * 
	 * @return
	 */
	public String getResponseMediaType() {
		return MEDIATYPE_JSON;
	}
	
	public String[] getResponseMediaTypes() {
		return new String[] {MEDIATYPE_JSON, MEDIATYPE_TEXT_HTML};
	}
	
	@Override
	public Schema getByIdSchema() {
		return schema;
	}
	
	public String getByIdSchemaName() {
		return schemaName;
	}
	
	public boolean isArrayType() {
		return isArrayType(this.schema);
	}
	
	private static boolean isArrayType(Schema schema) {
		return Optional.ofNullable(schema.getType())
			.filter(t -> "array".equalsIgnoreCase(t.name()))
			.isPresent();
	}

	private Optional<Schema> getSchemaFromResponses(String modelName) {
		return Optional.ofNullable(api.getComponents().getResponses())
				.map(r -> r.get(modelName))
				.map(r -> r.getContent()) 
				.map(c -> c.getMediaType(getResponseMediaType()))
				.map(m -> m.getSchema());
	}
	
	public static String resolveSchemaName(Schema schema) {
		if (isArrayType(schema)) {
			if (schema.getItems().getRef() != null) {
				return schema.getItems().getRef().substring(SCHEMA_PATH.length());
			} else {
				return schema.getItems().getType().name();
			}
		} else {
			return schema.getRef().substring(SCHEMA_PATH.length());
		}
		
	}
	
	private Optional<Schema> resolveRef(Schema proxy) {
		if (StringUtil.isNullOrEmpty(proxy.getRef())){
			return Optional.of(proxy);
		} else {
			
			if (proxy.getRef().startsWith(SCHEMA_PATH)) {
				return Optional.ofNullable(api.getComponents().getSchemas().get(proxy.getRef().substring(SCHEMA_PATH.length())));
			} else {
				return Optional.empty();
			}
		}
	}

	/**
	 * Searches for a GET operation that returns the schema as response. 
	 * Prefers prefer the path that contains the schema name.
	 */
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
	public Optional<Schema> getPatchSchema() {
		return patchPath()
				.map(Entry::getValue)
				.map(PathItem::getPATCH)
				.map(Operation::getRequestBody)
				.map(RequestBody::getContent)
				.map(c -> c.getMediaType(MEDIATYPE_JSON))
				.map(m -> m.getSchema());
	}
	
	@Override
	public Optional<Schema> getPutSchema() {
		return putPath()
				.map(Entry::getValue)
				.map(PathItem::getPUT)
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
		LOG.debug("Found {} potential paths", potentialIdPaths.size());
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
		return parentIdPathWithPostOp()
				.or(this::parentPatchPathWithPostOp)
				.or(this::anyMatchingPostResponse);
	}

	private Optional<Entry<String, PathItem>> anyMatchingPostResponse() {
		return api.getPaths().getPathItems().entrySet().stream().filter(this::matchPostResponse).findAny();
	}

	private Optional<? extends Entry<String, PathItem>> parentPatchPathWithPostOp() {
		List<String> parentOfPatchPaths = parentPaths(byIdPaths(this::matchPatchResponse));
		return api.getPaths().getPathItems().entrySet().stream()
				.filter(e -> parentOfPatchPaths.contains(e.getKey())).filter(e -> e.getValue().getPOST() != null)
				.findAny();
	}

	private Optional<Entry<String, PathItem>> parentIdPathWithPostOp() {
		List<String> parentOfIdPaths = parentPaths(byIdPaths(this::matchGetResponse));
		return api.getPaths().getPathItems().entrySet().stream().filter(e -> parentOfIdPaths.contains(e.getKey()))
				.filter(e -> e.getValue().getPOST() != null)
				.findFirst();
	}

	@Override
	public Optional<Entry<String, PathItem>> patchPath() {
		LOG.debug("Trying to find patch path for {}", schemaName);
		Optional<Entry<String, PathItem>> patchPath = idPathWithPatchOp();
		patchPath.ifPresent(p -> LOG.debug("Found patch path by id path {}", p.getKey()));
		Optional<Entry<String, PathItem>> result = patchPath.or(() -> deletePath().filter(p -> p.getValue().getPATCH() != null))
				.or(() -> api.getPaths().getPathItems().entrySet().stream().filter(this::matchPatchResponse).findAny());	
		result.ifPresent(p -> LOG.debug("Found patch path by delete path {}", p.getKey()));
		return result;
	}
	
	@Override
	public Optional<Entry<String, PathItem>> putPath() {
		LOG.debug("Trying to find put path for {}", schemaName);
		Optional<Entry<String, PathItem>> putPath = idPathWithPatchOp();
		putPath.ifPresent(p -> LOG.debug("Found potential put path {}", p.getKey()));
		return putPath.or(() -> deletePath().filter(p -> p.getValue().getPUT() != null))
				.or(() -> api.getPaths().getPathItems().entrySet().stream().filter(this::matchPutResponse).findAny());		
	}

	private Optional<Entry<String, PathItem>> idPathWithPatchOp() {
		return api.getPaths().getPathItems().entrySet().stream().filter(this::matchGetResponse)
			.filter(e -> e.getValue().getPATCH() != null).findFirst();
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
		Function<Entry<String, PathItem>, Operation> getGET = i -> i.getValue().getGET();
		return matchResponse(e, getGET, "200");
	}

	private boolean matchPatchResponse(Entry<String, PathItem> e) {
		Function<Entry<String, PathItem>, Operation> getPOST = i -> i.getValue().getPATCH();
		return matchResponse(e, getPOST, "200");
	}
	
	private boolean matchPutResponse(Entry<String, PathItem> e) {
		Function<Entry<String, PathItem>, Operation> getPUT = i -> i.getValue().getPUT();
		return matchResponse(e, getPUT, "200");
	}
	
	private boolean matchPostResponse(Entry<String, PathItem> e) {
		Function<Entry<String, PathItem>, Operation> getPOST = i -> i.getValue().getPOST();
		return matchResponse(e, getPOST, "201") || matchResponse(e, getPOST, "200");
	}
	
	private boolean matchResponse(Entry<String, PathItem> e, Function<Entry<String, PathItem>, Operation> f, String response) {
		Operation op = f.apply(e);
		return  op != null && op.getResponses().getAPIResponse(response) != null
				&& op.getResponses().getAPIResponse(response).getContent() != null
				&& op.getResponses().getAPIResponse(response).getContent()
						.getMediaType(getResponseMediaType()) != null
				&& responseTypeSchemaMatches(op, response);
	}
	
	private boolean responseTypeSchemaMatches(Operation op, String response) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Matching {} with {}", SCHEMA_PATH + schemaName, op.getResponses().getAPIResponse(response)
				.getContent().getMediaType(getResponseMediaType()).getSchema().getRef());
		}
		return Objects.equals(SCHEMA_PATH + schemaName, op.getResponses().getAPIResponse(response)
				.getContent().getMediaType(getResponseMediaType()).getSchema().getRef());
	}
	
}
