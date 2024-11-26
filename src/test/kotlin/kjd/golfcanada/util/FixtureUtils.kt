package kjd.golfcanada.util

import com.amazon.ask.model.Response
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.fasterxml.jackson.databind.ObjectMapper
import kjd.golfcanada.client.model.AuthToken
import java.net.URLEncoder
import java.util.*

fun apiGatewayHttpEventBuilder(
    method: String,
    rawPath: String,
    clientId: String = UUID.randomUUID().toString(),
    queryParameters: Map<String, String> = emptyMap()
): APIGatewayV2HTTPEvent.APIGatewayV2HTTPEventBuilder =
    APIGatewayV2HTTPEvent.builder()
        .withRequestContext(
            APIGatewayV2HTTPEvent.RequestContext.builder()
                .withHttp(
                    APIGatewayV2HTTPEvent.RequestContext.Http.builder().withMethod(method.uppercase()).build()
                )
                .build()
        )
        .withRawPath(rawPath)
        .withQueryStringParameters(
            mapOf("client_id" to clientId).plus(queryParameters)
        )

fun authToken(): AuthToken =
    AuthToken(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        3600,
        tokenType = "bearer"
    )

fun buildBody(parameters: Map<String, String>): String =
    parameters
        .map { (k, v) -> "${k}=${URLEncoder.encode(v, "UTF-8")}" }
        .joinToString("&")
        .let { String(Base64.getUrlEncoder().encode(it.toByteArray())) }

fun responseFixture(fixtureName: String): Response =
    object {}.javaClass.getResource(fixtureName).let {
        ObjectMapper().readValue(it, Response::class.java)
    }
