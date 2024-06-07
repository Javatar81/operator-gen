package org.acme.read.crud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.acme.read.ModelReader;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

class ResponseTypeMapperGiteaTest {
	
	private static OpenAPI model;
	private static ModelReader reader;
	private static final Set<String> EXCLUDED_RESPONSES = Set.of("conflict", "empty", "error", "forbidden",
			"invalidTopicsError", "notFound", "parameterBodies", "redirect", "string", "validationError",
			"EmptyRepository" /* Only used for 409 response */,
			"FileDeleteResponse" /* Used for single operation to delete a file in a repository */,
			"FileResponse" /* Used for single operation to update a file in a repository */,
			"FilesResponse" /* Modify multiple files in a repository */
			);
	
	
	
	@BeforeAll
	public static void setUp() {
		try (InputStream is = ResponseTypeMapperGiteaTest.class.getClassLoader().getResourceAsStream("gitea.json")) {
			try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
				OpenApiConfig openApiConfig = new OpenApiConfigImpl(ConfigProvider.getConfig());
				model = OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile);
				reader = new ModelReader(model);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
		}
	}

	@ParameterizedTest
	@MethodSource("responseTypesWithGetById")
	void getByIdPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> byIdPath = analyzer.getByIdPath();
			assertNotNull(byIdPath);
			assertTrue(byIdPath.isPresent());
			assertNotNull(byIdPath.get().getValue().getGET());
			System.out.println("GetById path for " + modelName + " is " + byIdPath.get().getKey());
			Schema schema = byIdPath.get().getValue().getGET().getResponses().getAPIResponse("200").getContent().getMediaType(analyzer.getResponseMediaType()).getSchema();
			assertEquals(analyzer.getByIdSchema().getRef(), schema.getRef());
		}
	}
	
	@ParameterizedTest
	@MethodSource("responseTypesWithDelete")
	void deletePath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> deletePath = analyzer.deletePath();
			assertNotNull(deletePath);
			assertTrue(deletePath.isPresent());
			assertNotNull(deletePath.get().getValue().getDELETE());
			System.out.println("Delete path for " + modelName + " is " + deletePath.get().getKey());
		}
	}
	
	@ParameterizedTest
	@MethodSource("responseTypesWithPatch")
	void patchPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> patchPath = analyzer.patchPath();
			assertNotNull(patchPath);
			assertTrue(patchPath.isPresent());
			assertNotNull(patchPath.get().getValue().getPATCH());
			System.out.println("Patch path for " + modelName + " is " + patchPath.get().getKey());
		}
	}
	
	@ParameterizedTest
	@MethodSource("responseTypesWithCreate")
	void createPath(String modelName) {
		ResponseTypeMapper analyzer = new ResponseTypeMapper(model, modelName);
		if (!analyzer.isArrayType()) {
			Optional<Entry<String, PathItem>> createPath = analyzer.createPath();
			assertNotNull(createPath);
			assertTrue(createPath.isPresent());
			assertNotNull(createPath.get().getValue().getPOST());
			System.out.println("Create path for " + modelName + " is " + createPath.get().getKey());
		}
	}
	
	private static Stream<String> responseTypesWithGetById() {
		Set<String> noFindById = new HashSet<String>(EXCLUDED_RESPONSES);
		noFindById.add("AccessToken" /* AccessToken cannot be read after it has been created */);
		noFindById.add("CommitStatus" /* Only a list of commit status can be retrieved */);
		noFindById.add("IssueDeadline" /* Issue deadline can only be created */);
		noFindById.add("LanguageStatistics" /* Only a list of LanguageStatistics can be retrieved */);
		noFindById.add("MarkdownRender" /* MarkdownRender can only be created */);
		noFindById.add("MarkupRender" /* MarkupRender can only be created */);
		noFindById.add("PullReviewComment" /* Only a list of comments can be retrieved */);
		noFindById.add("Reaction" /* Only a list of reactions can be retrieved */);
		noFindById.add("Reference" /* Although there is a findById path it returns a list of References */);
		noFindById.add("Secret" /* Secret can only be created */);
		noFindById.add("StopWatch" /* StopWatch can only be created and deleted */);
		noFindById.add("TrackedTime" /* Only a list of TrackedTime can be retrieved */);
		return reader.getResponseTypeOrSchemaNames(e -> !noFindById.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithPatch() {
		Set<String> noPatch = new HashSet<String>(EXCLUDED_RESPONSES);
		noPatch.add("AccessToken" /* AccessToken cannot be patched */);
		noPatch.add("ActivityPub" /* ActivityPub cannot be patched */);
		noPatch.add("AnnotatedTag" /* AnnotatedTag is read-onlyd */);
		noPatch.add("Branch" /* Branch cannot be patched */);
		noPatch.add("CombinedStatus" /* CombinedStatus is read-only */);
		noPatch.add("Commit" /* Commit is read-only */);
		noPatch.add("CommitStatus" /* CommitStatus cannot be patched */);
		noPatch.add("ContentsResponse" /* ContentsResponse cannot be patched */);
		noPatch.add("DeployKey" /* DeployKey cannot be patched */);
		noPatch.add("GPGKey" /* GPGKey cannot be patched */);
		noPatch.add("GeneralAPISettings" /* GeneralAPISettings is read-only */);
		noPatch.add("GeneralAttachmentSettings" /* GeneralAttachmentSettings is read-only */);
		noPatch.add("GeneralRepoSettings" /* GeneralRepoSettings is read-only */);
		noPatch.add("GeneralUISettings" /* GeneralUISettings is read-only */);
		noPatch.add("GitBlobResponse" /* GitBlobResponse is read-only*/);
		noPatch.add("GitTreeResponse" /* GitTreeResponse is read-only */);
		noPatch.add("GitignoreTemplateInfo" /* GitignoreTemplateInfo is read-only */);
		noPatch.add("IssueDeadline" /* Issue deadline can only be created */);
		noPatch.add("LanguageStatistics" /* LanguageStatistics is read-only */);
		noPatch.add("LicenseTemplateInfo" /* LicenseTemplateInfo can only be get by id */);
		noPatch.add("MarkdownRender" /* MarkdownRender can only be created */);
		noPatch.add("MarkupRender" /* MarkupRender can only be created */);
		noPatch.add("NodeInfo" /* NodeInfo is read-only */);
		noPatch.add("Note" /* Note is read-only */);
		noPatch.add("NotificationCount" /* Note is read-only */);
		noPatch.add("OrganizationPermissions" /* OrganizationPermissions is read-only */);
		noPatch.add("Package" /* Package cannot be patched */);
		noPatch.add("PublicKey" /* PublicKey cannot be patched */);
		noPatch.add("PullReview" /* PullReview cannot be patched */);
		noPatch.add("PullReviewComment" /* PullReviewComment is read-only */);
		noPatch.add("PushMirror" /* PushMirror cannot be patched */);
		noPatch.add("Reaction" /* Reaction cannot be patched */);
		noPatch.add("Reference" /* Reaction cannot be patched */);
		noPatch.add("RepoCollaboratorPermission" /* RepoCollaboratorPermission is read-only */);
		noPatch.add("RepoIssueConfig" /* RepoIssueConfig is read-only */);
		noPatch.add("RepoIssueConfigValidation" /* RepoIssueConfigValidation is read-only */);
		noPatch.add("RepoNewIssuePinsAllowed" /* RepoNewIssuePinsAllowed is read-only */);
		noPatch.add("SearchResults" /* SearchResults is read-only */);
		noPatch.add("Secret" /* Secret can only be created */);
		noPatch.add("ServerVersion" /* ServerVersion is read-only */);
		noPatch.add("StopWatch" /* StopWatch is read-only */);
		noPatch.add("Tag" /* Tag cannot be patched */);
		noPatch.add("TopicNames" /* TODO TopicNames cannot be patched but updated!! */);
		noPatch.add("TrackedTime" /* TrackedTime is read-only */);
		noPatch.add("WatchInfo" /* WatchInfo is read-only */);
		noPatch.add("WikiCommitList" /* WikiCommitList is read-only */);
		return reader.getResponseTypeOrSchemaNames(e -> !noPatch.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithCreate() {
		Set<String> noCreate = new HashSet<String>(EXCLUDED_RESPONSES);
		noCreate.add("ActivityPub" /* The post method has additional /inbox which is not expected */);
		noCreate.add("AnnotatedTag" /* AnnotatedTag is read-only */);
		noCreate.add("CombinedStatus" /* CombinedStatus is read-only */);
		noCreate.add("Commit" /* Commit is read-only */);
		noCreate.add("GeneralAPISettings" /* GeneralAPISettings is read-only */);
		noCreate.add("GeneralAttachmentSettings" /* GeneralAttachmentSettings is read-only */);
		noCreate.add("GeneralRepoSettings" /* GeneralRepoSettings is read-only */);
		noCreate.add("GeneralUISettings" /* GeneralUISettings is read-only */);
		noCreate.add("GitBlobResponse" /* GitBlobResponse is read-only*/);
		noCreate.add("GitHook" /*TODO The get method is in /git and the post one level above */);
		noCreate.add("GitTreeResponse" /* GitTreeResponse is read-only */);
		noCreate.add("GitignoreTemplateInfo" /* GitignoreTemplateInfo is read-only */);
		noCreate.add("LanguageStatistics" /* LanguageStatistics is read-only */);
		noCreate.add("LicenseTemplateInfo" /* LicenseTemplateInfo can only be get by id */);
		noCreate.add("MarkdownRender" /* TODO MarkdownRender schema is text/html, we don't support this currently*/);
		noCreate.add("MarkupRender" /* MarkupRender schema is text/html, we don't support this currently */);
		noCreate.add("NodeInfo" /* NodeInfo is read-only */);
		noCreate.add("Note" /* Note is read-only */);
		noCreate.add("NotificationCount" /* Note is read-only */);
		noCreate.add("NotificationThread" /* NotificationThread cannot be created */);
		noCreate.add("OrganizationPermissions" /* OrganizationPermissions is read-only */);
		noCreate.add("Package" /* Package cannot be created */);
		noCreate.add("PullReviewComment" /* PullReviewComment is read-only */);
		noCreate.add("Reference" /* Reference cannot be created is only part of ReferenceList */);
		noCreate.add("RepoCollaboratorPermission" /* RepoCollaboratorPermission is read-only */);
		noCreate.add("RepoIssueConfig" /* RepoIssueConfig is read-only */);
		noCreate.add("RepoIssueConfigValidation" /* RepoIssueConfigValidation is read-only */);
		noCreate.add("RepoNewIssuePinsAllowed" /* RepoNewIssuePinsAllowed is read-only */);
		noCreate.add("SearchResults" /* SearchResults is read-only */);
		noCreate.add("Secret" /* Secret can only be created */);
		noCreate.add("ServerVersion" /* ServerVersion is read-only */);
		noCreate.add("StopWatch" /* StopWatch is read-only */);
		noCreate.add("TopicNames" /* TopicNames cannot be created but updated */);
		noCreate.add("WatchInfo" /* WatchInfo is read-only */);
		noCreate.add("WikiCommitList" /* WikiCommitList is read-only */);
		return reader.getResponseTypeOrSchemaNames(e -> !noCreate.contains(e.getKey()));
	}
	
	private static Stream<String> responseTypesWithDelete() {
		Set<String> noDelete = new HashSet<String>(EXCLUDED_RESPONSES);
		noDelete.add("ActivityPub" /* ActivityPub cannot be deleted */);
		noDelete.add("AnnotatedTag" /* AnnotatedTag is read-only */);
		noDelete.add("CombinedStatus" /* CombinedStatus is read-only */);
		noDelete.add("Commit" /* Commit is read-only */);
		noDelete.add("CommitStatus" /* CommitStatus cannot be deleted */);
		noDelete.add("GeneralAPISettings" /* GeneralAPISettings is read-only */);
		noDelete.add("GeneralAttachmentSettings" /* GeneralAttachmentSettings is read-only */);
		noDelete.add("GeneralRepoSettings" /* GeneralRepoSettings is read-only */);
		noDelete.add("GeneralUISettings" /* GeneralUISettings is read-only */);
		noDelete.add("GitBlobResponse" /* GitBlobResponse is read-only*/);
		noDelete.add("GitTreeResponse" /* GitTreeResponse is read-only */);
		noDelete.add("GitignoreTemplateInfo" /* GitignoreTemplateInfo is read-only */);
		noDelete.add("IssueDeadline" /* Issue deadline can only be created */);
		noDelete.add("LanguageStatistics" /* LanguageStatistics is read-only */);
		noDelete.add("LicenseTemplateInfo" /* LicenseTemplateInfo can only be get by id */);
		noDelete.add("MarkdownRender" /* TODO MarkdownRender schema is text/html, we don't support this currently*/);
		noDelete.add("MarkupRender" /* MarkupRender schema is text/html, we don't support this currently */);
		noDelete.add("NodeInfo" /* NodeInfo is read-only */);
		noDelete.add("Note" /* Note is read-only */);
		noDelete.add("NotificationCount" /* Note is read-only */);
		noDelete.add("NotificationThread" /* NotificationThread cannot be created */);
		noDelete.add("OrganizationPermissions" /* OrganizationPermissions is read-only */);
		noDelete.add("PullReviewComment" /* PullReviewComment is read-only */);
		noDelete.add("Reference" /* Reference cannot be deleted */);
		noDelete.add("RepoCollaboratorPermission" /* RepoCollaboratorPermission is read-only */);
		noDelete.add("RepoIssueConfig" /* RepoIssueConfig is read-only */);
		noDelete.add("RepoIssueConfigValidation" /* RepoIssueConfigValidation is read-only */);
		noDelete.add("RepoNewIssuePinsAllowed" /* RepoNewIssuePinsAllowed is read-only */);
		noDelete.add("SearchResults" /* SearchResults is read-only */);
		noDelete.add("Secret" /* Secret can only be created */);
		noDelete.add("ServerVersion" /* ServerVersion is read-only */);
		noDelete.add("StopWatch" /* StopWatch is read-only */);
		noDelete.add("TopicNames" /*  TODOTopicNames have no findById but only list. Hence it cannot be found. */);
		noDelete.add("WikiCommitList" /* WikiCommitList is read-only */);
		return reader.getResponseTypeOrSchemaNames(e -> !noDelete.contains(e.getKey()));
	}
	
}
