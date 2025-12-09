package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.util.getUserOrThrow
import kjd.golfcanada.client.provider.ApiClientProvider
import kjd.golfcanada.client.provider.withAuthenticatedClient
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handles requests for the user's favorite courses list.
 * 
 * This handler fetches the list of courses associated with the member's account
 * from the Golf Canada API and reads them out to the user.
 * 
 * Example utterances:
 * - "What are my favorite courses?"
 * - "List my favorite courses"
 * - "Tell me my course list"
 */
class FavoriteCoursesIntentHandler(
    private val apiClientProvider: ApiClientProvider
) : RequestHandler {

    private val logger = LoggerFactory.getLogger(FavoriteCoursesIntentHandler::class.java)

    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.FAVORITE_COURSES.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        logger.info("Favorite courses requested")
        
        // Get user profile from session
        val user = input.getUserOrThrow()
        
        // Use the withAuthenticatedClient extension to simplify API client access
        return apiClientProvider.withAuthenticatedClient(input) { client ->
            // Fetch user's course list
            val courses = client.members.getCourseList(user.id!!)
            
            logger.info("Retrieved ${courses.size} favorite courses")
            
            // Prepare response data
            val dataModel = mapOf(
                "count" to courses.size,
                "courses" to courses
            )
            
            input.generateTemplateResponse("FavoriteCoursesIntentResponse", dataModel)
        }
    }
}
