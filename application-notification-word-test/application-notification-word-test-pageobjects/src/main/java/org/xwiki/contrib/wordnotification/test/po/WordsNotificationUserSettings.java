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
package org.xwiki.contrib.wordnotification.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.text.StringUtils;

/**
 * Page object for manipulating the user profile words notifications settings.
 *
 * @version $Id$
 * @since 1.0
 */
public class WordsNotificationUserSettings extends BasePage
{
    private static final String QUERY_LIST_CLASS = "query-list";

    /**
     * Go to the words notification settings of the given user.
     * @param username the user profile document name
     * @return an instance of the current page object
     */
    public static WordsNotificationUserSettings gotoPage(String username)
    {
        getUtil().gotoPage("XWiki", username, "view", "category=notification.word.default.userprofile");
        return new WordsNotificationUserSettings();
    }

    /**
     * @return {@code true} if the settings already contains some words queries, {@code false} otherwise.
     */
    public boolean hasQueries()
    {
        return !getDriver().hasElementWithoutWaiting(By.className("empty-query"));
    }

    /**
     * @return the full list of words queries ordered by how they appear on screen.
     */
    public List<String> getQueries()
    {
        WebElement queryListContainer = getDriver().findElementWithoutWaiting(By.className(QUERY_LIST_CLASS));
        return getDriver().findElementsWithoutWaiting(queryListContainer, By.className("query"))
            .stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * @return {@code true} if the settings can be edited by the logged in user.
     */
    public boolean canEdit()
    {
        return getDriver().hasElementWithoutWaiting(By.id("add-query-object"));
    }

    /**
     * Edit the settings to insert a new word query. This method waits for a page reload after inserting a query.
     * @param query the expression to watch
     */
    public void insertQuery(String query)
    {
        getDriver().findElementWithoutWaiting(By.id("wordsquery")).sendKeys(query);
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElementWithoutWaiting(By.name("addQuery")).click();
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * Remove a specific query to stop watching it.
     * This method checks first if the given query is actually watched: it will return a {@link NotFoundException} if
     * it cannot be found.
     * @param query the watched expression to remove
     */
    public void removeQuery(String query)
    {
        List<String> queries = getQueries();
        int index = 0;
        boolean found = false;
        for (; index < queries.size(); index++) {
            if (StringUtils.equals(query, queries.get(index))) {
                found = true;
                break;
            }
        }
        if (found) {
            WebElement queryListContainer = getDriver().findElementWithoutWaiting(By.className(QUERY_LIST_CLASS));
            List<WebElement> buttons =
                getDriver().findElementsWithoutWaiting(queryListContainer, By.className("words-query-remove-button"));
            if (buttons.size() != queries.size() || index >= buttons.size()) {
                throw new NotFoundException(String.format("The query [%s] has been found at index [%s] but the list "
                        + "of buttons contains [%s] elements (against [%s] for the queries)",
                    query, index, buttons.size(), queries.size()));
            } else {
                getDriver().addPageNotYetReloadedMarker();
                buttons.get(index).click();
                getDriver().waitUntilPageIsReloaded();
            }
        } else {
            throw new NotFoundException(String.format("Cannot find query [%s] to remove.",
                query));
        }
    }
}
