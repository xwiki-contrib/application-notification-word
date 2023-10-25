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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultUsersWordsQueriesManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@ComponentTest
class DefaultUsersWordsQueriesManagerTest
{
    @InjectMockComponents
    private DefaultUsersWordsQueriesManager usersWordsQueriesManager;

    @MockComponent
    private WordsQueryCache wordsQueryCache;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private WikiUserManager wikiUserManager;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private XWikiContext context;
    private XWiki wiki;

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @Test
    void getQueries() throws WordsAnalysisException, XWikiException
    {
        UserReference userReference = mock(UserReference.class);

        Set<WordsQuery> result = Set.of(mock(WordsQuery.class), mock(WordsQuery.class));
        when(this.wordsQueryCache.getWordsQueries(userReference)).thenReturn(Optional.of(result));

        assertEquals(result, this.usersWordsQueriesManager.getQueries(userReference));

        when(this.wordsQueryCache.getWordsQueries(userReference)).thenReturn(Optional.empty());

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "FooUser");
        when(this.documentReferenceUserReferenceSerializer.serialize(userReference)).thenReturn(documentReference);

        XWikiDocument userDocument = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(userDocument);

        BaseObject object1 = mock(BaseObject.class, "object1");
        BaseObject object2 = mock(BaseObject.class, "object2");
        List<BaseObject> objectList = new ArrayList<>();
        objectList.add(null);
        objectList.add(object1);
        objectList.add(null);
        objectList.add(object2);
        objectList.add(null);

        when(userDocument.getXObjects(WordsQueryXClassInitializer.XCLASS_REFERENCE)).thenReturn(objectList);

