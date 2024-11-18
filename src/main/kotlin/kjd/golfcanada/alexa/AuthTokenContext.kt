package kjd.golfcanada.alexa

object AuthTokenContext {
    private val context = ThreadLocal<String?>()

    fun setToken(token: String) = this.context.set(token)
    fun clearToken() = this.context.set(null)
}