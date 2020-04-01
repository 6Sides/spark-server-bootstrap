package dashflight.sparkbootstrap

import com.google.inject.Inject
import core.directives.auth.PolicyCheckProvider
import core.directives.auth.PolicyChecker

class DashflightPolicyCheckProvider @Inject constructor(private val policyChecker: PolicyChecker<RequestContext>) : PolicyCheckProvider<RequestContext> {

    override fun create(): PolicyChecker<RequestContext> {
        return policyChecker
    }
}