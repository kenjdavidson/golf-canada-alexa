package kjd.golfcanada.alexa

import io.micronaut.aws.alexa.MicronautSkillStreamHandler

/**
 * AWS Lambda entry point for the Golf Canada Alexa Skill.
 *
 * Extends [MicronautSkillStreamHandler] so that Micronaut bootstraps the application context,
 * discovers all [com.amazon.ask.dispatcher.request.handler.RequestHandler],
 * [com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor], and
 * [com.amazon.ask.dispatcher.exception.ExceptionHandler] singletons, and wires them into the
 * [com.amazon.ask.Skill] automatically.
 *
 * The skill ID is configured via the `SKILL_ID` environment variable, which maps to the
 * `alexa.skill.id` property defined in `application.yml`.
 */
class GolfCanadaAlexaSkill : MicronautSkillStreamHandler()