package dashflight.sparkbootstrap;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages Postgres connection pool
 */
public class PostgresConnectionPool {

    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource connectionPool;

    private PostgresConnectionPool() {}

    static {
        String dbUrl = String.format("jdbc:postgresql://%s:%s/?dbname=%s&user=%s&password=%s",
                System.getenv("pg_host"),
                System.getenv("pg_port"),
                System.getenv("pg_dbname"),
                System.getenv("pg_username"),
                System.getenv("pg_password"));


        URI dbUri = null;
        try {
            dbUri = new URI(dbUrl);
        } catch (URISyntaxException e) {
            // Kill the program
            System.exit(1);
        }

        if (dbUri.getUserInfo() != null) {
            config.setUsername(dbUri.getUserInfo().split(":")[0]);
            config.setPassword(dbUri.getUserInfo().split(":")[1]);
        }

        config.setJdbcUrl(dbUrl);
        config.addDataSourceProperty("cachePrepStmts" , "true");
        config.addDataSourceProperty("prepStmtCacheSize" , "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit" , "2048");
        connectionPool = new HikariDataSource(config);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> connectionPool.close()));
    }

    public static Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

}
