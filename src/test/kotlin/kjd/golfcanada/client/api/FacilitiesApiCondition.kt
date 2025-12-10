package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec
import kotlin.reflect.KClass

class FacilitiesApiCondition : EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>): Boolean {
        return System.getenv().containsKey("TEST_FACILITIES_API")
    }
}
