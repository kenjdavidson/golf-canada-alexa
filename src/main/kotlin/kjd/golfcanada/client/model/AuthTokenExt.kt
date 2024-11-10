package kjd.golfcanada.client.model

import org.openapitools.client.infrastructure.Serializer

fun AuthToken.code(): String =
    "${hashCode()}".replace("-","@")

fun AuthToken.toJson(): String =
    Serializer.moshi.adapter(AuthToken::class.java).toJson(this)
