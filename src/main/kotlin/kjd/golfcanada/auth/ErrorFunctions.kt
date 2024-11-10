package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

fun invalidAuthenticationRequest(code: ErrorCode) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        headers = mapOf("Content-Type" to "application/json;charset=UTF-8")
        body = """{
        |   "error": "${code.errorResponse}"
        |   "code": "${code.code}"
        |}""".trimMargin()
    }

fun invalidAuthenticationRequest(exception: Exception) =
    APIGatewayProxyResponseEvent().apply {
        statusCode = 400
        headers = mapOf("Content-Type" to "application/json;charset=UTF-8")
        body = """{
        |   "error": "invalid_request"
        |   "cause": "${exception.message}"
        |}""".trimMargin()
    }