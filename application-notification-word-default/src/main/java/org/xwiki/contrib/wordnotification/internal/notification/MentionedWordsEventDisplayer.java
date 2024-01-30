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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Dedicated displayer for the {@link MentionedWordsRecordableEvent} and {@link RemovedWordsRecordableEvent} events.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("MentionedWordsEventDisplayer")
@Singleton
public class MentionedWordsEventDisplayer implements NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "compositeEvent";
    private static final String IS_REMOVAL_BINDING_NAME = "isRemoval";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Block renderNotification(CompositeEvent compositeEvent) throws NotificationException
    {
        Block result = new GroupBlock();
        Collection<CompositeEvent> compositeEvents = this.groupEventsPerQuery(compositeEvent);
        boolean isRemoval = compositeEvent.getType().equals(RemovedWordsRecordableEvent.class.getCanonicalName());
        for (CompositeEvent event : compositeEvents) {
            ScriptContext scriptContext = scriptContextManager.getScriptContext();
            scriptContext.setAttribute(EVENT_BINDING_NAME, event, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute(IS_REMOVAL_BINDING_NAME, isRemoval, ScriptContext.ENGINE_SCOPE);

            Template template = this.templateManager.getTemplate("notificationWord/notification.vm");
            try {
                result.addChildren(this.templateManager.execute(template).getChildren());
            } catch (Exception e) {
                throw new NotificationException("Error when executing the notification template", e);
            } finally {
                scriptContext.removeAttribute(EVENT_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
                scriptContext.removeAttribute(IS_REMOVAL_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
            }
        }
        return result;
    }

    private Collection<CompositeEvent> groupEventsPerQuery(CompositeEvent compositeEvent) throws NotificationException
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

    @Override
    public List<String> getSupportedEvents()
    {
        return List.of(
            MentionedWordsRecordableEvent.class.getCanonicalName(),
            RemovedWordsRecordableEvent.class.getCanonicalName()
        );
    }
}
