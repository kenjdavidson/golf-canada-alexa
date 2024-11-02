package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

fun invalidClientIdResponse(clientId: String) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        body = "Invalid Client Id: '$clientId'"
    }

fun codeOrStateNotProvided(code: String?, state: String?) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        body = "Invalid code '$code' or state '$state'"
    }