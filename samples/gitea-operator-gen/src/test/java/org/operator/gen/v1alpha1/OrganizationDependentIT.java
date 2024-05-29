package org.operator.gen.v1alpha1;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import io.apisdk.gitea.json.models.Organization;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class OrganizationDependentIT extends DependentsIT<Organization, org.operator.gen.v1alpha1.Organization>{

	@AfterEach
   	void tearDown() {
		super.tearDown(org.operator.gen.v1alpha1.Organization.class);
	}
	
	@Override
	Organization apiGet(org.operator.gen.v1alpha1.Organization org) {
		return apiClient.orgs().byOrg(org.getMetadata().getName()).get();
	}

	@Override
	protected void assertResourceEquals(Organization resource, org.operator.gen.v1alpha1.Organization cr) {
		Assertions.assertEquals(cr.getSpec().getDescription(), resource.getDescription());
		Assertions.assertEquals(cr.getSpec().getEmail(), resource.getEmail());
		Assertions.assertEquals(cr.getSpec().getFull_name(), resource.getFullName());
		Assertions.assertEquals(cr.getSpec().getLocation(), resource.getLocation());
		Assertions.assertEquals(cr.getSpec().getUsername(), resource.getUsername());
		Assertions.assertEquals(cr.getSpec().getVisibility(), resource.getVisibility());
		Assertions.assertEquals(cr.getSpec().getWebsite(), resource.getWebsite());
	}

	@Override
	org.operator.gen.v1alpha1.Organization newCustomResource(String name) {
		org.operator.gen.v1alpha1.Organization org = new org.operator.gen.v1alpha1.Organization();
		org.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		OrganizationSpec spec = new OrganizationSpec();
		spec.setDescription(name);
		spec.setEmail("test@example.org");
		spec.setFull_name(name);
		spec.setLocation(name);
		spec.setUsername(name);
		spec.setVisibility("public");
		spec.setWebsite("https://example.org");
		org.setSpec(spec);
		return org;
	}

	@Override
	void edit(org.operator.gen.v1alpha1.Organization cr) {
		cr.getSpec().setDescription("update1");
		cr.getSpec().setLocation("update2");
		cr.getSpec().setWebsite("https://udpated.example.com");
	}

}
