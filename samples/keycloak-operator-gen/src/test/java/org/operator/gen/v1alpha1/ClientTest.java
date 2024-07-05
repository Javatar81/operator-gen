package org.operator.gen.v1alpha1;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;

import io.apisdk.keycloak.json.models.ClientRepresentation;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClientTest extends DependentsIT<ClientRepresentation, org.operator.gen.v1alpha1.ClientRepresentation>{

	

	@Override
	org.operator.gen.v1alpha1.ClientRepresentation newCustomResource(String name) {
		org.operator.gen.v1alpha1.ClientRepresentation rep = new org.operator.gen.v1alpha1.ClientRepresentation();
		ClientRepresentationSpec spec = new ClientRepresentationSpec();
		spec.setName(name);
		spec.setClientId(name);
		spec.setProtocol("openid-connect");
		spec.setRealm("quarkus");
		spec.setEnabled(true);
		spec.setPublicClient(true);
		spec.setClientAuthenticatorType("client-secret");
		spec.setServiceAccountsEnabled(true);
		spec.setDefaultClientScopes(List.of("email", "profile"));
		//spec.setId(name);
		rep.setSpec(spec);
		rep.setMetadata(new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());
		return rep;
	}

	@Override
	protected void assertResourceEquals(ClientRepresentation resource,
			org.operator.gen.v1alpha1.ClientRepresentation cr) {
		Assertions.assertEquals(cr.getSpec().getName(), resource.getName());
		Assertions.assertEquals(cr.getSpec().getClientId(), resource.getClientId());
		Assertions.assertEquals(cr.getSpec().getProtocol(), resource.getProtocol());
		Assertions.assertEquals(cr.getSpec().getEnabled(), resource.getEnabled());
		Assertions.assertEquals(cr.getSpec().getPublicClient(), resource.getPublicClient());
		Assertions.assertEquals(cr.getSpec().getClientAuthenticatorType(), resource.getClientAuthenticatorType());
		Assertions.assertEquals(cr.getSpec().getServiceAccountsEnabled(), resource.getServiceAccountsEnabled());
		Assertions.assertEquals(new HashSet<>(cr.getSpec().getDefaultClientScopes()), new HashSet<>(resource.getDefaultClientScopes()));
		Assertions.assertEquals(cr.getSpec().getRootUrl(), resource.getRootUrl());
	}

	@Override
	ClientRepresentation apiGet(org.operator.gen.v1alpha1.ClientRepresentation cr) {
		if (cr.getStatus() != null && cr.getStatus().getUuid() != null) {
			return apiClient.admin().realms().byRealm(cr.getSpec().getRealm()).clients().byClientUuid(cr.getStatus().getUuid()).get();
		} else {
			//System.out.println("Status uuid: " + cr.getStatus().getUuid());
			return null;
		}
	}

	@Override
	void edit(org.operator.gen.v1alpha1.ClientRepresentation cr) {
		cr.getSpec().setRootUrl("http://example.com/client");
	}

	@AfterEach
   	void tearDown() {
		super.tearDown(org.operator.gen.v1alpha1.ClientRepresentation.class);
	}

	@Override
	protected boolean readyToUpdate(org.operator.gen.v1alpha1.ClientRepresentation cr) {
		System.out.println("Status: " + cr.getStatus());
		if (cr.getStatus() != null) {
			System.out.println("Uuid: " + cr.getStatus().getUuid());
		}
		return cr.getStatus() != null && cr.getStatus().getUuid() != null;
	}
	
}
