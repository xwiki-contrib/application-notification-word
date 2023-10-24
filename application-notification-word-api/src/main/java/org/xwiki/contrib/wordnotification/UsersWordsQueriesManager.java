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
package org.xwiki.contrib.wordnotification;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * API for manipulating {@link WordsQuery}.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
@Role
public interface UsersWordsQueriesManager
{
    /**
     * Retrieve all queries for the given user.
     *
     * @param userReference the user for which to retrieve queries.
     * @return a set of {@link WordsQuery}.
     * @throws WordsAnalysisException in case of problem to retrieve the queries
     */
    Set<WordsQuery> getQueries(UserReference userReference) throws WordsAnalysisException;

    /**
     * Store a new query for the {@link UserReference} of the {@link WordsQuery}.
     * @param wordsQuery the query information to store
     * @return {@code true} if the query has been stored properly and {@code false} if the query was already existing.
     * @throws WordsAnalysisException in case of problem when storing the information
     */
    boolean insertQuery(WordsQuery wordsQuery) throws WordsAnalysisException;

    /**
     * Remove a stored query.
     * @param wordsQuery the query information to remove
     * @return {@code true} if the query has been found and properly removed and {@code false} if it has not been found.
     * @throws WordsAnalysisException in case of problem to save the changes
     */
    boolean removeQuery(WordsQuery wordsQuery) throws WordsAnalysisException;

    /**
     * Retrieve all users that have defined words queries in the given wiki.
     *
     * @param wikiReference the wiki for which to retrieve users with words queries.
     * @return a set of {@link UserReference} having words queries
     * @throws WordsAnalysisException in case of problem to retrieve the users
     */
    Set<UserReference> getUserReferenceWithWordsQuery(WikiReference wikiReference) throws WordsAnalysisException;
}
