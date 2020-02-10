package dashflight.sparkbootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.directives.auth.PermissionCheck;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.postgresql.util.PGobject;

public class RequestContext implements PermissionCheck {

    private static ObjectMapper mapper = new ObjectMapper();

    private String userId, organization;
    private String token, tokenFgp;

    public RequestContext(String token, String tokenFgp) {
        this.token = token;
        this.tokenFgp = tokenFgp;

        this.authenticate();
    }

    private void authenticate() {
        try {
            URL url = new URL("https://api.dashflight.net/auth/verify");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setRequestProperty("Access-Token", this.token);
            conn.setRequestProperty("Token-Fgp", this.tokenFgp);

            Map<String, Object> response = mapper.readValue(conn.getInputStream(),
                new TypeReference<HashMap<String, Object>>() {}
            );

            this.userId = (String) response.get("user_id");

            try (Connection con = PostgresConnectionPool.getConnection()) {
                String SQL = "select organization from accounts.users where id = ? limit 1";
                PreparedStatement stmt = con.prepareStatement(SQL);

                PGobject uid = new PGobject();
                uid.setType("uuid");
                uid.setValue(this.userId);

                stmt.setObject(1, uid);

                ResultSet res = stmt.executeQuery();

                if (res.next()) {
                    this.organization = res.getString("organization").toLowerCase();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String hasRoleSQL =
            "select description from accounts.user_permissions\n"
                    + "inner join accounts.permissions \n"
                    + "on user_permissions.permission_id = permissions.id \n"
                    + "where user_permissions.user_id = ? and\n"
                    + "permissions.prefix = ? and\n"
                    + "permissions.name = ?";

    @Override
    public boolean hasPermission(String permission) {
        if (this.userId == null) return false;

        if (this.userId.equals("admin")) return true;

        try(Connection conn = PostgresConnectionPool.getConnection()) {
            String[] parts = permission.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("The role is malformed. Valid role: `prefix:permission`");
            }

            PreparedStatement stmt = conn.prepareStatement(hasRoleSQL);
            PGobject id = new PGobject();
            id.setType("uuid");
            id.setValue(this.userId);
            stmt.setObject(1, id);
            stmt.setString(2, parts[0]);
            stmt.setString(3, parts[1]);

            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public String getId() {
        return this.userId;
    }

    public String getOrganization() {
        return this.organization;
    }
}
