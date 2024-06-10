package org.acme.read.crud;

import java.util.Optional;
import java.util.Map.Entry;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;

public interface CrudMapper {

	Optional<Entry<String, PathItem>> getByIdPath();

	Optional<Entry<String, PathItem>> deletePath();

	Optional<Entry<String, PathItem>> createPath();

	Optional<Entry<String, PathItem>> patchPath();

	Optional<Entry<String, PathItem>> putPath();
	
	Schema getByIdSchema();

	Optional<Schema> getCreateSchema();

	Optional<Schema> getPatchSchema();

	Optional<Schema> getPutSchema();

	Optional<Schema> resolveRef(Schema proxy);


}