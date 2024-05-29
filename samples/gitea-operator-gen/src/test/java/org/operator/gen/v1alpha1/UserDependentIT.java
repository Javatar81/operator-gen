package org.operator.gen.v1alpha1;

import org.junit.jupiter.api.Assertions;

import io.apisdk.gitea.json.models.User;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UserDependentIT extends DependentsIT<User, org.operator.gen.v1alpha1.User>{

	@Override
	protected void assertResourceEquals(User resource, org.operator.gen.v1alpha1.User cr) {
		Assertions.assertEquals(cr.getSpec().getDescription(), resource.getDescription());
		Assertions.assertEquals(cr.getSpec().getEmail(), resource.getEmail());
		Assertions.assertEquals(cr.getSpec().getVisibility(), resource.getVisibility());
		Assertions.assertEquals(cr.getSpec().getFull_name(), resource.getFullName());
		Assertions.assertEquals(cr.getSpec().getLogin_name(), resource.getLoginName());
		Assertions.assertEquals(cr.getSpec().getLocation(), resource.getLocation());
	}

	@Override
	User apiGet(org.operator.gen.v1alpha1.User cr) {
		return apiClient.users().byUsername(cr.getMetadata().getName()).get();
	}

	@Override
	void edit(org.operator.gen.v1alpha1.User cr) {
		cr.getSpec().setDescription("update1");
		cr.getSpec().setLocation("update2");
	}

	@Override
	org.operator.gen.v1alpha1.User newCustomResource(String name) {
		org.operator.gen.v1alpha1.User user = new org.operator.gen.v1alpha1.User();
		user.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		UserSpec spec = new UserSpec();
		spec.setDescription(name);
		spec.setPassword("Test124125");
		spec.setEmail("test@example.org");
		spec.setVisibility("public");
		spec.setFull_name(name);
		spec.setLogin_name(name);
		spec.setLocation("testloc");
		spec.setUsername(name);
		spec.setMust_change_password(false);
		user.setSpec(spec);
		return user;
	}

}
