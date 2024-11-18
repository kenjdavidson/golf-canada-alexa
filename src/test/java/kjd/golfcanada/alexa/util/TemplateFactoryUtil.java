package kjd.golfcanada.alexa.util;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.response.template.TemplateFactory;
import com.amazon.ask.response.template.impl.BaseTemplateFactory;
import com.amazon.ask.response.template.loader.TemplateLoader;
import com.amazon.ask.response.template.loader.impl.LocalTemplateFileLoader;
import com.amazon.ask.response.template.renderer.impl.FreeMarkerTemplateRenderer;
import com.amazon.ask.util.impl.JacksonJsonUnmarshaller;

import java.nio.file.Paths;

public abstract class TemplateFactoryUtil {
    @SuppressWarnings("unchecked")
    public static TemplateFactory<HandlerInput, Response> getTemplateFactory() {
        // Configure LocalTemplateFileLoader
        String templatePath = Paths.get("kjd", "golfcanada", "alexa", "responses").toString();
        TemplateLoader<HandlerInput> loader = LocalTemplateFileLoader.builder()
                .withDirectoryPath(templatePath)
                .withFileExtension("ftl")
                .build();

        // Configure FreeMarkerTemplateRenderer
        JacksonJsonUnmarshaller<Response> jacksonJsonUnmarshaller = JacksonJsonUnmarshaller
                .withTypeBinding(Response.class);
        FreeMarkerTemplateRenderer renderer = FreeMarkerTemplateRenderer.builder()
                .withUnmarshaller(jacksonJsonUnmarshaller)
                .build();

        // Configure BaseTemplateFactory
        return BaseTemplateFactory.builder()
                .withTemplateRenderer(renderer)
                .addTemplateLoader(loader)
                .build();
    }
}
