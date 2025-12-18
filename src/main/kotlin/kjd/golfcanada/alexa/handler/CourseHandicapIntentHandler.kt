package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.GenericIntentException
import kjd.golfcanada.alexa.exception.NoFacilityFoundException
import kjd.golfcanada.alexa.util.getUserOrThrow
import kjd.golfcanada.client.model.CourseHandicapCourse
import kjd.golfcanada.client.model.CourseHandicapInfo
import kjd.golfcanada.client.model.CourseHandicapTee
import kjd.golfcanada.client.provider.IApiClientProvider
import kjd.golfcanada.client.provider.ApiClientWrapper
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
    private val apiClientProvider: IApiClientProvider
) : RequestHandler {

    private val logger = LoggerFactory.getLogger(CourseHandicapIntentHandler::class.java)
    
    companion object {
        /**
         * Default handicap percentage used when fetching course handicap information.
         * A value of 100 represents 100% of the player's handicap index.
         */
        private const val DEFAULT_HANDICAP_PERCENT = 100
        
        /**
         * Normalizes tee name to handle variations like "Blue White" vs "Blue/White".
         */
        private fun normalizeTeeName(teeName: String): String =
            teeName.replace(" ", "/").replace("and", "").trim()
    }

    override fun canHandle(input: HandlerInput): Boolean =
        input.matches(intentName(IntentName.COURSE_HANDICAP.value))

    override fun handle(input: HandlerInput): Optional<Response> {
        val request = input.requestEnvelope.request as IntentRequest
        val slots = request.intent?.slots

        val userProfile = input.getUserOrThrow()

        val facilityName = slots?.get("FacilityName")?.value
        val teeName = slots?.get("TeeName")?.value

        return apiClientProvider.withAuthenticatedClient(input) { client ->
            val facilityInfo = getFacilityInfo(client, userProfile, facilityName)
            val courseHandicapInfo = getCourseHandicap(client, userProfile, facilityInfo)
            processCourseHandicapInfo(input, courseHandicapInfo, teeName, facilityInfo.name)
        }
    }
    
    /**
     * Data class to hold facility information for handicap lookup.
     */
    private data class FacilityInfo(
        val id: Long,
        val name: String?
    )
    
    /**
     * Gets facility information either from the user's default or by searching for a specific facility.
     * 
     * @param client The API client wrapper
     * @param userProfile The user profile session
     * @param facilityName The name of the facility to search for, or null for default
     * @return FacilityInfo containing the facility ID and name
     * @throws NoFacilityFoundException if no facility is found
     */
    private fun getFacilityInfo(
        client: ApiClientWrapper,
        userProfile: UserProfileSession,
        facilityName: String?
    ): FacilityInfo {
        return if (facilityName != null) {
            logger.info("Searching for facility: $facilityName")
            
            val searchResponse = client.facilities.searchFacilities(
                dollarTop = 10,
                nationalAssociation = null,
                text = facilityName
            )
            
            logger.info("Retrieved ${searchResponse.facilities?.size ?: 0} facilities from search")
            
            if (searchResponse.facilities.isNullOrEmpty()) {
                logger.info("No facilities found matching: $facilityName")
                throw NoFacilityFoundException(facilityName)
            }
            
            searchResponse.facilities.first().let { facility ->
                FacilityInfo(facility.id!!, facility.name)
            }
        } else {
            userProfile.facilityId?.let {
                logger.info("Using default facility: ${userProfile.facilityName} (ID: $it)")
                FacilityInfo(it, userProfile.facilityName)
            } ?: throw NoFacilityFoundException(null)
        }
    }
    
    /**
     * Retrieves course handicap information for the specified facility.
     * 
     * @param client The API client wrapper
     * @param userProfile The user profile session
     * @param facilityInfo The facility information
     * @return CourseHandicapInfo containing the handicap data
     */
    private fun getCourseHandicap(
        client: ApiClientWrapper,
        userProfile: UserProfileSession,
        facilityInfo: FacilityInfo
    ): CourseHandicapInfo {
        return client.courses.getCourseHandicapInfo(
            facilityId = facilityInfo.id,
            handicapPercent = DEFAULT_HANDICAP_PERCENT,
            individualId = userProfile.id!!
        )
    }
    
    /**
     * Processes course handicap info and generates appropriate response based on tee selection.
     */
    private fun processCourseHandicapInfo(
        input: HandlerInput,
        courseHandicapInfo: CourseHandicapInfo,
        teeName: String?,
        facilityName: String?
    ): Optional<Response> {
        val course = courseHandicapInfo.facility?.courses?.firstOrNull()
        val actualFacilityName = courseHandicapInfo.facility?.name ?: facilityName
        val allTees = course?.tees ?: emptyList()
        
        val filteredTees = if (teeName != null) {
            val normalizedTeeName = normalizeTeeName(teeName)
            allTees.filter { tee ->
                val normalizedTee = tee.name?.let { name -> normalizeTeeName(name) }
                normalizedTee?.contains(normalizedTeeName, ignoreCase = true) == true ||
                normalizedTeeName.contains(normalizedTee ?: "", ignoreCase = true) ||
                tee.name?.contains(teeName, ignoreCase = true) == true ||
                teeName.contains(tee.name ?: "", ignoreCase = true)
            }
        } else {
            allTees
        }
        
        if (filteredTees.isEmpty()) {
            logger.info("No tees found at facility")
            val dataModel = mapOf(
                "facilityName" to (actualFacilityName ?: "the facility"),
                "teeName" to teeName
            )
            return input.generateTemplateResponse("CourseHandicapIntentTeeNotFoundResponse", dataModel)
        }
        
        return generateTeesResponse(input, filteredTees, actualFacilityName)
    }
    
    /**
     * Generates a response for the provided list of tees.
     */
    private fun generateTeesResponse(
        input: HandlerInput,
        tees: List<CourseHandicapTee>,
        facilityName: String?
    ): Optional<Response> {
        if (tees.isEmpty()) {
            logger.info("No tees available")
            val dataModel = facilityName?.let { mapOf("facilityName" to it) } ?: emptyMap()
            return input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
        }
        
        val dataModel = mutableMapOf<String, Any>()
        facilityName?.let { dataModel["facilityName"] = it }
        
        val teeList = tees.mapNotNull { tee ->
            tee.name?.let { name ->
                mutableMapOf<String, Any>("name" to name).apply {
                    tee.targetScore?.let { this["targetScore"] = it }
                    tee.handicap?.let { this["courseHandicap"] = it }
                    tee.playingHandicap?.let { this["playingHandicap"] = it }
                    tee.rating?.let { this["rating"] = it }
                    tee.slope?.let { this["slope"] = it }
                    tee.par?.let { this["par"] = it }
                }
            }
        }
        
        if (teeList.isEmpty()) {
            logger.info("No tee scores available")
            return input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
        }
        
        dataModel["tees"] = teeList
        return input.generateTemplateResponse("CourseHandicapIntentResponse", dataModel)
    }

}
