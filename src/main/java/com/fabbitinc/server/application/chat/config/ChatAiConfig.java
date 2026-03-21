package com.fabbitinc.server.application.chat.config;

import com.fabbitinc.server.application.chat.tool.IssueCreateDraftTool;
import com.fabbitinc.server.application.chat.tool.PartIssueLookupTool;
import com.fabbitinc.server.application.chat.tool.PartLookupTool;
import com.fabbitinc.server.application.config.AppProperties;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ChatAiConfig {

    @Bean
    public ToolCallbackProvider chatToolCallbackProvider(
            PartLookupTool partLookupTool,
            PartIssueLookupTool partIssueLookupTool,
            IssueCreateDraftTool issueCreateDraftTool
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(partLookupTool, partIssueLookupTool, issueCreateDraftTool)
                .build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(
            AppProperties appProperties,
            ObjectProvider<ObservationRegistry> observationRegistryProvider
    ) {
        ObservationRegistry observationRegistry = observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(appProperties.llmTimeoutSeconds()));
        requestFactory.setReadTimeout(Duration.ofSeconds(appProperties.llmTimeoutSeconds()));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(appProperties.llmBaseUrl())
                .apiKey(appProperties.llmApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        OpenAiChatOptions defaultOptions = OpenAiChatOptions.builder()
                .model(appProperties.llmModel())
                .temperature(0.2)
                .parallelToolCalls(false)
                .internalToolExecutionEnabled(true)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(defaultOptions)
                .retryTemplate(new RetryTemplate(RetryPolicy.withMaxRetries(0)))
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean
    public ChatClient chatClient(
            OpenAiChatModel openAiChatModel,
            ToolCallbackProvider chatToolCallbackProvider,
            ObjectProvider<ObservationRegistry> observationRegistryProvider
    ) {
        ObservationRegistry observationRegistry = observationRegistryProvider.getIfAvailable(() -> ObservationRegistry.NOOP);

        return ChatClient.builder(openAiChatModel, observationRegistry, null, null)
                .defaultToolCallbacks(chatToolCallbackProvider)
                .build();
    }
}
