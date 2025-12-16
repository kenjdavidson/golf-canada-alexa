package kjd.golfcanada.alexa.handler

import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.dispatcher.request.handler.RequestHandler
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.Response
import com.amazon.ask.request.Predicates.intentName
import kjd.golfcanada.alexa.IntentName
import kjd.golfcanada.alexa.exception.AccountLinkingException
import kjd.golfcanada.alexa.exception.GenericIntentException
import kjd.golfcanada.alexa.util.getUserOrThrow
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
        val user = input.getUserOrThrow()

        val courseNameSlot = slots?.get("CourseName")
        val courseName = courseNameSlot?.value
        
        val teeNameSlot = slots?.get("TeeName")
        val teeName = teeNameSlot?.value

        return if (courseName != null) {
            handleSpecificCourse(input, user.id!!, courseName, teeName)
        } else {
            handleDefaultCourse(input, user.id!!)
        }
    }

    /**
     * Handles the request for the user's course handicap at their default/home course.
     * 
     * This method:
     * 1. Fetches the user's member snapshot to get the home course name
     * 2. Returns the course handicap from the snapshot
     * 
     * @param input The handler input
     * @param userId The user's ID
     * @return Response with the user's default course handicap information
     */
    private fun handleDefaultCourse(input: HandlerInput, userId: Long): Optional<Response> {
        logger.info("Default course handicap requested")
        
        try {
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Fetch user's snapshot which contains home course and course handicap
                val snapshot = client.members.getSnapshot(userId)
                
                logger.info("Retrieved member snapshot with home course: ${snapshot.homeCourse}")
                
                val dataModel = mutableMapOf<String, Any>()
                snapshot.homeCourse?.let { dataModel["courseName"] = it }
                snapshot.courseHandicap?.let { dataModel["courseHandicap"] = it }
                snapshot.defaultTee?.let { dataModel["defaultTee"] = it }
                
                if (snapshot.courseHandicap == null || snapshot.homeCourse == null) {
                    logger.info("No home course or course handicap available")
                    input.generateTemplateResponse("CourseHandicapIntentNoDefaultCourseResponse", emptyMap())
                } else {
                    input.generateTemplateResponse("CourseHandicapIntentResponse", dataModel)
                }
            }
        } catch (e: AccountLinkingException) {
            logger.warn("No access token available for fetching default course handicap")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch default course handicap: ${e.message}", e)
            throw GenericIntentException("Failed to fetch default course handicap", e)
        }
    }

    /**
     * Handles the request for a course handicap at a specific course.
     * 
     * This method:
     * 1. Fetches the user's course list
     * 2. Searches for a matching course by name
     * 3. Fetches course handicap information for the matched course
     * 4. Filters by tee name if provided
     * 5. Returns the tee's course handicap and expected score information
     * 
     * @param input The handler input
     * @param userId The user's ID
     * @param courseName The name of the course to search for
     * @param teeName The name of the tee (optional) to filter by
     * @return Response with the course handicap information
     */
    private fun handleSpecificCourse(input: HandlerInput, userId: Long, courseName: String, teeName: String? = null): Optional<Response> {
        logger.info("Specific course handicap requested for: $courseName")
        
        try {
            return apiClientProvider.withAuthenticatedClient(input) { client ->
                // Fetch user's course list
                val courses = client.members.getCourseList(userId)
                
                logger.info("Retrieved ${courses.size} courses from user's course list")
                
                if (courses.isEmpty()) {
                    logger.info("No courses found in user's course list")
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentNoCoursesResponse", emptyMap())
                }
                
                // Find matching course - case-insensitive search
                val matchingCourse = courses.find { course ->
                    course.name?.contains(courseName, ignoreCase = true) == true
                }
                
                if (matchingCourse == null) {
                    logger.info("No matching course found for: $courseName")
                    val dataModel = mapOf(
                        "courseName" to courseName
                    )
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentCourseNotFoundResponse", dataModel)
                }
                
                logger.info("Found matching course: ${matchingCourse.name} (ID: ${matchingCourse.id})")
                
                // Validate that the course has an ID
                if (matchingCourse.id == null) {
                    logger.error("Matching course has null ID: ${matchingCourse.name}")
                    val dataModel = mapOf(
                        "courseName" to courseName
                    )
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentCourseNotFoundResponse", dataModel)
                }
                
                // Fetch course handicap info for the matched course
                val courseHandicapInfo = client.courses.getCourseHandicapInfo(
                    facilityId = matchingCourse.id,
                    handicapPercent = DEFAULT_HANDICAP_PERCENT,
                    individualId = userId
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
                    logger.info("Tee '$teeName' not found at course")
                    val dataModel = mapOf(
                        "courseName" to (courseHandicapInfo.facility?.name ?: courseName),
                        "teeName" to teeName
                    )
                    return@withAuthenticatedClient input.generateTemplateResponse("CourseHandicapIntentTeeNotFoundResponse", dataModel)
                }
                
                val dataModel = mutableMapOf<String, Any>()
                courseHandicapInfo.facility?.name?.let { dataModel["courseName"] = it }
                tee?.name?.let { dataModel["teeName"] = it }
                tee?.handicap?.let { dataModel["courseHandicap"] = it }
                tee?.playingHandicap?.let { dataModel["playingHandicap"] = it }
                tee?.rating?.let { dataModel["rating"] = it }
                tee?.slope?.let { dataModel["slope"] = it }
                tee?.targetScore?.let { dataModel["targetScore"] = it }
                tee?.par?.let { dataModel["par"] = it }
                
                if (tee?.handicap == null) {
                    logger.info("No course handicap available for course")
                    input.generateTemplateResponse("CourseHandicapIntentNoCourseHandicapResponse", dataModel)
                } else {
                    input.generateTemplateResponse("CourseHandicapIntentSpecificCourseResponse", dataModel)
                }
            }
        } catch (e: AccountLinkingException) {
            logger.warn("No access token available for fetching course handicap")
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch course handicap for $courseName: ${e.message}", e)
            throw GenericIntentException("Failed to fetch course handicap", e)
        }
    }
}
