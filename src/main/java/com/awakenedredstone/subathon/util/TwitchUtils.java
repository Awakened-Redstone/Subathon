package com.awakenedredstone.subathon.util;

import com.awakenedredstone.subathon.Subathon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.twitch4j.TwitchClientPool;
import com.github.twitch4j.chat.TwitchChatConnectionPool;
import com.github.twitch4j.common.config.Twitch4JGlobal;
import com.github.twitch4j.common.util.ThreadUtils;
import com.github.twitch4j.common.util.TypeConvert;
import com.github.twitch4j.helix.TwitchHelixBuilder;
import com.github.twitch4j.helix.TwitchHelixErrorDecoder;
import com.github.twitch4j.helix.domain.CustomReward;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.helix.interceptor.*;
import com.netflix.hystrix.HystrixCommand;
import feign.*;
import feign.hystrix.HystrixFeign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import net.minecraft.text.Text;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.awakenedredstone.subathon.Subathon.getConfigData;
import static com.awakenedredstone.subathon.Subathon.usersProgressBar;

public class TwitchUtils {
    public void joinChannels(TwitchChatConnectionPool chatPool, TwitchClientPool client) {
        short joinedChannels = 0;
        int totalChannels = getConfigData().channels.size();

        usersProgressBar.setMaxValue(getConfigData().channels.size());
        usersProgressBar.setValue(0);
        usersProgressBar.setName(Text.translatable("text.subathon.load.users.data", 0, totalChannels));
        usersProgressBar.setVisible(true);

        TwitchUtils.Feign feign = new TwitchUtils.Builder().build();
        List<User> users = feign.getUsersInfo(null, getConfigData().channels).execute().getUsers();

        usersProgressBar.setName(Text.translatable("text.subathon.load.users", 0, totalChannels));

        for (User user : users) {
            chatPool.joinChannel(user.getLogin());
            client.getPubSub().listenForChannelPointsRedemptionEvents(null, user.getId());
            usersProgressBar.setName(Text.translatable("text.subathon.load.users", ++joinedChannels, totalChannels));
            usersProgressBar.setValue(joinedChannels);
        }

        usersProgressBar.setVisible(false);
    }

    public interface Feign {
        @RequestLine("GET /users?id={id}&login={login}")
        HystrixCommand<UserList> getUsersInfo(@Param("id") List<String> userIds, @Param("login") List<String> userNames);
    }

    public static class Builder {
        public Feign build() {
            TwitchHelixTokenManager tokenManager = new TwitchHelixTokenManager(Twitch4JGlobal.clientId, Twitch4JGlobal.clientSecret, null);
            TwitchHelixRateLimitTracker rateLimitTracker = new TwitchHelixRateLimitTracker(TwitchHelixBuilder.DEFAULT_BANDWIDTH, tokenManager);
            ObjectMapper mapper = TypeConvert.getObjectMapper();
            ObjectMapper serializer = mapper.copy().addMixIn(CustomReward.class, CustomRewardEncodeMixIn.class);
            int timeout = 5000;
            ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadUtils.getDefaultScheduledThreadPoolExecutor("subathon-" +
                    RandomStringUtils.random(4, true, true), 1);

            return HystrixFeign.builder()
                    .client(new TwitchHelixHttpClient(new OkHttpClient(Subathon.OKHTTPCLIENT), scheduledThreadPoolExecutor, tokenManager, rateLimitTracker, timeout))
                    .encoder(new JacksonEncoder(serializer))
                    .decoder(new TwitchHelixDecoder(mapper, rateLimitTracker))
                    .logger(new Slf4jLogger())
                    .logLevel(Logger.Level.NONE)
                    .errorDecoder(new TwitchHelixErrorDecoder(new JacksonDecoder(), rateLimitTracker))
                    .requestInterceptor(new TwitchHelixClientIdInterceptor(Twitch4JGlobal.userAgent, tokenManager))
                    .options(new Request.Options(timeout / 3, TimeUnit.MILLISECONDS, timeout, TimeUnit.MILLISECONDS, true))
                    .retryer(new Retryer.Default(500, timeout, 2))
                    .target(Feign.class, "https://twitch-api.awakenedredstone.com/v1");
        }
    }
}
