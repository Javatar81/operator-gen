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

import io.apisdk.gitea.json.ApiClient;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.kiota.http.vertx.VertXRequestAdapter;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import jakarta.inject.Inject;

public abstract class DependentsIT<T, U extends CustomResource<?, ?>> {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	
	ApiClient apiClient;
	@Inject
	Vertx vertx;
	@Inject
	HeaderAuthentication auth;
	@ConfigProperty(name = "gitea.api.uri")
	String giteaApiUri;
	@ConfigProperty(name = "test.kubernetes.namespace")
	String namespace;

	@BeforeEach
   	void setUp() {
    	WebClient webClient = WebClient.create(vertx);
		WebClientSession webClientSession = WebClientSession.create(webClient);
		auth.addAuthHeaders(webClientSession);
		VertXRequestAdapter requestAdapter = new VertXRequestAdapter(webClientSession);
		requestAdapter.setBaseUrl(giteaApiUri);
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
    void update() {
		U cr = newCustomResource("update");
		client.resource(cr).create();
		client.resource(cr).edit(o -> {
	        edit(cr);
	        return cr;
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
	
	protected abstract void assertResourceEquals(T resource, U cr);

	abstract T apiGet(U cr);
	
	abstract void edit(U cr);

	abstract U newCustomResource(String name);
	
}
