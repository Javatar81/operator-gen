package org.acme;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.acme.client.ApiClientMethodCallFactory;
import org.acme.client.KiotaMethodCallFactory;
import org.acme.client.ParameterResolver;
import org.acme.gen.CrdResourceGen;
import org.acme.gen.DependentGen;
import org.acme.gen.ReconcilerGen;
import org.acme.read.ModelReader;
import org.acme.read.crud.ResponseTypeMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.expr.Name;

import io.quarkus.smallrye.openapi.runtime.OpenApiConstants;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.Format;

/**
 * Goal which touches a timestamp file.
 */
@Mojo( name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES )
public class OperatorGenMojo
    extends AbstractMojo
{
	
	private static final Logger LOG = LoggerFactory.getLogger(OperatorGenMojo.class);
	
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
	
	@Parameter(property = "schemas", required = false)
	private List<String> schemas = null;
	
	@Parameter(property = "pathParamMappings", required = false)
	private List<String> pathParamMappings = null;
	
    /**
     * Location of the generated source code.
     */
    @Parameter(
            required = true,
            defaultValue = "${project.build.directory}/generated-sources/opcode")
    private File sourceDestinationFolder;
    
    @Parameter(
            required = true,
            defaultValue = "${project.basedir}/src/main/openapi")
    private File openAPISourceFolder;

    private Configuration config;
    
    
    public void execute()
        throws MojoExecutionException
    {
    	config = new Configuration(ConfigProvider.getConfig(), pathParamMappings);
        File f = sourceDestinationFolder;
        if ( !f.exists() )
        {
            f.mkdirs();
        }  
        processOpenApiFolder();
    }
    
    public void processOpenApiFolder() {
    	if (openAPISourceFolder.exists()) {
    		LOG.info("Processing openapi folder");
	    	File[] jsonsFiles = openAPISourceFolder.listFiles(f -> f.getName().endsWith(".json"));
	    	Arrays.stream(jsonsFiles).forEach(this::pocessOpenApiFile);
    	} else {
    		LOG.error("openapi folder not found");
    	}
    }

	private void pocessOpenApiFile(File jsonsFile) {
		OpenAPI openApiDoc = loadOpenApiDoc(jsonsFile, config.getConfig());
		ModelReader responseTypeReader = new ModelReader(openApiDoc);
		responseTypeReader.getResponseTypeOrSchemaNames(e -> schemas.contains(e.getKey()))
			.forEach(r -> processResponseType(openApiDoc, jsonsFile, r));
	}

	private void processResponseType(OpenAPI openApiDoc, File jsonsFile, String responseType) {
		String crdVersion = config.getCrdVersion();
		String basePackage = config.getCrdPackage();
		ResponseTypeMapper mapper = new ResponseTypeMapper(openApiDoc, responseType);
		ParameterResolver resolver = new ParameterResolver(config, openApiDoc);
		ApiClientMethodCallFactory methodCalls = new KiotaMethodCallFactory(mapper, resolver);
		String className = responseType.substring(0, 1).toUpperCase() + responseType.substring(1);
		Name crdName = new Name(new Name(basePackage), className);
		try {
			Path crdResOutputDir = sourceDestinationFolder.toPath().resolve("kubernetes");
			if (!Files.exists(crdResOutputDir)) {
				Files.createDirectory(crdResOutputDir);
			}
			CrdResourceGen resourceGen = new CrdResourceGen(crdResOutputDir.resolve(responseType + ".yaml"), jsonsFile.toPath(), crdName, mapper, resolver);
			resourceGen.create();
		} catch (IOException e) {
			LOG.error(String.format("Error processing response type '%s'", responseType), e);
		}
		
		Name qualifierWithVersion = new Name(basePackage + "." + crdVersion);
		Name resource = new Name(new Name("io.apisdk." + jsonsFile.getName() +".models"), className);
		ReconcilerGen reconciler = new ReconcilerGen(sourceDestinationFolder.toPath(), new Name(qualifierWithVersion, className), resource);
		reconciler.create();
		
		
		DependentGen dependent = new DependentGen(sourceDestinationFolder.toPath(), new Name(qualifierWithVersion, className), resource, methodCalls, mapper);
		dependent.create();
	}
    
    public OpenAPI loadOpenApiDoc(File file, Config config) {
    	try (InputStream is = new FileInputStream(file)) {
	        try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, Format.JSON)) {
	        	OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);
	            return OpenApiProcessor.modelFromStaticFile(openApiConfig, staticFile);
	        }
        } catch (IOException ex) {
            throw new RuntimeException("Could not find [" + OpenApiConstants.BASE_NAME + Format.JSON + "]");
        }
    }
}
