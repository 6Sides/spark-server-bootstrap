package dashflight.sparkbootstrap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static Connection conn;

    static {
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://3.136.167.180:5000/?dbname=main&user=postgres&password=db_password");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() {
        return conn;
    }

}
