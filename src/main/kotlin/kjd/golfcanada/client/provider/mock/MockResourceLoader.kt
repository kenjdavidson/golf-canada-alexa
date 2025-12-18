package kjd.golfcanada.client.provider.mock

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.slf4j.LoggerFactory

/**
 * Utility for loading mock API responses from JSON resource files.
 * 
 * This loader implements a naming convention where API method responses are stored as:
 * /resources/client/{serviceName}/{methodName}_{parameter}.json
 * 
 * If a parameter-specific file isn't found, it falls back to:
 * /resources/client/{serviceName}/{methodName}.json
 * 
 * Example:
 * - getMemberHandicap(1) -> client/members/getMemberHandicap_1.json
 * - getFriendsList() -> client/members/getFriendsList.json
 */
object MockResourceLoader {
    private val logger = LoggerFactory.getLogger(MockResourceLoader::class.java)
    
    /**
     * Moshi instance configured with the same settings as the production ApiClient.
     * This ensures date formats and snake_case/camelCase mappings are identical.
     */
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * Load and parse a mock API response from a JSON resource file.
     * 
     * @param serviceName The API service name (e.g., "members", "scores", "auth")
     * @param methodName The API method name (e.g., "getFriendsList", "getHandicapCalculation")
     * @param parameter Optional parameter to include in the file name (e.g., member ID)
     * @param clazz The class type to deserialize the JSON into
     * @return The parsed object, or null if the resource file doesn't exist
     */
    fun <T> loadResource(
        serviceName: String,
        methodName: String,
        parameter: String? = null,
        clazz: Class<T>
    ): T? {
        // Try parameter-specific file first
        if (parameter != null) {
            val parameterizedPath = "client/$serviceName/${methodName}_$parameter.json"
            logger.debug("Attempting to load mock resource: $parameterizedPath")
            
            val resource = this::class.java.classLoader.getResourceAsStream(parameterizedPath)
            if (resource != null) {
                logger.info("Loaded mock resource: $parameterizedPath")
                return parseJson(resource.readBytes().toString(Charsets.UTF_8), clazz)
            }
        }
        
        // Fall back to generic method file
        val genericPath = "client/$serviceName/$methodName.json"
        logger.debug("Attempting to load mock resource: $genericPath")
        
        val resource = this::class.java.classLoader.getResourceAsStream(genericPath)
        if (resource != null) {
            logger.info("Loaded mock resource: $genericPath")
            return parseJson(resource.readBytes().toString(Charsets.UTF_8), clazz)
        }
        
        val paramInfo = parameter?.let { " with parameter $it" } ?: ""
        logger.warn("Mock resource not found for $serviceName.$methodName$paramInfo")
        return null
    }
    
    /**
     * Parse JSON string into the specified type.
     * 
     * @param json The JSON string to parse
     * @param clazz The class type to deserialize into
     * @return The parsed object
     * @throws Exception if JSON parsing fails
     */
    private fun <T> parseJson(json: String, clazz: Class<T>): T {
        val adapter = moshi.adapter(clazz)
        return adapter.fromJson(json) 
            ?: throw IllegalStateException("Failed to parse JSON for ${clazz.simpleName}")
    }
}
