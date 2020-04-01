package dashflight.sparkbootstrap

import com.google.inject.Inject
import core.directives.auth.PolicyChecker
import net.dashflight.data.postgres.PostgresClient
import java.util.concurrent.ConcurrentHashMap

class DashflightPolicyChecker @Inject constructor(
        private val postgresClient: PostgresClient,
        private val policyCache: PolicyCache
) : PolicyChecker<RequestContext> {

    /**
     * Returning null means user has permission.
     * @param policy
     * @return The response body if the user is not authorized, null otherwise.
     */
    override fun hasPermission(ctx: RequestContext?, policy: String?): Any? {

        val userId = ctx?.userId

        policyIdMap.computeIfAbsent(policy!!) {
            try {
                postgresClient.connection.use { conn ->
                    val stmt = conn.prepareStatement("select id from accounts.policies where prefix = ? and name = ?")
                    val parts = policy.split(":").toTypedArray()
                    stmt.setString(1, parts[0])
                    stmt.setString(2, parts[1])
                    val res = stmt.executeQuery()
                    if (res.next()) {
                        return@computeIfAbsent res.getInt("id")
                    }
                }
            } catch (e: java.sql.SQLException) {
                e.printStackTrace()
            }
            null
        }
        return if (policyCache.check(userId, policyIdMap[policy])) {
            null
        } else UnauthorizedErrorResponse
    }

    companion object {
        private val policyIdMap: MutableMap<String, Int?> = ConcurrentHashMap()

        private object UnauthorizedErrorResponse {
            private const val type = "Authorization Error"
            private const val title = "You are not authorized to access this resource"
            private const val message = "Your account does not have access to this resource. Contact your system administrator if you believe this is a mistake."
        }
    }
}
