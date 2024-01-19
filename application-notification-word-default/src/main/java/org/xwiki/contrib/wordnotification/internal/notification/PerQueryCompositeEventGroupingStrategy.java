/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.wordnotification.internal.notification;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;

/**
 * Custom grouping strategy for the events.
 *
 * @version $Id$
 * @since 1.1
 */
@Singleton
@Component(roles = PerQueryCompositeEventGroupingStrategy.class)
public class PerQueryCompositeEventGroupingStrategy
{
    /**
     * Group the events depending on the parameter named {@code query}.
     * @param compositeEvent the composite event to split and regroup differently
     * @return a new collection of composite events grouped using the query parameter
     * @throws NotificationException in case of problem to perform the grouping
     */
    public Collection<CompositeEvent> groupEventsPerQuery(CompositeEvent compositeEvent) throws NotificationException
    {
        Map<String, CompositeEvent> eventMap = new HashMap<>();
        for (Event event : compositeEvent.getEvents()) {
            String query = (String) event.getCustom().get(AbstractMentionedWordsRecordableEvent.QUERY_FIELD);
            CompositeEvent internalCompositeEvent;
            if (eventMap.containsKey(query)) {
                internalCompositeEvent = eventMap.get(query);
                internalCompositeEvent.add(event, 10);
            } else {
                internalCompositeEvent = new CompositeEvent(event);
                eventMap.put(query, internalCompositeEvent);
            }
        }
        return eventMap.values();
    }
}
