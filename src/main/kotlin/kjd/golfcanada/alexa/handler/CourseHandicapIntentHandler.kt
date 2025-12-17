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
import kjd.golfcanada.client.provider.ApiClientProvider
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
    private val apiClientProvider: ApiClientProvider
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

        // Get user profile from session - validate once for all requests
        val userProfile = input.getUserOrThrow()

        // Parse slots
        val facilityName = slots?.get("FacilityName")?.value
        val teeName = slots?.get("TeeName")?.value

        return apiClientProvider.withAuthenticatedClient(input) { client ->
            // Get facility info (either default or specified)
            val facilityInfo = getFacilityInfo(client, userProfile, facilityName)
            
            // Get course handicap for the facility
            val courseHandicapInfo = getCourseHandicap(client, userProfile, facilityInfo)
            
            // Process and return response based on tee selection
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
            // Search for the specified facility
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
            
            val matchingFacility = searchResponse.facilities.first()
            logger.info("Found matching facility: ${matchingFacility.name} (ID: ${matchingFacility.id})")
            
            if (matchingFacility.id == null) {
                logger.error("Matching facility has null ID: ${matchingFacility.name}")
                throw NoFacilityFoundException(facilityName)
            }
            
            FacilityInfo(matchingFacility.id, matchingFacility.name)
        } else {
            // Use default facility from user profile
            val facilityId = userProfile.facilityId
            
            if (facilityId == null) {
                logger.info("No default facility configured for user")
                throw NoFacilityFoundException(null)
            }
            
            logger.info("Using default facility: ${userProfile.facilityName} (ID: $facilityId)")
            FacilityInfo(facilityId, userProfile.facilityName)
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
     * Generates a response for the provided list of tees.
     * Handles single tee, multiple tees, or all tees with a unified approach.
     * 
     * @param useSingleTeeFormat If true and there's exactly one tee, uses the specific tee response format.
     *                           Otherwise, uses the all-tees format regardless of tee count.
     */
    private fun generateTeesResponse(
        input: HandlerInput,
        tees: List<CourseHandicapTee>,
        facilityName: String?,
        useSingleTeeFormat: Boolean = false
    ): Optional<Response> {
        if (tees.isEmpty()) {
            logger.info("No tees available")
            val dataModel = facilityName?.let { mapOf("facilityName" to it) } ?: emptyMap()
            return input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
        }
        
        // Use single tee format only if explicitly requested and there's exactly one tee
        if (useSingleTeeFormat && tees.size == 1) {
            val tee = tees[0]
            val dataModel = mutableMapOf<String, Any>()
            facilityName?.let { dataModel["facilityName"] = it }
            tee.name?.let { dataModel["teeName"] = it }
            tee.handicap?.let { dataModel["courseHandicap"] = it }
            tee.playingHandicap?.let { dataModel["playingHandicap"] = it }
            tee.rating?.let { dataModel["rating"] = it }
            tee.slope?.let { dataModel["slope"] = it }
            tee.targetScore?.let { dataModel["targetScore"] = it }
            tee.par?.let { dataModel["par"] = it }
            
            return if (tee.handicap == null) {
                logger.info("No course handicap available for specified tee")
                input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
            } else {
                input.generateTemplateResponse("CourseHandicapIntentSpecificCourseResponse", dataModel)
            }
        }
        
        // Use the all-tees response format (for multiple tees or when single tee format not requested)
        val dataModel = mutableMapOf<String, Any>()
        facilityName?.let { dataModel["courseName"] = it }
        
        // Create a list of tees with their scores
        val teeList = tees.mapNotNull { tee ->
            if (tee.name != null && tee.targetScore != null) {
                mapOf("name" to tee.name, "score" to tee.targetScore)
            } else null
        }
        
        if (teeList.isEmpty()) {
            logger.info("No tee scores available")
            return input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
        }
        
        dataModel["tees"] = teeList
        return input.generateTemplateResponse("CourseHandicapIntentAllTeesResponse", dataModel)
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
        
        // Filter tees based on teeName (or include all if no teeName provided)
        val filteredTees = if (teeName != null) {
            val normalizedTeeName = normalizeTeeName(teeName)
            allTees.filter { tee ->
                val normalizedTee = tee.name?.let { name -> normalizeTeeName(name) }
                // Match if the normalized tee name contains the search term or vice versa
                normalizedTee?.contains(normalizedTeeName, ignoreCase = true) == true ||
                normalizedTeeName.contains(normalizedTee ?: "", ignoreCase = true) ||
                tee.name?.contains(teeName, ignoreCase = true) == true ||
                teeName.contains(tee.name ?: "", ignoreCase = true)
            }
        } else {
            allTees
        }
        
        // Handle case where tee was specified but not found
        if (teeName != null && filteredTees.isEmpty()) {
            logger.info("Tee '$teeName' not found at facility")
            val dataModel = mapOf(
                "facilityName" to (actualFacilityName ?: "the facility"),
                "teeName" to teeName
            )
            return input.generateTemplateResponse("CourseHandicapIntentTeeNotFoundResponse", dataModel)
        }
        
        // Generate response for the filtered tees
        // Use single tee format only when a tee was specifically requested and exactly one match was found
        val useSingleTeeFormat = teeName != null && filteredTees.size == 1
        return generateTeesResponse(input, filteredTees, actualFacilityName, useSingleTeeFormat)
    }

}
