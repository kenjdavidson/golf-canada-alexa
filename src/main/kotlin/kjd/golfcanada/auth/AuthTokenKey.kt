package kjd.golfcanada.auth

data class AuthTokenKey(
    val state: String,
    val code: String
)
