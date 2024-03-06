package com.gearwenxin.service;

import com.gearwenxin.common.*;
import com.gearwenxin.config.WenXinProperties;
import com.gearwenxin.core.ChatCore;
import com.gearwenxin.core.ChatUtils;
import com.gearwenxin.entity.BaseRequest;
import com.gearwenxin.entity.chatmodel.ChatBaseRequest;
import com.gearwenxin.entity.chatmodel.ChatErnieRequest;
import com.gearwenxin.entity.request.ErnieRequest;
import com.gearwenxin.entity.response.ChatResponse;
import com.gearwenxin.entity.Message;
import com.gearwenxin.validator.RequestValidator;
import com.gearwenxin.validator.RequestValidatorFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author Ge Mingjia
 * {@code @date} 2023/7/20
 */
@Slf4j
@Service
public class ChatService {

    @Resource
    private WenXinProperties wenXinProperties;

    private static Map<String, Deque<Message>> CHAT_MESSAGES_HISTORY_MAP = new ConcurrentHashMap<>();

    private final ChatCore chatCore = new ChatCore();

    private String getAccessToken() {
        return wenXinProperties.getAccessToken();
    }

    public Map<String, Deque<Message>> getMessageHistoryMap() {
        return CHAT_MESSAGES_HISTORY_MAP;
    }

    public void initMessageHistoryMap(Map<String, Deque<Message>> map) {
        CHAT_MESSAGES_HISTORY_MAP = map;
    }

    public <T extends ChatBaseRequest> Mono<ChatResponse> chatOnce(T chatRequest, ModelConfig config) {
        return Mono.from(chatProcess(chatRequest, null, false, config));
    }

    public <T extends ChatBaseRequest> Flux<ChatResponse> chatOnceStream(T chatRequest, ModelConfig config) {
        return Flux.from(chatProcess(chatRequest, null, true, config));
    }

    public <T extends ChatBaseRequest> Mono<ChatResponse> chatContinuous(T chatRequest, String msgUid, ModelConfig config) {
        return Mono.from(chatProcess(chatRequest, msgUid, false, config));
    }

    public <T extends ChatBaseRequest> Flux<ChatResponse> chatContinuousStream(T chatRequest, String msgUid, ModelConfig config) {
        return Flux.from(chatProcess(chatRequest, msgUid, true, config));
    }

    public <T extends ChatBaseRequest> Publisher<ChatResponse> chatProcess(T request, String msgUid, boolean stream, ModelConfig config) {
        validRequest(request);
        boolean isContinuous = (msgUid != null);
        String url = config.getModelUrl();
        String accessToken = config.getAccessToken() == null ? getAccessToken() : config.getAccessToken();
        Object targetRequest;
        if (isContinuous) {
            Deque<Message> messagesHistory = getMessageHistoryMap().computeIfAbsent(
                    msgUid, key -> new ConcurrentLinkedDeque<>()
            );
            targetRequest = buildTargetRequest(messagesHistory, stream, request);
            Message message = WenXinUtils.buildUserMessage(request.getContent());
            ChatUtils.offerMessage(messagesHistory, message);

            log.info("model: {}, stream: {}, continuous: {}", request, stream, true);

            return stream ? chatCore.historyFluxPost(url, accessToken, targetRequest, messagesHistory, config) :
                    chatCore.historyMonoPost(url, accessToken, targetRequest, messagesHistory, config);
        } else {
            targetRequest = buildTargetRequest(null, stream, request);
        }

        log.info("model: {}, stream: {}, continuous: {}", request.getClass(), stream, false);

        return stream ? chatCore.fluxPost(url, accessToken, targetRequest, ChatResponse.class) :
                chatCore.monoPost(url, accessToken, targetRequest, ChatResponse.class);
    }

    public <T extends ChatBaseRequest> void validRequest(T request) {
        RequestValidator validator = RequestValidatorFactory.getValidator(request);
        validator.validate(request);
    }

    public static <T extends ChatBaseRequest> Object buildTargetRequest(Deque<Message> messagesHistory, boolean stream, T request) {
        Object targetRequest = null;
        if (request.getClass() == ChatBaseRequest.class) {
            BaseRequest.BaseRequestBuilder requestBuilder = ConvertUtils.toBaseRequest(request).stream(stream);
            if (messagesHistory != null) requestBuilder.messages(messagesHistory);
            targetRequest = requestBuilder.build();
        } else if (request.getClass() == ChatErnieRequest.class) {
            ErnieRequest.ErnieRequestBuilder requestBuilder = ConvertUtils.toErnieRequest((ChatErnieRequest) request).stream(stream);
            if (messagesHistory != null) requestBuilder.messages(messagesHistory);
            targetRequest = requestBuilder.build();
        }
        return targetRequest;
    }

}