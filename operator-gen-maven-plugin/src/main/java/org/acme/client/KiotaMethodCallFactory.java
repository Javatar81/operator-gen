package org.acme.client;

import java.util.Map.Entry;
import java.util.Optional;

import org.acme.read.crud.CrudMapper;
import org.eclipse.microprofile.openapi.models.PathItem;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

public class KiotaMethodCallFactory implements ApiClientMethodCallFactory {
	
	private final CrudMapper mapper;

	public KiotaMethodCallFactory(CrudMapper mapper) {
		this.mapper = mapper;
	}
	
	@Override
	public Optional<MethodCallExpr> findById(NameExpr apiClient, NodeList<Expression> args) {
		Optional<Entry<String, PathItem>> byIdPath = mapper.getByIdPath();
		return byIdPath
				.map(Entry::getKey)
				.map(k -> toMethodCallExpression(apiClient, k, args));
	}
	
	private MethodCallExpr toMethodCallExpression(Expression prev, String pathKey, NodeList<Expression> args) {		
		if (pathKey.startsWith("/")) {
			pathKey = pathKey.substring(1);
		}
		int firstSegmentLength = pathKey.indexOf("/");
		if (firstSegmentLength < 0) {
			firstSegmentLength = pathKey.length();
		}
		String methodName = pathKey.substring(0, firstSegmentLength);
		MethodCallExpr methodCallExpr;
		if (methodName.startsWith("{")) {
			methodName = "by" + Character.toUpperCase(methodName.charAt(1)) + methodName.substring(2, methodName.length() - 1);
			methodCallExpr = new MethodCallExpr(prev, methodName, args);
		} else {
			methodCallExpr = new MethodCallExpr(prev, methodName);
		}
		if (firstSegmentLength < pathKey.length()) {
			return toMethodCallExpression(methodCallExpr, pathKey.substring(firstSegmentLength), args);
		} else {
			return methodCallExpr;
		}

	}

}