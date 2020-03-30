package dashflight.sparkbootstrap;

import com.google.inject.Inject;
import java.util.List;
import java.util.UUID;
import net.dashflight.data.postgres.PostgresClient;
import net.dashflight.data.redis.RedisClient;
import org.jdbi.v3.core.Jdbi;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

/**
 * Caches and verifies user policies
 */
public class PolicyCache {

    private static final int KEY_TTL = 3600 * 24;

    private final Jdbi jdbi;
    private final JedisPool redisPool;

    @Inject
    public PolicyCache(PostgresClient postgresClient, RedisClient redisClient) {
        this.jdbi = postgresClient.getJdbi();
        this.redisPool = redisClient.getPool();
    }

    public boolean check(UUID userId, Integer policyId) {
        if (policyId == null) {
            return false;
        }

        return this.checkHelper(userId, policyId, false);
    }

    private boolean checkHelper(UUID userId, Integer policyId, boolean hasRecursed) {
        boolean result = false;

        try (Jedis client = redisPool.getResource()) {

            Transaction trans = client.multi();
            trans.exists(userId.toString());
            trans.sismember(userId.toString(), policyId.toString());
            trans.sismember(userId.toString(), "admin");

            List<Object> response = trans.exec();


            if (response.get(0).equals(false)) {
                this.cacheUserPolicies(userId);

                if (!hasRecursed) {
                    result = this.checkHelper(userId, policyId, true);
                }
            } else {
                result = response.get(1).equals(true) || response.get(2).equals(true);
            }
        }

        return result;
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
    private void cacheUserPolicies(UUID userId) {
        try (Jedis client = redisPool.getResource()) {
            Transaction trans = client.multi();

            boolean isAdmin = jdbi.withExtension(PolicyCheckDao.class, dao -> dao.checkIfUserIsAdmin(userId));

            if (isAdmin) {
                trans.sadd(userId.toString(), "admin");
            } else {
                String[] policyIds = jdbi.withExtension(PolicyCheckDao.class, dao -> dao.getUserPolicies(userId));
                trans.sadd(userId.toString(), policyIds);
            }

            trans.expire(userId.toString(), KEY_TTL);

            trans.exec();
        }
    }
}
