package bot.botGradle.service;

import bot.botGradle.config.BotConfig;
import bot.botGradle.model.User;
import bot.botGradle.model.Words;
import bot.botGradle.repository.UserRepository;
import bot.botGradle.repository.WordsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static bot.botGradle.service.Constants.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    Keyboards keyboard = new Keyboards();
    String language, word, choose;
    int wordId;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WordsRepository wordsRepository;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начинаем"));
        listOfCommands.add(new BotCommand("/dict", "словарь"));
        listOfCommands.add(new BotCommand("/learn", "учим слова"));
        listOfCommands.add(new BotCommand("/help", "справка"));
        try {
            execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start", "/back" -> {
                    start(chatId, update.getMessage().getChat().getFirstName(), update.getMessage());
                }
                case "/dict" -> {
                    userRepository.updateUserChooseByChatId(DICT, chatId);
                    dict(chatId);
                }
                case "/help" -> prepareAndSendMessage(chatId, HELP_TEXT);
                case "/learn" -> {
                    userRepository.updateUserChooseByChatId(LEARN, chatId);
                    learn(chatId);
                }
                default -> {
                    choose = userRepository.findUserDataByChatId(chatId).getUserChoose();
                    if (choose.equals(DICT)) {
                        dictFind(messageText, chatId);
                    } else {
                        findTranslateWord(messageText, chatId);
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            switch (callbackData) {
                case LEARN -> {
                    userRepository.updateUserChooseByChatId(callbackData, chatId);
                    learn(chatId);
                }
                case DICT -> {
                    userRepository.updateUserChooseByChatId(callbackData, chatId);
                    dict(chatId);
                }
                case RU, EN -> {
                    userRepository.updateUserLanguageByChatId(callbackData, chatId);
                    findRandomWord(chatId);
                }
                case NEXT -> {
                    choose = userRepository.findUserDataByChatId(chatId).getUserChoose();
                    if (choose.equals(LEARN)) {
                        findRandomWord(chatId);
                    } else if (choose.equals(DICT)) {
                        dict(chatId);
                    } else if (choose.isEmpty()) {
                        start(chatId, update.getMessage().getChat().getFirstName(), update.getMessage());
                    }
                }
                case TRANSLATE -> {
                    String word = userRepository.findUserDataByChatId(chatId).getUserLastWord();
                    findAllTranslates(word, chatId);
                }
                case BACK -> back(chatId);
                case COUNT -> resetCounter(chatId);
            }
        }
    }

    private void resetCounter(long chatId) {
        language = userRepository.findUserDataByChatId(chatId).getUserLanguage();
        if(language.equals(RU)){
            wordsRepository.updateCountWordRuByChatId();
            back(chatId);
        }else {
            wordsRepository.updateCountWordEnByChatId();
            back(chatId);
        }
    }

    private void dict(long chatId) {
        executeMessageText(FIND_WORD, chatId);
    }

    private void dictFind(String str, long chatId) {
        identifyLanguage(str);
        findAllTranslates(str, chatId);
    }

    private void identifyLanguage(String message) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(message.charAt(0));
        if (Character.UnicodeBlock.CYRILLIC.equals(block)) {
            language = RU;
        } else if (Character.UnicodeBlock.BASIC_LATIN.equals(block)) {
            language = EN;
        }
    }

    private void findAllTranslates(String word, long chatId) {
        String answer = "";
        language = userRepository.findUserDataByChatId(chatId).getUserLanguage();
        if (word == null) {
            answer = WORD_IS_NOT_EXISTS;
        } else {
            if (language.equals(RU)) {
                List<Words> text = wordsRepository.findByWordRu(word.toLowerCase());
                answer = text.isEmpty() ? WORD_IS_NOT_EXISTS : text.stream()
                        .map(Words::getWordEn)
                        .collect(Collectors.joining(", "));
            } else if (language.equals(EN)) {
                List<Words> text = wordsRepository.findByWordEn(word.toLowerCase());
                answer = text.isEmpty() ? WORD_IS_NOT_EXISTS : text.stream()
                        .map(Words::getWordRu)
                        .collect(Collectors.joining(", "));
            }
        }
        executeMessageTextAddKeyboard(answer, chatId, "navShort");
    }

    private void findRandomWord(long chatId) {

        language = userRepository.findUserDataByChatId(chatId).getUserLanguage();
        String textSend;

        if (language.equals(RU)) {
            Words text = wordsRepository.findRandomWordRu();
            if(text == null){
                executeMessageTextAddKeyboard(ALL_DONE, chatId, "count");
                return;
            }else {
                wordId = text.getId();
                word = text.getWordRu();
            }
        } else if (language.equals(EN)) {
            Words text = wordsRepository.findRandomWordEn();
            if(text == null) {
                executeMessageTextAddKeyboard(ALL_DONE, chatId, "count");
                return;
            }else {
                wordId = text.getId();
                word = text.getWordEn();
            }
        }
        userRepository.updateUserLastWordByChatId(word, chatId);
        textSend = "Твой вариант перевода слова <b>" + word + "</b>:";

        executeMessageText(textSend, chatId);
    }

    private void findTranslateWord(String message, long chatId) {
        String textSend = "";
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        language = userRepository.findUserDataByChatId(chatId).getUserLanguage();

        List<Words> list;
        boolean flag = false;
        if (language.equals(EN)) {
            list = wordsRepository.findByWordRu(message.toLowerCase());
            for (Words w : list) {
                if (w.getWordRu().equals(message.toLowerCase())) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                int count = wordsRepository.findCountWordEnByChatId(wordId) + 1;
                if(count <= 5){
                    wordsRepository.updateCountWordEnByChatId(count, wordId);
                    textSend = ANSWER_RIGHT;
                }else {
                    textSend = ALL_DONE;
                }
            } else {
                textSend = ANSWER_WRONG;
            }

        } else if (language.equals(RU)) {
            list = wordsRepository.findByWordEn(message.toLowerCase());
            for (Words w : list) {
                if (w.getWordEn().equals(message.toLowerCase())) {
                    flag = true;
                    break;
                }
            }
            if (flag) {
                int count = wordsRepository.findCountWordRuByChatId(wordId) + 1;
                if(count <= 5){
                    wordsRepository.updateCountWordRuByChatId(count, wordId);
                    textSend = ANSWER_RIGHT;
                }else {
                    textSend = ALL_DONE;
                }
            } else {
                textSend = ANSWER_WRONG;
            }
        }

        executeMessageTextAddKeyboard(textSend, chatId, "nav");
    }

    private void learn(long chatId) {
        executeMessageTextAddKeyboard(CHOOSE_LANGUAGE, chatId, "lang");
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("registerUser/User saved: " + user);
        }
    }

    private void start(long chatId, String name, Message message) {
        String answer = "Привет, " + name + ", что будем делать? Выбирай:";
        registerUser(message);
        executeMessageTextAddKeyboard(answer, chatId, "menu");
    }

    private void back(long chatId) {
        executeMessageTextAddKeyboard(NEW_CHOOSE, chatId, "menu");
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        executeMessage(message);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        message.setParseMode("HTML");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessageText(String text, long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessageTextAddKeyboard(String text, long chatId, String kb) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");

        switch (kb) {
            case "menu" -> keyboard.menuKeyboard(message);
            case "nav" -> keyboard.navigationKeyboard(message);
            case "navShort" -> keyboard.navigationShortKeyboard(message);
            case "lang" -> keyboard.languageKeyboard(message);
            case "count" -> keyboard.counterKeyboard(message);
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            message.setParseMode("HTML");
            execute(message);
        } catch (TelegramApiException e) {
            log.error("error occurred: " + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        executeMessage(message);
    }
}
