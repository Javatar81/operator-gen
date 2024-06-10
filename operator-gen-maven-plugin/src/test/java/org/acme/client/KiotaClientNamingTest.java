package org.acme.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Optional;

import org.acme.Configuration;
import org.acme.read.crud.CrudMapper;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

class KiotaClientNamingTest {

	static class TestEntry<K, V> implements Entry<K, V>{

		K key;
		V value;
		
		TestEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			
			return value;
		}

		@Override
		public V setValue(V value) {
			this.value = value;
			return value;
		}
		
	}
	
	private CrudMapper mapper;
	private ApiClientMethodCallFactory client;
	
	@BeforeEach
	void setUp() {
		mapper = Mockito.mock(CrudMapper.class);
		client = new KiotaMethodCallFactory(mapper, new ParameterResolver(new Configuration(ConfigProvider.getConfig(), new ArrayList<>(), null), null));
	}

	@Test
	void findByIdMethodCall() {
		Mockito.when(mapper.getByIdPath()).thenReturn(Optional.of(new TestEntry<String,PathItem>("/users/{username}", null)));
		mapper.getByIdPath();
		Optional<MethodCallExpr> byIdMethodCall = client.findById(new NameExpr("client"), new NameExpr("username"), new NodeList<>(new NameExpr("username")));
		assertTrue(byIdMethodCall.isPresent());
		assertEquals("client.users().byUsername(username)", byIdMethodCall.get().toString());
	}
	
	@Test
	void createMethodCall() {
		Mockito.when(mapper.createPath()).thenReturn(Optional.of(new TestEntry<String,PathItem>("/users", null)));
		mapper.getByIdPath();
		Optional<MethodCallExpr> byIdMethodCall = client.create(new NameExpr("client"), new NameExpr("createUserOpt"), new NodeList<>(), new NodeList<>(new NameExpr("createUserOpt")));
		assertTrue(byIdMethodCall.isPresent());
		assertEquals("client.users().post(createUserOpt)", byIdMethodCall.get().toString());
	}
}
