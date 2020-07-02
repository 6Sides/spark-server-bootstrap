package dashflight.sparkbootstrap

import core.instrumentation.ThrottleInstrumentation
import graphql.GraphQL
import graphql.execution.instrumentation.ChainedInstrumentation
import graphql.execution.instrumentation.Instrumentation
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentationOptions
import net.dashflight.data.config.RuntimeEnvironment

data class SparkInitializerConfiguration(
        val contextProvider: SparkRequestContextProvider,
        val runtimeEnvironment: RuntimeEnvironment = RuntimeEnvironment.currentEnvironment,
        val graphqlEndpoint: String,
        val graphQL: GraphQL? = null,
        val port: Int = 8080,
        val allowedHeaders: List<String> = emptyList()
)

data class BuiltSparkInitializerConfiguration(
        val contextProvider: SparkRequestContextProvider,
        val runtimeEnvironment: RuntimeEnvironment,
        val graphqlEndpoint: String,
        val graphQL: GraphQL? = null,
        val port: Int,
        val allowedHeaders: String,
        val instrumentation: Instrumentation,
        val originHeader: String,
        val cookieAttributes: String
)

fun SparkInitializerConfiguration.build(): BuiltSparkInitializerConfiguration {

    check(graphqlEndpoint.indexOf("/") == 0) { "The GraphQL endpoint name must begin with a `/`" }

    val defaultAllowedHeaders: List<String> = listOf(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "Content-Length",
            "Accept",
            "Origin",
            "Cookie",
            "Secure-Fgp"
    )

    val allowedHeaders = listOf(this.allowedHeaders, defaultAllowedHeaders).flatten().joinToString()

    val defaultCookieAttributes = when (runtimeEnvironment) {
        RuntimeEnvironment.PRODUCTION -> "HttpOnly; SameSite=Strict; Secure; "
        else -> "HttpOnly; SameSite=Strict; "
    }

    val originHeader = when (runtimeEnvironment) {
        RuntimeEnvironment.STAGING -> "https://staging.dashflight.net"
        RuntimeEnvironment.PRODUCTION -> "https://www.dashflight.net"
        else -> "http://dashflight.net"
    }

    val instrumentation = ChainedInstrumentation(
        listOf(
            ThrottleInstrumentation(true),
            DataLoaderDispatcherInstrumentation(
                    DataLoaderDispatcherInstrumentationOptions.newOptions().includeStatistics(runtimeEnvironment != RuntimeEnvironment.PRODUCTION)
            )
        )
    )

    return BuiltSparkInitializerConfiguration(
            contextProvider = contextProvider,
            runtimeEnvironment = runtimeEnvironment,
            graphqlEndpoint = graphqlEndpoint,
            graphQL = graphQL,
            port = port,
            allowedHeaders = allowedHeaders,
            instrumentation = instrumentation,
            originHeader = originHeader,
            cookieAttributes = defaultCookieAttributes
    )
}