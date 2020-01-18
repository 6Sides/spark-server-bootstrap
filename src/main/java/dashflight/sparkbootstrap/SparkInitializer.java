package dashflight.sparkbootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionInput;
import graphql.GraphQL;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import spark.Spark;

/**
 * Initializes the Spark server based on the current environment.
 */
public class SparkInitializer {

    private static int port = 8080;
    private static RuntimeEnvironment environment;

    private static String graphQLEndpoint;
    private static GraphQL graphQL;
    private static SparkRequestContextGenerator contextGenerator = new DefaultRequestContext();

    public static RuntimeEnvironment getEnvironment() {
        return environment;
    }

    public static void setGraphQLEndpoint(String graphQLEndpoint) {
        SparkInitializer.graphQLEndpoint = graphQLEndpoint;
    }

    public static void setContextGenerator(SparkRequestContextGenerator contextGenerator) {
        SparkInitializer.contextGenerator = contextGenerator;
    }

    /**
     * Overrides the environment specified by the system's `environment` env variable.
     */
    public static void setEnvironment(RuntimeEnvironment environment) {
        SparkInitializer.environment = environment;
    }

    /**
     * Overrides the default port (8080)
     */
    public static void setPort(int port) {
        SparkInitializer.port = port;
    }

    /**
     * Adds a header type to the list of Access-Control-Allow-Headers header
     */
    public static void addAllowedHeader(String headerType) {
        allowedHeaders.add(headerType);
    }

    /**
     * Configures the Spark server with the necessary routes and headers
     * based on the current environment. This must be called AFTER any modifications
     * to the server settings.
     */
    public static void startServer() {
        if (graphQL == null) {
            throw new IllegalStateException("You must specify a GraphQL object to use");
        }
        if (graphQLEndpoint == null) {
            throw new IllegalStateException("You must specify a GraphQL endpoint! Generally it should "
                    + "be the name of the widget the api is written for (e.g. `/lost-and-found`, `/auth`, etc.)");
        } else if (graphQLEndpoint.indexOf("/") != 0) {
            throw new IllegalStateException("The GraphQL endpoint name must begin with a `/`");
        }

        if (environment == null) {
            environment = RuntimeEnvironment.fromString(System.getenv("environment"));
        }

        //============================Basic Configuration=================================

        Spark.port(port);

        Spark.exception(Exception.class,
                (exception, request, response) -> exception.printStackTrace());

        // Block trace requests due to old vulnerability with HttpOnly cookie setting
        Spark.trace("*", (req, res) -> {
            res.status(405);
            return res.raw();
        });

        // Allow options for pre-flight requests
        Spark.options("*", (req, res) -> res.raw());

        // Configure headers
        Spark.before("*", (req, res) -> {
            res.type("application/json");
            res.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Origin", getOriginHeader());
            res.header("Access-Control-Allow-Credentials", "true");
            res.header("Access-Control-Allow-Headers", getAllowedHeaders());
        });

        // Used for health checks by AWS ECS
        Spark.get("/ping", (req, res) -> "pong!");


        //============================GraphQL Configuration=================================

        ObjectMapper mapper = new ObjectMapper();

        Spark.post(graphQLEndpoint, (req, res) -> {
            Object ctx;
            if (environment == RuntimeEnvironment.DEVELOPMENT) {
                ctx = contextGenerator.createContext("admin");
            } else {
                String token = req.headers("Access-Token");
                String tokenFgp = req.cookie("Secure-Fgp");

                System.out.println(token);
                System.out.println(tokenFgp);

                URL url = new URL("https://api.dashflight.net/auth/verify");
                URLConnection conn = url.openConnection();

                conn.setRequestProperty("Access-Token", token);
                conn.setRequestProperty("Token-Fgp", tokenFgp);

                conn.setDoOutput(true);

                Map<String, Object> response = mapper.readValue(conn.getInputStream(),
                        new TypeReference<HashMap<String, Object>>(){});

                if (!((Boolean) response.get("verified"))) {
                    Spark.halt(401,
                            "{\"message\": \"Your current session is invalid. Please login again.\"}");
                }

                ctx = contextGenerator.createContext((String) response.get("user_id"));
            }

            Map<String, Object> data = mapper.readValue(
                        req.body(),
                        new TypeReference<HashMap<String, Object>>(){}
                    );

            try {
                ExecutionInput input = ExecutionInput.newExecutionInput()
                        .query((String) data.get("query"))
                        .variables((Map<String, Object>) data.get("variables"))
                        .context(ctx)
                        .build();

                return mapper.writeValueAsString(graphQL.execute(input).toSpecification());
            } catch (ClassCastException e) {
                Spark.halt(400, "The variables supplied were malformed");
            }

            Spark.halt(400, "Whoops! Something went wrong");

            return "";
        });
    }

    public static void setGraphQL(GraphQL gql) {
        graphQL = gql;
    }

    /**
     * Gets the required cookie attributes for the environment. Append any
     * other attribute to the return value of this method.
     */
    public static String getDefaultCookieAttributes() {
        switch(environment) {
            case DEVELOPMENT:
            case STAGING:
                return "HttpOnly; SameSite=Strict; ";
            case PRODUCTION:
                return "HttpOnly; SameSite=Strict; Secure; ";
        }

        throw new IllegalStateException("The current environment is not supported");
    }

    private static String getOriginHeader() {
        switch(environment) {
            case DEVELOPMENT:
            case STAGING:
                return "http://localhost:3000";
            case PRODUCTION:
                return "https://www.dashflight.net";
        }

        throw new IllegalStateException("The current environment is not supported");
    }

    private static ArrayList<String> allowedHeaders = new ArrayList<>(
        Arrays.asList(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Content-Length",
                "Accept",
                "Origin",
                "Cookie"
        )
    );

    private static String getAllowedHeaders() {
        switch(environment) {
            case DEVELOPMENT:
            case STAGING:
            case PRODUCTION:
                return allowedHeaders.stream().reduce((s1, s2) -> String.format("%s,%s", s1, s2)).orElse("");
        }

        throw new IllegalStateException("The current environment is not supported");
    }

}
