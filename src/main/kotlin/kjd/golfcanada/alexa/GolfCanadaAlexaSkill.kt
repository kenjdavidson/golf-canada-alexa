package kjd.golfcanada.alexa

import com.amazon.ask.Skill
import com.amazon.ask.SkillStreamHandler
import com.amazon.ask.Skills

class GolfCanadaAlexaSkill: SkillStreamHandler(getSkills()) {
    companion object {
        fun getSkills(): Skill =
            Skills.custom()
                .build()
    }
}