package kjd.golfcanada.client.api

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.DescribeSpec
import kjd.golfcanada.auth.AuthenticationHandler.Companion.DEFAULT_SCOPES
import kjd.golfcanada.client.model.AuthToken
import okhttp3.OkHttpClient

/**
 * Base test class for API tests that require authentication.
 * 
 * This class handles the authentication flow in beforeSpec, making the access token
 * and authenticated OkHttpClient available to extending test classes.
 * 
 * Tests extending this class are only enabled when TEST_USERNAME and TEST_PASSWORD
 * environment variables are set.
 */
@EnabledIf(UsernamePasswordCondition::class)
abstract class AuthenticatedApiTest(body: AuthenticatedApiTest.() -> Unit = {}) : DescribeSpec() {
    
    lateinit var authToken: AuthToken
    lateinit var authenticatedClient: OkHttpClient
    
    /**
     * Shared OkHttpClient for connection pooling across requests
     */
    companion object {
        val sharedClient: OkHttpClient by lazy { OkHttpClient.Builder().build() }
    }
    
    init {
        beforeSpec {
            val username = System.getenv("TEST_USERNAME") ?: ""
            val password = System.getenv("TEST_PASSWORD") ?: ""
            
            val authApi = AuthApi()
            authToken = authApi.getAuthToken(
                AuthApi.GrantTypeGetAuthToken.PASSWORD,
                DEFAULT_SCOPES,
                username = username,
                password = password
            )
            
            // Create an authenticated OkHttpClient that adds the Bearer token
            authenticatedClient = sharedClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer ${authToken.accessToken}")
                        .build()
                    chain.proceed(request)
                }
                .build()
        }
        
        body()
    }
}
