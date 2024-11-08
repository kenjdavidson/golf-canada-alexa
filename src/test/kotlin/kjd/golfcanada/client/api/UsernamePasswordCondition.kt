package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec
import kotlin.reflect.KClass

class UsernamePasswordCondition: EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>): Boolean {
        return System.getenv().containsKey("TEST_USERNAME")
                && System.getenv().containsKey("TEST_PASSWORD")
    }
}