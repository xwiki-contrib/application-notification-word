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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.diff.display.UnifiedDiffBlock;
import org.xwiki.diff.display.UnifiedDiffElement;
import org.xwiki.diff.script.DiffDisplayerScriptService;
import org.xwiki.diff.script.DiffScriptService;
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
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
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
 * Tests the {@code notificationWord/notification/email.*.vm} notification templates.
 *
 * @version $Id: 2cafc94346ba4061bd85f680ba0ea477de6467cc $
 */
@ComponentList({
    TemplateScriptService.class
})
@HTML50ComponentList
class NotificationEmailPageTest extends PageTest
{
    // We should rely on two users in the events, but CompositeEvent#users relies on a hashset which is not stable in
    // tests...
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    private static final String TEMPLATE_PATH = "notificationWord/notification/email.%s.vm";
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

    @Mock
    private DiffScriptService diffScriptService;

    @Mock
    private DiffDisplayerScriptService diffDisplayerScriptService;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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

        when(this.dateScriptService.displayTimeAgo(any(Date.class))).thenAnswer(invocation -> {
            Date date = invocation.getArgument(0);
            return String.format("it's been %s milliseconds", date.getTime());
        });
        when(this.oldcore.getSpyXWiki().formatDate(any(Date.class), isNull(), eq(this.oldcore.getXWikiContext())))
            .thenAnswer(invocation -> {
            Date date = invocation.getArgument(0);
            return String.format("formatted date of %s milliseconds", date.getTime());
        });

        this.oldcore.getMocker().registerComponent(ScriptService.class, "diff", this.diffScriptService);
        when(this.diffScriptService.get("display")).thenReturn(this.diffDisplayerScriptService);

        when(this.localizationScriptService.render("web.history.changes.summary.documentProperties"))
            .thenReturn("Document properties");
        when(this.localizationScriptService.render("web.history.changes.document.content"))
            .thenReturn("Content");

        when(this.localizationScriptService.render(eq("notifications.email.seeChanges"), any(Collection.class)))
            .thenAnswer(invocation -> {
                List<String> parameters = invocation.getArgument(1);
                assertEquals(1, parameters.size());
                return String.format("See changes: %s", parameters.get(0));
            });

        when(this.localizationScriptService.render(eq("notifications.events.lastChange"), any(Collection.class)))
            .thenAnswer(invocation -> {
                List<String> parameters = invocation.getArgument(1);
                assertEquals(1, parameters.size());
                return String.format("Last changes date: %s", parameters.get(0));
            });

