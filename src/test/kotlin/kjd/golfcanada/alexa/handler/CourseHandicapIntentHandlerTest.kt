package kjd.golfcanada.alexa.handler

import com.amazon.ask.attributes.AttributesManager
import com.amazon.ask.dispatcher.request.handler.HandlerInput
import com.amazon.ask.model.Intent
import com.amazon.ask.model.IntentRequest
import com.amazon.ask.model.RequestEnvelope
import com.amazon.ask.model.Slot
import com.amazon.ask.model.ui.PlainTextOutputSpeech
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import kjd.golfcanada.alexa.data.UserProfileSession
import kjd.golfcanada.alexa.exception.NoUserDetailsException
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.alexa.util.TemplateFactoryUtil
import kjd.golfcanada.client.api.CoursesApi
import kjd.golfcanada.client.api.FacilitiesApi
import kjd.golfcanada.client.api.MembersApi
import kjd.golfcanada.client.model.*
import kjd.golfcanada.client.provider.ApiClientProvider
import kjd.golfcanada.client.provider.ApiClientWrapper
import java.util.*

class CourseHandicapIntentHandlerTest : DescribeSpec({
    context("canHandle") {
        it("should return true for GOLFCANADA.CourseHandicap") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("GOLFCANADA.CourseHandicap")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = CourseHandicapIntentHandler(apiClientProvider)
            val canHandle = handler.canHandle(input)

            canHandle shouldBe true
        }

        it("should return false for other intents") {
            val templateFactory = TemplateFactoryUtil.getTemplateFactory()
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val input = HandlerInput.builder()
                .withRequestEnvelope(RequestEnvelope.builder()
                    .withRequest(IntentRequest.builder()
                        .withIntent(Intent.builder()
                            .withName("AMAZON.StopIntent")
                            .build()
                        ).build()
                    )
                    .build()
                )
                .withTemplateFactory(templateFactory)
                .build()

            val handler = CourseHandicapIntentHandler(apiClientProvider)
            val canHandle = handler.canHandle(input)

            canHandle shouldBe false
        }
    }

    context("handle") {
        it("should throw NoUserDetailsException when user is not in session") {
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            every { attributesManager.sessionAttributes } returns mutableMapOf()
            
            val input = mockk<HandlerInput>(relaxed = true)
            every { input.requestEnvelope } returns RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.CourseHandicap")
                        .build()
                    )
                    .build()
                )
                .build()
            every { input.attributesManager } returns attributesManager

            val handler = CourseHandicapIntentHandler(apiClientProvider)

            shouldThrow<NoUserDetailsException> {
                handler.handle(input)
            }
        }

        it("should return default course handicap when no facility name is provided") {
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val apiClientWrapper = mockk<ApiClientWrapper>(relaxed = true)
            val coursesApi = mockk<CoursesApi>(relaxed = true)
            
            val user = User(
                id = 123456L,
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com"
            )
            
            val userProfileSession = UserProfileSession(
                id = 123456L,
                firstName = "John",
                lastName = "Doe",
                facilityName = "Blue Springs Golf Club",
                facilityId = 12345L
            )
            
            val tee = CourseHandicapTee(
                name = "White",
                rating = 71.5,
                slope = 125,
                par = 72,
                handicap = "12",
                targetScore = 84,
                playingHandicap = "10"
            )
            
            val course = CourseHandicapCourse(
                id = 1L,
                name = "Championship",
                status = "Active",
                tees = listOf(tee)
            )
            
            val facility = CourseHandicapFacility(
                id = 12345L,
                nationalAssociation = "RCGA",
                name = "Blue Springs Golf Club",
                courses = listOf(course),
                city = "Anytown",
                region = "ON",
                postalCode = "A1A 1A1",
                phone = "123-456-7890"
            )
            
            val courseHandicapInfo = CourseHandicapInfo(
                individualId = user.id!!,
                name = "John Doe",
                handicapPercent = 100,
                facility = facility
            )
            
            every { apiClientWrapper.courses } returns coursesApi
            every { coursesApi.getCourseHandicapInfo(12345L, 100, user.id!!) } returns courseHandicapInfo
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            val sessionAttributes: MutableMap<String, Any> = mutableMapOf(
                UserProfileInterceptor.USER_SESSION_KEY to userProfileSession
            )
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            val input = mockk<HandlerInput>(relaxed = true)
            val requestEnvelope = RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.CourseHandicap")
                        .withSlots(emptyMap())
                        .build()
                    )
                    .build()
                )
                .withContext(com.amazon.ask.model.Context.builder()
                    .withSystem(com.amazon.ask.model.interfaces.system.SystemState.builder()
                        .withUser(com.amazon.ask.model.User.builder()
                            .withAccessToken("test-access-token")
                            .build()
                        )
                        .build()
                    )
                    .build()
                )
                .build()
            every { input.requestEnvelope } returns requestEnvelope
            every { input.attributesManager } returns attributesManager
            every { apiClientProvider.getClient(any()) } returns apiClientWrapper
            every { input.generateTemplateResponse(any(), any()) } answers {
                val templateName = firstArg<String>()
                val dataModel = secondArg<Map<String, Any>>()
                
                val text = when (templateName) {
                    "CourseHandicapIntentResponse" -> 
                        "Your course handicap at ${dataModel["courseName"]} from the ${dataModel["defaultTee"]} tees is ${dataModel["courseHandicap"]}."
                    else -> "Unexpected template: $templateName"
                }
                
                val response = com.amazon.ask.model.Response.builder()
                    .withOutputSpeech(PlainTextOutputSpeech.builder().withText(text).build())
                    .withShouldEndSession(false)
                    .build()
                Optional.of(response)
            }

            val handler = CourseHandicapIntentHandler(apiClientProvider)

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldContain "Blue Springs Golf Club"
            outputSpeech.text shouldContain "12"
            outputSpeech.text shouldContain "White"
        }

        it("should return course handicap for specific facility when facility name is provided") {
            val apiClientProvider = mockk<ApiClientProvider>(relaxed = true)
            val apiClientWrapper = mockk<ApiClientWrapper>(relaxed = true)
            val facilitiesApi = mockk<FacilitiesApi>(relaxed = true)
            val coursesApi = mockk<CoursesApi>(relaxed = true)
            
            val user = User(
                id = 123456L,
                firstName = "John",
                lastName = "Doe",
                email = "john@example.com"
            )
            
            val userProfileSession = UserProfileSession(
                id = 123456L,
                firstName = "John",
                lastName = "Doe"
            )
            
            val facilitySearchResult = FacilitySearchResult(
                id = 20679L,
                name = "Glen Abbey Golf Club",
                city = "Oakville",
                region = "ON",
                nationalAssociation = "RCGA"
            )
            
            val searchResponse = FacilitySearchResponse(
                totalCount = 1,
                facilities = listOf(facilitySearchResult)
            )
            
            val tee = CourseHandicapTee(
                name = "Blue",
                rating = 73.5,
                slope = 135,
                par = 72,
                handicap = "15",
                targetScore = 87,
                playingHandicap = "13"
            )
            
            val course = CourseHandicapCourse(
                id = 20679L,
                name = "Championship",
                status = "Active",
                tees = listOf(tee)
            )
            
            val facility = CourseHandicapFacility(
                id = 20679L,
                nationalAssociation = "RCGA",
                name = "Glen Abbey Golf Club",
                courses = listOf(course),
                city = "Oakville",
                region = "ON",
                postalCode = "L6M 1J6",
                phone = "905-844-1800"
            )
            
            val courseHandicapInfo = CourseHandicapInfo(
                individualId = user.id!!,
                name = "John Doe",
                handicapPercent = 100,
                facility = facility
            )
            
            every { apiClientWrapper.facilities } returns facilitiesApi
            every { apiClientWrapper.courses } returns coursesApi
            every { facilitiesApi.searchFacilities(10, null, "Glen Abbey") } returns searchResponse
            every { coursesApi.getCourseHandicapInfo(20679L, 100, user.id!!) } returns courseHandicapInfo
            
            val attributesManager = mockk<AttributesManager>(relaxed = true)
            val sessionAttributes: MutableMap<String, Any> = mutableMapOf(
                UserProfileInterceptor.USER_SESSION_KEY to userProfileSession
            )
            every { attributesManager.sessionAttributes } returns sessionAttributes
            
            val facilityNameSlot = Slot.builder()
                .withName("FacilityName")
                .withValue("Glen Abbey")
                .build()
            
            val input = mockk<HandlerInput>(relaxed = true)
            val requestEnvelope = RequestEnvelope.builder()
                .withRequest(IntentRequest.builder()
                    .withIntent(Intent.builder()
                        .withName("GOLFCANADA.CourseHandicap")
                        .withSlots(mapOf("FacilityName" to facilityNameSlot))
                        .build()
                    )
                    .build()
                )
                .withContext(com.amazon.ask.model.Context.builder()
                    .withSystem(com.amazon.ask.model.interfaces.system.SystemState.builder()
                        .withUser(com.amazon.ask.model.User.builder()
                            .withAccessToken("test-access-token")
                            .build()
                        )
                        .build()
                    )
                    .build()
                )
                .build()
            every { input.requestEnvelope } returns requestEnvelope
            every { input.attributesManager } returns attributesManager
            every { apiClientProvider.getClient(any()) } returns apiClientWrapper
            every { input.generateTemplateResponse(any(), any()) } answers {
                val templateName = firstArg<String>()
                val dataModel = secondArg<Map<String, Any>>()
                
                val text = when (templateName) {
                    "CourseHandicapIntentSpecificCourseResponse" -> 
                        "Your course handicap at ${dataModel["facilityName"]} from the ${dataModel["teeName"]} tees is ${dataModel["courseHandicap"]}."
                    else -> "Unexpected template: $templateName"
                }
                
                val response = com.amazon.ask.model.Response.builder()
                    .withOutputSpeech(PlainTextOutputSpeech.builder().withText(text).build())
                    .withShouldEndSession(false)
                    .build()
                Optional.of(response)
            }

            val handler = CourseHandicapIntentHandler(apiClientProvider)

            val response = handler.handle(input).get()
            val outputSpeech = response.outputSpeech as PlainTextOutputSpeech

            response.shouldEndSession shouldBe false
            outputSpeech.type shouldBe "PlainText"
            outputSpeech.text shouldContain "Glen Abbey Golf Club"
            outputSpeech.text shouldContain "15"
            outputSpeech.text shouldContain "Blue"
        }
    }
})
