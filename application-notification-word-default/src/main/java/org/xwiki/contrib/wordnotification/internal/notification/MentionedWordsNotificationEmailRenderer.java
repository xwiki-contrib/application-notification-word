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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Dedicated email renderer for both {@link MentionedWordsRecordableEvent} and {@link RemovedWordsRecordableEvent}.
 *
 * @version $Id$
 * @since 1.1
 */
// We need several hints since we use same component for both events.
@Component(hints = {
    "org.xwiki.contrib.wordnotification.internal.notification.MentionedWordsRecordableEvent",
    "org.xwiki.contrib.wordnotification.internal.notification.RemovedWordsRecordableEvent"
})
@Singleton
public class MentionedWordsNotificationEmailRenderer implements NotificationEmailRenderer
{
    private static final String TEMPLATES_PATH = "notificationWord/notification/";
    private static final String PLAIN_TEMPLATE = TEMPLATES_PATH + "email.plain.vm";
    private static final String HTML_TEMPLATE = TEMPLATES_PATH + "email.html.vm";
    private static final String EVENT_BINDING_NAME = "compositeEvent";
    private static final String IS_REMOVAL_BINDING_NAME = "isRemoval";

    @Inject
    private EmailTemplateRenderer emailTemplateRenderer;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private PerQueryCompositeEventGroupingStrategy groupingStrategy;

    private Block executeTemplate(CompositeEvent event, String userId, Template template, Syntax syntax)
        throws NotificationException
    {
        GroupBlock groupBlock = new GroupBlock();
        for (CompositeEvent compositeEvent : this.groupingStrategy.groupEventsPerQuery(event)) {
            groupBlock.addChild(this.emailTemplateRenderer.executeTemplate(compositeEvent, userId, template, syntax,
                Map.of(
                    EVENT_BINDING_NAME, compositeEvent,
                    IS_REMOVAL_BINDING_NAME,
                    compositeEvent.getType().equals(RemovedWordsRecordableEvent.class.getCanonicalName())
                )
            ));
        }
        return groupBlock;
    }

    @Override
    public String renderHTML(CompositeEvent compositeEvent, String userId) throws NotificationException
    {
        Template template = this.templateManager.getTemplate(HTML_TEMPLATE);
        Block block = executeTemplate(compositeEvent, userId, template, Syntax.XHTML_1_0);
        return emailTemplateRenderer.renderHTML(block);
    }

    @Override
    public String renderPlainText(CompositeEvent compositeEvent, String userId) throws NotificationException
    {
        Template template = this.templateManager.getTemplate(PLAIN_TEMPLATE);
        Block block = executeTemplate(compositeEvent, userId, template, Syntax.PLAIN_1_0);
        return emailTemplateRenderer.renderPlainText(block);
    }

    @Override
    public String generateEmailSubject(CompositeEvent compositeEvent, String userId) throws NotificationException
    {
        // We don't care it's never used.
        return "";
    }
}
