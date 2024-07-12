package org.acme.gen;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.acme.client.ApiClientMethodCallFactory;
import org.acme.client.ParameterResolver;
import org.acme.client.ResponseResolver;
import org.acme.read.crud.CrudMapper;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier.Keyword;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.utils.SourceRoot;
import com.microsoft.kiota.ApiException;
import com.microsoft.kiota.NativeResponseHandler;
import com.microsoft.kiota.ResponseHandlerOption;
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
import io.vertx.ext.web.client.HttpResponse;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class DependentGen {
	private static final String FIELD_CLIENT_PROVIDER = "clientProvider";
	//private static final String VAR_WEB_CLIENT_SESSION = "webClientSession";
	private static final String FIELD_API_CLIENT = "apiClient";
	private static final String FIELD_NAME_VERTX = "vertx";
	private static final String NAME_POSTFIX = "Dependent";
	private final Path path;
	private final ParameterResolver paramResolver;
	private final ResponseResolver respResolver;
	private final Name name;
	private final Name resource;
	private final ApiClientMethodCallFactory methodCalls;
	private final CrudMapper mapper;
	private final ClassOrInterfaceType contextType;
	private final ClassOrInterfaceType crdType;
	private final ClassOrInterfaceType resourceType;
	private final ClassOrInterfaceType collectionsType;
	
	public DependentGen(Path path, Name name, Name resource, ApiClientMethodCallFactory methodCalls, CrudMapper mapper, ParameterResolver paramResolver, ResponseResolver respResolver) {
		this.path = path;
		this.name = name;
		this.resource = resource;
		this.methodCalls = methodCalls;
		this.mapper = mapper;
		this.paramResolver = paramResolver;
		this.respResolver = respResolver;
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
			//.map(e -> e.getValue().getPOST())
			.ifPresent(p -> 
				createMethod(cu, clazz, p.getKey(), p.getValue().getPOST())
			);
		mapper.patchPath()
				.map(e -> e.getValue().getPATCH())
				.or(() -> mapper.putPath().map(e -> e.getValue().getPUT()))
				.ifPresent(op -> 
					updateMethod(cu, clazz, mapper.getByIdPath().get().getKey(), op)
				);
		deleteMethod(cu, clazz);
		fromResourceMethod(clazz);
		boolean isMappedNameCreated = mappedNameMethod(cu, clazz);
		extractParamValuesMethod(cu, clazz, isMappedNameCreated);
		
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

	private void createMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz, String path, Operation op) {
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
			if (hasPostResponse(op)) {
				createReturn = createCall
					.map(c -> respResolver.resolveResponse(path, c))	
					.map(ReturnStmt::new)
					.orElse(new ReturnStmt(new NullLiteralExpr()));
			} else {
				createReturn = extractParamStatements(cu, path, createCall, body);
			}
			body.addStatement(createReturn);
			createMethod.setBody(body);
		}
	}

	private ReturnStmt extractParamStatements(CompilationUnit cu, String path, Optional<MethodCallExpr> call, BlockStmt body) {
		ReturnStmt createReturn;
		cu.addImport(NativeResponseHandler.class);
		cu.addImport(ResponseHandlerOption.class);
		cu.addImport(HttpResponse.class);
		cu.addImport(URI.class);
		ClassOrInterfaceType nativeResponseHandlerType = new ClassOrInterfaceType(null, NativeResponseHandler.class.getSimpleName());
		ClassOrInterfaceType responseHandlerOptionType = new ClassOrInterfaceType(null, ResponseHandlerOption.class.getSimpleName());
		ClassOrInterfaceType uriType = new ClassOrInterfaceType(null, URI.class.getSimpleName());
		ClassOrInterfaceType httpResponseType = new ClassOrInterfaceType(null, new SimpleName(HttpResponse.class.getSimpleName()), new NodeList<>(new WildcardType()));
		body.addStatement(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(nativeResponseHandlerType, "responseHandler"), new ObjectCreationExpr(null, nativeResponseHandlerType, new NodeList<>()), Operator.ASSIGN)));
		body.addStatement(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(responseHandlerOptionType, "responseHandlerOption"), new ObjectCreationExpr(null, responseHandlerOptionType, new NodeList<>()), Operator.ASSIGN)));
		body.addStatement(new ExpressionStmt(new MethodCallExpr(new NameExpr("responseHandlerOption"), "setResponseHandler", new NodeList<>(new NameExpr("responseHandler")))));
		call.ifPresent(c -> c.addArgument(new LambdaExpr(new Parameter(new UnknownType(), "r"), new MethodCallExpr(new FieldAccessExpr(new NameExpr("r"), "options"), "add", new NodeList<>(new NameExpr("responseHandlerOption"))))));
		call.ifPresent(body::addStatement);
		body.addStatement(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(httpResponseType, "resp"), new CastExpr(httpResponseType, new MethodCallExpr(new NameExpr("responseHandler"), "getValue", new NodeList<>())), Operator.ASSIGN)));
		MethodCallExpr respGetHeader = new MethodCallExpr(new NameExpr("resp"), "getHeader", new NodeList<>(new StringLiteralExpr("Location")));
		MethodCallExpr getLocationHeader = new MethodCallExpr(new TypeExpr(uriType), "create", new NodeList<>(respGetHeader));
		MethodCallExpr extractParamValues = new MethodCallExpr(null, "extractParamValues", new NodeList<>(new StringLiteralExpr(path), getLocationHeader));
		
		MethodCallExpr putToAdditionalData = new MethodCallExpr(new MethodCallExpr(new NameExpr("desired"), "getAdditionalData"), "putAll", new NodeList<>(extractParamValues));
		IfStmt headerNullGuard = new IfStmt(new BinaryExpr(respGetHeader, new NullLiteralExpr(), com.github.javaparser.ast.expr.BinaryExpr.Operator.NOT_EQUALS), new ExpressionStmt(putToAdditionalData), null);
		body.addStatement(headerNullGuard);
		createReturn = new ReturnStmt(new NameExpr("desired"));
		return createReturn;
	}

	private boolean hasPostResponse(Operation op) {
		return (op.getResponses().getAPIResponse("201") != null && op.getResponses().getAPIResponse("201").getContent() != null)
				|| (op.getResponses().getAPIResponse("200") != null && op.getResponses().getAPIResponse("200").getContent() != null);
	}
	
	private boolean hasUpdateResponse(Operation op) {
		return op.getResponses().getAPIResponse("200") != null && op.getResponses().getAPIResponse("200").getContent() != null;
	}
	
	private void updateMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz, String path, Operation op) {
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
		if (hasUpdateResponse(op)) {
			updateReturn = updateCall
					.map(m -> new ReturnStmt(m)).orElse(new ReturnStmt(new NullLiteralExpr()));
		} else {
			updateReturn = extractParamStatements(cu, path, updateCall, body);
			// TODO
			//updateCall
			//	.ifPresent(m -> body.addStatement(m));
			//updateReturn = new ReturnStmt(new NameExpr("desired"));
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
	
	/**
	 * private static String renamed(String key) {
	 * @param key
	 * @return 
	 */
	private boolean mappedNameMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz) {
		Map<String, String> parameterMap = mapper.getByIdPath()
				.map(p-> paramResolver.getParameterNameMappings(p.getKey()))
				.orElse(Collections.emptyMap());
		NodeList<Expression> methodArgs = new NodeList<>();
		parameterMap.entrySet().stream()
			.flatMap(e -> Stream.of(e.getKey(), e.getValue()))
			.map(StringLiteralExpr::new)
			.forEach(methodArgs::add);
		
		if (!methodArgs.isEmpty()) {
			MethodDeclaration mappedNameMethod = clazz.addMethod("mappedName", Keyword.PRIVATE)
					.addParameter(String.class, "key")
					.setType(String.class);
			
			cu.addImport(Map.class);
			ClassOrInterfaceType map = new ClassOrInterfaceType(null, Map.class.getSimpleName());
			
			MethodCallExpr mapOf = new MethodCallExpr(new TypeExpr(map), "of", methodArgs);
			MethodCallExpr getOrDefault = new MethodCallExpr(mapOf, "getOrDefault", new NodeList<>(new NameExpr("key"), new NameExpr("key")));
			mappedNameMethod.setBody(new BlockStmt(new NodeList<>(new ReturnStmt(getOrDefault))));
			return true;
		} else {
			return false;
		}
	}
	
	private void extractParamValuesMethod(CompilationUnit cu, ClassOrInterfaceDeclaration clazz, boolean mapped) {
		ClassOrInterfaceType string = new ClassOrInterfaceType(null, String.class.getSimpleName());
		ClassOrInterfaceType mapOfStrings = new ClassOrInterfaceType(null, new SimpleName(Map.class.getSimpleName()), new NodeList<>(string, string));
		ClassOrInterfaceType hashMap = new ClassOrInterfaceType(null, new SimpleName(HashMap.class.getSimpleName()), new NodeList<>(new UnknownType()));
		ClassOrInterfaceType intType = new ClassOrInterfaceType(null, "int");
		
		cu.addImport(Map.class);
		cu.addImport(HashMap.class);
		cu.addImport(URI.class);
		MethodDeclaration initClientMethod = clazz.addMethod("extractParamValues", Keyword.PRIVATE)
				.addParameter(String.class, "templatePath")
				.addParameter(URI.class, "location")
				.setType(mapOfStrings);
		
		ClassOrInterfaceType stringArray = new ClassOrInterfaceType(null, String[].class.getSimpleName());

		
		NodeList<Statement> extractParamValues = new NodeList<>();
		
		extractParamValues.add(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(mapOfStrings, "result"), new ObjectCreationExpr(null, hashMap, new NodeList<>()), Operator.ASSIGN)));
		extractParamValues.add(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(stringArray, "locationSegements"), new MethodCallExpr(new MethodCallExpr(new NameExpr("location"), "getPath"), "split", new NodeList<>(new StringLiteralExpr("/"))), Operator.ASSIGN)));
		extractParamValues.add(new ExpressionStmt(new AssignExpr(new VariableDeclarationExpr(stringArray, "templateSegments"), new MethodCallExpr(new NameExpr("templatePath"), "split", new NodeList<>(new StringLiteralExpr("/"))), Operator.ASSIGN)));
		BinaryExpr segmentsLengthCondition = new BinaryExpr(new NameExpr("i"), new FieldAccessExpr(new NameExpr("templateSegments"), "length"), com.github.javaparser.ast.expr.BinaryExpr.Operator.LESS);
		
		
		IfStmt elseIf = new IfStmt(new UnaryExpr(new MethodCallExpr(new ArrayAccessExpr(new NameExpr("locationSegements"), new NameExpr("i")), "equals", new NodeList<>(new ArrayAccessExpr(new NameExpr("templateSegments"), new NameExpr("i")))), com.github.javaparser.ast.expr.UnaryExpr.Operator.LOGICAL_COMPLEMENT), new ReturnStmt(new NameExpr("result")), null);
		ArrayAccessExpr templateSegementsArrayElement = new ArrayAccessExpr(new NameExpr("templateSegments"), new NameExpr("i"));
	
		MethodCallExpr templateSegmentsName = new MethodCallExpr(templateSegementsArrayElement, "substring", new NodeList<>(new IntegerLiteralExpr("1"), new BinaryExpr(new MethodCallExpr(templateSegementsArrayElement, "length", new NodeList<>()), new IntegerLiteralExpr("1"), com.github.javaparser.ast.expr.BinaryExpr.Operator.MINUS)));
		MethodCallExpr mappedName = new MethodCallExpr(null, "mappedName", new NodeList<>(templateSegmentsName));
		if (!mapped) {
			mappedName = templateSegmentsName;
		}
		
		IfStmt varSegment = new IfStmt(new MethodCallExpr(templateSegementsArrayElement, "startsWith", new NodeList<>(new StringLiteralExpr("{"))), new BlockStmt(new NodeList<>(new ExpressionStmt(new MethodCallExpr(new NameExpr("result"), "put", new NodeList<>(mappedName, new ArrayAccessExpr(new NameExpr("locationSegements"), new NameExpr("i"))))))), elseIf);
		ForStmt forAllTemplateSegements = new ForStmt(new NodeList<>(new AssignExpr(new VariableDeclarationExpr(intType, "i"), new IntegerLiteralExpr("0"), Operator.ASSIGN)), segmentsLengthCondition, new NodeList<>(new AssignExpr(new NameExpr("i"), new IntegerLiteralExpr("1"), Operator.PLUS)), varSegment);
		extractParamValues.add(forAllTemplateSegements);
		extractParamValues.add(new ReturnStmt(new NameExpr("result")));
		initClientMethod.setBody(new BlockStmt(extractParamValues));
	}
	
	
}
