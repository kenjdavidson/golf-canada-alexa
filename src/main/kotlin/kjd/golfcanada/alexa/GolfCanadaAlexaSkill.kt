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
import kjd.golfcanada.alexa.interceptor.HandicapLookupInterceptor
import kjd.golfcanada.alexa.interceptor.UserProfileInterceptor

class GolfCanadaAlexaSkill: SkillStreamHandler(getSkills()) {
    companion object {
        fun getSkills(): Skill {
            return Skills.custom()
                .withSkillId(System.getenv("SKILL_ID") ?: "ERROR - No SKILL_ID provided!!!")
                .addRequestHandlers(
                    LaunchRequestHandler(),
                    HelpIntentHandler(),
                    CancelAndStopIntentHandler(),
                    NavigateHomeIntentHandler(),
                    FallbackIntentHandler(),
                    HandicapIntentRequestHandler(),
                    FavoritePlayerHistoryIntentHandler(),
                    PlayerProfileMembershipIntentHandler(),
                    PlayerProfileHistoryIntentHandler(),
                    AddScorecardIntentHandler(),
                    SessionEndedRequestHandler()
                )
                .addRequestInterceptor(AuthenticationRequestInterceptor())
                .addRequestInterceptor(UserProfileInterceptor())
                .addRequestInterceptor(HandicapLookupInterceptor())
                .addExceptionHandler(AccountLinkingExceptionHandler())
                .addExceptionHandler(NoUserDetailsExceptionHandler())
                .addExceptionHandler(GolfCanadaApiExceptionHandler())
                .build()
        }
    }
}