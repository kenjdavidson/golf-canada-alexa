package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

fun invalidAuthenticationRequest(exception: String) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        body = exception
    }

fun invalidAuthenticationRequest(exception: Exception) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        body = exception.localizedMessage
    }

fun invalidClientIdResponse(clientId: String) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 401
        body = "Invalid Client Id: '$clientId'"
    }

fun codeOrStateNotProvided(code: String?, state: String?) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        body = "Invalid code '$code' or state '$state'"
    }

fun queryParameterNotProvided(parameterName: String) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        body = "Required parameter not provided: '${parameterName}'"
    }