package kjd.golfcanada.alexa

import com.amazon.ask.Skill
import com.amazon.ask.SkillStreamHandler
import com.amazon.ask.Skills
import kjd.golfcanada.alexa.handler.AccountLinkingExceptionHandler
import kjd.golfcanada.alexa.handler.CancelAndStopIntentHandler
import kjd.golfcanada.alexa.handler.HelpIntentHandler
import kjd.golfcanada.alexa.handler.LaunchRequestHandler
import kjd.golfcanada.alexa.handler.SessionEndedRequestHandler
import kjd.golfcanada.alexa.interceptor.AuthenticationRequestInterceptor
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
                    SessionEndedRequestHandler()
                )
                .addRequestInterceptor(AuthenticationRequestInterceptor())
                .addRequestInterceptor(UserProfileInterceptor())
                .addExceptionHandler(AccountLinkingExceptionHandler())
                .build()
        }
    }
}