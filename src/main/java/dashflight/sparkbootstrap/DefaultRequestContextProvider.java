package dashflight.sparkbootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.postgresql.util.PGobject;

public class DefaultRequestContextProvider implements SparkRequestContextGenerator {

    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object createContext(String token, String tokenFgp) {
        String userId = null;
        Organization organization = null;
        Location homeLocation = null;

        try {
            URL url = new URL("https://api.dashflight.net/auth/verify");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setRequestProperty("Access-Token", token);
            conn.setRequestProperty("Token-Fgp", tokenFgp);

            Map<String, Object> response = mapper.readValue(conn.getInputStream(),
                    new TypeReference<HashMap<String, Object>>() {}
            );

            userId = (String) response.get("user_id");

            try (Connection con = PostgresConnectionPool.getConnection()) {
                String SQL = "select organizations.id as org_id, organizations.name as org_name, locations.id as home_location_id, locations.name as home_location_name from accounts.users "
                        + "inner join accounts.organizations on users.organization_id = organizations.id "
                        + "left join accounts.locations on users.home_location_id = locations.id "
                        + "where users.id = ? limit 1";

                PreparedStatement stmt = con.prepareStatement(SQL);

                PGobject uid = new PGobject();
                uid.setType("uuid");
                uid.setValue(userId);

                stmt.setObject(1, uid);

                ResultSet res = stmt.executeQuery();

                if (res.next()) {
                    organization = new Organization(
                            res.getInt("org_id"),
                            res.getString("org_name")
                    );
                    homeLocation = Location.withFields(
                            res.getInt("home_location_id"),
                            res.getString("home_location_name")
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new RequestContext(userId, organization, homeLocation);
    }
}
