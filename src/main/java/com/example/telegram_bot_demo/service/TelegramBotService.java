package com.example.telegram_bot_demo.service;

import com.example.telegram_bot_demo.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    private final BotConfig config;
    private final OpenAIService openAIService;

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        }

        if (update.hasMessage() && update.getMessage().hasVoice()) {
            handleVoiceMessage(update);

        }
    }

    @Async
    public void handleTextMessage(Update update) {
        var msg = update.getMessage().getText();
        var chatId = update.getMessage().getChatId();
        try {
            String answer = openAIService.getAnswerFromOpenAi(msg);
            sendMessage(chatId, answer);
        } catch (Exception e) {
            log.error("Error processing text message: {}", e.getMessage());
            sendMessage(chatId, "Sorry, something went wrong while processing your request.");
        }
    }

    @Async
    public void handleVoiceMessage(Update update) {
        String fileId = update.getMessage().getVoice().getFileId();
        long chatId = update.getMessage().getChatId();
        try {
            String filePath = downloadAudioFile(fileId);
            String transcribedText = openAIService.transcribe(filePath);
            sendMessage(chatId, transcribedText);
            cleanUpTempFile(filePath);
        } catch (Exception e) {
            log.error("Error processing voice message: {}", e.getMessage());
            sendMessage(chatId, "Sorry, something went wrong while processing the audio file.");
        }
    }

    private void sendMessage(Long chatId, String msgToSend) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText(msgToSend);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Error to send message: " + e.getMessage());
        }
    }

    private String downloadAudioFile(String fileId) throws IOException, TelegramApiException {
        String fileUrl = execute(new GetFile(fileId)).getFileUrl(getBotToken());
        File audioFile = new File("audio.ogg");
        FileUtils.copyURLToFile(new URL(fileUrl), audioFile);
        return audioFile.getAbsolutePath();
    }

    private void cleanUpTempFile(String filePath) { // Очистка временных файлов, чтобы предотвратить переполнение диска
        File file = new File(filePath);
        if (file.exists() && !file.delete()) {
            log.warn("Failed to delete temporary file: {}", filePath);
        }
    }
}
