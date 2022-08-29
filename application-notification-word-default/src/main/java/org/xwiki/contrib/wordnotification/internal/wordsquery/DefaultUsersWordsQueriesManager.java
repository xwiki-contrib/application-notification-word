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
package org.xwiki.contrib.wordnotification.internal.wordsquery;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.UsersWordsQueriesManager;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link UsersWordsQueriesManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultUsersWordsQueriesManager implements UsersWordsQueriesManager
{
    @Inject
    private WordsQueryCache wordsQueryCache;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private QueryManager queryManager;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Set<WordsQuery> getQueries(UserReference userReference) throws WordsAnalysisException
    {
        Optional<Set<WordsQuery>> wordsQueriesOpt = this.wordsQueryCache.getWordsQueries(userReference);
        Set<WordsQuery> result;
        if (wordsQueriesOpt.isPresent()) {
            result = wordsQueriesOpt.get();
        } else {
            result = this.readWordsQuery(userReference);
            this.wordsQueryCache.setWordsQueries(userReference, result);
        }
        return result;
    }

    private Set<WordsQuery> readWordsQuery(UserReference userReference) throws WordsAnalysisException
    {
        DocumentReference docUserReference = this.documentReferenceUserReferenceSerializer.serialize(userReference);
        XWikiContext context = contextProvider.get();
        try {
            XWikiDocument docUser = context.getWiki().getDocument(docUserReference, context);
            List<BaseObject> xObjects = docUser.getXObjects(WordsQueryXClassInitializer.XCLASS_REFERENCE);
            return this.readWordsQuery(xObjects, userReference);
        } catch (XWikiException e) {
            throw new WordsAnalysisException(
                String.format("Error when trying to read user document [%s]", docUserReference), e);
        }
    }

    private Set<WordsQuery> readWordsQuery(List<BaseObject> xobjects, UserReference userReference)
    {
        Set<WordsQuery> result = new HashSet<>();

        for (BaseObject xobject : xobjects) {
            String query = xobject.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD);
            result.add(new WordsQuery(query, userReference));
        }
        return result;
    }

    @Override
    public Set<UserReference> getUserReferenceWithWordsQuery(WikiReference wikiReference) throws WordsAnalysisException
    {
        Optional<Set<UserReference>> userReferencesOpt = this.wordsQueryCache.getUserReferences(wikiReference);
        Set<UserReference> result;
        if (userReferencesOpt.isEmpty()) {
            // FIXME: handle UC when an update is performed on subwiki and the user belongs to main wiki?
            String query = String.format(", BaseObject as obj, BaseObject as obj2 "
                    + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' "
                    + "and doc.fullName=obj2.name and obj2.className='%s'",
                this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE));

            try {
                List<String> usersList = this.queryManager.createQuery(query, Query.HQL)
                    .setWiki(wikiReference.getName())
                    .execute();

                result = usersList.stream().map(this.userReferenceResolver::resolve).collect(Collectors.toSet());
                this.wordsQueryCache.setUserReferences(result, wikiReference);
            } catch (QueryException e) {
                throw new WordsAnalysisException("Error while trying to get the list of users with a words query", e);
            }
        } else {
            result = userReferencesOpt.get();
        }
        return result;
    }
}
