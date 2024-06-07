package org.operator.gen.v1alpha1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.apisdk.gitea.json.models.Comment;
import io.apisdk.gitea.json.models.CreateIssueOption;
import io.apisdk.gitea.json.models.CreateRepoOption;
import io.apisdk.gitea.json.models.CreateUserOption;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CommentDependentIT extends DependentsIT<Comment, org.operator.gen.v1alpha1.Comment>{

	private String owner = "commentuser";
	private String repo = "commentrepo";
	private Long issueindex = 1L;
	
	@BeforeEach
   	void setUp() {
    	super.setUp();
    	CreateUserOption newUser = new CreateUserOption();
    	newUser.setPassword("Test124125");
    	newUser.setEmail(owner + "@example.org");
    	newUser.setVisibility("public");
    	newUser.setFullName(owner);
    	newUser.setLoginName(owner);
    	newUser.setUsername(owner);
		newUser.setMustChangePassword(false);
		newUser.setRestricted(false);
		newUser.setSendNotify(false);
		apiClient.admin().users().post(newUser);
		CreateRepoOption newRepo = new CreateRepoOption();
		newRepo.setDescription(repo);
		newRepo.setName(repo);
		newRepo.setPrivate(false);
		apiClient.admin().users().byUsername(owner).repos().post(newRepo);
		CreateIssueOption newIssue = new CreateIssueOption();
		newIssue.setBody("myissuebody");
		newIssue.setTitle("myissue");
		apiClient.repos().byOwner(owner).byRepo(repo).issues().post(newIssue);
   	}
	
	@AfterEach
   	void tearDown() {
		super.tearDown(org.operator.gen.v1alpha1.Comment.class);
		apiClient.repos().byOwner(owner).byRepo(repo).issues().byIndex(issueindex).delete();
		apiClient.repos().byOwner(owner).byRepo(repo).delete();
		apiClient.admin().users().byUsername(owner).delete();
	}
	
	
	@Override
	protected void assertResourceEquals(Comment resource, org.operator.gen.v1alpha1.Comment cr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	Comment apiGet(org.operator.gen.v1alpha1.Comment cr) {
		if (cr.getStatus() != null) {
			System.out.println("ID: " + cr.getStatus().getId());
			System.out.println(apiClient.repos().byOwner(cr.getSpec().getOwnerlogin()).byRepo(cr.getSpec().getRepo()).issues().comments().byId(cr.getStatus().getId()).get()); 
			return apiClient.repos().byOwner(cr.getSpec().getOwnerlogin()).byRepo(cr.getSpec().getRepo()).issues().comments().byId(cr.getStatus().getId()).get();
		} else {
			return null;
		}
		
	}

	@Override
	void edit(org.operator.gen.v1alpha1.Comment cr) {
		cr.getSpec().setBody("mychangedbody");
	}

	@Override
	org.operator.gen.v1alpha1.Comment newCustomResource(String name) {
		org.operator.gen.v1alpha1.Comment org = new org.operator.gen.v1alpha1.Comment();
		org.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		CommentSpec spec = new CommentSpec();
		spec.setIssueindex(issueindex);
		spec.setOwnerlogin(owner);
		spec.setRepo(repo);
		spec.setBody("This is a new comment");
		org.setSpec(spec);
		return org;
	}

}
