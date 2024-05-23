package org.acme.client;

import java.util.Optional;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

public interface ApiClientMethodCallFactory {

	Optional<MethodCallExpr> delete(NameExpr apiClient, NameExpr primary, NodeList<Expression> byIdArgs);

	Optional<MethodCallExpr> update(NameExpr apiClient, NameExpr primary, NodeList<Expression> byIdArgs,
			NodeList<Expression> patchArgs);

	Optional<MethodCallExpr> findById(NameExpr apiClient, NameExpr primary, NodeList<Expression> args);

	Optional<MethodCallExpr> create(NameExpr apiClient, NameExpr primary, NodeList<Expression> byIdArgs,
			NodeList<Expression> postArgs);

}
