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

import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.internal.wordsquery.WordsQueryXClassInitializer;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryManager;
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
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
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
        when(userDoc.getURL("objectremove",
            "classname=NotificationWords.Code.WordsQueryXClass"
                + "&classid=18"
                + "&xredirect=redirectUrl"
                + "&form_token=someToken", this.context)).thenReturn(objectRemoveURL);

        Map<String, Object> expectedResult = Map.of(
            WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD, entryId,
            WordsQueryXClassInitializer.QUERY_FIELD, query,
            WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, true,
            WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD, objectRemoveURL
        );
        assertEquals(Optional.of(expectedResult), this.entryStore.get(entryId));
    }
}