package org.acme.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.acme.Configuration;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

public class ParameterResolver {
	
	private final Configuration config;
	private final OpenAPI openApiDoc;

	public ParameterResolver(Configuration config, OpenAPI openApiDoc) {
		this.config = config;
		this.openApiDoc = openApiDoc;
	}
	
	public Map<String, String> getParameterNameMappings(String path) {
		if (config.getPathParamMappings().containsKey(path)) {
			Map<String, String> parameterNameMappings = new HashMap<>();
			
			List<String> paramMappingValueList = paramMappingValueList(config.getPathParamMappings().get(path));
			for (int i = 0; i < paramMappingValueList.size(); i++) {
				if (getParameter(path, i).isPresent()) {
					parameterNameMappings.put(getParameter(path, i).get().getName(), attributeName(paramMappingValueList.get(i)));				
				}
			}
			return parameterNameMappings;
		} else {
			return Collections.emptyMap();
		}
	}
	
	public List<Parameter> getParameters(String path) {
		List<Parameter> params = new ArrayList<>();
		Optional<Parameter> parameter = getParameter(path, 0);
		for (int i = 0; parameter.isPresent(); i++) {
			params.add(parameter.get());
			parameter = getParameter(path, i + 1);
		}
		return params;
	}
	
	private String attributeName(String mappingValue) {
		String[] valueSplit = mappingValue.split("\\.");
		return valueSplit[valueSplit.length - 1];
	}
	
	public Optional<Parameter> getParameter(String path, int paramIndex) {
		Optional<PathItem> pathItem = Optional.ofNullable(openApiDoc.getPaths().getPathItem(path));
		return pathItem.flatMap(p -> {
			Optional<Operation> oneOfIdOps = Optional.ofNullable(p.getGET())
					.or(() -> Optional.ofNullable(p.getPATCH()))
					.or(() -> Optional.ofNullable(p.getDELETE()));
				return oneOfIdOps.map(o -> o.getParameters()).or(() -> Optional.ofNullable(p.getParameters()))
						.filter(ps -> ps.size() > paramIndex)
						.map(ps -> ps.get(paramIndex));
		});
		
	}
	
	public NodeList<Expression> resolveArgs(String path, NameExpr primary, NodeList<Expression> defaultArgs) {
		return getRawParamMappingValues(path::equalsIgnoreCase)
			.findFirst()
			.map(k -> paramMappingValueList(k).stream().map(v -> getterChain(primary, v)).toList())
			.map(s -> {
				NodeList<Expression> nodeList = new NodeList<>(s); 
				nodeList.addAll(defaultArgs);
				return nodeList;
			})
			.orElse(defaultArgs);
	}
	
	public Set<String> getPathParamMappingKeys() {
		return config.getPathParamMappings().keySet();
	}
	
	public Map<String, String> getPathParamMappings() {
		return config.getPathParamMappings();
	}

	
	public Stream<String> getRawParamMappingValues(Predicate<String> filter) {
		return config.getPathParamMappings().keySet().stream()
			.filter(filter)
			.map(k -> config.getPathParamMappings().get(k));
	}
	
	public List<String> paramMappingValueList(String value) {
		return Arrays.asList(value.split("\\|"));
	}
	
	private Expression getterChain(NameExpr primary, String attrPath) {
		return getterChain(primary, attrPath, primary);
	}
	
	private Expression getterChain(NameExpr primary, String attrPath, Expression args) {
		int indexOfDot = attrPath.indexOf('.');
		if (indexOfDot > 0) {
			return getterChain(primary, attrPath.substring(indexOfDot + 1), new MethodCallExpr(args, "get" + toFirstUpper(attrPath.substring(0, indexOfDot))));
		}
		return new MethodCallExpr(args, "get" + toFirstUpper(attrPath));
	}
	
	private String toFirstUpper(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
	
	public static void main(String[] args) {
		
	}
}
