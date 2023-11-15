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
package org.xwiki.contrib.wordnotification.script;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.PatternAnalysisHelper;
import org.xwiki.contrib.wordnotification.UsersWordsQueriesManager;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service related to the word notifications.
 * This component mainly aims at manipulating words query storage for now.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named("notificationwords")
public class NotificationWordsScriptService implements ScriptService
{
    @Inject
    private UsersWordsQueriesManager usersWordsQueriesManager;

    @Inject
    private PatternAnalysisHelper patternAnalysisHelper;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Retrieve all queries of the given user.
     * @param userReference the user for whom to get queries
     * @return the actual set of queries
     * @throws WordsAnalysisException in case of problem to retrieve the queries
     * @see UsersWordsQueriesManager#getQueries(UserReference)
     */
    public Set<String> getUserQueries(UserReference userReference) throws WordsAnalysisException
    {
        return this.usersWordsQueriesManager.getQueries(userReference)
            .stream().map(WordsQuery::getQuery)
            .collect(Collectors.toSet());
    }

    /**
     * Store a new query to be used.
     * @param userReference the user for whom to store a query.
     * @param query the new query to be stored.
     * @return {@code true} if the query has been stored and {@code false} if it was already existing.
     * @throws WordsAnalysisException in case of problem to save the changes
     * @see UsersWordsQueriesManager#insertQuery(WordsQuery)
     */
    public boolean insertQuery(UserReference userReference, String query) throws WordsAnalysisException
    {
        return this.usersWordsQueriesManager.insertQuery(new WordsQuery(query, userReference));
    }

    /**
     * Remove an existing query to not use it anymore.
     * @param userReference the user for whom to remove the query.
     * @param query the query to be removed.
     * @return {@code true} if the query has been removed and {@code false} if it cannot be found
     * @throws WordsAnalysisException in case of problem to save the changes
     * @see UsersWordsQueriesManager#removeQuery(WordsQuery)
     */
    public boolean removeQuery(UserReference userReference, String query) throws WordsAnalysisException
    {
        return this.usersWordsQueriesManager.removeQuery(new WordsQuery(query, userReference));
    }

    /**
     * Allow to test a query against a text to analyze.
     *
     * @param query the query to test
     * @param textToAnalyze the text to analyze
     * @return a list of localization
     * @since 1.1
     */
    public List<WordsMentionLocalization> testPattern(String query, String textToAnalyze)
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference documentReference = context.getDoc().getDocumentReference();
        return this.patternAnalysisHelper.getRegions(query, List.of(textToAnalyze), documentReference);
    }
}