        when(this.iconManagerScriptService.renderHTML("file-text")).thenReturn("Icon file text");
        when(this.diffDisplayerScriptService.unified(any(String.class), any(String.class), isNull()))
            .then(invocationOnMock -> {
                UnifiedDiffBlock<String, Character> result = new UnifiedDiffBlock<>();
                result.add(0, new UnifiedDiffElement<>(0, UnifiedDiffElement.Type.DELETED,
                    invocationOnMock.getArgument(0)));
                result.add(0, new UnifiedDiffElement<>(0, UnifiedDiffElement.Type.ADDED,
                    invocationOnMock.getArgument(1)));
                return List.of(result);
            });
    }

    @Test
    void mentionedWordsHtmlNotificationTemplate() throws Exception
    {
        DocumentReference docWithMentionRef = new DocumentReference("xwiki", "Space", "DocWithMention");
        XWikiDocument docWithMention = new XWikiDocument(docWithMentionRef);
        docWithMention.setTitle("Doc with Mention");
        docWithMention.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        String version1 = docWithMention.getVersion();
        docWithMention.setContent("some other content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 2");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 3");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 4");

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(MentionedWordsRecordableEvent.class.getCanonicalName());
        testEvent.setDate(new Date(12));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(docWithMentionRef);
        testEvent.setDocumentVersion(version1);
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
        testEvent2.setDocumentVersion(docWithMention.getVersion());
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

        CompositeEvent compositeEvent = new CompositeEvent(testEvent);
        this.scriptContext.setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("emailUser", "user1", ScriptContext.ENGINE_SCOPE);

        String htmlTemplatePath = String.format(TEMPLATE_PATH, "html");
        String result = this.templateManager.render(htmlTemplatePath);
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "              <strong>Words Notifications</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                          <div>\n"
            + "    <div style=\"background-color: #f5f5f5; color: #777777; padding: 4px 8px; border-radius: 4px; "
            + "font-size: 8px;\">\n"
            + "      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                       "
            + "                                                                                                 "
            + "xwiki / Space / Doc with Mention\n"
            + "      </div>\n"
            + "                  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "  </div>\n"
            + "                                            has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>User "
            + " </span> and contains 2 new occurrences of fooQuery (on a total of 3)\n"
            + "          <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      it&#39;s been 12 milliseconds\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                              \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                 "
            + "             <div style=\"border-top: 1px dashed #e8e8e8; font-size: 0.9em;\">\n"
            + "                        <dl class=\"diff-group\">\n"
            + "            <dt id=\"diff-329847360\"   data-reference=\"DOCUMENT:\"\n"
            + ">\n"
            + "          <span class=\"diff-icon diff-icon-change\" title=\"change\">\n"
            + "    Icon file text\n"
            + "  </span>\n"
            + "      Document properties\n"
            + "    </dt>\n"
            + "    <dd style=\"margin-left: 0\">\n"
            + "      <dl>\n"
            + "                                    <dt style=\"border: 1px solid #E8E8E8; "
            + "border-left: 5px solid #E8E8E8; color: #656565; padding: .5em .2em;\" data-property-name=\"content\">\n"
            + "    Content\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,1 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+some content</td>\n"
            + "        </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"background-color: #ffcccc;\">-</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "          </dl>\n"
            + "    </dd>\n"
            + "  </dl>\n"
            + "        </div>\n"
            + "  \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());
        assertEquals(4, this.logCapture.size());
        assertEquals("Left side ($documentPropertiesSummary.modified) of comparison operation has null value at "
            + "/templates/notificationWord/notification/email.html.vm[line 421, column 45]",
            this.logCapture.getMessage(0));
        assertEquals("Left side ($docDiff.metaData.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 369, column 34]",
            this.logCapture.getMessage(1));
        assertEquals("Left side ($docDiff.attachments.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 375, column 37]",
            this.logCapture.getMessage(2));
        assertEquals("Left side ($docDiff.classProperties.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 387, column 41]",
            this.logCapture.getMessage(3));

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
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        result = this.templateManager.render(htmlTemplatePath);
        expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "              <strong>Words Notifications</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                          <div>\n"
            + "    <div style=\"background-color: #f5f5f5; color: #777777; padding: 4px 8px; "
            + "border-radius: 4px; font-size: 8px;\">\n"
            + "      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                   "
            + "                                                                                                     "
            + "xwiki / Space / Doc with Mention\n"
            + "      </div>\n"
            + "                  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "  </div>\n"
            + "                                            has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>User "
            + " </span> users and contains new occurrences of fooQuery (on a total of 4)\n"
            + "          <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      it&#39;s been 89 milliseconds\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                "
            + "<table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; border-top: 1px dashed #e8e8e8\">\n"
            + "                              <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        "
            + "<img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">            added 1 occurrences (new total: 4)\n"
            + "</td>\n"
            + "                <td>  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=5.1\">"
            + "formatted date of 89 milliseconds</a>\n"
            + "</td>\n"
            + "              </tr>\n"
            + "                      <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        "
            + "<img src=\"cid:User.jpg\" alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">            added 2 occurrences (new total: 3)\n"
            + "</td>\n"
            + "                <td>  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=2.1\">"
            + "formatted date of 12 milliseconds</a>\n"
            + "</td>\n"
            + "              </tr>\n"
            + "            </table>\n"
            + "                      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                   "
            + "           <div style=\"border-top: 1px dashed #e8e8e8; font-size: 0.9em;\">\n"
            + "                        <dl class=\"diff-group\">\n"
            + "            <dt id=\"diff-329847360\"   data-reference=\"DOCUMENT:\"\n"
            + ">\n"
            + "          <span class=\"diff-icon diff-icon-change\" title=\"change\">\n"
            + "    Icon file text\n"
            + "  </span>\n"
            + "      Document properties\n"
            + "    </dt>\n"
            + "    <dd style=\"margin-left: 0\">\n"
            + "      <dl>\n"
            + "                                    <dt style=\"border: 1px solid #E8E8E8; "
            + "border-left: 5px solid #E8E8E8; color: #656565; padding: .5em .2em;\" data-property-name=\"content\">\n"
            + "    Content\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,1 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+some other content 3</td>\n"
            + "        </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"background-color: #ffcccc;\">-</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "          </dl>\n"
            + "    </dd>\n"
            + "  </dl>\n"
            + "        </div>\n"
            + "  \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());
        assertEquals(8, this.logCapture.size());
        assertEquals("Left side ($documentPropertiesSummary.modified) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 421, column 45]",
            this.logCapture.getMessage(4));
        assertEquals("Left side ($docDiff.metaData.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 369, column 34]",
            this.logCapture.getMessage(5));
        assertEquals("Left side ($docDiff.attachments.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 375, column 37]",
            this.logCapture.getMessage(6));
        assertEquals("Left side ($docDiff.classProperties.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 387, column 41]",
            this.logCapture.getMessage(7));
    }

    @Test
    void removedWordsNotificationHtmlTemplate() throws Exception
    {
        DocumentReference docWithMentionRef = new DocumentReference("xwiki", "Space", "DocWithMention");
        XWikiDocument docWithMention = new XWikiDocument(docWithMentionRef);
        docWithMention.setTitle("Doc with Mention");
        docWithMention.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        String version1 = docWithMention.getVersion();
        docWithMention.setContent("some other content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 4");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(RemovedWordsRecordableEvent.class.getCanonicalName());
        testEvent.setDate(new Date(489));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(docWithMentionRef);
        testEvent.setDocumentVersion(version1);
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
        testEvent2.setUser(USER_REFERENCE);
        testEvent2.setDocument(docWithMentionRef);
        testEvent2.setDocumentVersion(docWithMention.getVersion());
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
        CompositeEvent compositeEvent = new CompositeEvent(testEvent2);

        this.scriptContext.setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("isRemoval", true, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("emailUser", "user2", ScriptContext.ENGINE_SCOPE);

        String htmlTemplatePath = String.format(TEMPLATE_PATH, "html");
        String result = this.templateManager.render(htmlTemplatePath);
        String expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "              <strong>Words Notifications</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                          <div>\n"
            + "    <div style=\"background-color: #f5f5f5; color: #777777; padding: 4px 8px; "
            + "border-radius: 4px; font-size: 8px;\">\n"
            + "      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                     "
            + "                                                                                                   "
            + "xwiki / Space / Doc with Mention\n"
            + "      </div>\n"
            + "                  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "  </div>\n"
            + "                                            has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>User"
            + "  </span> "
            + "and contains 8 less occurrences of myQuery (on a total of 0)\n"
            + "          <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      it&#39;s been 8899 milliseconds\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                              \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                    "
            + "          <div style=\"border-top: 1px dashed #e8e8e8; font-size: 0.9em;\">\n"
            + "                        <dl class=\"diff-group\">\n"
            + "            <dt id=\"diff-329847360\"   data-reference=\"DOCUMENT:\"\n"
            + ">\n"
            + "          <span class=\"diff-icon diff-icon-change\" title=\"change\">\n"
            + "    Icon file text\n"
            + "  </span>\n"
            + "      Document properties\n"
            + "    </dt>\n"
            + "    <dd style=\"margin-left: 0\">\n"
            + "      <dl>\n"
            + "                                    <dt style=\"border: 1px solid #E8E8E8; "
            + "border-left: 5px solid #E8E8E8; color: #656565; padding: .5em .2em;\" data-property-name=\"content\">\n"
            + "    Content\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,1 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+some other content 4</td>\n"
            + "        </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"background-color: #ffcccc;\">-some other content</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "          </dl>\n"
            + "    </dd>\n"
            + "  </dl>\n"
            + "        </div>\n"
            + "  \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());
        assertEquals(4, this.logCapture.size());
        assertEquals("Left side ($documentPropertiesSummary.modified) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 421, column 45]",
            this.logCapture.getMessage(0));
        assertEquals("Left side ($docDiff.metaData.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 369, column 34]",
            this.logCapture.getMessage(1));
        assertEquals("Left side ($docDiff.attachments.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 375, column 37]",
            this.logCapture.getMessage(2));
        assertEquals("Left side ($docDiff.classProperties.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 387, column 41]",
            this.logCapture.getMessage(3));


        compositeEvent = new CompositeEvent(testEvent);
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
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        result = this.templateManager.render(htmlTemplatePath);
        expectedResult = "<table width=\"100%\">\n"
            + "    <tr>\n"
            + "                        <td width=\"25%\" style=\"width: 25%; vertical-align: top;\" valign=\"top\">\n"
            + "              <strong>Words Notifications</strong>\n"
            + "\n"
            + "      </td>\n"
            + "                        <td style=\"vertical-align: top;\" valign=\"top\">\n"
            + "                          <div>\n"
            + "    <div style=\"background-color: #f5f5f5; color: #777777; padding: 4px 8px; border-radius: 4px; "
            + "font-size: 8px;\">\n"
            + "      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                       "
            + "                                                                                                "
            + " xwiki / Space / Doc with Mention\n"
            + "      </div>\n"
            + "                  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention\">Doc with Mention</a>\n"
            + "  </div>\n"
            + "                                            has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; "
            + "Name\"/>User  "
            + "</span> "
            + "users and contains less occurrences of myQuery (on a total of 0)\n"
            + "          <div>\n"
            + "    <small style=\"color: #777777; font-size: 0.8em;\">\n"
            + "      it&#39;s been 8899 milliseconds\n"
            + "    </small>\n"
            + "  </div>\n"
            + "                                <table width=\"100%\" style=\"margin: 5px 0; font-size: 0.8em; "
            + "border-top: 1px dashed #e8e8e8\">\n"
            + "                              <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">            removed 8 occurrences (new total: 0)\n"
            + "</td>\n"
            + "                <td>  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=4.1\">formatted date of 8899 "
            + "milliseconds</a>\n"
            + "</td>\n"
            + "              </tr>\n"
            + "                      <tr style=\"vertical-align: top;\">\n"
            + "                <td width=\"33%\" style=\"overflow: hidden;\">        <img src=\"cid:User.jpg\" "
            + "alt=\"U\" width=\"16\" height=\"16\" style=\"vertical-align: middle;\"/>\n"
            + "     <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/XWiki/User\">First & Name</a>\n"
            + "</td>\n"
            + "                <td width=\"45%\">            removed 3 occurrences (new total: 8)\n"
            + "</td>\n"
            + "                <td>  <a color=\"#0088CC\" style=\"color: #0088CC; text-decoration: none;\" "
            + "href=\"/xwiki/bin/view/Space/DocWithMention?viewer=changes&#38;rev2=2.1\">formatted date of 489 "
            + "milliseconds</a>\n"
            + "</td>\n"
            + "              </tr>\n"
            + "            </table>\n"
            + "                      \n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "\n"
            + "                                                                                                       "
            + "       <div style=\"border-top: 1px dashed #e8e8e8; font-size: 0.9em;\">\n"
            + "                        <dl class=\"diff-group\">\n"
            + "            <dt id=\"diff-329847360\"   data-reference=\"DOCUMENT:\"\n"
            + ">\n"
            + "          <span class=\"diff-icon diff-icon-change\" title=\"change\">\n"
            + "    Icon file text\n"
            + "  </span>\n"
            + "      Document properties\n"
            + "    </dt>\n"
            + "    <dd style=\"margin-left: 0\">\n"
            + "      <dl>\n"
            + "                                    <dt style=\"border: 1px solid #E8E8E8; "
            + "border-left: 5px solid #E8E8E8; color: #656565; padding: .5em .2em;\" data-property-name=\"content\">\n"
            + "    Content\n"
            + "      </dt>\n"
            + "            <dd style=\"margin-left: 0\">  <div style=\"border: 1px solid #E8E8E8; "
            + "font-family: Monospace; overflow: auto;\">\n"
            + "    <table>\n"
            + "          <tr >\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "        <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">...</td>\n"
            + "                <td class=\"diff-line \" style=\"background-color: #eee; "
            + "color: rgba(101, 101, 101, 0.5); font-family: Monospace; padding: .4em .5em;\">@@ -1,1 +1,1 @@</td>\n"
            + "      </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"background-color: #ccffcc;\">+some other content 4</td>\n"
            + "        </tr>\n"
            + "              <tr >\n"
            + "                    <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\">1</td>\n"
            + "          <td style=\"border-right: 1px solid #E8E8E8; color: rgba(101, 101, 101, 0.5); "
            + "font-family: Monospace; text-align: right; vertical-align: top;\"></td>\n"
            + "          <td style=\"background-color: #ffcccc;\">-</td>\n"
            + "        </tr>\n"
            + "                    </table>\n"
            + "  </div>\n"
            + "</dd>\n"
            + "          </dl>\n"
            + "    </dd>\n"
            + "  </dl>\n"
            + "        </div>\n"
            + "  \n"
            + "      </td>\n"
            + "    </tr>\n"
            + "  </table>";
        assertEquals(expectedResult, result.trim());
        assertEquals(8, this.logCapture.size());
        assertEquals("Left side ($documentPropertiesSummary.modified) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 421, column 45]",
            this.logCapture.getMessage(4));
        assertEquals("Left side ($docDiff.metaData.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 369, column 34]",
            this.logCapture.getMessage(5));
        assertEquals("Left side ($docDiff.attachments.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 375, column 37]",
            this.logCapture.getMessage(6));
        assertEquals("Left side ($docDiff.classProperties.size()) of comparison operation has null value at "
                + "/templates/notificationWord/notification/email.html.vm[line 387, column 41]",
            this.logCapture.getMessage(7));
    }

    @Test
    void mentionedWordsPlainNotificationTemplate() throws Exception
    {
        DocumentReference docWithMentionRef = new DocumentReference("xwiki", "Space", "DocWithMention");
        XWikiDocument docWithMention = new XWikiDocument(docWithMentionRef);
        docWithMention.setTitle("Doc with Mention");
        docWithMention.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        String version1 = docWithMention.getVersion();
        docWithMention.setContent("some other content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 2");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 3");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 4");

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(MentionedWordsRecordableEvent.class.getCanonicalName());
        testEvent.setDate(new Date(12));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(docWithMentionRef);
        testEvent.setDocumentVersion(version1);
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
        testEvent2.setDocumentVersion(docWithMention.getVersion());
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

        CompositeEvent compositeEvent = new CompositeEvent(testEvent);
        this.scriptContext.setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("emailUser", "user1", ScriptContext.ENGINE_SCOPE);

        String plainTemplatePath = String.format(TEMPLATE_PATH, "plain");
        String result = this.templateManager.render(plainTemplatePath);
        String expectedResult = "Words Notifications:  [Doc with Mention](/xwiki/bin/view/Space/DocWithMention).\n"
            + "                            has been updated by             <span class=\"notification-event-user\" "
            + "data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span> and contains 2 new occurrences of fooQuery (on a total of 3)\n"
            + "        formatted date of 12 milliseconds\n"
            + "                    See changes: /xwiki/bin/view/Space/DocWithMention?viewer=changes&rev1=1.1&rev2=2.1";
        assertEquals(expectedResult, result.trim());

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
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);

        result = this.templateManager.render(plainTemplatePath);
        expectedResult = "Words Notifications:  [Doc with Mention](/xwiki/bin/view/Space/DocWithMention).\n"
            + "                            has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; Name\"/>"
            + "User  </span> users and contains new occurrences of fooQuery (on a total of 4)\n"
            + "        Last changes date: formatted date of 89 milliseconds\n"
            + "                    See changes: /xwiki/bin/view/Space/DocWithMention?viewer=changes&rev1=1.1&rev2=5.1";
        assertEquals(expectedResult, result.trim());
    }

    @Test
    void removedWordsNotificationPlainTemplate() throws Exception
    {
        DocumentReference docWithMentionRef = new DocumentReference("xwiki", "Space", "DocWithMention");
        XWikiDocument docWithMention = new XWikiDocument(docWithMentionRef);
        docWithMention.setTitle("Doc with Mention");
        docWithMention.setSyntax(Syntax.XWIKI_2_1);
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        String version1 = docWithMention.getVersion();
        docWithMention.setContent("some other content");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);
        docWithMention.setContent("some other content 4");
        this.oldcore.getSpyXWiki().saveDocument(docWithMention, this.context);

        Event testEvent = new DefaultEvent();
        testEvent.setApplication(NOTIF_APPLI_NAME);
        testEvent.setType(RemovedWordsRecordableEvent.class.getCanonicalName());
        testEvent.setDate(new Date(489));
        testEvent.setUser(USER_REFERENCE);
        testEvent.setDocument(docWithMentionRef);
        testEvent.setDocumentVersion(version1);
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
        testEvent2.setUser(USER_REFERENCE);
        testEvent2.setDocument(docWithMentionRef);
        testEvent2.setDocumentVersion(docWithMention.getVersion());
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
        CompositeEvent compositeEvent = new CompositeEvent(testEvent2);

        this.scriptContext.setAttribute("compositeEvent", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("isRemoval", true, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        this.scriptContext.setAttribute("emailUser", "user2", ScriptContext.ENGINE_SCOPE);

        String plainTemplatePath = String.format(TEMPLATE_PATH, "plain");
        String result = this.templateManager.render(plainTemplatePath);
        String expectedResult = "Words Notifications:  [Doc with Mention](/xwiki/bin/view/Space/DocWithMention).\n"
            + "                            has been updated by             <span class=\"notification-event-user\" "
            + "data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; "
            + "Name\"/>User  "
            + "</span> and contains 8 less occurrences of myQuery (on a total of 0)\n"
            + "        formatted date of 8899 milliseconds\n"
            + "                    See changes: /xwiki/bin/view/Space/DocWithMention?viewer=changes&rev1=3.1&rev2=4.1";
        assertEquals(expectedResult, result.trim());

        compositeEvent = new CompositeEvent(testEvent);
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
        this.scriptContext.setAttribute("event", compositeEvent, ScriptContext.ENGINE_SCOPE);
        result = this.templateManager.render(plainTemplatePath);
        expectedResult = "Words Notifications:  [Doc with Mention](/xwiki/bin/view/Space/DocWithMention).\n"
            + "                            has been updated by             "
            + "<span class=\"notification-event-user\" data-xwiki-lightbox=\"false\">\n"
            + "    <img src=\"/xwiki/bin/skin/skins/flamingo/icons/xwiki/noavatar.png\" alt=\"First &#38; "
            + "Name\"/>User  "
            + "</span> users"
            + " and contains less occurrences of myQuery (on a total of 0)\n"
            + "        Last changes date: formatted date of 8899 milliseconds\n"
            + "                    See changes: /xwiki/bin/view/Space/DocWithMention?viewer=changes&rev1=1.1&rev2=4.1";
        assertEquals(expectedResult, result.trim());
    }
}
