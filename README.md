# file_catalog-java-client
Java API to talk with the file_catalog server

## Usage
The client is a Java class that wraps the HTTP requests into methods.

### Get File List
In order to get the file list, just do:

	import edu.wisc.icecube.filecatalog.Client;
	import edu.wisc.icecube.filecatalog.gson.FileList;
	
    final Client c = Client('http://localhost', 8888);
    inal FileList fl = c.get_list();

The parameters `start`, `limit`, and `query` are also supported:

    fl = c.get_list("{\"filesize\": {\"$exists\": true}}");
    
    // getList(final String query, final Integer limit, final Integer start)
    fl = c.get_list("{\"filesize\": {\"$exists\": true}}", 42, 3);

If you only want to provide the start value, pass `null` for `limit`. In case you don't want to set the `query` parameter, use `getList(final Integer limit, final Integer start)`.

### Create a New File
To create a new file (that means a new entry for the metadata for a file) one can just use the `create()` method.

    final Creation creation = c.create("{\"uid\": \"1234\", \"checksum\": \"3d539...f5\", \"locations\": [\"/a/path/to/a/copy/file.dat\"]}");

The passed JSON string needs to fulfill the requirements of the [server](https://github.com/dsschult/file_catalog).

In order to get the link to the created file, use

	final String link = creation.getFile();

If you want to get the `mongo_id`, use

	final String mongoId = Client.getMongoIdFromPath(creation.getFile());

### Get File Meta Data
The metadata for a certain file can be queried by using `get()`. One can either query by `uid` or `mongo_id`. The result is a `com.google.gson.internal.LinkedTreeMap<?, ?>`.

	import com.google.gson.internal.LinkedTreeMap;

	// Query by `uid`
    final LinkedTreeMap<?, ?> metadata = c.getByUid("1234");
    
    // Query by `mongo_id`
    final LinkedTreeMap<?, ?> otherMetadata = c.get("57fd49163a7d4957ca064089");

### Delete a File
To delete the metadata of a file, use `delete()`:

	// Delete by `uid`
    c.deleteByUid("1234");
    
    // Delete by `mongo_id`
    c.delete("57fd49163a7d4957ca064089");

### Update a File
In order to update a file, `update()` can be used. One can use `mongo_id` or `uid` as identifier. `update()` utilizes the cache to find the `etag`. If no `etag` has been cached for this file, it queries the `etag` prior the update.

**Note:** If you want to ignore the cached `etag`, use `clearCache = true`.

	// Update by `uid`
    final LinkedTreeMap<?, ?> updatedMetadata = c.updateByUid("1234", "{\"backupd\": true}");
    
    // Update by `mongo_id`
    final LinkedTreeMap<?, ?> otherUpdatedMetadata = c.update("57fd49163a7d4957ca064089", "{\"backupd\": true}");

	// Don't use the etag cache:
	final LinkedTreeMap<?, ?> anotherUpdatedMetadata = c.updateByUid("1234", "{\"backupd\": true}", true);

### Replace a File
Replacing the metadata of a file is pretty similar to updating it. The difference is that any key that is not passed via the `metadata` will be deleted. Therefore, be sure to add the mandatory fields except for the `uid` and `mongo_id` since they cannot be changed.

**Note:** If you want to ignore the cached `etag`, use `clearCache = true`.

	// Replace by `uid`
    c.replaceByUid("1234", "{\"checksum\": \"3d539...f5\", \"locations\": [\"/a/path/to/a/copy/file.dat\"], \"backup\": False}");
    
    // Replace by `mongo_id`
    c.replace("57fd49163a7d4957ca064089", "{\"checksum\": \"3d539...f5\", \"locations\": [\"/a/path/to/a/copy/file.dat\"], \"backup\": False}");

## Errors
There are two types of errors: client side errors and server side errors. Client side errors are instances of `edu.wisc.icecube.filecatalog.ClientException`. Server side errors are instances of `edu.wisc.icecube.filecatalog.Error`.

### `edu.wisc.icecube.filecatalog.Error`
`edu.wisc.icecube.filecatalog.Error` is a sub class of `org.apache.http.client.HttpResponseException`. Thus, it has also the method `getStatusCode()`.

**Note:** The server usually responds with a JSON string and the error message has the key `message`. Therefore, the class tries to extract that message and tries to store only the message in the attribute `message`.

Subclasses of `edu.wisc.icecube.filecatalog.Error`:
* `BadRequestError`: status code 400
* `NotFoundError`: status code 404
* `ConflictError`: status code 409
* `TooManyRequestsError`: status code 429
* `UnspecificServerError`: status code 500
* `ServiceUnavailableError`: status code 503