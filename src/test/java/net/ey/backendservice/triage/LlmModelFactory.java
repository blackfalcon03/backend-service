package net.ey.backendservice.triage;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Factory for creating ChatLanguageModel instances dynamically based on environment or .env configuration.
 * Enables zero-code switching between Google Gemini, OpenAI, self-hosted Ollama, and an offline Mock model.
 */
public class LlmModelFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmModelFactory.class);

    public static ChatLanguageModel createModel() {
        String provider = EnvConfig.get("LLM_PROVIDER", "gemini").toLowerCase().trim();
        log.info("Initializing LangChain4j ChatLanguageModel with provider: [{}]", provider);

        return switch (provider) {
            case "gemini" -> buildGeminiModel();
            case "openai" -> buildOpenAiModel();
            case "ollama" -> buildOllamaModel();
            case "mock" -> buildMockModel();
            default -> throw new IllegalArgumentException(
                    "Unsupported LLM_PROVIDER: '" + provider + "'. Supported values: gemini, openai, ollama, mock");
        };
    }

    private static ChatLanguageModel buildGeminiModel() {
        String apiKey = EnvConfig.get("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set. Please specify GEMINI_API_KEY in your .env file, shell environment, or set LLM_PROVIDER=mock for offline demo.");
        }
        String modelName = EnvConfig.get("GEMINI_MODEL", "gemini-1.5-flash");
        log.info("Connecting to Google Gemini model: [{}]", modelName);

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private static ChatLanguageModel buildOpenAiModel() {
        String apiKey = EnvConfig.get("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not set. Please specify OPENAI_API_KEY in your .env file, shell environment, or set LLM_PROVIDER=mock for offline demo.");
        }
        String modelName = EnvConfig.get("OPENAI_MODEL", "gpt-4o-mini");
        log.info("Connecting to OpenAI model: [{}]", modelName);

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private static ChatLanguageModel buildOllamaModel() {
        String baseUrl = EnvConfig.get("OLLAMA_BASE_URL", "http://localhost:11434");
        String modelName = EnvConfig.get("OLLAMA_MODEL", "llama3.2");
        log.info("Connecting to self-hosted Ollama at [{}] with model [{}]", baseUrl, modelName);

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    private static ChatLanguageModel buildMockModel() {
        log.info("Using Mock Offline ChatLanguageModel (useful for local offline SDET testing)");
        return messages -> Response.from(AiMessage.from(
                "### [AI Triage Report (Offline Mock Mode)]\n" +
                "- **Classification**: Product Bug (Regression)\n" +
                "- **Confidence Score**: 95%\n" +
                "- **Root Cause**: The test failure was triggered by a mismatch in business calculation logic.\n" +
                "- **Suggested Action**: Inspect recent modifications in the service layer against test assertions."
        ));
    }
}
