package dashflight.sparkbootstrap

import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator
import org.jdbi.v3.sqlobject.statement.SqlQuery
import java.util.*

@UseClasspathSqlLocator
interface PolicyCheckDao {

    @SqlQuery
    fun getUserPolicies(@Bind("userId") userId: UUID?): Array<String>

    @SqlQuery
    fun checkIfUserIsAdmin(@Bind("userId") userId: UUID?): Boolean
}