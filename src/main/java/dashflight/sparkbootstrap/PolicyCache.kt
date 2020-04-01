package dashflight.sparkbootstrap

import com.google.inject.Inject
import net.dashflight.data.postgres.PostgresClient
import net.dashflight.data.redis.RedisClient
import org.jdbi.v3.core.Jdbi
import redis.clients.jedis.JedisPool
import java.util.*

/**
 * Caches and verifies user policies
 */
class PolicyCache @Inject constructor(postgresClient: PostgresClient, redisClient: RedisClient) {

    private val jdbi: Jdbi = postgresClient.jdbi!!
    private val redisPool: JedisPool = redisClient.pool

    fun check(userId: UUID?, policyId: Int?): Boolean {
        return if (policyId == null) {
            false
        } else checkHelper(userId, policyId)
    }

    private fun checkHelper(userId: UUID?, policyId: Int, hasRecursed: Boolean = false): Boolean {
        var result = false
        redisPool.resource.use { client ->
            val trans = client.multi()
            trans.exists(userId.toString())
            trans.sismember(userId.toString(), policyId.toString())
            trans.sismember(userId.toString(), "admin")
            val response = trans.exec()
            if (response[0] == false) {
                cacheUserPolicies(userId)
                if (!hasRecursed) {
                    result = checkHelper(userId, policyId, true)
                }
            } else {
                result = response[1] == true || response[2] == true
            }
        }
        return result
    }

    /**
     * Caches user policies and expires them after 24 hours.
     *
     * Caches as follows:
     *
     * If user is NOT admin:
     * userId: set[policy1, policy2, ...]
     *
     * If user IS admin:
     * userId: set[admin]
     */
    private fun cacheUserPolicies(userId: UUID?) {
        redisPool.resource.use { client ->
            val trans = client.multi()
            val isAdmin = jdbi.withExtension<Boolean, PolicyCheckDao, RuntimeException>(PolicyCheckDao::class.java) { dao: PolicyCheckDao -> dao.checkIfUserIsAdmin(userId) }
            if (isAdmin) {
                trans.sadd(userId.toString(), "admin")
            } else {
                val policyIds = jdbi.onDemand(PolicyCheckDao::class.java).getUserPolicies(userId)

                trans.sadd(userId.toString(), *policyIds)
            }
            trans.expire(userId.toString(), KEY_TTL)
            trans.exec()
        }
    }

    companion object {
        private const val KEY_TTL = 3600 * 24
    }

}