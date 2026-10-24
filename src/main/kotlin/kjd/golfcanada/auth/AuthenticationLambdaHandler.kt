package kjd.golfcanada.auth

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import io.micronaut.function.aws.MicronautRequestHandler
import jakarta.inject.Inject

/**
 * AWS Lambda entry point for the Golf Canada authentication service.
 *
 * Extends [MicronautRequestHandler] so that Micronaut bootstraps the application context and
 * injects the [AuthenticationHandler] singleton that contains all the routing and business logic.
 *
 * The SAM template points its `Handler` property at this class.
 */
class AuthenticationLambdaHandler : MicronautRequestHandler<APIGatewayV2HTTPEvent, APIGatewayProxyResponseEvent>() {

    @Inject
    lateinit var handler: AuthenticationHandler

    override fun execute(input: APIGatewayV2HTTPEvent): APIGatewayProxyResponseEvent =
        handler.handleRequest(input)
}
