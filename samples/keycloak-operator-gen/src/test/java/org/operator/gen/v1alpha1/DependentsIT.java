package org.operator.gen.v1alpha1;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.kiota.ApiException;

import io.apisdk.keycloak.json.ApiClient;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.kiota.http.vertx.VertXRequestAdapter;
import io.vertx.core.Vertx;
import jakarta.inject.Inject;

public abstract class DependentsIT<T, U extends CustomResource<?, ?>> {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	@Inject
	WebClientProvider webClientProvider;
	ApiClient apiClient;
	@Inject
	Vertx vertx;
	@ConfigProperty(name = "keycloak.api.uri")
	String keycloakApiUri;
	@ConfigProperty(name = "test.kubernetes.namespace")
	String namespace;

	@BeforeEach
   	void setUp() {
		VertXRequestAdapter requestAdapter = new VertXRequestAdapter(webClientProvider.provide());
		requestAdapter.setBaseUrl(keycloakApiUri);
		apiClient = new ApiClient(requestAdapter);
   	}
	
	void tearDown(Class<? extends HasMetadata> crType) {
		while (!client.resources(crType).inNamespace(namespace).list().getItems().isEmpty()) {
    		client.resources(crType).inNamespace(namespace).delete();
    	}
	}
	
	@Test
    void create() {
		U cr = newCustomResource("create");
		client.resource(cr).create();
		await().ignoreException(ApiException.class).atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			T resource = apiGet(client.resource(cr).get());
			assertNotNull(resource);
			assertResourceEquals(resource, cr);
        });
    }
	
	@Test
    void update() throws InterruptedException {
		U cr = newCustomResource("update");
		client.resource(cr).create();
		client.resource(cr).waitUntilCondition(this::readyToUpdate, 20, TimeUnit.SECONDS);
		client.resource(client.resource(cr).get()).edit(o -> {
	        edit(o);
	        return o;
		});
		await().ignoreException(ApiException.class).atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			T resource = apiGet(client.resource(cr).get());
			assertNotNull(resource);
			assertResourceEquals(resource, cr);
        });
    }
	
	@Test
    void delete() {
		U cr = newCustomResource("delete");
		client.resource(cr).create();
		client.resource(cr).waitUntilCondition(r -> r.getStatus() != null, 10, TimeUnit.SECONDS);
		U crUpdated = client.resource(cr).get();
		client.resource(crUpdated).delete();
		await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			try {
				apiGet(crUpdated);
				fail("Api Exception expected");
			} catch (ApiException e) {
				assertEquals(404, e.getResponseStatusCode());
			}
			
        });
    }
	
	protected abstract boolean readyToUpdate(U cr);
	
	protected abstract void assertResourceEquals(T resource, U cr);

	abstract T apiGet(U cr);
	
	abstract void edit(U cr);

	abstract U newCustomResource(String name);
	
}
