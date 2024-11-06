package kjd.golfcanada.auth

interface TokenRepository<KEY, TOKEN> {
    fun store(key: KEY, token: TOKEN)
    fun get(key: KEY): TOKEN?
}