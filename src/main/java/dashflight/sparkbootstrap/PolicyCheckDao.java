package dashflight.sparkbootstrap;

import java.util.UUID;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

@UseClasspathSqlLocator
public interface PolicyCheckDao {

    @SqlQuery
    String[] getUserPolicies(@Bind("userId") UUID userId);

    @SqlQuery
    boolean checkIfUserIsAdmin(@Bind("userId") UUID userId);

}
