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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Dedicated displayer for the {@link MentionedWordsRecordableEvent} events.
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

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Block renderNotification(CompositeEvent compositeEvent) throws NotificationException
    {
        // FIXME: Right now the composite event will contain event with different kind of queries
        // this should be solved with a proper SimilarityCalculator algorithm
        // See: https://jira.xwiki.org/browse/XWIKI-17034

        Block result = new GroupBlock();
        ScriptContext scriptContext = scriptContextManager.getScriptContext();
        scriptContext.setAttribute(EVENT_BINDING_NAME, compositeEvent, ScriptContext.ENGINE_SCOPE);

        Template template = this.templateManager.getTemplate("notificationWord/notification.vm");
        try {
            result.addChildren(this.templateManager.execute(template).getChildren());
        } catch (Exception e) {
            throw new NotificationException("Error when executing the notification template", e);
        } finally {
            scriptContext.removeAttribute(EVENT_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
        }
        return result;
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Collections.singletonList(MentionedWordsRecordableEvent.class.getCanonicalName());
    }
}
