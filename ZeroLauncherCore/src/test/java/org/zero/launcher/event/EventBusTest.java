package org.zero.launcher.event;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public final class EventBusTest {

    static class ParentEvent extends Event {
        public ParentEvent(Object source) {
            super(source);
        }
    }

    static class ChildEvent extends ParentEvent {
        public ChildEvent(Object source) {
            super(source);
        }
    }

    static class GrandChildEvent extends ChildEvent {
        public GrandChildEvent(Object source) {
            super(source);
        }
    }

    @Test
    public void testPolymorphicDispatch() {
        AtomicInteger parentReceived = new AtomicInteger(0);
        AtomicInteger childReceived = new AtomicInteger(0);
        AtomicInteger grandChildReceived = new AtomicInteger(0);

        EventBus.EVENT_BUS.channel(ParentEvent.class).register(e -> parentReceived.incrementAndGet());
        EventBus.EVENT_BUS.channel(ChildEvent.class).register(e -> childReceived.incrementAndGet());
        EventBus.EVENT_BUS.channel(GrandChildEvent.class).register(e -> grandChildReceived.incrementAndGet());

        GrandChildEvent grandChild = new GrandChildEvent(this);
        EventBus.EVENT_BUS.fireEvent(grandChild);

        assertEquals(1, parentReceived.get());
        assertEquals(1, childReceived.get());
        assertEquals(1, grandChildReceived.get());

        ChildEvent child = new ChildEvent(this);
        EventBus.EVENT_BUS.fireEvent(child);

        assertEquals(2, parentReceived.get());
        assertEquals(2, childReceived.get());
        assertEquals(1, grandChildReceived.get());
    }
}
