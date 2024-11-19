package com.srk;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemResponse;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.srk.domain.ConsentItem;

import java.util.Optional;
import java.util.UUID;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
//    @FunctionName("HttpExample")
//    public HttpResponseMessage run(
//            @HttpTrigger(
//                name = "req",
//                methods = {HttpMethod.GET, HttpMethod.POST},
//                authLevel = AuthorizationLevel.ANONYMOUS)
//                HttpRequestMessage<Optional<String>> request,
//            final ExecutionContext context) {
//        context.getLogger().info("Java HTTP trigger processed a request.");
//
//        // Parse query parameter
//        final String query = request.getQueryParameters().get("name");
//        final String name = request.getBody().orElse(query);
//
//        if (name == null) {
//            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a name on the query string or in the request body").build();
//        } else {
//            return request.createResponseBuilder(HttpStatus.OK).body("Hello, " + name).build();
//        }
//    }

    // Initialize Cosmos DB client
    private static final String COSMOS_DB_ENDPOINT = System.getenv("COSMOS_DB_ENDPOINT");
    private static final String COSMOS_DB_KEY = System.getenv("COSMOS_DB_KEY");
    private static final String DATABASE_NAME = System.getenv("COSMOS_DB_DATABASE");
    private static final String CONTAINER_NAME = System.getenv("COSMOS_DB_CONTAINER");

    private static final CosmosClient cosmosClient = new CosmosClientBuilder()
            .endpoint(COSMOS_DB_ENDPOINT)
            .key(COSMOS_DB_KEY)
            .consistencyLevel(ConsistencyLevel.EVENTUAL)
            .buildClient();

    private static final CosmosDatabase database = cosmosClient.getDatabase(DATABASE_NAME);
    private static final CosmosContainer container = database.getContainer(CONTAINER_NAME);

    @FunctionName("ConsentFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "v1.0/consent/search")
            HttpRequestMessage<Optional<ConsentItem>> request,

            final ExecutionContext context) {

        context.getLogger().info("Processing request to write an item to Cosmos DB...");

        // Parse the request body
        Optional<ConsentItem> requestBody = request.getBody();
        if (!requestBody.isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Invalid request: Body is missing or invalid.")
                    .build();
        }

        ConsentItem item = requestBody.get();
        item.setId(UUID.randomUUID().toString()); // Generate a unique ID for the item

        // Write the item to Cosmos DB
        try {
            CosmosItemResponse<ConsentItem> response = container.createItem(item);
            // Check the response
            context.getLogger().info("Cosmos DB Response Status Code: " + response.getStatusCode());
            if (response.getStatusCode() == 201) {
                ConsentItem createdItem = response.getItem();
                if (createdItem == null) {
                    context.getLogger().severe("Item creation succeeded but returned a null item.");
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error: Item created but returned null.")
                            .build();
                }

                // Return success response
                return request.createResponseBuilder(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(createdItem)
                        .build();
            } else {
                context.getLogger().severe("Failed to create item in Cosmos DB. Status Code: " + response.getStatusCode());
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error: Failed to create item in Cosmos DB. Status Code: " + response.getStatusCode())
                        .build();
            }

        } catch (CosmosException e) {
            context.getLogger().severe("Cosmos DB Error: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: Cosmos DB operation failed. " + e.getMessage())
                    .build();
        }
    }
}