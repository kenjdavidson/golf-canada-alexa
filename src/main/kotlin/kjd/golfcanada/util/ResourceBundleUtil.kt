package kjd.golfcanada.util

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import java.util.*

object ResourceBundleUtil {
    private const val RESOURCE_BASE = "content"

    fun getResourceBundle(input: HandlerInput): ResourceBundle =
        input.requestEnvelope.request.locale.let {
            ResourceBundle.getBundle(RESOURCE_BASE, Locale.forLanguageTag(it))
        }
}