package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.GenericIntentException
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.client.provider.ApiClientProvider
import kjd.golfcanada.client.provider.withAuthenticatedClient
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Handles requests for course handicap information.
 * 
 * This handler supports two scenarios:
 * 1. Default Course: When no course name is provided, uses the user's home course from the snapshot
 * 2. Specific Course: When a course name is provided, searches the user's course list
 * 
 * Example utterances:
 * - "What is my course handicap?"
 * - "Tell me my course handicap"
 * - "What is my handicap at Glen Abbey?"
 * - "What is my course handicap for Oakdale Golf Club?"
 */
class CourseHandicapIntentHandler(
    private val apiClientProvider: ApiClientProvider
) : RequestHandler {

    private val logger = LoggerFactory.getLogger(CourseHandicapIntentHandler::class.java)
    
    companion object {
        /**
         * Default handicap percentage used when fetching course handicap information.
         * A value of 100 represents 100% of the player's handicap index.
         */
        private const val DEFAULT_HANDICAP_PERCENT = 100
    }

    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.COURSE_HANDICAP.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        val request = input.requestEnvelope.request as IntentRequest
        val slots = request.intent?.slots

        // Get user profile from session - validate once for all requests
        val sessionAttributes = input.attributesManager.sessionAttributes
        val userProfile = sessionAttributes[UserProfileInterceptor.USER_SESSION_KEY] as? UserProfileSession
            ?: throw NoUserDetailsException()
        
        if (userProfile.id == null) {
            throw NoUserDetailsException()
        }

        val facilityNameSlot = slots?.get("FacilityName")
        val facilityName = facilityNameSlot?.value
        
        val teeNameSlot = slots?.get("TeeName")
        val teeName = teeNameSlot?.value

        return if (facilityName != null) {
            handleSpecificFacility(input, userProfile, facilityName, teeName)
        } else {
            handleDefaultCourse(input, userProfile)
        }
    }

    /**
     * Handles the request for the user's course handicap at their default/home course.
     * 
     * This method:
     * 1. Uses the user's home facility information from session data
     * 2. Fetches course handicap information for the home facility
     * 3. Returns the course handicap
     * 
     * @param input The handler input
     * @param userProfile The user profile session containing user and facility information
     * @return Response with the user's default course handicap information
     */
    private fun handleDefaultCourse(input: HandlerInput, userProfile: UserProfileSession): Optional<Response> {
        logger.info("Default course handicap requested")
        
        try {
            val facilityId = userProfile.facilityId
            val facilityName = userProfile.facilityName
            
            if (facilityId == null) {
                logger.info("No default facility configured for user")
                return input.generateTemplateResponse("CourseHandicapIntentNoDefaultCourseResponse", emptyMap())
            }
            
            logger.info("Using stored facility: $facilityName (ID: $facilityId)")
            
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Fetch course handicap info for the user's default facility
                val courseHandicapInfo = client.courses.getCourseHandicapInfo(
                    facilityId = facilityId,
                    handicapPercent = DEFAULT_HANDICAP_PERCENT,
                    individualId = userProfile.id!!
                )
                
                // Get the first course and first tee for the default response
                val course = courseHandicapInfo.facility?.courses?.firstOrNull()
                val tee = course?.tees?.firstOrNull()
                
                val dataModel = mutableMapOf<String, Any>()
                facilityName?.let { dataModel["courseName"] = it }
                tee?.name?.let { dataModel["defaultTee"] = it }
                tee?.handicap?.let { dataModel["courseHandicap"] = it }
                
                if (tee?.handicap == null) {
                    logger.info("No course handicap available for default facility")
                    input.generateTemplateResponse("CourseHandicapIntentNoDefaultCourseResponse", emptyMap())
                } else {
                    input.generateTemplateResponse("CourseHandicapIntentResponse", dataModel)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch default course handicap: ${e.message}", e)
            throw GenericIntentException("Failed to fetch default course handicap", e)
        }
    }

    /**
     * Handles the request for a course handicap at a specific facility.
     * 
     * This method:
     * 1. Searches for facilities matching the given name
     * 2. Fetches course handicap information for the matched facility
     * 3. Filters by tee name if provided
     * 4. Returns the tee's course handicap and expected score information
     * 
     * @param input The handler input
     * @param userProfile The user profile session containing user information
     * @param facilityName The name of the facility to search for
     * @param teeName The name of the tee (optional) to filter by
     * @return Response with the course handicap information
     */
    private fun handleSpecificFacility(input: HandlerInput, userProfile: UserProfileSession, facilityName: String, teeName: String? = null): Optional<Response> {
        logger.info("Specific course handicap requested for facility: $facilityName")
        
        try {
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Search for facilities matching the name
                val searchResponse = client.facilities.searchFacilities(
                    dollarTop = 10,
                    nationalAssociation = null,
                    text = facilityName
                )
                
                logger.info("Retrieved ${searchResponse.facilities?.size ?: 0} facilities from search")
                
                if (searchResponse.facilities.isNullOrEmpty()) {
                    logger.info("No facilities found matching: $facilityName")
                    val dataModel = mapOf(
                        "facilityName" to facilityName
                    )
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentFacilityNotFoundResponse", dataModel)
                }
                
                // Use the first matching facility
                val matchingFacility = searchResponse.facilities.first()
                
                logger.info("Found matching facility: ${matchingFacility.name} (ID: ${matchingFacility.id})")
                
                // Validate that the facility has an ID
                if (matchingFacility.id == null) {
                    logger.error("Matching facility has null ID: ${matchingFacility.name}")
                    val dataModel = mapOf(
                        "facilityName" to facilityName
                    )
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentFacilityNotFoundResponse", dataModel)
                }
                
                // Fetch course handicap info for the matched facility
                val courseHandicapInfo = client.courses.getCourseHandicapInfo(
                    facilityId = matchingFacility.id,
                    handicapPercent = DEFAULT_HANDICAP_PERCENT,
                    individualId = userProfile.id!!
                )
                
                // Get the first course and filter tees if tee name is provided
                val course = courseHandicapInfo.facility?.courses?.firstOrNull()
                val tee = if (teeName != null) {
                    // Find matching tee by name (case-insensitive)
                    course?.tees?.find { it.name?.equals(teeName, ignoreCase = true) == true }
                } else {
                    // Use first tee if no tee name specified
                    course?.tees?.firstOrNull()
                }
                
                if (tee == null && teeName != null) {
                    logger.info("Tee '$teeName' not found at facility")
                    val dataModel = mapOf(
                        "facilityName" to (courseHandicapInfo.facility?.name ?: facilityName),
                        "teeName" to teeName
                    )
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentTeeNotFoundResponse", dataModel)
                }
                
                val dataModel = mutableMapOf<String, Any>()
                courseHandicapInfo.facility?.name?.let { dataModel["facilityName"] = it }
                tee?.name?.let { dataModel["teeName"] = it }
                tee?.handicap?.let { dataModel["courseHandicap"] = it }
                tee?.playingHandicap?.let { dataModel["playingHandicap"] = it }
                tee?.rating?.let { dataModel["rating"] = it }
                tee?.slope?.let { dataModel["slope"] = it }
                tee?.targetScore?.let { dataModel["targetScore"] = it }
                tee?.par?.let { dataModel["par"] = it }
                
                if (tee?.handicap == null) {
                    logger.info("No course handicap available for facility")
                    input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
                } else {
                    input.generateTemplateResponse("CourseHandicapIntentSpecificCourseResponse", dataModel)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch course handicap for $facilityName: ${e.message}", e)
            throw GenericIntentException("Failed to fetch course handicap", e)
        }
    }
}
