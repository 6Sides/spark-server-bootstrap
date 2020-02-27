package dashflight.sparkbootstrap;

import java.util.List;
import java.util.UUID;
import net.dashflight.data.postgres.PostgresConnectionPool;
import net.dashflight.data.redis.BasicRedisCache;
import org.jdbi.v3.core.Jdbi;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

public class PolicyCache extends BasicRedisCache {

    private static Jdbi jdbi = PostgresConnectionPool.getJdbi();


    public PolicyCache() {
        super();
    }

    public boolean check(UUID userId, String policyId) {
        boolean result;

        try (Jedis client = pool.getResource()) {
            client.select(this.database);

            Transaction trans = client.multi();
            trans.exists(userId.toString());
            trans.get(userId.toString());
            trans.sismember(userId.toString(), policyId);

            List<Object> response = trans.exec();


            if (response.get(0).equals(false)) {
                this.cacheUserPolicies(userId);
                return this.check(userId, policyId);
            } else {
                result = response.get(2).equals(true) || response.get(1).equals("admin");
            }
        }

        return result;
    }

    private void cacheUserPolicies(UUID userId) {
        try (Jedis client = pool.getResource()) {
            Transaction trans = client.multi();

            boolean isAdmin = jdbi.withExtension(PolicyCheckDao.class, dao -> dao.checkIfUserIsAdmin(userId));

            if (isAdmin) {
                trans.set(userId.toString(), "admin");
            } else {
                String[] policyIds = jdbi.withExtension(PolicyCheckDao.class, dao -> dao.getUserPolicies(userId));
                trans.sadd(userId.toString(), policyIds);
            }

            trans.expire(userId.toString(), 3600 * 24);

            trans.exec();
        }
    }
}
