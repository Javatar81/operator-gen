package org.operator.gen.v1alpha1;

import org.junit.jupiter.api.AfterEach;

import io.apisdk.keycloak.json.models.RealmRepresentation;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class RealmTest extends DependentsIT<RealmRepresentation, org.operator.gen.v1alpha1.RealmRepresentation>{

	@AfterEach
   	void tearDown() {
		/*try {
			Thread.sleep(500000000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		super.tearDown(org.operator.gen.v1alpha1.RealmRepresentation.class);
	}
	
	@Override
	protected void assertResourceEquals(RealmRepresentation resource,
			org.operator.gen.v1alpha1.RealmRepresentation cr) {
		// TODO Auto-generated method stub
		
	}

	@Override
	RealmRepresentation apiGet(org.operator.gen.v1alpha1.RealmRepresentation cr) {
		return apiClient.admin().realms().byRealm(cr.getMetadata().getName()).get();
	}

	@Override
	void edit(org.operator.gen.v1alpha1.RealmRepresentation cr) {
		cr.getSpec().setDisplayName("quarkuschanged");
	}

	@Override
	org.operator.gen.v1alpha1.RealmRepresentation newCustomResource(String name) {
		org.operator.gen.v1alpha1.RealmRepresentation rep = new org.operator.gen.v1alpha1.RealmRepresentation();
		RealmRepresentationSpec spec = new RealmRepresentationSpec();
		spec.setDisplayName(name);
		spec.setId(name);
		rep.setSpec(spec);
		rep.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		return rep;
	}

	@Override
	protected boolean readyToUpdate(org.operator.gen.v1alpha1.RealmRepresentation cr) {
		return true;
	}

}
