package kjd.golfcanada.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import java.util.*

object ResourceBundleUtil {
    private const val RESOURCE_BASE = "content"

    fun getResourceBundle(input: HandlerInput): ResourceBundle =
        input.requestEnvelope.request.locale.let {
            val languageAndCountry = it.split("_")
            ResourceBundle.getBundle(RESOURCE_BASE, Locale.of(languageAndCountry[0], languageAndCountry[1]))
        }
}