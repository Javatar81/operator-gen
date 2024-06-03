package org.operator.gen.v1alpha1;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.microsoft.kiota.serialization.ParseNode;

import io.apisdk.gitea.json.models.CreateIssueOption;
import io.kiota.serialization.json.JsonParseNodeFactory;

class KiotaParseNodeTest {
	
	@Test
	void unknownPrimitiveType() {
		String primaryAsJson = "{\"apiVersion\":\"gen.operator.org/v1alpha1\",\"kind\":\"Issue\",\"metadata\":{\"creationTimestamp\":\"2024-05-31T11:56:45Z\",\"finalizers\":[\"issues.gen.operator.org/finalizer\"],\"generation\":1,\"name\":\"create\",\"namespace\":\"bschmeli-devjoy-test2\",\"resourceVersion\":\"123890\",\"uid\":\"089809f1-31dd-41f3-bbe2-8ce776be98d9\"},\"spec\":{\"body\":\"This is a new issue\",\"issueindex\":1,\"ownerlogin\":\"myissueuser\",\"repo\":\"myissuerepo\",\"title\":\"create\"},\"status\":{}}";
		ParseNode parseNode = new JsonParseNodeFactory().getParseNode("application/json", new ByteArrayInputStream(primaryAsJson.getBytes(StandardCharsets.UTF_8)));
        parseNode.getChildNode("spec").getObjectValue(CreateIssueOption::createFromDiscriminatorValue);
	}
	
	@Test
	void withoutIssueindex() {
		String primaryAsJson = "{\"apiVersion\":\"gen.operator.org/v1alpha1\",\"kind\":\"Issue\",\"metadata\":{\"creationTimestamp\":\"2024-05-31T11:56:45Z\",\"finalizers\":[\"issues.gen.operator.org/finalizer\"],\"generation\":1,\"name\":\"create\",\"namespace\":\"bschmeli-devjoy-test2\",\"resourceVersion\":\"123890\",\"uid\":\"089809f1-31dd-41f3-bbe2-8ce776be98d9\"},\"spec\":{\"body\":\"This is a new issue\",\"ownerlogin\":\"myissueuser\",\"repo\":\"myissuerepo\",\"title\":\"create\"},\"status\":{}}";
		ParseNode parseNode = new JsonParseNodeFactory().getParseNode("application/json", new ByteArrayInputStream(primaryAsJson.getBytes(StandardCharsets.UTF_8)));
        parseNode.getChildNode("spec").getObjectValue(CreateIssueOption::createFromDiscriminatorValue);
	}
}
