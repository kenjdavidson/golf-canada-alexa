package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

class AuthenticationException(
    val response: APIGatewayProxyResponseEvent
): RuntimeException(response.body) {
}