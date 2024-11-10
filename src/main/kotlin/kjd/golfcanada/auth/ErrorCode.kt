package kjd.golfcanada.auth

enum class ErrorCode(
    val code: Int,
    val errorResponse: String
) {
    CLIENT_ERROR(1, "invalid_request"),
    INVALID_API_CALL(100, "invalid_request"),
    INVALID_CLIENT_ID(101, "unauthorized_client"),
    INVALID_SECRET(102, "unauthorized_client"),
    CODE_NOT_FOUND(103, "invalid_request"),
    INVALID_STATE(104, "invalid_request"),
    INVALID_CODE(105, "invalid_request"),
    INVALID_RESPONSE_TYPE(106, "invalid_request"),
    INVALID_REDIRECT_URI(107, "invalid_request"),
    INVALID_SCOPE(108, "invalid_scope"),
    INVALID_USERNAME(109, "invalid_request"),
    INVALID_PASSWORD(110, "invalid_request"),
    INVALID_GRANT_TYPE(111, "invalid_grant")
    ;

    override fun toString(): String {
        return "$code"
    }
}