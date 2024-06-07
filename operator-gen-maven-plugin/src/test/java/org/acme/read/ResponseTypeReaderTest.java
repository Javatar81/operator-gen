package org.acme.read;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

class ResponseTypeReaderTest {

	private static OpenAPI model;

	@BeforeAll
	public static void setUp() {
		try (InputStream is = ResponseTypeReaderTest.class.getClassLoader().getResourceAsStream("gitea.json")) {
			try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
				OpenApiConfig openApiConfig = new OpenApiConfigImpl(ConfigProvider.getConfig());
				model = OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile);
			}
		} catch (IOException ex) {
			throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
		}
	}

	@ParameterizedTest
	@MethodSource("expectedResponseTypes")
	void read(String responseType) {
		ModelReader analyzer = new ModelReader(model);
		assertTrue(analyzer.getResponseTypeOrSchemaNames(e -> responseType.equals(e.getKey())).findFirst().isPresent());
		
	}

	private static Stream<String> expectedResponseTypes() {
		return Stream.of("AccessToken", "AccessTokenList", "ActivityFeedsList", "ActivityPub", "AnnotatedTag",
				"Attachment", "AttachmentList", "Branch", "BranchList", "BranchProtection", "BranchProtectionList",
				"ChangedFileList", "CombinedStatus", "Comment", "CommentList", "Commit", "CommitList", "CommitStatus",
				"CommitStatusList", "ContentsListResponse", "ContentsResponse", "CronList", "DeployKey",
				"DeployKeyList", "EmailList", "EmptyRepository", "FileDeleteResponse", "FileResponse", "FilesResponse",
				"GPGKey", "GPGKeyList", "GeneralAPISettings", "GeneralAttachmentSettings", "GeneralRepoSettings",
				"GeneralUISettings", "GitBlobResponse", "GitHook", "GitHookList", "GitTreeResponse",
				"GitignoreTemplateInfo", "GitignoreTemplateList", "Hook", "HookList", "Issue", "IssueDeadline",
				"IssueList", "IssueTemplates", "Label", "LabelList", "LabelTemplateInfo", "LabelTemplateList",
				"LanguageStatistics", "LicenseTemplateInfo", "LicenseTemplateList", "MarkdownRender", "MarkupRender",
				"Milestone", "MilestoneList", "NodeInfo", "Note", "NotificationCount", "NotificationThread",
				"NotificationThreadList", "OAuth2Application", "OAuth2ApplicationList", "Organization",
				"OrganizationList", "OrganizationPermissions", "Package", "PackageFileList", "PackageList", "PublicKey",
				"PublicKeyList", "PullRequest", "PullRequestList", "PullReview", "PullReviewComment",
				"PullReviewCommentList", "PullReviewList", "PushMirror", "PushMirrorList", "Reaction", "ReactionList",
				"Reference", "ReferenceList", "Release", "ReleaseList", "RepoCollaboratorPermission", "RepoIssueConfig",
				"RepoIssueConfigValidation", "RepoNewIssuePinsAllowed", "Repository", "RepositoryList", "SearchResults",
				"Secret", "SecretList", "ServerVersion", "StopWatch", "StopWatchList", "StringSlice", "Tag", "TagList",
				"Team", "TeamList", "TimelineList", "TopicListResponse", "TopicNames", "TrackedTime", "TrackedTimeList",
				"User", "UserHeatmapData", "UserList", "UserSettings", "WatchInfo", "WikiCommitList", "WikiPage",
				"WikiPageList", "conflict", "empty", "error", "forbidden", "invalidTopicsError", "notFound",
				"parameterBodies", "redirect", "string", "validationError", "AccessToken", "AccessTokenList",
				"ActivityFeedsList", "ActivityPub", "AnnotatedTag", "Attachment", "AttachmentList", "Branch",
				"BranchList", "BranchProtection", "BranchProtectionList", "ChangedFileList", "CombinedStatus",
				"Comment", "CommentList", "Commit", "CommitList", "CommitStatus", "CommitStatusList",
				"ContentsListResponse", "ContentsResponse", "CronList", "DeployKey", "DeployKeyList", "EmailList",
				"EmptyRepository", "FileDeleteResponse", "FileResponse", "FilesResponse", "GPGKey", "GPGKeyList",
				"GeneralAPISettings", "GeneralAttachmentSettings", "GeneralRepoSettings", "GeneralUISettings",
				"GitBlobResponse", "GitHook", "GitHookList", "GitTreeResponse", "GitignoreTemplateInfo",
				"GitignoreTemplateList", "Hook", "HookList", "Issue", "IssueDeadline", "IssueList", "IssueTemplates",
				"Label", "LabelList", "LabelTemplateInfo", "LabelTemplateList", "LanguageStatistics",
				"LicenseTemplateInfo", "LicenseTemplateList", "MarkdownRender", "MarkupRender", "Milestone",
				"MilestoneList", "NodeInfo", "Note", "NotificationCount", "NotificationThread",
				"NotificationThreadList", "OAuth2Application", "OAuth2ApplicationList", "Organization",
				"OrganizationList", "OrganizationPermissions", "Package", "PackageFileList", "PackageList", "PublicKey",
				"PublicKeyList", "PullRequest", "PullRequestList", "PullReview", "PullReviewComment",
				"PullReviewCommentList", "PullReviewList", "PushMirror", "PushMirrorList", "Reaction", "ReactionList",
				"Reference", "ReferenceList", "Release", "ReleaseList", "RepoCollaboratorPermission", "RepoIssueConfig",
				"RepoIssueConfigValidation", "RepoNewIssuePinsAllowed", "Repository", "RepositoryList", "SearchResults",
				"Secret", "SecretList", "ServerVersion", "StopWatch", "StopWatchList", "StringSlice", "Tag", "TagList",
				"Team", "TeamList", "TimelineList", "TopicListResponse", "TopicNames", "TrackedTime", "TrackedTimeList",
				"User", "UserHeatmapData", "UserList", "UserSettings", "WatchInfo", "WikiCommitList", "WikiPage",
				"WikiPageList", "conflict", "empty", "error", "forbidden", "invalidTopicsError", "notFound",
				"parameterBodies", "redirect", "string", "validationError");
	}
}
