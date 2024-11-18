package kjd.golfcanada.alexa.interceptor

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor
import com.amazon.ask.dispatcher.request.interceptor.ResponseInterceptor
import com.amazon.ask.model.Response
import kjd.golfcanada.alexa.AuthTokenContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Intercepts request and responses setting and clearing the AuthTokenContext for
 * use within the ApiClient.
 */
class AuthenticationInterceptor: RequestInterceptor, ResponseInterceptor {
    override fun process(input: HandlerInput) {
        logger.debug("Attempting to add request AuthToken to AuthTokenContext")

        input
            .requestEnvelope

    }

    override fun process(input: HandlerInput, response: Optional<Response>?) {
        logger.debug("Clearing request AuthTokenContext")

        AuthTokenContext.clearToken()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthenticationInterceptor::class.java)
    }
}