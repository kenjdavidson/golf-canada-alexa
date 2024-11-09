package kjd.golfcanada.auth.ext

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import kjd.golfcanada.auth.AuthenticationException
import kjd.golfcanada.auth.invalidClientIdResponse

fun APIGatewayV2HTTPEvent.assertHttpMethod(
    method: String,
    responseProvider: () -> APIGatewayProxyResponseEvent
) {
    if (method.uppercase() != this.requestContext.http.method.uppercase()) {
        throw AuthenticationException(responseProvider())
    }
}

fun APIGatewayV2HTTPEvent.assertQueryParameter(
    parameterName: String,
    expectedValue: String?,
    responseProvider: (invalidValue: String) -> APIGatewayProxyResponseEvent
) {
    val parameterValue = this.queryStringParameters.getOrDefault(parameterName, "")
    if (parameterValue !== expectedValue) {
        throw AuthenticationException(responseProvider(parameterValue))
    }
}

fun APIGatewayV2HTTPEvent.assertQueryParameter(
    parameterName: String,
    responseProvider: (invalidValue: String) -> APIGatewayProxyResponseEvent
): String {
    return this.queryStringParameters[parameterName]
        ?: throw AuthenticationException(responseProvider(parameterName))
}