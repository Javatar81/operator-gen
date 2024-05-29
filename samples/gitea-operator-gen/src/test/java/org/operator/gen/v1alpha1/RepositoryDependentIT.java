package org.operator.gen.v1alpha1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import io.apisdk.gitea.json.models.CreateUserOption;
import io.apisdk.gitea.json.models.Repository;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RepositoryDependentIT extends DependentsIT<Repository, org.operator.gen.v1alpha1.Repository>{

	private String owner = "myuser";
	@Override
	protected void assertResourceEquals(Repository resource, org.operator.gen.v1alpha1.Repository cr) {
		Assertions.assertEquals(cr.getSpec().getDescription(), resource.getDescription());
		Assertions.assertEquals(cr.getSpec().getName(), resource.getName());
		Assertions.assertEquals(cr.getSpec().get_private(), resource.getPrivate());
		Assertions.assertEquals(cr.getSpec().getOwnerlogin(), resource.getOwner().getLogin());
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
   	}
	
	@AfterEach
   	void tearDown() {
		super.tearDown(org.operator.gen.v1alpha1.Repository.class);
		apiClient.admin().users().byUsername(owner).delete();
	}

	@Override
	Repository apiGet(org.operator.gen.v1alpha1.Repository cr) {
		return apiClient.repos().byOwner(cr.getSpec().getOwnerlogin()).byRepo(cr.getMetadata().getName()).get();
	}

	@Override
	void edit(org.operator.gen.v1alpha1.Repository cr) {
		cr.getSpec().setDescription("updatedDesc");
	}

	@Override
	org.operator.gen.v1alpha1.Repository newCustomResource(String name) {
		org.operator.gen.v1alpha1.Repository repo = new org.operator.gen.v1alpha1.Repository();
		repo.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		RepositorySpec spec = new RepositorySpec();
		spec.setDescription(name);
		spec.setName(name);
		spec.set_private(false);
		spec.setOwnerlogin(owner);
		repo.setSpec(spec);
		return repo;
	}

}
