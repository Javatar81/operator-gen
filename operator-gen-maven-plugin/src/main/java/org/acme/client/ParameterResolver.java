package org.acme.client;

import java.util.Arrays;

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
		return config.getPathParamMappings().keySet().stream()
			.filter(k -> path.equalsIgnoreCase(k))
			.map(k -> config.getPathParamMappings().get(k))
			.findFirst()
			.map(k -> Arrays.asList(k.split("\\|")).stream().map(v -> getterChain(primary, v)).toList())
			.map(s -> {
				NodeList<Expression> nodeList = new NodeList<>(s); 
				nodeList.addAll(defaultArgs);
				return nodeList;
			})
			.orElse(defaultArgs);
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
