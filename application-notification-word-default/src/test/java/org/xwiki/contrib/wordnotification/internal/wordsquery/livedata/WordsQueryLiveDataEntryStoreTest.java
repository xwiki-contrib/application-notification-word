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
package org.xwiki.contrib.wordnotification.internal.wordsquery.livedata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.internal.wordsquery.WordsQueryXClassInitializer;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WordsQueryLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
@ReferenceComponentList
class WordsQueryLiveDataEntryStoreTest
{
    private static final String CURRENT_DOC_PARAMETER = "currentDoc";

    @InjectMockComponents
    private WordsQueryLiveDataEntryStore entryStore;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private ContextualLocalizationManager l10n;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> readOnlyContextProvider;

    @MockComponent
    private CSRFToken csrfToken;

    private XWikiContext context;
    private XWiki xwiki;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager)
    {
        // Needed for manipulating BaseObjectReference
        Utils.setComponentManager(componentManager);
    }

    @BeforeEach
    void beforeEach()
    {
        this.context = mock(XWikiContext.class);
        this.xwiki = mock(XWiki.class);
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.readOnlyContextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void getEntryWithId() throws XWikiException, LiveDataException
    {
        String entryId = "XWiki.Foo^NotificationWords.Code.WordsQueryXClass[18]";

        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");

        XWikiDocument userDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(userReference, this.context)).thenReturn(userDoc);

        // No base object found in the doc
        assertEquals(Optional.empty(), this.entryStore.get(entryId));

        BaseObject baseObject = mock(BaseObject.class);
        when(userDoc.getXObject(new DocumentReference(WordsQueryXClassInitializer.XCLASS_REFERENCE,
            new WikiReference("xwiki")), 18)).thenReturn(baseObject);
        when(contextualAuthorizationManager.hasAccess(Right.EDIT, userReference)).thenReturn(true);

        String query = "someQuery";
        when(baseObject.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD)).thenReturn(query);
        String csrfToken = "someToken";
        when(this.csrfToken.getToken()).thenReturn(csrfToken);
        String redirectUrl = "redirectUrl";
        when(userDoc.getURL("view", "category=notification.word.default.userprofile", this.context))
            .thenReturn(redirectUrl);
        String objectRemoveURL = "objectRemoveUrl";
        String queryParams = "classname=NotificationWords.Code.WordsQueryXClass"
            + "&classid=18"
            + "&form_token=someToken"
            + "&xredirect=redirectUrl";
        when(userDoc.getURL("objectremove", queryParams, this.context)).thenReturn(objectRemoveURL);

        Map<String, Object> expectedResult = Map.of(
            WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD, entryId,
            WordsQueryXClassInitializer.QUERY_FIELD, query,
            WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, true,
            WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD, objectRemoveURL
        );
        assertEquals(Optional.of(expectedResult), this.entryStore.get(entryId));
    }

    @Test
    void getFromQuery() throws XWikiException, QueryException, LiveDataException
    {
        LiveDataQuery query = mock(LiveDataQuery.class);
        LiveDataQuery.Source source = mock(LiveDataQuery.Source.class);
        when(query.getSource()).thenReturn(source);
        String currentDoc = "sub:XWiki.Foo";
        when(source.getParameters()).thenReturn(Map.of(CURRENT_DOC_PARAMETER, currentDoc));
        Long offset = 32L;
        when(query.getOffset()).thenReturn(offset);
        int limit = 23;
        when(query.getLimit()).thenReturn(limit);
        DocumentReference docReference = new DocumentReference("sub", "XWiki", "Foo");
        when(contextualAuthorizationManager.hasAccess(Right.EDIT, docReference)).thenReturn(true);
        XWikiDocument userDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(docReference, this.context)).thenReturn(userDoc);

        String expectedQuery = "select obj.number, prop.value from XWikiDocument doc, BaseObject obj, "
            + "StringProperty prop "
            + "where doc.fullName=obj.name and obj.className=:className and prop.id.id=obj.id "
            + "and prop.name=:propertyName and doc.fullName=:docFullName  order by prop.value asc";
        Query hqlQuery = mock(Query.class, "query1");
        when(this.queryManager.createQuery(expectedQuery, Query.HQL)).thenReturn(hqlQuery);
        when(hqlQuery.setLimit(limit)).thenReturn(hqlQuery);
        when(hqlQuery.setOffset(offset.intValue())).thenReturn(hqlQuery);
        when(hqlQuery.bindValue("className", "NotificationWords.Code.WordsQueryXClass")).thenReturn(hqlQuery);
        when(hqlQuery.bindValue("propertyName", "query")).thenReturn(hqlQuery);
        when(hqlQuery.bindValue("docFullName", "XWiki.Foo")).thenReturn(hqlQuery);
        when(hqlQuery.setWiki("sub")).thenReturn(hqlQuery);

        List<Object> queryResult = List.of(
            new Object[] { 10, "query1" },
            new Object[] { 22, "query2" },
            new Object[] { 58, "query3" }
        );
        when(hqlQuery.execute()).thenReturn(queryResult);

        String csrfToken = "someToken";
        when(this.csrfToken.getToken()).thenReturn(csrfToken);
        String redirectUrl = "redirectUrl";
        when(userDoc.getURL("view", "category=notification.word.default.userprofile", this.context))
            .thenReturn(redirectUrl);

        String objectRemoveURL1 = "objectRemoveUrl1";
        when(userDoc.getURL("objectremove", "classname=NotificationWords.Code.WordsQueryXClass"
            + "&classid=10"
            + "&form_token=someToken"
            + "&xredirect=redirectUrl", this.context)).thenReturn(objectRemoveURL1);

        String objectRemoveURL2 = "objectRemoveUrl1";
        when(userDoc.getURL("objectremove", "classname=NotificationWords.Code.WordsQueryXClass"
            + "&classid=22"
            + "&form_token=someToken"
            + "&xredirect=redirectUrl", this.context)).thenReturn(objectRemoveURL2);

        String objectRemoveURL3 = "objectRemoveUrl1";
        when(userDoc.getURL("objectremove", "classname=NotificationWords.Code.WordsQueryXClass"
            + "&classid=58"
            + "&form_token=someToken"
            + "&xredirect=redirectUrl", this.context)).thenReturn(objectRemoveURL3);

        LiveData expectedResult = new LiveData();

        expectedResult.getEntries().addAll(List.of(
            Map.of(
                WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD,
                "XWiki.Foo^NotificationWords.Code.WordsQueryXClass[10]",
                WordsQueryXClassInitializer.QUERY_FIELD, "query1",
                WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, true,
                WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD,
                objectRemoveURL1
            ),
            Map.of(
                WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD,
                "XWiki.Foo^NotificationWords.Code.WordsQueryXClass[22]",
                WordsQueryXClassInitializer.QUERY_FIELD, "query2",
                WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, true,
                WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD,
                objectRemoveURL2
            ),
            Map.of(
                WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD,
                "XWiki.Foo^NotificationWords.Code.WordsQueryXClass[58]",
                WordsQueryXClassInitializer.QUERY_FIELD, "query3",
                WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, true,
                WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD,
                objectRemoveURL3
            )
        ));
        expectedResult.setCount(3);
        assertEquals(expectedResult, this.entryStore.get(query));
        verify(hqlQuery).setLimit(limit);
        verify(hqlQuery).setOffset(offset.intValue());
        verify(hqlQuery).bindValue("className", "NotificationWords.Code.WordsQueryXClass");
        verify(hqlQuery).bindValue("propertyName", "query");
        verify(hqlQuery).bindValue("docFullName", "XWiki.Foo");
        verify(hqlQuery).setWiki("sub");

        // With partial matching filter and sort
        LiveDataQuery.Filter filter = mock(LiveDataQuery.Filter.class);
        when(filter.isMatchAll()).thenReturn(false);
        LiveDataQuery.Constraint filterConstraint = mock(LiveDataQuery.Constraint.class);
        String filterValue = "qu";
        when(filterConstraint.getValue()).thenReturn(filterValue);
        when(filter.getConstraints()).thenReturn(List.of(filterConstraint));
        when(query.getFilters()).thenReturn(List.of(filter));

        LiveDataQuery.SortEntry sortEntry = mock(LiveDataQuery.SortEntry.class);
        when(sortEntry.isDescending()).thenReturn(true);
        when(query.getSort()).thenReturn(List.of(sortEntry));

        String expectedQuery2 = "select obj.number, prop.value from XWikiDocument doc, BaseObject obj, "
            + "StringProperty prop "
            + "where doc.fullName=obj.name and obj.className=:className and prop.id.id=obj.id "
            + "and prop.name=:propertyName and doc.fullName=:docFullName and prop.value like :propValue "
            + "order by prop.value desc";

        Query hqlQuery2 = mock(Query.class, "query2");
        when(this.queryManager.createQuery(expectedQuery2, Query.HQL)).thenReturn(hqlQuery2);
        when(hqlQuery2.setLimit(limit)).thenReturn(hqlQuery2);
        when(hqlQuery2.setOffset(offset.intValue())).thenReturn(hqlQuery2);
        QueryParameter queryParameter = mock(QueryParameter.class);
        when(hqlQuery2.bindValue("propValue")).thenReturn(queryParameter);
        when(queryParameter.anyChars()).thenReturn(queryParameter);
        when(queryParameter.literal(filterValue)).thenReturn(queryParameter);
        when(queryParameter.query()).thenReturn(hqlQuery2);
        when(hqlQuery2.bindValue("className", "NotificationWords.Code.WordsQueryXClass")).thenReturn(hqlQuery2);
        when(hqlQuery2.bindValue("propertyName", "query")).thenReturn(hqlQuery2);
        when(hqlQuery2.bindValue("docFullName", "XWiki.Foo")).thenReturn(hqlQuery2);
        when(hqlQuery2.setWiki("sub")).thenReturn(hqlQuery2);

        when(hqlQuery2.execute()).thenReturn(queryResult);
        assertEquals(expectedResult, this.entryStore.get(query));
        verify(hqlQuery2).setLimit(limit);
        verify(hqlQuery2).setOffset(offset.intValue());
        verify(hqlQuery2).bindValue("className", "NotificationWords.Code.WordsQueryXClass");
        verify(hqlQuery2).bindValue("propertyName", "query");
        verify(hqlQuery2).bindValue("docFullName", "XWiki.Foo");
        verify(hqlQuery2).bindValue("propValue");
        verify(hqlQuery2).setWiki("sub");
        verify(queryParameter, times(2)).anyChars();
        verify(queryParameter).literal(filterValue);
    }

    @Test
    void save() throws XWikiException, LiveDataException
    {
        LiveDataException liveDataException =
            assertThrows(LiveDataException.class, () -> this.entryStore.save(Map.of("key", "value")));
        assertEquals("The entry must contain a reference.", liveDataException.getMessage());

        String entryId = "XWiki.Foo^NotificationWords.Code.WordsQueryXClass[44]";
        DocumentReference docReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(contextualAuthorizationManager.hasAccess(Right.EDIT, docReference)).thenReturn(false);

        XWikiDocument userDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(docReference, this.context)).thenReturn(userDoc);

        liveDataException = assertThrows(LiveDataException.class, () ->
                this.entryStore.save(Map.of(WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD, entryId)));
        assertEquals("Current user cannot edit the document [xwiki:XWiki.Foo].", liveDataException.getMessage());
        verifyNoInteractions(userDoc);

        when(contextualAuthorizationManager.hasAccess(Right.EDIT, docReference)).thenReturn(true);
        liveDataException = assertThrows(LiveDataException.class, () ->
            this.entryStore.save(Map.of(WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD, entryId)));
        assertEquals("Cannot load base object with reference "
            + "[Object xwiki:XWiki.Foo^NotificationWords.Code.WordsQueryXClass[44]]", liveDataException.getMessage());

        BaseObject baseObject = mock(BaseObject.class);
        when(userDoc.getXObject(new DocumentReference(WordsQueryXClassInitializer.XCLASS_REFERENCE,
            new WikiReference("xwiki")), 44)).thenReturn(baseObject);

        String newQuery = "newQuery";
        String saveComment = "saveComment";
        when(this.l10n.getTranslationPlain("wordsNotification.storage.saveQuery")).thenReturn(saveComment);
        assertEquals(Optional.of(entryId), this.entryStore.save(Map.of(
            WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD, entryId,
            WordsQueryXClassInitializer.QUERY_FIELD, newQuery
        )));
        verify(baseObject).setStringValue(WordsQueryXClassInitializer.QUERY_FIELD, newQuery);
        verify(xwiki).saveDocument(userDoc, saveComment, true, context);
    }
}