package org.acme.read.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.acme.read.ResponseTypeReader;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

class ResponseTypeMapperTest {
	
	private static OpenAPI model;
	private static ResponseTypeReader reader;
	private static final Set<String> EXCLUDED_RESPONSES = Set.of("conflict", "empty", "error", "forbidden",
			"invalidTopicsError", "notFound", "parameterBodies", "redirect", "string", "validationError",
			"EmptyRepository" /* Only used for 409 response */,
			"FileDeleteResponse" /* Used for single operation to delete a file in a repository */,
			"FileResponse" /* Used for single operation to update a file in a repository */,
			"FilesResponse" /* Modify multiple files in a repository */
			);
	
	
	
	@BeforeAll
	public static void setUp() {
		try (InputStream is = ResponseTypeMapperTest.class.getClassLoader().getResourceAsStream("gitea.json")) {
			try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
				OpenApiConfig openApiConfig = new OpenApiConfigImpl(ConfigProvider.getConfig());
				model = OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile);
				reader = new ResponseTypeReader(model);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
		}
	}
	
	@Test
	public void test() {
		System.out.println(model.getComponents().getResponses().get("Organization")); 
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNodeTree;
		try {
			jsonNodeTree = objectMapper.readTree(ResponseTypeMapperTest.class.getClassLoader().getResourceAsStream("gitea.json"));
			String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree.get("components").get("schemas").get("Organization"));
			//System.out.println(new KubernetesSerialization().unmarshal(schemas).getClass());
			System.err.println(jsonAsYaml);
			JsonNode schema = jsonNodeTree.get("components").get("schemas").get("Organization");
			
			
			
			JSONSchemaPropsBuilder specBuilder = new JSONSchemaPropsBuilder().withType("object");
			schema.get("properties").fields().forEachRemaining(
					f -> specBuilder.addToProperties(f.getKey(), new JSONSchemaPropsBuilder().withType(f.getValue().get("type").asText()).build())
			);
			
			CustomResourceDefinitionBuilder defBuilder = new CustomResourceDefinitionBuilder();
			CustomResourceDefinition customResourceDefinition = defBuilder.editOrNewMetadata()
					.withName("Organization")
				.endMetadata()
				.editOrNewSpec()
					.withGroup("opgen.io")
					.withNewNames().withKind("Organization").endNames()
					.withScope("Namespaced")
					.withVersions(new CustomResourceDefinitionVersionBuilder()
							.withName("v1alpha1")
							.withServed(true)
							.withStorage(true)
							.withNewSchema()
							.editOrNewOpenAPIV3Schema()
							.addToProperties("status", new JSONSchemaPropsBuilder().withType("object").build())
							.addToProperties("spec", specBuilder.build())
							.endOpenAPIV3Schema()
							.and()
							.build())	
				.endSpec()
				.build();
			
			String myPodAsYaml = Serialization.asYaml(customResourceDefinition);
		System.out.println(myPodAsYaml);		
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
			assertEquals(analyzer.getByIdSchema().getRef(), schema.getRef());
		}
	}
	
	@ParameterizedTest
	@MethodSource("modelsToTest")
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
	void patchPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> patchPath = analyzer.patchPath();
			assertNotNull(patchPath);
			assertTrue(patchPath.isPresent());
			assertNotNull(patchPath.get().getValue().getPATCH());
			System.out.println("Patch path for " + modelName + " is " + patchPath.get().getKey());
		}
	}
	
	@ParameterizedTest
	@MethodSource("modelsToTest")
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
		noFindById.add("AccessToken" /* AccessToken cannot be read after it has been created */);
		noFindById.add("CommitStatus" /* Only a list of commit status can be retrieved */);
		noFindById.add("IssueDeadline" /* Issue deadline can only be created */);
		noFindById.add("LanguageStatistics" /* Only a list of LanguageStatistics can be retrieved */);
		noFindById.add("MarkdownRender" /* MarkdownRender can only be created */);
		noFindById.add("MarkupRender" /* MarkupRender can only be created */);
		noFindById.add("PullReviewComment" /* Only a list of comments can be retrieved */);
		noFindById.add("Reaction" /* Only a list of reactions can be retrieved */);
		noFindById.add("Reference" /* Although there is a findById path it returns a list of References */);
		noFindById.add("Secret" /* Secret can only be created */);
		noFindById.add("StopWatch" /* StopWatch can only be created and deleted */);
		noFindById.add("TrackedTime" /* Only a list of TrackedTime can be retrieved */);
		return reader.getResponseTypeNames(e -> !noFindById.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithPatch() {
		Set<String> noFindById = new HashSet<String>(EXCLUDED_RESPONSES);
		noFindById.add("AccessToken" /* AccessToken cannot be patched */);
		noFindById.add("ActivityPub" /* The post method has additional /index which is not expected */);
		noFindById.add("AnnotatedTag" /* AnnotatedTag cannot be patched */);
		noFindById.add("Branch" /* Branch cannot be patched */);
		noFindById.add("CombinedStatus" /* CombinedStatus is read-only */);
		noFindById.add("Commit" /* Commit is read-only */);
		noFindById.add("CommitStatus" /* CommitStatus cannot be patched */);
		noFindById.add("ContentsResponse" /* ContentsResponse cannot be patched */);
		noFindById.add("DeployKey" /* DeployKey cannot be patched */);
		noFindById.add("GPGKey" /* GPGKey cannot be patched */);
		
		//TODO Why is patch path for EmailList is /user/settings
		//TODO Why is patch path for GPGKeyList is /user/settings
		
		return reader.getResponseTypeNames(e -> !noFindById.contains(e.getKey()));
	}
	
	private static Stream<String> modelsToTest() {
	    return reader.getResponseTypeNames(e -> !EXCLUDED_RESPONSES.contains(e.getKey()));
	}
	
	
}
