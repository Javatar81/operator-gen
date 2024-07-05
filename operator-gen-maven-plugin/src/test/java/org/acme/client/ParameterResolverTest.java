package org.acme.client;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.acme.Configuration;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

public class ParameterResolverTest {

	private static ParameterResolver parameterResolver;

	@BeforeEach
	public void setUp() {
		try (InputStream is = ParameterResolverTest.class.getClassLoader().getResourceAsStream("keycloak.json")) {
			try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
				OpenApiConfig openApiConfig = new OpenApiConfigImpl(ConfigProvider.getConfig());
				OpenAPI model = OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile);
				parameterResolver = new ParameterResolver(
						new Configuration(ConfigProvider.getConfig(),
								List.of("/admin/realms/{realm}/clients=spec.realm",
										"/admin/realms/{realm}/clients/{client-uuid}=spec.realm|status.uuid"),
								null),
						model);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
		}
	}
	
	@Test
	void getParameterWrongIndex() {
		assertFalse(parameterResolver.getParameter("/admin/realms/{realm}", 1).isPresent());
		assertFalse(parameterResolver.getParameter("/admin/realms/{realm}/clients/{client-uuid}", 2).isPresent());
	}
	
	@Test
	void getParameterWrongPath() {
		assertFalse(parameterResolver.getParameter("/admin/realms/{realm}/test123", 1).isPresent());
	}
	
	@Test
	void getParameterSuccess() {
		assertEquals("STRING", parameterResolver.getParameter("/admin/realms/{realm}", 0).get().getSchema().getType().name());
		assertEquals("STRING", parameterResolver.getParameter("/admin/realms/{realm}/clients", 0).get().getSchema().getType().name());
		assertEquals("STRING", parameterResolver.getParameter("/admin/realms/{realm}/clients/{client-uuid}", 0).get().getSchema().getType().name());
		assertEquals("STRING", parameterResolver.getParameter("/admin/realms/{realm}/clients/{client-uuid}", 1).get().getSchema().getType().name());
	}
	
	@Test
	void getPathParamMappingKeys() {
		assertEquals(Set.of("/admin/realms/{realm}/clients/{client-uuid}", "/admin/realms/{realm}/clients"), parameterResolver.getPathParamMappingKeys());
	}
	
	@Test
	void getPathParamMapping() {
		assertEquals(Map.of("/admin/realms/{realm}/clients", "spec.realm", "/admin/realms/{realm}/clients/{client-uuid}", "spec.realm|status.uuid"), parameterResolver.getPathParamMappings());
	}
	
	@Test
	void getPathParamMappingUnfiltered() {
		assertEquals(Set.of("spec.realm", "spec.realm|status.uuid"), parameterResolver.getRawParamMappingValues(p -> true).collect(Collectors.toSet()));
	}
	
	@Test
	void getParameterNameMappingsSuccess() {
		assertEquals(Map.of("client-uuid", "uuid", "realm", "realm"), parameterResolver.getParameterNameMappings("/admin/realms/{realm}/clients/{client-uuid}"));
	}
	
	@Test
	void getParameterNameMappingsNoPath() {
		assertEquals(Collections.emptyMap(), parameterResolver.getParameterNameMappings("/admin/realms/{realm}/clients/{client-uuid}/doesnotexist"));
	}
}
