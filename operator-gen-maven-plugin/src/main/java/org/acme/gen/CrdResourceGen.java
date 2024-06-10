package org.acme.gen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.acme.Configuration;
import org.acme.client.ParameterResolver;
import org.acme.read.crud.CrudMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ast.expr.Name;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaPropsOrArrayBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;

/**
 * Generates a CustomResourceDefinition (CRD) as yaml file. Translates the
 * OpenAPI schema properties into .spec.versions.openAPIV3Schema of the CRD.
 * Combines all properties from post request type and patch request type. Most
 * fields of the post and patch request schema should be the same but there
 * could be different fields, e.g. fields that can only be set when creating the
 * resource and fields that cannot be created but set after the resource has
 * been created.
 * 
 * Fields that are read-only because they are neither in the post nor in the
 * patch request schema will be moved to the CRD status.
 */
public class CrdResourceGen {
	
	private static final String ATTR_PROPS = "properties";



	private static class FieldNameType {
		String name;
		String type;
		FieldNameType(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}
	
	
	private static final Logger LOG = LoggerFactory.getLogger(CrdResourceGen.class);
	private static final String NODE_TEMPLATE = "{\"type\": \"%s\"}";
	private final Path path;
	private final Path openApiJson;
	private final Name name;
	private final CrudMapper mapper;
	private final ParameterResolver resolver;
	private final ObjectMapper objMapper = new ObjectMapper();
	private final Configuration config;
	
	public CrdResourceGen(Path path, Path openApiJson, Name name, CrudMapper mapper, ParameterResolver resolver, Configuration config) {
		super();
		this.path = path;
		this.openApiJson = openApiJson;
		this.name = name;
		this.mapper = mapper;
		this.resolver = resolver;
		this.config = config;
	}

