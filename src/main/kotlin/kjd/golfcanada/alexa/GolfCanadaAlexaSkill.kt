package kjd.golfcanada.alexa

import com.amazon.ask.Skill
import com.amazon.ask.SkillStreamHandler
import com.amazon.ask.Skills
import kjd.golfcanada.alexa.handler.AccountLinkingExceptionHandler
import kjd.golfcanada.alexa.handler.AddScorecardIntentHandler
import kjd.golfcanada.alexa.handler.CancelAndStopIntentHandler
import kjd.golfcanada.alexa.handler.FallbackIntentHandler
import kjd.golfcanada.alexa.handler.FavoritePlayerHistoryIntentHandler
import kjd.golfcanada.alexa.handler.GolfCanadaApiExceptionHandler
import kjd.golfcanada.alexa.handler.HandicapIntentRequestHandler
import kjd.golfcanada.alexa.handler.HelpIntentHandler
import kjd.golfcanada.alexa.handler.LaunchRequestHandler
import kjd.golfcanada.alexa.handler.NavigateHomeIntentHandler
import kjd.golfcanada.alexa.handler.NoUserDetailsExceptionHandler
import kjd.golfcanada.alexa.handler.PlayerProfileHistoryIntentHandler
import kjd.golfcanada.alexa.handler.PlayerProfileMembershipIntentHandler
import kjd.golfcanada.alexa.handler.SessionEndedRequestHandler
import kjd.golfcanada.alexa.interceptor.AuthenticationRequestInterceptor
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor
import kjd.golfcanada.client.provider.ApiClientProvider

class GolfCanadaAlexaSkill: SkillStreamHandler(getSkills()) {
    companion object {
        fun getSkills(): Skill {
            // Create a single instance of ApiClientProvider to be shared across all requests
            // This ensures HTTP client resources are reused while keeping tokens isolated per request
            val apiClientProvider = ApiClientProvider()
            
            return Skills.custom()
                .withSkillId(System.getenv("SKILL_ID") ?: "ERROR - No SKILL_ID provided!!!")
                .addRequestHandlers(
                    LaunchRequestHandler(),
                    HelpIntentHandler(),
                    CancelAndStopIntentHandler(),
                    NavigateHomeIntentHandler(),
                    FallbackIntentHandler(),
                    HandicapIntentRequestHandler(apiClientProvider),
                    FavoritePlayerHistoryIntentHandler(),
                    PlayerProfileMembershipIntentHandler(),
                    PlayerProfileHistoryIntentHandler(),
                    AddScorecardIntentHandler(),
                    SessionEndedRequestHandler()
                )
                .addRequestInterceptors(
                    AuthenticationRequestInterceptor(),
                    UserProfileInterceptor()
                )
                .addExceptionHandlers(
                    AccountLinkingExceptionHandler(),
                    NoUserDetailsExceptionHandler(),
                    GolfCanadaApiExceptionHandler()
                )
                .build()
        }
    }
}