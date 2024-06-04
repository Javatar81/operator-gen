package org.operator.gen.v1alpha1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import io.apisdk.gitea.json.models.CreateRepoOption;
import io.apisdk.gitea.json.models.CreateUserOption;
import io.apisdk.gitea.json.models.Issue;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IssueDependentIT extends DependentsIT<Issue, org.operator.gen.v1alpha1.Issue>{
	
	private String owner = "myissueuser";
	private String repo = "myissuerepo";
	
	@Override
	protected void assertResourceEquals(Issue resource, org.operator.gen.v1alpha1.Issue cr) {
		Assertions.assertEquals(cr.getSpec().getTitle(), resource.getTitle());
		Assertions.assertEquals(cr.getSpec().getBody(), resource.getBody());	
	}
	
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
   	}

	@Override
	Issue apiGet(org.operator.gen.v1alpha1.Issue cr) {
		return apiClient.repos().byOwner(cr.getSpec().getOwnerlogin()).byRepo(cr.getSpec().getRepo()).issues().byIndex(cr.getSpec().getIssueindex()).get();
	}

	@Override
	void edit(org.operator.gen.v1alpha1.Issue cr) {
		cr.getSpec().setBody("changed!");
	}
	
	@AfterEach
   	void tearDown() {
		super.tearDown(org.operator.gen.v1alpha1.Issue.class);
		apiClient.repos().byOwner(owner).byRepo(repo).delete();
		apiClient.admin().users().byUsername(owner).delete();
	}

	@Override
	org.operator.gen.v1alpha1.Issue newCustomResource(String name) {
		org.operator.gen.v1alpha1.Issue org = new org.operator.gen.v1alpha1.Issue();
		org.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		IssueSpec spec = new IssueSpec();
		spec.setTitle(name);
		spec.setOwnerlogin(owner);
		spec.setRepo(repo);
		spec.setBody("This is a new issue");
		spec.setIssueindex(1L);
		org.setSpec(spec);
		return org;
	}

}