	public void create() {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNodeTree;
		try {
			jsonNodeTree = objectMapper.readTree(Files.newInputStream(openApiJson));
			JSONSchemaPropsBuilder specBuilder = new JSONSchemaPropsBuilder().withType("object");
			JSONSchemaPropsBuilder statusBuilder = new JSONSchemaPropsBuilder().withType("object");
			mapProperties(jsonNodeTree, specBuilder, statusBuilder);
			
			
			CustomResourceDefinitionBuilder defBuilder = new CustomResourceDefinitionBuilder();
			CustomResourceDefinition customResourceDefinition = defBuilder.editOrNewMetadata()
					.withName(name.getIdentifier()).endMetadata().editOrNewSpec()
					.withGroup(name.getQualifier().map(Name::toString).map(this::reverseQualifier).orElse("opgen.io"))
					.withNewNames().withKind(name.getIdentifier()).endNames().withScope("Namespaced")
					.withVersions(new CustomResourceDefinitionVersionBuilder().withName("v1alpha1").withServed(true)
							.withStorage(true).withNewSchema().editOrNewOpenAPIV3Schema()
							.addToProperties("spec", specBuilder.build())
							.addToProperties("status", statusBuilder.build())
							
							.endOpenAPIV3Schema().and().build())
					.endSpec().build();
			Files.write(path, Serialization.asYaml(customResourceDefinition).getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void mapProperties(JsonNode jsonNodeTree, JSONSchemaPropsBuilder specBuilder, JSONSchemaPropsBuilder statusBuilder) {
		Set<Entry<String, JsonNode>> allSpecFields = mapper.getCreateSchema().flatMap(crtSch -> mapper.getPatchSchema().or(mapper::getPutSchema).map(uptSch -> {
			JsonNode crtSchemaProps = Optional.ofNullable(crtSch.getRef())
					.map(s -> jsonNodeTree.at(removeLeadingHash(s)))
					.map(n -> n.get(ATTR_PROPS)).orElse(null);
			JsonNode uptSchemaProps = Optional.ofNullable(uptSch.getRef())
					.map(s -> jsonNodeTree.at(removeLeadingHash(s)))
					.map(n -> n.get(ATTR_PROPS)).orElse(null);
			Set<Entry<String, JsonNode>> unionOfFields = unionOfFields(crtSchemaProps, uptSchemaProps);
			removeIgnoredFields(unionOfFields);
			addMissingFieldsFromPathParamMappings(unionOfFields, "spec");
			mapProperties(specBuilder, unionOfFields, jsonNodeTree);
			return unionOfFields;
		})).orElse(Collections.emptySet());
		
		JsonNode getSchemaProps = Optional.ofNullable(mapper.getByIdSchema().getRef())
				.map(s -> jsonNodeTree.at(removeLeadingHash(s)))
				.map(n -> n.get(ATTR_PROPS)).orElse(null);
		Set<Entry<String, JsonNode>> fieldsOfNotIn = fieldsOfNotIn(getSchemaProps, allSpecFields.stream().map(Entry::getKey).toList());
		removeIgnoredFields(fieldsOfNotIn);
		addMissingFieldsFromPathParamMappings(fieldsOfNotIn, "status");
		mapProperties(statusBuilder, fieldsOfNotIn, jsonNodeTree);
	}
	
	
	
	private void addMissingFieldsFromPathParamMappings(Set<Entry<String, JsonNode>> fields, String prefix) {
		resolver.getPathParamMappingKeys().stream()
			.filter(this::oneOfCrudPathsMatches)
			.flatMap(s -> {
				List<String> paramMappingValueList = resolver.paramMappingValueList(resolver.getPathParamMappings().get(s));
				List<FieldNameType> fieldEntries = new ArrayList<>(); 
				for (int i = 0; i < paramMappingValueList.size(); i++) {
					fieldEntries.add(new FieldNameType(paramMappingValueList.get(i), resolver.getParameterType(s, i)
							.map(p -> p.getSchema().getType().toString()).orElse("string")));
				}
				return fieldEntries.stream();
			})
			.filter(v -> v.name.startsWith(prefix + "."))
			.map(v -> removePrefix(prefix, v))
			.filter(v -> fields.stream().noneMatch(e -> e.getKey().equals(v.name)))
			.forEach(v -> {
				try {
					fields.add(Map.entry(v.name, objMapper.readTree(String.format(NODE_TEMPLATE, v.type))));
				} catch (JsonProcessingException e) {
					LOG.error("Error adding JSON node", e);
				} 
			});		
	}

	private FieldNameType removePrefix(String prefix, FieldNameType fieldNameType) {
		fieldNameType.name = fieldNameType.name.substring(prefix.length() + 1);
		return fieldNameType;
	}

	private boolean oneOfCrudPathsMatches(String s) {
		return mapper.getByIdPath().filter(p -> s.equals(p.getKey())).isPresent()
		|| mapper.createPath().filter(p -> s.equals(p.getKey())).isPresent()
		|| mapper.patchPath().filter(p -> s.equals(p.getKey())).isPresent()
		|| mapper.putPath().filter(p -> s.equals(p.getKey())).isPresent()
		|| mapper.deletePath().filter(p -> s.equals(p.getKey())).isPresent();
	}
	
	private String removeLeadingHash(String input) {
		if (input.startsWith("#")) {
			return input.substring(1);
		} else {
			return input;
		}
	}

	private void mapProperties(JSONSchemaPropsBuilder builder, Set<Entry<String, JsonNode>> fields, JsonNode jsonNodeTree) {
		mapProperties(builder, fields, jsonNodeTree, new HashSet<>());
	}
	
	private void mapProperties(JSONSchemaPropsBuilder builder, Set<Entry<String, JsonNode>> fields, JsonNode jsonNodeTree, Set<JsonNode> visitedNodes) {
		mapPrimitiveTypeProps(builder, fields);
		mapArrayTypeProps(builder, fields, jsonNodeTree, visitedNodes);
		mapObjectTypeProps(builder, fields, jsonNodeTree, visitedNodes);
	}
	
	private void mapArrayTypeProps(JSONSchemaPropsBuilder builder, Set<Entry<String, JsonNode>> fields, JsonNode jsonNodeTree, Set<JsonNode> visitedNodes) {
		fields.stream()
			.filter(f -> f.getValue().get("type") != null)
			.filter(f -> "array".equals(f.getValue().get("type").asText()))
			.filter(f -> f.getValue().get("items") != null)
			.forEach(f -> {
				LOG.info("Array {} ",f.getKey());
				JSONSchemaPropsBuilder jsonSchemaPropsBuilder = new JSONSchemaPropsBuilder();
				JSONSchemaPropsBuilder itemPropsBuilder = new JSONSchemaPropsBuilder();
				if (f.getValue().get("items").get("type") != null) {
					itemPropsBuilder.withType(f.getValue().get("items").get("type").asText());
				}
				if (f.getValue().get("items").get("$ref") != null) {
					JsonNode schema = jsonNodeTree.at(removeLeadingHash(f.getValue().get("items").get("$ref").asText()));
					LOG.info("Schema {} ", f.getValue().get("items").get("$ref"));
					if(!visitedNodes.contains(schema)) {
						visitedNodes.add(schema);
						mapProperties(itemPropsBuilder, fields(schema.get(ATTR_PROPS)), jsonNodeTree, visitedNodes);
					}
					itemPropsBuilder.withType("object");
				}
				builder.addToProperties(f.getKey(),
								jsonSchemaPropsBuilder.withItems(new JSONSchemaPropsOrArrayBuilder().withSchema(itemPropsBuilder.build()).build()).withType(f.getValue().get("type").asText()).build());
			});
	}
	
	private void mapPrimitiveTypeProps(JSONSchemaPropsBuilder builder, Set<Entry<String, JsonNode>> fields) {
		fields.stream()
			.filter(f -> f.getValue().get("type") != null)
			.filter(f -> !"array".equals(f.getValue().get("type").asText()))
			.forEach(f -> builder.addToProperties(f.getKey(),
						new JSONSchemaPropsBuilder().withType(f.getValue().get("type").asText()).build()));
	}
	
	private void mapObjectTypeProps(JSONSchemaPropsBuilder builder, Set<Entry<String, JsonNode>> fields, JsonNode jsonNodeTree,
			Set<JsonNode> visitedNodes) {
		fields.stream()
			.filter(f -> f.getValue().get("$ref") != null)
			.forEach(f -> {
				JsonNode schema = jsonNodeTree.at(removeLeadingHash(f.getValue().get("$ref").asText()));
				if(!visitedNodes.contains(schema)) {
					visitedNodes.add(schema);
					JSONSchemaPropsBuilder objectTypeBuilder = new JSONSchemaPropsBuilder();
					mapProperties(objectTypeBuilder, fields(schema.get(ATTR_PROPS)), jsonNodeTree, visitedNodes);
					builder.addToProperties(f.getKey(),
							objectTypeBuilder.withType("object").build());
				}
			});
	}	

	private Set<Entry<String, JsonNode>> unionOfFields(JsonNode a, JsonNode b) {
		Set<Entry<String, JsonNode>> union = new LinkedHashSet<>();
		if (a != null) {
			a.fields().forEachRemaining(union::add);
		}
		if (b != null) {
			b.fields().forEachRemaining(union::add);
		}
		return union;
	}
	
	private Set<Entry<String, JsonNode>> removeIgnoredFields(Set<Entry<String, JsonNode>> props) {
		props.removeIf(e -> config.getIgnoreProps().contains(e.getKey()));
		return props;
	}
	
	private Set<Entry<String, JsonNode>> fields(JsonNode a) {
		Set<Entry<String, JsonNode>> fields = new LinkedHashSet<>();
		if (a != null) {
			a.fields().forEachRemaining(fields::add);
		}
		return fields;
	}
	
	private Set<Entry<String, JsonNode>> fieldsOfNotIn(JsonNode a, Collection<String> b) {
		Set<Entry<String, JsonNode>> exclusiveSet = new LinkedHashSet<>();
		if (a != null) {
			a.fields().forEachRemaining(e -> {
				if (!b.contains(e.getKey())) {
					exclusiveSet.add(e);
				}
			});
		}
		return exclusiveSet;
	}
	
	

	private String reverseQualifier(String qualifier) {
		List<String> qualifierList = Arrays.asList(qualifier.split("\\."));
		Collections.reverse(qualifierList);
		return qualifierList.stream().collect(Collectors.joining("."));
	}
}
