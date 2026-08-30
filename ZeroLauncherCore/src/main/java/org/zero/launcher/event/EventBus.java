/*
 * ZeroLauncher
 * Copyright (C) 2020  Zero <Zero@zerolauncher.net> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.zero.launcher.event;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static org.zero.launcher.util.logging.Logger.LOG;

/// Central event bus for publishing and subscribing to typed events with polymorphic hierarchy dispatch.
@NotNullByDefault
public final class EventBus extends ClassValue<EventManager<?>> {

    /// Global singleton instance of the event bus.
    public static final EventBus EVENT_BUS = new EventBus();

    private EventBus() {
    }

    @Override
    protected EventManager<?> computeValue(Class<?> type) {
        return new EventManager<>();
    }

    /// Returns the event manager channel associated with the given event class.
    @SuppressWarnings("unchecked")
    public <T extends Event> EventManager<T> channel(Class<T> clazz) {
        return (EventManager<T>) get(clazz);
    }

    /// Fires an event across the event bus, dispatching to listeners registered for the event's class,
    /// superclasses, and interfaces.
    @SuppressWarnings("unchecked")
    public Event.Result fireEvent(Event obj) {
        LOG.info(obj + " gets fired");

        Event.Result finalResult = Event.Result.DEFAULT;
        Set<Class<?>> visited = new HashSet<>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(obj.getClass());

        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (current == null || !visited.add(current) || !Event.class.isAssignableFrom(current)) {
                continue;
            }

            EventManager<Event> manager = (EventManager<Event>) get(current);
            Event.Result res = manager.fireEvent(obj);
            if (res != Event.Result.DEFAULT) {
                finalResult = res;
            }

            Class<?> superClass = current.getSuperclass();
            if (superClass != null && Event.class.isAssignableFrom(superClass)) {
                queue.add(superClass);
            }
            for (Class<?> intf : current.getInterfaces()) {
                if (Event.class.isAssignableFrom(intf)) {
                    queue.add(intf);
                }
            }
        }

        return finalResult;
    }
}
