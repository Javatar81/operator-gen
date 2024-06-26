package org.acme.gen;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.acme.client.ApiClientMethodCallFactory;
import org.acme.read.crud.CrudMapper;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.utils.SourceRoot;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.serialization.Parsable;
import com.microsoft.kiota.serialization.ParsableFactory;
import com.microsoft.kiota.serialization.ParseNode;

import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.external.PerResourcePollingDependentResource;
import io.kiota.http.vertx.VertXRequestAdapter;
import io.kiota.serialization.json.JsonParseNodeFactory;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class DependentGen {
	private static final String FIELD_CLIENT_PROVIDER = "clientProvider";
	//private static final String VAR_WEB_CLIENT_SESSION = "webClientSession";
	private static final String FIELD_API_CLIENT = "apiClient";
	private static final String FIELD_NAME_VERTX = "vertx";
	private static final String NAME_POSTFIX = "Dependent";
	private final Path path;
	private final Name name;
	private final Name resource;
	private final ApiClientMethodCallFactory methodCalls;
	private final CrudMapper mapper;
	private final ClassOrInterfaceType contextType;
	private final ClassOrInterfaceType crdType;
	private final ClassOrInterfaceType resourceType;
	private final ClassOrInterfaceType collectionsType;
	
	public DependentGen(Path path, Name name, Name resource, ApiClientMethodCallFactory methodCalls, CrudMapper mapper) {
		this.path = path;
		this.name = name;
		this.resource = resource;
		this.methodCalls = methodCalls;
		this.mapper = mapper;
		crdType = new ClassOrInterfaceType(null, name.toString());
		contextType = new ClassOrInterfaceType(null,
				new SimpleName(Context.class.getSimpleName()),
				new NodeList<>(crdType));
		resourceType = new ClassOrInterfaceType(null, name.getIdentifier());
		collectionsType = new ClassOrInterfaceType(null, Collections.class.getSimpleName());
	}

	public void create() {
		String className = name.getIdentifier() + NAME_POSTFIX;
		var cu = new CompilationUnit(name.getQualifier().map(Name::toString).orElse(""));
		cu.addImport(PerResourcePollingDependentResource.class);
		cu.addImport(Set.class);
		cu.addImport(resource.toString());
		cu.addImport(Vertx.class);
		cu.addImport(PostConstruct.class);
		cu.addImport(VertXRequestAdapter.class);
		cu.addImport(Inject.class);
		cu.addImport(Context.class);
		cu.addImport(ApiException.class);
		cu.addImport(Collections.class);
		cu.addImport(Parsable.class);
		cu.addImport(ParsableFactory.class);
		cu.addImport(Serialization.class);
		cu.addImport(ParseNode.class);
		cu.addImport(JsonParseNodeFactory.class);
		cu.addImport(ByteArrayInputStream.class);
		cu.addImport(StandardCharsets.class);
		cu.addImport(resource.getQualifier().map(Name::toString).orElse("").replace(".models", "") + ".ApiClient");

		ClassOrInterfaceType dependentType = new ClassOrInterfaceType(null,
				new SimpleName(PerResourcePollingDependentResource.class.getSimpleName()),
				new NodeList<>(resourceType, crdType));
		
		ClassOrInterfaceDeclaration clazz = cu.addClass(className, Keyword.PUBLIC)
				.addExtendedType(dependentType);
				
		fields(clazz);
		constructor(clazz);
		initClientMethod(clazz);
		desiredMethod(clazz);
		fetchMethod(clazz);
		mapper.createPath()
				.map(e -> e.getValue().getPOST())
				.ifPresent(op -> 
					createMethod(cu, clazz, op)
				);
		mapper.patchPath()
				.map(e -> e.getValue().getPATCH())
				.or(() -> mapper.putPath().map(e -> e.getValue().getPUT()))
				.ifPresent(op -> 
					updateMethod(cu, clazz, op)
				);
		deleteMethod(cu, clazz);
		fromResourceMethod(clazz);
		cu.setStorage(path.resolve(String.format("%s/%s.java",
				name.getQualifier().map(Name::toString).map(n -> n.replace(".", "/")).orElse(""), className)));
		SourceRoot dest = new SourceRoot(path);
		dest.add(cu);
		dest.saveAll();
	}

	private void fetchMethod(ClassOrInterfaceDeclaration clazz) {
		ClassOrInterfaceType setOfResourceType = new ClassOrInterfaceType(null,
				new SimpleName(Set.class.getSimpleName()), new NodeList<>(resourceType));
		ClassOrInterfaceType setType = new ClassOrInterfaceType(null, Set.class.getSimpleName());


		MethodDeclaration fetchResourcesMethod = clazz.addMethod("fetchResources", Keyword.PUBLIC)
				.addAnnotation(Override.class).addParameter(crdType, "primaryResource").setType(setOfResourceType);
		setFetchResourcesBody(setType, fetchResourcesMethod);
	}

	private void deleteMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz) {
		cu.addImport(Deleter.class);
		ClassOrInterfaceType deleterType = new ClassOrInterfaceType(null,
				new SimpleName(Deleter.class.getSimpleName()),
				new NodeList<>(crdType));
		clazz.addImplementedType(deleterType);
		MethodDeclaration deleteMethod = clazz.addMethod("delete", Keyword.PUBLIC)
				.addAnnotation(Override.class)
				.addParameter(crdType, "primary")
				.addParameter(contextType, "context");
		methodCalls.delete(new NameExpr(FIELD_API_CLIENT), new NameExpr("primary"),
				new NodeList<>(new MethodCallExpr(new MethodCallExpr(new NameExpr("primary"), "getMetadata"),
						"getName")))
		.ifPresent(m -> deleteMethod.setBody(new BlockStmt(new NodeList<>(new ExpressionStmt(m)))));
	}

	private void desiredMethod(ClassOrInterfaceDeclaration clazz) {
		MethodDeclaration desiredMethod = clazz.addMethod("desired", Keyword.PROTECTED)
				.addAnnotation(Override.class)
				.addParameter(crdType, "primary")
				.addParameter(contextType, "context")
				.setType(resourceType);
		desiredMethod.setBody(new BlockStmt(new NodeList<>(new ReturnStmt(new MethodCallExpr(null, "fromResource", new NodeList<>(new NameExpr("primary"), new MethodReferenceExpr(new TypeExpr(resourceType), new NodeList<>(),"createFromDiscriminatorValue")))))));
	}

	private void initClientMethod(ClassOrInterfaceDeclaration clazz) {
		MethodDeclaration initClientMethod = clazz.addMethod("initClient", Keyword.PUBLIC)
				.addAnnotation(PostConstruct.class);
		
		
		//ClassOrInterfaceType webClientSessionType = new ClassOrInterfaceType(null, WebClientSession.class.getSimpleName());
		//ClassOrInterfaceType webClientType = new ClassOrInterfaceType(null, WebClient.class.getSimpleName());
		ClassOrInterfaceType vertxRequestAdapterType = new ClassOrInterfaceType(null, VertXRequestAdapter.class.getSimpleName());
		ClassOrInterfaceType apiClientType = new ClassOrInterfaceType(null, "ApiClient");
		
		NodeList<Statement> initClientStatements = new NodeList<>();
		
		//initClientStatements.add(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(webClientSessionType, VAR_WEB_CLIENT_SESSION), new MethodCallExpr(new TypeExpr(webClientSessionType), "create", new NodeList<>(new MethodCallExpr(new TypeExpr(webClientType), "create", new NodeList<>(new NameExpr(FIELD_NAME_VERTX))))), Operator.ASSIGN)));
		//initClientStatements.add(new ExpressionStmt(new MethodCallExpr(new NameExpr(FIELD_CLIENT_PROVIDER), "provide", new NodeList<>())));
		initClientStatements.add(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(vertxRequestAdapterType, "requestAdapter"), new ObjectCreationExpr(null, vertxRequestAdapterType, new NodeList<>(new MethodCallExpr(new NameExpr(FIELD_CLIENT_PROVIDER), "provide", new NodeList<>()))), Operator.ASSIGN)));
		initClientStatements.add(new ExpressionStmt(new MethodCallExpr(new NameExpr("urlProvider"), "provide", new NodeList<>(new NameExpr("requestAdapter")))));
		initClientStatements.add(new ExpressionStmt(new AssignExpr(new NameExpr(FIELD_API_CLIENT), new ObjectCreationExpr(null, apiClientType, new NodeList<>(new NameExpr("requestAdapter"))), Operator.ASSIGN)));
		initClientMethod.setBody(new BlockStmt(initClientStatements));
	}

	private void fields(ClassOrInterfaceDeclaration clazz) {
		clazz.addField(Vertx.class, FIELD_NAME_VERTX).addAnnotation(Inject.class);
		clazz.addField("ApiClient", FIELD_API_CLIENT, Keyword.PRIVATE);
		clazz.addField("WebClientProvider", FIELD_CLIENT_PROVIDER).addAnnotation(Inject.class);
		clazz.addField("BaseUrlProvider", "urlProvider").addAnnotation(Inject.class);
	}

	private void constructor(ClassOrInterfaceDeclaration clazz) {
		clazz.addConstructor(Keyword.PUBLIC).setBody(new BlockStmt(new NodeList<>(new ExpressionStmt(
				new MethodCallExpr("super", new FieldAccessExpr(new TypeExpr(resourceType), "class"))))));
	}

	private void createMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz, Operation op) {
		Schema schema = op.getRequestBody().getContent().getMediaType("application/json").getSchema();
		String typeName;
		if (schema.getRef() != null) {
			typeName = schema.getRef().substring(schema.getRef().lastIndexOf("/") + 1, schema.getRef().length());
			ClassOrInterfaceType createOptionType = new ClassOrInterfaceType(null, typeName);
			
			cu.addImport(resource.getQualifier().map(Name::toString).orElse("") + "." + createOptionType);
			cu.addImport(Creator.class);
			ClassOrInterfaceType creatorType = new ClassOrInterfaceType(null,
					new SimpleName(Creator.class.getSimpleName()),
					new NodeList<>(resourceType, crdType));
			clazz.addImplementedType(creatorType);
			MethodDeclaration createMethod = clazz.addMethod("create", Keyword.PUBLIC)
					.addAnnotation(Override.class)
					.addParameter(resourceType, "desired")
					.addParameter(crdType, "primary")
					.addParameter(contextType, "context")
					.setType(resourceType);
			Optional<MethodCallExpr> createCall = methodCalls.create(new NameExpr(FIELD_API_CLIENT), new NameExpr("primary"), new NodeList<>(new MethodCallExpr(new MethodCallExpr(new NameExpr("primary"), "getMetadata"),
					"getName")),
					new NodeList<>(new NameExpr("createOption")));
			AssignExpr assignCreateOpt = new AssignExpr(new VariableDeclarationExpr(createOptionType, "createOption"), new MethodCallExpr(null, "fromResource", new NodeList<>(new NameExpr("primary"), new MethodReferenceExpr(new TypeExpr(createOptionType), new NodeList<>(),"createFromDiscriminatorValue"))),Operator.ASSIGN);
			MethodCallExpr clearAdditionalData = new MethodCallExpr(new MethodCallExpr(new NameExpr("createOption"), "getAdditionalData"), "clear");
			BlockStmt body = new BlockStmt(new NodeList<>(new ExpressionStmt(assignCreateOpt), new ExpressionStmt(clearAdditionalData)));
			ReturnStmt createReturn;
			if (op.getResponses().getAPIResponse("201") != null && op.getResponses().getAPIResponse("201").getContent() != null) {
				createReturn = createCall
					.map(m -> new ReturnStmt(m)).orElse(new ReturnStmt(new NullLiteralExpr()));
			} else {
				createCall
					.ifPresent(m -> body.addStatement(m));
				createReturn = new ReturnStmt(new NameExpr("desired"));
			}
			body.addStatement(createReturn);
			createMethod.setBody(body);
		}
	}
	
	private void updateMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz, Operation op) {
		String ref = op.getRequestBody().getContent().getMediaType("application/json").getSchema().getRef();
		String typeName = ref.substring(ref.lastIndexOf("/") + 1, ref.length());
		ClassOrInterfaceType updateOptionType = new ClassOrInterfaceType(null, typeName);
		cu.addImport(Updater.class);
		cu.addImport(resource.getQualifier().map(Name::toString).orElse("") + "." + updateOptionType);
		ClassOrInterfaceType updaterType = new ClassOrInterfaceType(null,
				new SimpleName(Updater.class.getSimpleName()),
				new NodeList<>(resourceType, crdType));
		clazz.addImplementedType(updaterType);
		MethodDeclaration updateMethod = clazz.addMethod("update", Keyword.PUBLIC)
				.addAnnotation(Override.class)
				.addParameter(resourceType, "actual")
				.addParameter(resourceType, "desired")
				.addParameter(crdType, "primary")
				.addParameter(contextType, "context")
				.setType(resourceType);
		Optional<MethodCallExpr> updateCall = methodCalls.update(new NameExpr(FIELD_API_CLIENT), new NameExpr("primary"),
				new NodeList<>(new MethodCallExpr(new MethodCallExpr(new NameExpr("primary"), "getMetadata"),
						"getName")),
				new NodeList<>(new NameExpr("editOption")));
		AssignExpr assignUpdateOpt = new AssignExpr(new VariableDeclarationExpr(updateOptionType, "editOption"), new MethodCallExpr(null, "fromResource", new NodeList<>(new NameExpr("primary"), new MethodReferenceExpr(new TypeExpr(updateOptionType), new NodeList<>(),"createFromDiscriminatorValue"))),Operator.ASSIGN);
		MethodCallExpr clearAdditionalData = new MethodCallExpr(new MethodCallExpr(new NameExpr("editOption"), "getAdditionalData"), "clear");
		BlockStmt body = new BlockStmt(new NodeList<>(new ExpressionStmt(assignUpdateOpt), new ExpressionStmt(clearAdditionalData)));
		ReturnStmt updateReturn;
		if (op.getResponses().getAPIResponse("200") != null && op.getResponses().getAPIResponse("200").getContent() != null) {
			updateReturn = updateCall
					.map(m -> new ReturnStmt(m)).orElse(new ReturnStmt(new NullLiteralExpr()));
		} else {
			updateCall
				.ifPresent(m -> body.addStatement(m));
			updateReturn = new ReturnStmt(new NameExpr("desired"));
		}
		body.addStatement(updateReturn);
		updateMethod.setBody(body);
	}
	
	private void fromResourceMethod(ClassOrInterfaceDeclaration clazz) {
		ClassOrInterfaceType parsableType = new ClassOrInterfaceType(null, Parsable.class.getSimpleName());
		ClassOrInterfaceType parsableFactoryType = new ClassOrInterfaceType(null, new SimpleName(ParsableFactory.class.getSimpleName()), new NodeList<>(new TypeParameter("T")));
		MethodDeclaration fromResource = clazz.addMethod("fromResource", Keyword.PRIVATE)
			.addParameter(crdType, "primary")
			.addParameter(parsableFactoryType, "parsableFactory")
			.setType(new TypeParameter("T"))
			.setTypeParameters(new NodeList<>(new TypeParameter("T", new NodeList<>(parsableType))));
		ClassOrInterfaceType serializationType = new ClassOrInterfaceType(null, Serialization.class.getSimpleName());
		ClassOrInterfaceType stringType = new ClassOrInterfaceType(null, String.class.getSimpleName());
		ClassOrInterfaceType parseNodeType = new ClassOrInterfaceType(null, ParseNode.class.getSimpleName());
		ClassOrInterfaceType parseNodeFactoryType = new ClassOrInterfaceType(null, JsonParseNodeFactory.class.getSimpleName());
		ClassOrInterfaceType byteArrayInputStreamType = new ClassOrInterfaceType(null, ByteArrayInputStream.class.getSimpleName());
		ClassOrInterfaceType stdCharsetsType = new ClassOrInterfaceType(null, StandardCharsets.class.getSimpleName());
		AssignExpr assignAsJson = new AssignExpr(new VariableDeclarationExpr(stringType, "primaryAsJson"), new MethodCallExpr(new TypeExpr(serializationType), "asJson", new NodeList<>(new NameExpr("primary"))), Operator.ASSIGN);
		AssignExpr assignParseNode = new AssignExpr(new VariableDeclarationExpr(parseNodeType, "parseNode"), new MethodCallExpr(new ObjectCreationExpr(null, parseNodeFactoryType, new NodeList<>()), "getParseNode", new NodeList<>(new StringLiteralExpr("application/json"), new ObjectCreationExpr(null, byteArrayInputStreamType, new NodeList<>(new MethodCallExpr(new NameExpr("primaryAsJson"), "getBytes", new NodeList<>(new FieldAccessExpr(new TypeExpr(stdCharsetsType), "UTF_8"))))))), Operator.ASSIGN);
		ReturnStmt parseNodeReturn = new ReturnStmt(new MethodCallExpr(new MethodCallExpr(new NameExpr("parseNode"), "getChildNode", new NodeList<>(new StringLiteralExpr("spec"))), "getObjectValue", new NodeList<>(new NameExpr("parsableFactory"))));
		fromResource.setBody(new BlockStmt(new NodeList<>(new ExpressionStmt(assignAsJson), new ExpressionStmt(assignParseNode), parseNodeReturn)));
		
	}

	private void setFetchResourcesBody(ClassOrInterfaceType setType, MethodDeclaration fetchResourcesMethod) {

		methodCalls.findById(new NameExpr(FIELD_API_CLIENT), new NameExpr("primaryResource"),
				new NodeList<>(new MethodCallExpr(new MethodCallExpr(new NameExpr("primaryResource"), "getMetadata"),
						"getName")))
				.ifPresent(m -> {
					ReturnStmt returnEmptySet = new ReturnStmt(new MethodCallExpr(new TypeExpr(collectionsType),
							"emptySet", new NodeList<>()));
					System.out.println("Args: " + allArguments(m));
					boolean methodChainContainsGetSpec = methodChainContains(m, "getSpec");
					boolean methodChainContainsGetStatus = methodChainContains(m, "getStatus");
					var nodesInTry = new NodeList<Statement>();
					if (methodChainContainsGetSpec || methodChainContainsGetStatus) {
						BinaryExpr ifCondition;
						BinaryExpr specNullCheck = new BinaryExpr(new MethodCallExpr(new NameExpr("primaryResource"),"getSpec"), new NullLiteralExpr(), com.github.javaparser.ast.expr.BinaryExpr.Operator.EQUALS);
						BinaryExpr statusNullCheck = new BinaryExpr(new MethodCallExpr(new NameExpr("primaryResource"),"getStatus"), new NullLiteralExpr(), com.github.javaparser.ast.expr.BinaryExpr.Operator.EQUALS);
						if (methodChainContainsGetSpec && methodChainContainsGetStatus) {
							ifCondition = new BinaryExpr(specNullCheck, statusNullCheck, com.github.javaparser.ast.expr.BinaryExpr.Operator.OR);
						} else if (methodChainContainsGetSpec) {
							ifCondition = specNullCheck;
						} else if (methodChainContainsGetStatus) {
							ifCondition = statusNullCheck;
						} else {
							ifCondition = null;
						}
						nodesInTry.add(new IfStmt(createNullGuardsForParams(ifCondition, m), returnEmptySet, null));
					}
					nodesInTry.add(new ReturnStmt(new MethodCallExpr(new TypeExpr(setType),
							"of", new NodeList<>(new MethodCallExpr(m, "get")))));
					fetchResourcesMethod
							.setBody(new BlockStmt(new NodeList<>(new TryStmt(new BlockStmt(nodesInTry), new NodeList<>(catch404()), null))));
				});
	}
	
	private BinaryExpr createNullGuardsForParams(BinaryExpr leftSide, MethodCallExpr methodCallExpr) {
		Set<MethodCallExpr> allArguments = allArguments(methodCallExpr);
		if(allArguments.isEmpty()) {
			return leftSide;
		} else {
			BinaryExpr last = leftSide;
			Iterator<MethodCallExpr> iterator = allArguments.iterator();
			do {
				MethodCallExpr methodExpr = iterator.next();
				BinaryExpr nextBinExpr = new BinaryExpr(last, new BinaryExpr(methodExpr, new NullLiteralExpr(), com.github.javaparser.ast.expr.BinaryExpr.Operator.EQUALS), com.github.javaparser.ast.expr.BinaryExpr.Operator.OR);
				if (!iterator.hasNext()) {
					return nextBinExpr;
				} else {
					last = nextBinExpr;
				}
			} while (true);
		}
	}
	
	
	
	private Set<MethodCallExpr> allArguments(MethodCallExpr methodCallExpr) {
		Set<MethodCallExpr> args = new HashSet<>();
		args.addAll(methodCallExpr.getArguments().stream().filter(a -> a.isMethodCallExpr()).map(a -> a.asMethodCallExpr()).toList());
		args.addAll(methodCallExpr.getScope().stream().filter(a -> a.isMethodCallExpr()).map(a -> a.asMethodCallExpr()).flatMap(m -> allArguments(m).stream()).toList());
		return args;
	}
	
	private boolean methodChainContains(MethodCallExpr methodCallExpr, String methodName) {
		if (methodCallExpr.getNameAsString().contains(methodName)) {
			return true;
		} else {
			if (methodCallExpr.getArguments().stream().filter(a -> a.isMethodCallExpr()).map(a -> a.asMethodCallExpr())
					.anyMatch(a -> a.getName().toString().contains(methodName) || methodChainContains(a, methodName))
			) {
				return true;
			} else {
				return methodCallExpr.getScope().filter(a -> a instanceof MethodCallExpr).map(a -> (MethodCallExpr) a)
						.map(m -> methodChainContains(m, methodName)).orElse(false);
			}
		}
	}
	
	private CatchClause catch404() {
		ClassOrInterfaceType apiExceptionType = new ClassOrInterfaceType(null, ApiException.class.getSimpleName());
		
		return new CatchClause(new Parameter(apiExceptionType, "e"), new BlockStmt(new NodeList<>(new IfStmt(new BinaryExpr(new MethodCallExpr(new NameExpr("e"), "getResponseStatusCode"), new IntegerLiteralExpr("404"), com.github.javaparser.ast.expr.BinaryExpr.Operator.EQUALS), new ReturnStmt(new MethodCallExpr(new TypeExpr(collectionsType), "emptySet")), new ThrowStmt(new NameExpr("e"))))));
	}
}
