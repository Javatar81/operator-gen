package org.acme.client;

import org.acme.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class ResponseResolver {
	private static final Logger LOG = LoggerFactory.getLogger(ResponseResolver.class);
	private final Configuration config;
	
	public ResponseResolver(Configuration config) {
		this.config = config;
	}
	
	public Expression resolveResponse(String path, Expression input) {
		LOG.debug("Resolving response mapping for path {}", path);
		if (config.getPathResponseMappings().containsKey(path)) {
			LOG.debug("Found response mapping for path {}", path);
			String fieldMapping = config.getPathResponseMappings().get(path);
			String getter = toGetter(fieldMapping);
			return new MethodCallExpr(input, getter, new NodeList<>());
		}
		return input;
	}
	
	private String toGetter(String fieldName) {
		return String.format("get%s%s", String.valueOf(fieldName.charAt(0)).toUpperCase(), fieldName.substring(1, fieldName.length()));
	}
}
