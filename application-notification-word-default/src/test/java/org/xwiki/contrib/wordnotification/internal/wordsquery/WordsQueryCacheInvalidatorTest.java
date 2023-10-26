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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WordsQueryCacheInvalidator}.
 *
 * @version $Id$
 * @since 1.0
 */
@ComponentTest
class WordsQueryCacheInvalidatorTest
{
    @InjectMockComponents
    private WordsQueryCacheInvalidator queryCacheInvalidator;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    private WordsQueryCache wordsQueryCache;

    @Test
    void onUpdatedEvent()
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        XObjectUpdatedEvent event = new XObjectUpdatedEvent(WordsQueryXClassInitializer.XCLASS_REFERENCE);
        when(sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE)).thenReturn(null);

        this.queryCacheInvalidator.onEvent(event, sourceDoc, null);
        verifyNoInteractions(wordsQueryCache);

        when(sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE))
            .thenReturn(mock(BaseObject.class));

        DocumentReference documentReference = mock(DocumentReference.class);
        when(sourceDoc.getDocumentReference()).thenReturn(documentReference);

        UserReference userReference = mock(UserReference.class);
        when(this.documentReferenceUserReferenceResolver.resolve(documentReference)).thenReturn(userReference);

        this.queryCacheInvalidator.onEvent(event, sourceDoc, null);
        verify(this.wordsQueryCache).invalidateQueriesFrom(userReference);
        verify(this.wordsQueryCache, never()).invalidateUsersWithQueriesFrom(any());
    }

    @Test
    void onAddedEvent()
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        XObjectAddedEvent event = new XObjectAddedEvent(WordsQueryXClassInitializer.XCLASS_REFERENCE);
        when(sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE)).thenReturn(null);

        this.queryCacheInvalidator.onEvent(event, sourceDoc, null);
        verifyNoInteractions(wordsQueryCache);

        when(sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE))
            .thenReturn(mock(BaseObject.class));

        DocumentReference documentReference = mock(DocumentReference.class);
        when(sourceDoc.getDocumentReference()).thenReturn(documentReference);

        WikiReference wikiReference = mock(WikiReference.class);
        when(documentReference.getWikiReference()).thenReturn(wikiReference);

        UserReference userReference = mock(UserReference.class);
        when(this.documentReferenceUserReferenceResolver.resolve(documentReference)).thenReturn(userReference);

        this.queryCacheInvalidator.onEvent(event, sourceDoc, null);
        verify(this.wordsQueryCache).invalidateQueriesFrom(userReference);
        verify(this.wordsQueryCache).invalidateUsersWithQueriesFrom(wikiReference);
    }

    @Test
    void onDeletedEvent()
    {
        XWikiDocument sourceDoc = mock(XWikiDocument.class);
        XObjectDeletedEvent event = new XObjectDeletedEvent(WordsQueryXClassInitializer.XCLASS_REFERENCE);
        when(sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE)).thenReturn(null);

        this.queryCacheInvalidator.onEvent(event, sourceDoc, null);
        verifyNoInteractions(wordsQueryCache);

        when(sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE))
            .thenReturn(mock(BaseObject.class));

        DocumentReference documentReference = mock(DocumentReference.class);
        when(sourceDoc.getDocumentReference()).thenReturn(documentReference);

        WikiReference wikiReference = mock(WikiReference.class);
        when(documentReference.getWikiReference()).thenReturn(wikiReference);

        UserReference userReference = mock(UserReference.class);
        when(this.documentReferenceUserReferenceResolver.resolve(documentReference)).thenReturn(userReference);

        this.queryCacheInvalidator.onEvent(event, sourceDoc, null);
        verify(this.wordsQueryCache).invalidateQueriesFrom(userReference);
        verify(this.wordsQueryCache).invalidateUsersWithQueriesFrom(wikiReference);
    }
}