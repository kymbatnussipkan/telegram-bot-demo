package com.example.telegram_bot_demo.service;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class OpenAIService {

    private final OpenAiService service;

    public OpenAIService(@Value("${open.ai.token}") String openAiToken) {
        this.service = new OpenAiService(openAiToken);
    }


    public String getAnswerFromOpenAi(String question) {
        try {
            var chatCompletionRequest = ChatCompletionRequest.builder()
                    .messages(List.of(new ChatMessage("user", question)))
                    .model("gpt-3.5-turbo")
                    .build();

            return service.createChatCompletion(chatCompletionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            log.error("Error during OpenAI request: " + e.getMessage());
            return "There was an error processing your request.";
        }
    }

    public String transcribe(String fileUrl) {
        try {
            var transcription = service.createTranscription(CreateTranscriptionRequest.builder()
                            .model("whisper-1")
                            .build(),
                    fileUrl
            );
            return transcription.getText();
        } catch (Exception e) {
            log.error("Error during OpenAI request: " + e.getMessage());
            return "There was an error processing your request.";
        }
    }
}
