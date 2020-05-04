package dashflight.sparkbootstrap

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.Inject
import com.google.inject.Singleton
import graphql.ExecutionInput
import schemabuilder.processor.pipelines.parsing.dataloaders.DataLoaderRepository
import spark.Request
import spark.Response
import spark.Spark
import java.util.*

/**
 * Initializes the Spark server based on the current environment.
 */
@Singleton
class SparkInitializer @Inject constructor(private val configBuilder: SparkInitializerConfiguration) {

    /**
     * Overrides the environment specified by the system's `environment` env variable.
     */


    /**
     * Overrides the default port (8080)
     */

    /**
     * Adds a header type to the list of Access-Control-Allow-Headers header
     */


    /**
     * Configures the Spark server with the necessary routes and headers
     * based on the current environment. This must be called AFTER any modifications
     * to the server settings.
     */
    fun startServer() {
        val configuration = configBuilder.build()
        //============================Basic Configuration=================================
        Spark.port(configuration.port)
        Spark.exception(Exception::class.java) {
            exception: Exception, _: Request?, _: Response? -> exception.printStackTrace()
        }

        // Block trace requests due to old vulnerability with HttpOnly cookie setting
        Spark.trace("*") { _: Request?, res: Response ->
            res.status(405)
            res.raw()
        }

        // Allow options for pre-flight requests
        Spark.options("*") { _: Request?, res: Response -> res.raw() }

        // Configure headers
        Spark.before("*") { _: Request?, res: Response ->
            res.type("application/json")
            res.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS")
            res.header("Access-Control-Allow-Origin", configuration.originHeader)
            res.header("Access-Control-Allow-Credentials", "true")
            res.header("Access-Control-Allow-Headers", configuration.allowedHeaders)
        }

        // Used for health checks by AWS ECS
        Spark.get("/ping") { _: Request?, _: Response? -> "pong!" }

        if (configuration.graphQL == null) return

        //============================GraphQL Configuration=================================
        val mapper = ObjectMapper().registerModule(KotlinModule())
        // configuration.graphQL.transform { builder: GraphQL.Builder -> builder.instrumentation(configuration.instrumentation) }
        Spark.post(configuration.graphqlEndpoint) { req: Request, _: Response? ->
            val ctx: Any?
            val token = req.headers("Authorization")?.replace("Bearer ", "")
            val tokenFgp = req.cookie("Secure-Fgp")
            ctx = configuration.contextProvider.createContext(token, tokenFgp)

            val body = req.body()
            println(body)

            val data = mapper.readValue(
                    body,
                    object : TypeReference<HashMap<String, Any?>>() {}
            )
            try {
                val input = ExecutionInput.newExecutionInput()
                        .query(data["query"] as String?)
                        .variables(data["variables"] as Map<String?, Any?>?)
                        .dataLoaderRegistry(DataLoaderRepository.dataLoaderRegistry)
                        .context(ctx)
                        .build()
                return@post mapper.writeValueAsString(configuration.graphQL.execute(input)?.toSpecification())
            } catch (e: ClassCastException) {
                Spark.halt(400, "The variables supplied were malformed")
            }
            Spark.halt(400, "Whoops! Something went wrong")
            "An error occurred."
        }
    }
}