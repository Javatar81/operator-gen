package org.acme.read.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.acme.read.ModelReader;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

@Disabled
public class ResponseTypeMapperApicurioTest {
	private static OpenAPI model;
	private static ModelReader reader;
	private static final Set<String> EXCLUDED_RESPONSES = Set.of("conflict", "empty", "error", "forbidden",
			"invalidTopicsError", "notFound", "parameterBodies", "redirect", "string", "validationError",
			"EmptyRepository" /* Only used for 409 response */,
			"FileDeleteResponse" /* Used for single operation to delete a file in a repository */,
			"FileResponse" /* Used for single operation to update a file in a repository */,
			"FilesResponse" /* Modify multiple files in a repository */
			);
	
	
	
	@BeforeAll
	public static void setUp() {
		try (InputStream is = ResponseTypeMapperGiteaTest.class.getClassLoader().getResourceAsStream("apicurio.json")) {
			try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
				OpenApiConfig openApiConfig = new OpenApiConfigImpl(ConfigProvider.getConfig());
				model = OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile);
				reader = new ModelReader(model);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
		}
	}

	@ParameterizedTest
	@MethodSource("responseTypesWithGetById")
	void getByIdPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> byIdPath = analyzer.getByIdPath();
			assertNotNull(byIdPath);
			assertTrue(byIdPath.isPresent());
			assertNotNull(byIdPath.get().getValue().getGET());
			System.out.println("GetById path for " + modelName + " is " + byIdPath.get().getKey());
			Schema schema = byIdPath.get().getValue().getGET().getResponses().getAPIResponse("200").getContent().getMediaType(analyzer.getResponseMediaType()).getSchema();
			assertEquals(analyzer.getByIdSchemaName(), ResponseTypeMapper.resolveSchemaName(schema));
		}
	}
	
	@ParameterizedTest
	@MethodSource("responseTypesWithDelete")
	void deletePath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> deletePath = analyzer.deletePath();
			assertNotNull(deletePath);
			assertTrue(deletePath.isPresent());
			assertNotNull(deletePath.get().getValue().getDELETE());
			System.out.println("Delete path for " + modelName + " is " + deletePath.get().getKey());
		}
	}
	
	@ParameterizedTest
	@MethodSource("responseTypesWithPatch")
	void putPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> putPath = analyzer.putPath();
			assertNotNull(putPath);
			assertTrue(putPath.isPresent());
			assertNotNull(putPath.get().getValue().getPUT());
			System.out.println("Patch path for " + modelName + " is " + putPath.get().getKey());
		}
	}
	
	@ParameterizedTest
	@MethodSource("responseTypesWithCreate")
	void createPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> createPath = analyzer.createPath();
			assertNotNull(createPath);
			assertTrue(createPath.isPresent());
			assertNotNull(createPath.get().getValue().getPOST());
			System.out.println("Create path for " + modelName + " is " + createPath.get().getKey());
		}
	}
	
	private static Stream<String> responseTypesWithGetById() {
		Set<String> noFindById = new HashSet<String>(EXCLUDED_RESPONSES);
		return reader.getResponseTypeOrSchemaNames(e -> !noFindById.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithPatch() {
		Set<String> noPatch = new HashSet<String>(EXCLUDED_RESPONSES);
		return reader.getResponseTypeOrSchemaNames(e -> !noPatch.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithCreate() {
		Set<String> noCreate = new HashSet<String>(EXCLUDED_RESPONSES);
		return reader.getResponseTypeOrSchemaNames(e -> !noCreate.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithDelete() {
		Set<String> noDelete = new HashSet<String>(EXCLUDED_RESPONSES);
		return reader.getResponseTypeOrSchemaNames(e -> !noDelete.contains(e.getKey()));
	}
}
