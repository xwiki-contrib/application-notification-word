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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.script.EventStreamScriptService;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.platform.date.script.DateScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.wiki.script.WikiManagerScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@code notificationWord/notification/alert.vm} notification templates.
 *
 * @version $Id: 2cafc94346ba4061bd85f680ba0ea477de6467cc $
 */
@ComponentList({
    TemplateScriptService.class
})
@HTML50ComponentList
class NotificationAlertPageTest extends PageTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");
    private static final DocumentReference USER_REFERENCE_2 = new DocumentReference("xwiki", "XWiki", "User2");

    private static final String TEMPLATE_PATH = "notificationWord/notification/alert.vm";
    private static final String NOTIF_APPLI_NAME = "Notification Words";
    private static final String DESCRIPTOR_ICON = "eye";
    @Mock
    private EventStreamScriptService eventStreamScriptService;

    @Mock
    private LocalizationScriptService localizationScriptService;

    @Mock
    private DateScriptService dateScriptService;

    @Mock
    private IconManagerScriptService iconManagerScriptService;

    @Mock
    private WikiManagerScriptService wikiManagerScriptService;

    private TemplateManager templateManager;

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "eventstream", this.eventStreamScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "localization", this.localizationScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "date", this.dateScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "icon", this.iconManagerScriptService);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "wiki", this.wikiManagerScriptService);

        RecordableEventDescriptor recordableEventDescriptor = mock(RecordableEventDescriptor.class);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(MentionedWordsRecordableEvent.class.getCanonicalName(), true))
            .thenReturn(recordableEventDescriptor);
        when(this.eventStreamScriptService
            .getDescriptorForEventType(RemovedWordsRecordableEvent.class.getCanonicalName(), true))
            .thenReturn(recordableEventDescriptor);
        when(recordableEventDescriptor.getApplicationName()).thenReturn(NOTIF_APPLI_NAME);
        when(recordableEventDescriptor.getApplicationIcon()).thenReturn(DESCRIPTOR_ICON);
        when(this.iconManagerScriptService.renderHTML(DESCRIPTOR_ICON)).thenReturn("Icon Eye");
        when(this.localizationScriptService.render(NOTIF_APPLI_NAME)).thenReturn("Words Notifications");

        when(this.oldcore.getMockRightService().hasProgrammingRights(this.context)).thenReturn(true);

        // Mock the user's name.
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE, this.context)).thenReturn("First & Name");
        when(this.oldcore.getSpyXWiki().getPlainUserName(USER_REFERENCE_2, this.context)).thenReturn("User2");

        when(this.dateScriptService.displayTimeAgo(any(Date.class))).thenAnswer(invocation -> {
            Date date = invocation.getArgument(0);
            return String.format("it's been %s milliseconds", date.getTime());
        });
        when(this.oldcore.getSpyXWiki().formatDate(any(Date.class), isNull(), eq(this.oldcore.getXWikiContext())))
            .thenAnswer(invocation -> {
            Date date = invocation.getArgument(0);
            return String.format("formatted date of %s milliseconds", date.getTime());
        });
    }

    @Test
    void mentionedWordsNotificationTemplate() throws Exception
    {
        DocumentReference docWithMentionRef = new DocumentReference("xwiki", "Space", "DocWithMention");
        XWikiDocument docWithMention = new XWikiDocument(docWithMentionRef);
        docWithMention.setTitle("Doc with Mention");
        docWithMention.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(MentionedWordsRecordableEvent.class.getCanonicalName());
        testEvent.setDate(new Date(12));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(docWithMentionRef);
        testEvent.setDocumentVersion("1.8");
        testEvent.setCustom(Map.of(
            AbstractMentionedWordsRecordableEvent.IS_NEW_FIELD, false,
            AbstractMentionedWordsRecordableEvent.NEW_OCCURRENCES_FIELD, 3,
            AbstractMentionedWordsRecordableEvent.OLD_OCCURRENCES_FIELD, 1,
            AbstractMentionedWordsRecordableEvent.QUERY_FIELD, "fooQuery"
        ));

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(MentionedWordsRecordableEvent.class.getCanonicalName());
        testEvent2.setDate(new Date(89));
        testEvent2.setUser(USER_REFERENCE);
        testEvent2.setDocument(docWithMentionRef);
        testEvent2.setDocumentVersion("2.15");
        testEvent2.setCustom(Map.of(
            AbstractMentionedWordsRecordableEvent.IS_NEW_FIELD, false,
            AbstractMentionedWordsRecordableEvent.NEW_OCCURRENCES_FIELD, 4,
            AbstractMentionedWordsRecordableEvent.OLD_OCCURRENCES_FIELD, 3,
            AbstractMentionedWordsRecordableEvent.QUERY_FIELD, "fooQuery"
        ));

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("wordNotification.notifications.description"),
            any(Collection.class)))
            .then(invocationOnMock -> {
            List<String> parameters = invocationOnMock.getArgument(1);
            assertEquals(4, parameters.size());

            return String.format("has been updated by %s and contains %s new occurrences of %s (on a total of %s)",
                parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));
        });

        this.scriptContext.setAttribute("compositeEvent", new CompositeEvent(testEvent), ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(TEMPLATE_PATH);
        String expectedResult = "<div class=\"clearfix row\">\n"
            + "    <div class=\"col-xs-3 notification-icon\">\n"
            + "      <div class=\"img-thumbnail\">\n"
            + "        Icon Eye\n"
            + "      </div>\n"
            + "          </div>\n"
            + "    <div class=\"col-xs-9 notification-content\">\n"
            + "      <div class=\"notification-page\">\n"
            + "  <a href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "            </div>\n"
            + "<div class=\"notification-description\">\n"
            + "                                has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span> and contains 2 new occurrences of fooQuery (on a total of 3)\n"
            + "      <div><small class=\"text-muted\">it&#39;s been 12 milliseconds</small></div>\n"
            + "</div>\n"
            + "\n"
            + "          </div>\n"
            + "      </div>";
        assertEquals(expectedResult, result.trim());


        CompositeEvent compositeEvent = new CompositeEvent(testEvent);
        compositeEvent.add(testEvent2, 0);

        when(this.localizationScriptService.render(
            eq("wordNotification.notifications.description.multipleUsers"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(3, parameters.size());

                return String.format("has been updated by %s users and contains new occurrences of %s"
                        + " (on a total of %s)",
                    parameters.get(0), parameters.get(1), parameters.get(2));
            });
        when(this.localizationScriptService.render(
            eq("wordNotification.notifications.description.detail"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(2, parameters.size());

                return String.format("added %s occurrences (new total: %s)",
                    parameters.get(0), parameters.get(1));
            });

        this.scriptContext.setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        result = this.templateManager.render(TEMPLATE_PATH);
        expectedResult = "<div class=\"clearfix row\">\n"
            + "    <div class=\"col-xs-3 notification-icon\">\n"
            + "      <div class=\"img-thumbnail\">\n"
            + "        Icon Eye\n"
            + "      </div>\n"
            + "          </div>\n"
            + "    <div class=\"col-xs-9 notification-content\">\n"
            + "      <div class=\"notification-page\">\n"
            + "  <a href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "            </div>\n"
            + "<div class=\"notification-description\">\n"
            + "                                has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span> users and contains new occurrences of fooQuery (on a total of 4)\n"
            + "      <div><small class=\"text-muted\">it&#39;s been 89 milliseconds</small></div>\n"
            + "</div>\n"
            + "\n"
            + "            <button class=\"btn btn-xs toggle-notification-event-details\" type=\"submit\">\n"
            + "        <span class=\"fa fa-ellipsis-h\"></span>\n"
            + "        "
            + "<span class=\"sr-only\">$services.localization.render('notifications.macro.showEventDetails')</span>\n"
            + "      </button>\n"
            + "          </div>\n"
            + "                              <div class=\"col-xs-12 clearfix\">\n"
            + "  <table class=\"notification-event-details\">\n"
            + "                                <tr>\n"
            + "                    <td>            "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span></td>\n"
            + "                    <td class=\"description\">            added 1 occurrences (new total: 4)\n"
            + "</td>\n"
            + "                    <td class=\"text-right text-muted\">"
            + "<a href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=2.15\">"
            + "formatted date of 89 milliseconds</a></td>\n"
            + "                </tr>\n"
            + "                            <tr>\n"
            + "                    <td>            "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span></td>\n"
            + "                    <td class=\"description\">            added 2 occurrences (new total: 3)\n"
            + "</td>\n"
            + "                    <td class=\"text-right text-muted\">"
            + "<a href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=1.8\">"
            + "formatted date of 12 milliseconds</a></td>\n"
            + "                </tr>\n"
            + "                    \n"
            + "  </table>\n"
            + "</div>\n"
            + "    \n"
            + "      </div>";
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void removedWordsNotificationTemplate() throws Exception
    {
        DocumentReference docWithMentionRef = new DocumentReference("xwiki", "Space", "DocWithMention");
        XWikiDocument docWithMention = new XWikiDocument(docWithMentionRef);
        docWithMention.setTitle("Doc with Mention");
        docWithMention.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(RemovedWordsRecordableEvent.class.getCanonicalName());
        testEvent.setDate(new Date(489));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(docWithMentionRef);
        testEvent.setDocumentVersion("2.1");
        testEvent.setCustom(Map.of(
            AbstractMentionedWordsRecordableEvent.IS_NEW_FIELD, false,
            AbstractMentionedWordsRecordableEvent.NEW_OCCURRENCES_FIELD, 8,
            AbstractMentionedWordsRecordableEvent.OLD_OCCURRENCES_FIELD, 11,
            AbstractMentionedWordsRecordableEvent.QUERY_FIELD, "myQuery"
        ));

        Event testEvent2 = new DefaultEvent();
        testEvent2.setApplication(NOTIF_APPLI_NAME);
        testEvent2.setType(RemovedWordsRecordableEvent.class.getCanonicalName());
        testEvent2.setDate(new Date(8899));
        testEvent2.setUser(USER_REFERENCE_2);
        testEvent2.setDocument(docWithMentionRef);
        testEvent2.setDocumentVersion("2.15");
        testEvent2.setCustom(Map.of(
            AbstractMentionedWordsRecordableEvent.IS_NEW_FIELD, false,
            AbstractMentionedWordsRecordableEvent.NEW_OCCURRENCES_FIELD, 0,
            AbstractMentionedWordsRecordableEvent.OLD_OCCURRENCES_FIELD, 8,
            AbstractMentionedWordsRecordableEvent.QUERY_FIELD, "myQuery"
        ));

        this.context.setOriginalWikiId("xwiki");
        when(this.localizationScriptService.render(
            eq("wordNotification.notifications.removal.description"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(4, parameters.size());

                return String.format("has been updated by %s and contains %s less occurrences of %s (on a total of %s)",
                    parameters.get(0), parameters.get(1), parameters.get(2), parameters.get(3));
            });

        this.scriptContext.setAttribute("compositeEvent", new CompositeEvent(testEvent2), ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("isRemoval", true, ScriptContext.ENGINE_SCOPE);

        String result = this.templateManager.render(TEMPLATE_PATH);
        String expectedResult = "<div class=\"clearfix row\">\n"
            + "    <div class=\"col-xs-3 notification-icon\">\n"
            + "      <div class=\"img-thumbnail\">\n"
            + "        Icon Eye\n"
            + "      </div>\n"
            + "          </div>\n"
            + "    <div class=\"col-xs-9 notification-content\">\n"
            + "      <div class=\"notification-page\">\n"
            + "  <a href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "            </div>\n"
            + "<div class=\"notification-description\">\n"
            + "                                has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"User2\"/>User2  </span>"
            + " and contains 8 less occurrences of myQuery (on a total of 0)\n"
            + "      <div><small class=\"text-muted\">it&#39;s been 8899 milliseconds</small></div>\n"
            + "</div>\n"
            + "\n"
            + "          </div>\n"
            + "      </div>";
        assertEquals(expectedResult, result.trim());


        CompositeEvent compositeEvent = new CompositeEvent(testEvent);
        compositeEvent.add(testEvent2, 0);

        when(this.localizationScriptService.render(
            eq("wordNotification.notifications.removal.description.multipleUsers"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(3, parameters.size());

                return String.format("has been updated by %s users and contains less occurrences of %s"
                        + " (on a total of %s)",
                    parameters.get(0), parameters.get(1), parameters.get(2));
            });
        when(this.localizationScriptService.render(
            eq("wordNotification.notifications.removal.description.detail"),
            any(Collection.class)))
            .then(invocationOnMock -> {
                List<String> parameters = invocationOnMock.getArgument(1);
                assertEquals(2, parameters.size());

                return String.format("removed %s occurrences (new total: %s)",
                    parameters.get(0), parameters.get(1));
            });

        this.scriptContext.setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        result = this.templateManager.render(TEMPLATE_PATH);
        expectedResult = "<div class=\"clearfix row\">\n"
            + "    <div class=\"col-xs-3 notification-icon\">\n"
            + "      <div class=\"img-thumbnail\">\n"
            + "        Icon Eye\n"
            + "      </div>\n"
            + "          </div>\n"
            + "    <div class=\"col-xs-9 notification-content\">\n"
            + "      <div class=\"notification-page\">\n"
            + "  <a href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "            </div>\n"
            + "<div class=\"notification-description\">\n"
            + "                                has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>  "
            + "</span>            <span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"User2\"/>  </span> "
            + "users and contains less occurrences of myQuery (on a total of 0)\n"
            + "      <div><small class=\"text-muted\">it&#39;s been 8899 milliseconds</small></div>\n"
            + "</div>\n"
            + "\n"
            + "            <button class=\"btn btn-xs toggle-notification-event-details\" type=\"submit\">\n"
            + "        <span class=\"fa fa-ellipsis-h\"></span>\n"
            + "        <span class=\"sr-only\">$services.localization.render('notifications.macro.showEventDetails')"
            + "</span>\n"
            + "      </button>\n"
            + "          </div>\n"
            + "                              <div class=\"col-xs-12 clearfix\">\n"
            + "  <table class=\"notification-event-details\">\n"
            + "                                <tr>\n"
            + "                    <td>            "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"User2\"/>User2  </span>"
            + "</td>\n"
            + "                    <td class=\"description\">            removed 8 occurrences (new total: 0)\n"
            + "</td>\n"
            + "                    <td class=\"text-right text-muted\">"
            + "<a href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=2.15\">"
            + "formatted date of 8899 milliseconds</a></td>\n"
            + "                </tr>\n"
            + "                            <tr>\n"
            + "                    <td>            "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span></td>\n"
            + "                    <td class=\"description\">           "
            + " removed 3 occurrences (new total: 8)\n"
            + "</td>\n"
            + "                    <td class=\"text-right text-muted\">"
            + "<a href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=2.1\">"
            + "formatted date of 489 milliseconds</a></td>\n"
            + "                </tr>\n"
            + "                    \n"
            + "  </table>\n"
            + "</div>\n"
            + "    \n"
            + "      </div>";
        assertEquals(expectedResult, result.trim());
    }
}
