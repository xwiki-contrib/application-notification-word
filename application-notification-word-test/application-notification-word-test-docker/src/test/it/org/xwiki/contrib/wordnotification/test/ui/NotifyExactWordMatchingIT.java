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
package org.xwiki.contrib.wordnotification.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.test.po.TaskConsumerUtil;
import org.xwiki.contrib.wordnotification.test.po.WordsNotificationUserSettings;
import org.xwiki.platform.notifications.test.po.NotificationsTrayPage;
import org.xwiki.platform.notifications.test.po.NotificationsUserProfilePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BootstrapSwitch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scenario for checking that the exact word matching is working.
 *
 * @version $Id$
 * @since 1.0
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // The Solr store is not ready yet to be installed as extension
        "org.xwiki.platform:xwiki-platform-eventstream-store-solr",
        "org.xwiki.platform:xwiki-platform-wiki-user-default"
    },
    resolveExtraJARs = true
)
public class NotifyExactWordMatchingIT
{
    private static final String USER_PREFIX = "NotifyExactWordMatchingIT";
    private static final String USER_EDITOR = USER_PREFIX + "Editor";
    private static final String USER_LISTENER = USER_PREFIX + "Listener";

    private static final String LISTENED_WORD_1 = "A not obvious expression 444551";

    @BeforeAll
    void setup(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createUser(USER_EDITOR, USER_EDITOR, "");
        testUtils.createUser(USER_LISTENER, USER_LISTENER, "");
        testUtils.setGlobalRights("", String.format("XWiki.%s,XWiki.%s", USER_EDITOR, USER_LISTENER), "script", true);
        testUtils.updateObject("XWiki", USER_EDITOR, "XWiki.XWikiUsers", 0, "editor", "wiki");
    }

    @Test
    @Order(0)
    void checkNoNotificationForPreviousEditions(TestUtils testUtils, TestReference testReference) throws Exception
    {
        NotificationsUserProfilePage notificationsUserProfilePage =
            NotificationsUserProfilePage.gotoPage(USER_LISTENER);
        notificationsUserProfilePage
            .setApplicationState("notificationWord.application.name", "alert", BootstrapSwitch.State.ON);

        testUtils.login(USER_EDITOR, USER_EDITOR);
        String content = String.format("Some content\n"
            + "And in the middle of a line\n"
            + "Here: %s with some suffix\n"
            + "Another line\n", LISTENED_WORD_1);
        testUtils.createPage(testReference, content, "a title");

        testUtils.login(USER_LISTENER, USER_LISTENER);
        WordsNotificationUserSettings wordsNotificationUserSettings =
            WordsNotificationUserSettings.gotoPage(USER_LISTENER);
        assertTrue(wordsNotificationUserSettings.canEdit(), "User cannot edit their own settings.");
        assertFalse(wordsNotificationUserSettings.hasQueries());
        wordsNotificationUserSettings.insertQuery(LISTENED_WORD_1);
        assertTrue(wordsNotificationUserSettings.hasQueries());

        TaskConsumerUtil.INSTANCE.waitUntilWordsNotificationTaskIsDone();

        NotificationsTrayPage trayPage = new NotificationsTrayPage();
        assertEquals(0, trayPage.getUnreadNotificationsCount());
    }

    @Test
    @Order(1)
    void checkAddingExpressionInContent(TestUtils testUtils, TestReference testReference)
    {
        testUtils.login(USER_EDITOR, USER_EDITOR);
        String content = String.format("Some content\n"
            + "And in the middle of a line\n"
            + "Here: %s with some suffix\n"
            + "Another line\n", LISTENED_WORD_1);
        testUtils.createPage(testReference, content, "checkAddingExpressionInContent page");

        testUtils.login(USER_LISTENER, USER_LISTENER);
        TaskConsumerUtil.INSTANCE.waitUntilWordsNotificationTaskIsDone();
        NotificationsTrayPage trayPage = new NotificationsTrayPage();
        trayPage.showNotificationTray();
        assertEquals(1, trayPage.getUnreadNotificationsCount());
        assertEquals("Mark event as read\n"
            + "checkAddingExpressionInContent page\n"
            + "has been updated by NotifyExactWordMatchingITEditor and contains 1 new occurrences of A not obvious "
            + "expression 444551 (on a total of 1)\n"
            + "moments ago", trayPage.getNotificationContent(0));
    }
}
