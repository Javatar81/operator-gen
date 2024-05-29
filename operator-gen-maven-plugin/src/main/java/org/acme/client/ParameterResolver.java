package org.acme.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.acme.Configuration;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

public class ParameterResolver {
	
	private final Configuration config;

	public ParameterResolver(Configuration config) {
		this.config = config;
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
	
}