        String query1 = "query1";
        String query2 = "query2";
        when(object1.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD)).thenReturn(query1);
        when(object2.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD)).thenReturn(query2);

        WordsQuery wordsQuery1 = new WordsQuery(query1, userReference);
        WordsQuery wordsQuery2 = new WordsQuery(query2, userReference);

        result = Set.of(wordsQuery1, wordsQuery2);
        assertEquals(result, this.usersWordsQueriesManager.getQueries(userReference));
        verify(this.wordsQueryCache).setWordsQueries(userReference, result);
    }

    @Test
    void insertQuery() throws WordsAnalysisException, XWikiException
    {
        UserReference userReference = mock(UserReference.class);
        String newQuery = "newQuery";
        WordsQuery newWordsQuery = new WordsQuery(newQuery, userReference);

        WordsQuery wordsQuery1 = new WordsQuery(newQuery, userReference);
        WordsQuery wordsQuery2 = new WordsQuery("query2", userReference);

        Set<WordsQuery> result = Set.of(wordsQuery1, wordsQuery2);
        when(this.wordsQueryCache.getWordsQueries(userReference)).thenReturn(Optional.of(result));
        assertFalse(this.usersWordsQueriesManager.insertQuery(newWordsQuery));

        wordsQuery1 = new WordsQuery("query1", userReference);
        result = Set.of(wordsQuery1, wordsQuery2);
        when(this.wordsQueryCache.getWordsQueries(userReference)).thenReturn(Optional.of(result));

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "FooUser");
        when(this.documentReferenceUserReferenceSerializer.serialize(userReference)).thenReturn(documentReference);

        XWikiDocument userDocument = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(userDocument);

        BaseObject baseObject = mock(BaseObject.class);
        when(userDocument.newXObject(WordsQueryXClassInitializer.XCLASS_REFERENCE, this.context))
            .thenReturn(baseObject);

        String saveMessage = "saveMessage";
        when(this.contextualLocalizationManager.getTranslationPlain("wordsNotification.storage.saveQuery"))
            .thenReturn(saveMessage);

        assertTrue(this.usersWordsQueriesManager.insertQuery(newWordsQuery));

        verify(baseObject).setStringValue(WordsQueryXClassInitializer.QUERY_FIELD, newQuery);
        verify(this.wiki).saveDocument(userDocument, saveMessage, true, this.context);
        verify(this.wordsQueryCache).invalidateQueriesFrom(userReference);
    }

    @Test
    void removeQuery() throws XWikiException, WordsAnalysisException
    {
        UserReference userReference = mock(UserReference.class);
        String oldQuery = "oldQuery";
        WordsQuery oldWordsQuery = new WordsQuery(oldQuery, userReference);

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "FooUser");
        when(this.documentReferenceUserReferenceSerializer.serialize(userReference)).thenReturn(documentReference);

        XWikiDocument userDocument = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(userDocument);

        BaseObject object1 = mock(BaseObject.class, "object1");
        BaseObject object2 = mock(BaseObject.class, "object2");
        List<BaseObject> objectList = new ArrayList<>();
        objectList.add(null);
        objectList.add(object1);
        objectList.add(null);
        objectList.add(object2);
        objectList.add(null);

        when(userDocument.getXObjects(WordsQueryXClassInitializer.XCLASS_REFERENCE)).thenReturn(objectList);
        when(object1.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD)).thenReturn("query1");
        when(object2.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD)).thenReturn("query2");

        assertFalse(this.usersWordsQueriesManager.removeQuery(oldWordsQuery));
        verify(userDocument, never()).removeXObject(any());
        verify(this.wiki, never()).saveDocument(eq(userDocument), any(), anyBoolean(), eq(this.context));

        when(object2.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD)).thenReturn(oldQuery);

        when(userDocument.removeXObject(object2)).thenReturn(true);
        String saveMessage = "saveMessage";
        when(this.contextualLocalizationManager.getTranslationPlain("wordsNotification.storage.saveQuery"))
            .thenReturn(saveMessage);
        assertTrue(this.usersWordsQueriesManager.removeQuery(oldWordsQuery));
        verify(userDocument).removeXObject(object2);
        verify(this.wiki).saveDocument(userDocument, saveMessage, true, this.context);
        verify(this.wordsQueryCache).invalidateQueriesFrom(userReference);
    }

    @Test
    void getUserReferenceWithWordsQueryMainWiki() throws QueryException, WordsAnalysisException
    {
        WikiReference wikiReference = new WikiReference("mainWiki");
        when(this.wikiDescriptorManager.isMainWiki(wikiReference.getName())).thenReturn(true);

        UserReference userReference1 = mock(UserReference.class, "user1");
        UserReference userReference2 = mock(UserReference.class, "user2");

        Set<UserReference> result = Set.of(userReference1, userReference2);
        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.of(result));

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verifyNoInteractions(this.queryManager);

        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.empty());

        String serializedClass = "serializedClass";
        when(this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE))
            .thenReturn(serializedClass);

        String expectedQuery = String.format(", BaseObject as obj, BaseObject as obj2 "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' "
            + "and doc.fullName=obj2.name and obj2.className='%s'", serializedClass);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.setWiki(wikiReference.getName())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("user1", "user2"));

        when(this.userReferenceResolver.resolve("user1")).thenReturn(userReference1);
        when(this.userReferenceResolver.resolve("user2")).thenReturn(userReference2);

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verify(this.wordsQueryCache).setUserReferences(result, wikiReference);
    }

    @Test
    void getUserReferenceWithWordsQuerySubWikiOnlyLocal()
        throws QueryException, WordsAnalysisException, WikiUserManagerException
    {
        WikiReference wikiReference = new WikiReference("subWikiOnlyLocal");
        WikiReference mainWikiReference = new WikiReference("mainWiki");
        when(this.wikiDescriptorManager.isMainWiki(wikiReference.getName())).thenReturn(false);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(mainWikiReference.getName());
        when(this.wikiUserManager.getUserScope(wikiReference.getName())).thenReturn(UserScope.LOCAL_ONLY);

        UserReference userReference1 = mock(UserReference.class, "user1");
        UserReference userReference2 = mock(UserReference.class, "user2");

        Set<UserReference> result = Set.of(userReference1, userReference2);
        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.of(result));

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verifyNoInteractions(this.queryManager);

        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.empty());

        String serializedClass = "serializedClass";
        when(this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE))
            .thenReturn(serializedClass);

        String expectedQuery = String.format(", BaseObject as obj, BaseObject as obj2 "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' "
            + "and doc.fullName=obj2.name and obj2.className='%s'", serializedClass);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.setWiki(wikiReference.getName())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("user1", "user2"));

        when(this.userReferenceResolver.resolve("user1")).thenReturn(userReference1);
        when(this.userReferenceResolver.resolve("user2")).thenReturn(userReference2);

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verify(this.wordsQueryCache).setUserReferences(result, wikiReference);
    }

    @Test
    void getUserReferenceWithWordsQuerySubWikiOnlyGlobal()
        throws QueryException, WordsAnalysisException, WikiUserManagerException
    {
        WikiReference wikiReference = new WikiReference("subWikiOnlyGlobal");
        WikiReference mainWikiReference = new WikiReference("mainWiki");
        when(this.wikiDescriptorManager.isMainWiki(wikiReference.getName())).thenReturn(false);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(mainWikiReference.getName());
        when(this.wikiUserManager.getUserScope(wikiReference.getName())).thenReturn(UserScope.GLOBAL_ONLY);

        UserReference userReference1 = mock(UserReference.class, "user1");
        UserReference userReference2 = mock(UserReference.class, "user2");

        Set<UserReference> result = Set.of(userReference1, userReference2);
        when(this.wordsQueryCache.getUserReferences(mainWikiReference)).thenReturn(Optional.of(result));

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verifyNoInteractions(this.queryManager);

        when(this.wordsQueryCache.getUserReferences(mainWikiReference)).thenReturn(Optional.empty());

        String serializedClass = "serializedClass";
        when(this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE))
            .thenReturn(serializedClass);

        String expectedQuery = String.format(", BaseObject as obj, BaseObject as obj2 "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' "
            + "and doc.fullName=obj2.name and obj2.className='%s'", serializedClass);

        Query query = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query);
        when(query.setWiki(mainWikiReference.getName())).thenReturn(query);
        when(query.execute()).thenReturn(List.of("user1", "user2"));

        when(this.userReferenceResolver.resolve("user1")).thenReturn(userReference1);
        when(this.userReferenceResolver.resolve("user2")).thenReturn(userReference2);

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verify(this.wordsQueryCache).setUserReferences(result, mainWikiReference);
    }

    @Test
    void getUserReferenceWithWordsQuerySubWikiGlobalAndLocal()
        throws QueryException, WordsAnalysisException, WikiUserManagerException
    {
        WikiReference wikiReference = new WikiReference("subWikiOnlyGlobal");
        WikiReference mainWikiReference = new WikiReference("mainWiki");
        when(this.wikiDescriptorManager.isMainWiki(wikiReference.getName())).thenReturn(false);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(mainWikiReference.getName());
        when(this.wikiUserManager.getUserScope(wikiReference.getName())).thenReturn(UserScope.LOCAL_AND_GLOBAL);

        UserReference userReference1 = mock(UserReference.class, "user1");
        UserReference userReference2 = mock(UserReference.class, "user2");
        UserReference userReference3 = mock(UserReference.class, "user3");
        UserReference userReference4 = mock(UserReference.class, "user4");

        Set<UserReference> resultGlobal = Set.of(userReference1, userReference2);
        when(this.wordsQueryCache.getUserReferences(mainWikiReference)).thenReturn(Optional.of(resultGlobal));
        Set<UserReference> resultLocal = Set.of(userReference3, userReference4);
        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.of(resultLocal));

        Set<UserReference> result = new HashSet<>(resultLocal);
        result.addAll(resultGlobal);
        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verifyNoInteractions(this.queryManager);

        when(this.wordsQueryCache.getUserReferences(mainWikiReference)).thenReturn(Optional.empty());

        String serializedClass = "serializedClass";
        when(this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE))
            .thenReturn(serializedClass);

        String expectedQuery = String.format(", BaseObject as obj, BaseObject as obj2 "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' "
            + "and doc.fullName=obj2.name and obj2.className='%s'", serializedClass);

        Query query1 = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query1);
        when(query1.setWiki(mainWikiReference.getName())).thenReturn(query1);
        when(query1.execute()).thenReturn(List.of("user1", "user2"));

        when(this.userReferenceResolver.resolve("user1")).thenReturn(userReference1);
        when(this.userReferenceResolver.resolve("user2")).thenReturn(userReference2);

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verify(this.wordsQueryCache).setUserReferences(resultGlobal, mainWikiReference);
        verify(this.wordsQueryCache, never()).setUserReferences(any(), eq(wikiReference));

        when(this.wordsQueryCache.getUserReferences(mainWikiReference)).thenReturn(Optional.of(resultGlobal));
        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.empty());

        Query query2 = mock(Query.class);
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query2);
        when(query2.setWiki(wikiReference.getName())).thenReturn(query2);
        when(query2.execute()).thenReturn(List.of("user3", "user4"));

        when(this.userReferenceResolver.resolve("user3")).thenReturn(userReference3);
        when(this.userReferenceResolver.resolve("user4")).thenReturn(userReference4);

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));
        verify(this.wordsQueryCache).setUserReferences(resultGlobal, mainWikiReference);
        verify(this.wordsQueryCache).setUserReferences(resultLocal, wikiReference);

        when(this.wordsQueryCache.getUserReferences(mainWikiReference)).thenReturn(Optional.empty());
        when(this.wordsQueryCache.getUserReferences(wikiReference)).thenReturn(Optional.empty());

        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(query1);
        when(query1.setWiki(mainWikiReference.getName())).thenReturn(query1);
        when(query1.setWiki(wikiReference.getName())).thenReturn(query1);
        when(query1.execute())
            .thenReturn(List.of("user3", "user4"))
            .thenReturn(List.of("user1", "user2"));

        assertEquals(result, this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference));

        verify(this.wordsQueryCache, times(2)).setUserReferences(resultGlobal, mainWikiReference);
        verify(this.wordsQueryCache, times(2)).setUserReferences(resultLocal, wikiReference);
    }
}