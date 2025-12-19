package kjd.golfcanada.client.provider.mock

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.slf4j.LoggerFactory

/**
 * OkHttp interceptor that intercepts HTTP requests and returns mock responses from JSON resource files.
 * 
 * This interceptor parses the request URL to determine which mock resource file to load, then
 * returns a mock HTTP response with the JSON content from that file.
 * 
 * URL parsing pattern:
 * - Extract service name from the path (e.g., /api/v2/members/123 -> "members")
 * - Extract method/endpoint name (e.g., "getFriends", "getHandicapCalculation")
 * - Extract parameters from the URL path segments
 * 
 * Mock file lookup:
 * - Tries parameter-specific file: client/{service}/{method}_{params}.json
 * - Falls back to generic file: client/{service}/{method}.json
 */
class MockHttpInterceptor : Interceptor {
    private val logger = LoggerFactory.getLogger(MockHttpInterceptor::class.java)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        
        logger.info("Intercepting request: ${request.method} ${url.encodedPath}")
        
        // Parse the URL to determine service and endpoint
        val pathSegments = url.pathSegments
        
        // Expected URL structure: /api/v2/{service}/{id?}/{endpoint?}
        // or simpler: /{service}/{id?}/{endpoint?}
        val (serviceName, methodName, parameters) = parseUrlForMockLookup(pathSegments, url.encodedPath)
        
        if (serviceName.isEmpty() || methodName.isEmpty()) {
            logger.warn("Could not parse service/method from URL: ${url.encodedPath}")
            return createErrorResponse(request, "Could not determine mock resource for URL")
        }
        
        // Try to load the mock resource
        val mockJson = loadMockResponse(serviceName, methodName, parameters)
        
        if (mockJson != null) {
            logger.info("Returning mock response for $serviceName.$methodName")
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(mockJson.toResponseBody("application/json".toMediaType()))
                .build()
        } else {
            logger.warn("No mock resource found for $serviceName.$methodName with params: $parameters")
            return createErrorResponse(request, "Mock resource not found")
        }
    }
    
    /**
     * Parse URL path to determine service name, method name, and parameters.
     * 
     * Common patterns:
     * - /api/v2/members/123/friends -> service="members", method="getFriends", params=["123"]
     * - /api/v2/scores/123 -> service="scores", method="getHandicapCalculation", params=["123"]
     * - /auth/token -> service="auth", method="getAuthToken", params=[]
     * 
     * @return Triple of (serviceName, methodName, parameters)
     */
    private fun parseUrlForMockLookup(pathSegments: List<String>, fullPath: String): Triple<String, String, Map<String, Any?>> {
        if (pathSegments.isEmpty()) {
            return Triple("", "", emptyMap())
        }
        
        // Remove common prefixes like "api", "v2", etc.
        val relevantSegments = pathSegments.filterNot { it in listOf("api", "v1", "v2", "v3") }
        
        if (relevantSegments.isEmpty()) {
            return Triple("", "", emptyMap())
        }
        
        // First segment is usually the service name
        val serviceName = relevantSegments[0]
        
        // Determine method and parameters based on path structure
        val (methodName, params) = when (serviceName) {
            "members" -> {
                when {
                    relevantSegments.size >= 3 && relevantSegments[2] == "friends" -> 
                        Pair("getFriends", mapOf("userId" to relevantSegments[1]))
                    relevantSegments.size >= 3 && relevantSegments[2] == "courses" -> 
                        Pair("getCourseList", mapOf("userId" to relevantSegments[1]))
                    relevantSegments.size >= 3 && relevantSegments[2] == "handicap-history" -> 
                        Pair("getHandicapHistory", mapOf("userId" to relevantSegments[1]))
                    relevantSegments.size >= 3 && relevantSegments[2] == "snapshot" -> 
                        Pair("getSnapshot", mapOf("userId" to relevantSegments[1]))
                    relevantSegments.size >= 2 -> 
                        Pair("getSnapshot", mapOf("userId" to relevantSegments[1]))
                    else -> Pair("", emptyMap())
                }
            }
            "scores" -> {
                when {
                    fullPath.contains("handicap-calculation") && relevantSegments.size >= 2 -> 
                        Pair("getHandicapCalculation", mapOf("individualId" to relevantSegments[1]))
                    relevantSegments.size >= 2 -> 
                        Pair("getScoreData", mapOf("individualId" to relevantSegments[1]))
                    else -> Pair("", emptyMap())
                }
            }
            "auth" -> {
                Pair("getAuthToken", emptyMap())
            }
            "courses" -> {
                when {
                    fullPath.contains("handicap") && relevantSegments.size >= 2 -> 
                        Pair("getCourseHandicapInfo", mapOf("courseId" to relevantSegments[1]))
                    else -> Pair("", emptyMap())
                }
            }
            "facilities" -> {
                Pair("searchFacilities", emptyMap())
            }
            else -> Pair("", emptyMap())
        }
        
        return Triple(serviceName, methodName, params)
    }
    
    /**
     * Load mock response JSON from resource file.
     */
    private fun loadMockResponse(serviceName: String, methodName: String, parameters: Map<String, Any?>): String? {
        // Use MockResourceLoader to load the JSON as a string
        // We need to load it as Map first, then convert back to JSON string
        return try {
            val resource = if (parameters.isNotEmpty()) {
                val paramSuffix = parameters.values.filterNotNull().joinToString("_")
                "client/$serviceName/${methodName}_$paramSuffix.json"
            } else {
                "client/$serviceName/$methodName.json"
            }
            
            // Try parameter-specific file first
            var inputStream = this::class.java.classLoader.getResourceAsStream(resource)
            
            // Fall back to generic file
            if (inputStream == null && parameters.isNotEmpty()) {
                val genericResource = "client/$serviceName/$methodName.json"
                inputStream = this::class.java.classLoader.getResourceAsStream(genericResource)
            }
            
            inputStream?.readBytes()?.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Error loading mock response: ${e.message}", e)
            null
        }
    }
    
    /**
     * Create an error response when mock cannot be found or loaded.
     */
    private fun createErrorResponse(request: okhttp3.Request, message: String): Response {
        val errorBody = """{"error": "$message"}"""
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body(errorBody.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
