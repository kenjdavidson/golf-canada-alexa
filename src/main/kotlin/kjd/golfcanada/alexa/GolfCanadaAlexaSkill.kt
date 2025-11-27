package kjd.golfcanada.alexa

import com.amazon.ask.Skill
import com.amazon.ask.SkillStreamHandler
import com.amazon.ask.Skills
import kjd.golfcanada.alexa.handler.CancelAndStopIntentHandler
import kjd.golfcanada.alexa.handler.HelpIntentHandler
import kjd.golfcanada.alexa.handler.LaunchRequestHandler
import kjd.golfcanada.alexa.handler.SessionEndedRequestHandler
import kjd.golfcanada.alexa.interceptor.AuthenticationInterceptor

class GolfCanadaAlexaSkill: SkillStreamHandler(getSkills()) {
    companion object {
        fun getSkills(): Skill {
            val authInterceptor = AuthenticationInterceptor()

            return Skills.custom()
                .withSkillId(System.getenv("SKILL_ID") ?: "ERROR - No SKILL_ID provided!!!")
                .addRequestInterceptors(authInterceptor)
                .addResponseInterceptors(authInterceptor)
                .addRequestHandlers(
                    LaunchRequestHandler(),
                    HelpIntentHandler(),
                    CancelAndStopIntentHandler(),
                    SessionEndedRequestHandler()
                )
                .build()
        }
    }
}