package old.twitch;

import com.github.twitch4j.chat.events.AbstractChannelEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.common.events.domain.EventUser;

import java.util.Collection;

public class SpecificSubGiftEvent extends AbstractChannelEvent {
    private final EventUser gifter;
    private final Collection<SubscriptionEvent> events;

    public SpecificSubGiftEvent(EventChannel channel, EventUser gifter, Collection<SubscriptionEvent> events) {
        super(channel);
        this.gifter = gifter;
        this.events = events;
    }

    public EventUser getGifter() {
        return this.gifter;
    }

    public Collection<SubscriptionEvent> getEvents() {
        return this.events;
    }
}
