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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.UsersWordsQueriesManager;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

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
    private static final String SAVE_COMMENT = "wordsNotification.storage.saveQuery";

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
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

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

    @Override
    public boolean insertQuery(WordsQuery wordsQuery) throws WordsAnalysisException
    {
        UserReference userReference = wordsQuery.getUserReference();
        Set<WordsQuery> wordsQueries = this.readWordsQuery(userReference);
        boolean result = false;
        if (wordsQueries.stream().filter(item -> item.equals(wordsQueries)).findFirst().isEmpty()) {
            try {
                XWikiDocument wordsQueryDocument = this.getWordsQueryDocument(userReference);
                XWikiContext context = contextProvider.get();
                BaseObject baseObject =
                    wordsQueryDocument.newXObject(WordsQueryXClassInitializer.XCLASS_REFERENCE, context);
                baseObject.setStringValue(WordsQueryXClassInitializer.QUERY_FIELD, wordsQuery.getQuery());
                String saveMessage =
                    this.contextualLocalizationManager.getTranslationPlain(SAVE_COMMENT);
                context.getWiki().saveDocument(wordsQueryDocument, saveMessage, true, context);
                this.wordsQueryCache.invalidateQueriesFrom(userReference);
                result = true;
            } catch (XWikiException e) {
                throw new WordsAnalysisException(String.format("Error when trying to insert a new words query [%s]",
                    wordsQuery), e);
            }
        }
        return result;
    }

    @Override
    public boolean removeQuery(WordsQuery wordsQuery) throws WordsAnalysisException
    {
        boolean result = false;
        UserReference userReference = wordsQuery.getUserReference();
        try {
            XWikiDocument docUser = this.getWordsQueryDocument(userReference);
            List<BaseObject> xObjects =
                docUser.getXObjects(WordsQueryXClassInitializer.XCLASS_REFERENCE);
            BaseObject objectToRemove = null;
            for (BaseObject xobject : xObjects) {
                if (xobject != null) {
                    String query = xobject.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD);
                    if (StringUtils.equals(query, wordsQuery.getQuery())) {
                        objectToRemove = xobject;
                        break;
                    }
                }
            }
            if (objectToRemove != null) {
                result = docUser.removeXObject(objectToRemove);
                if (result) {
                    XWikiContext context = this.contextProvider.get();
                    String saveMessage =
                        this.contextualLocalizationManager.getTranslationPlain(SAVE_COMMENT);
                    context.getWiki().saveDocument(docUser, saveMessage, true, context);
                    this.wordsQueryCache.invalidateQueriesFrom(userReference);
                }
            }
        } catch (XWikiException e) {
            throw new WordsAnalysisException(
                String.format("Error when accessing xobjects in document [%s]", userReference), e);
        }
        return result;
    }

    private XWikiDocument getWordsQueryDocument(UserReference userReference) throws XWikiException
    {
        DocumentReference docUserReference = this.documentReferenceUserReferenceSerializer.serialize(userReference);
        XWikiContext context = contextProvider.get();
        return context.getWiki().getDocument(docUserReference, context);
    }

    private Set<WordsQuery> readWordsQuery(UserReference userReference) throws WordsAnalysisException
    {
        try {
            XWikiDocument docUser = this.getWordsQueryDocument(userReference);
            List<BaseObject> xObjects =
                docUser.getXObjects(WordsQueryXClassInitializer.XCLASS_REFERENCE);
            return this.readWordsQuery(xObjects, userReference);
        } catch (XWikiException e) {
            throw new WordsAnalysisException(
                String.format("Error when trying to read user document [%s]", userReference), e);
        }
    }

    private Set<WordsQuery> readWordsQuery(List<BaseObject> xobjects, UserReference userReference)
    {
        Set<WordsQuery> result = new HashSet<>();

        for (BaseObject xobject : xobjects) {
            if (xobject != null) {
                String query = xobject.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD);
                result.add(new WordsQuery(query, userReference));
            }
        }
        return result;
    }

    @Override
    public Set<UserReference> getUserReferenceWithWordsQuery(WikiReference wikiReference) throws WordsAnalysisException
    {
        Set<UserReference> result;
        boolean isMainWiki = this.wikiDescriptorManager.isMainWiki(wikiReference.getName());
        if (!isMainWiki) {
            WikiReference mainWikiReference = new WikiReference(this.wikiDescriptorManager.getMainWikiId());
            try {
                UserScope userScope = this.wikiUserManager.getUserScope(wikiReference.getName());
                switch (userScope) {
                    case LOCAL_ONLY:
                        result = this.getUsersWithWordsQueryForWiki(wikiReference);
                        break;

                    case GLOBAL_ONLY:
                        result =
                            this.getUsersWithWordsQueryForWiki(mainWikiReference);
                        break;

                    case LOCAL_AND_GLOBAL:
                    default:
                        result = new HashSet<>();
                        result.addAll(this.getUsersWithWordsQueryForWiki(wikiReference));
                        result.addAll(this.getUsersWithWordsQueryForWiki(mainWikiReference));
                }
            } catch (WikiUserManagerException e) {
                throw new WordsAnalysisException(
                    String.format("Error when trying to access the user scope definition of wiki [%s]",
                        wikiReference), e);
            }
        } else {
            result = this.getUsersWithWordsQueryForWiki(wikiReference);
        }
        return result;
    }

    private Set<UserReference> getUsersWithWordsQueryForWiki(WikiReference wikiReference) throws WordsAnalysisException
    {
        Set<UserReference> result;
        Optional<Set<UserReference>> userReferencesOpt = this.wordsQueryCache.getUserReferences(wikiReference);
        if (userReferencesOpt.isEmpty()) {
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
