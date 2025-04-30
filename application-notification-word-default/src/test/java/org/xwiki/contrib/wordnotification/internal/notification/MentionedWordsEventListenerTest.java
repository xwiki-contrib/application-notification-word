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
package org.xwiki.contrib.wordnotification.internal.notification;

import java.util.Collections;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.RemovedWordsEvent;
import org.xwiki.contrib.wordnotification.WordsAnalysisResults;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MentionedWordsEventListener}.
 *
 * @version $Id$
 */
@ComponentTest
class MentionedWordsEventListenerTest
{
    @InjectMockComponents
    private MentionedWordsEventListener mentionedWordsEventListener;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @Test
    void processLocalEventWithSingleWordAnalysisResult() throws XWikiException
    {
        WordsAnalysisResults currentResult = mock(WordsAnalysisResults.class);
        long occurences = 6;
        when(currentResult.getOccurrences()).thenReturn(occurences);

        DocumentReference docReference = new DocumentReference("xwiki", "Foo", "Bar");
        String version = "4.2";
        DocumentVersionReference documentVersionReference = new DocumentVersionReference(docReference, version);
        when(currentResult.getReference()).thenReturn(documentVersionReference);

        WordsQuery wordsQuery = mock(WordsQuery.class);
        when(currentResult.getQuery()).thenReturn(wordsQuery);

        UserReference queryUser = mock(UserReference.class, "queryUser");
        when(wordsQuery.getUserReference()).thenReturn(queryUser);

        String query = "my query";
        when(wordsQuery.getQuery()).thenReturn(query);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentVersionReference, version)).thenReturn(document);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(document.getAuthors()).thenReturn(documentAuthors);

        UserReference contentAuthor = mock(UserReference.class);
        when(documentAuthors.getOriginalMetadataAuthor()).thenReturn(contentAuthor);

        String queryUserStr = "queryUser";
        when(this.userReferenceSerializer.serialize(queryUser)).thenReturn(queryUserStr);

        MentionedWordsRecordableEvent expectedEvent =
            new MentionedWordsRecordableEvent(Collections.singleton(queryUserStr), occurences, 0, query, contentAuthor);
        expectedEvent.setNew(true);

        this.mentionedWordsEventListener.processLocalEvent(null, null, currentResult);
        verify(this.observationManager).notify(expectedEvent, MentionedWordsEventListener.NOTIFIER_SOURCE, document);
    }

    @Test
    void processLocalEventWithTwoWordAnalysisResult() throws XWikiException
    {
        WordsAnalysisResults currentResult = mock(WordsAnalysisResults.class, "currentResult");
        WordsAnalysisResults previousResult = mock(WordsAnalysisResults.class, "previousResult");
        long occurences = 6;
        long previousOccurrences = 2;
        when(currentResult.getOccurrences()).thenReturn(occurences);
        when(previousResult.getOccurrences()).thenReturn(previousOccurrences);

        DocumentReference docReference = new DocumentReference("xwiki", "Foo", "Bar");
        String version = "4.2";
        DocumentVersionReference documentVersionReference = new DocumentVersionReference(docReference, version);
        when(currentResult.getReference()).thenReturn(documentVersionReference);

        WordsQuery wordsQuery = mock(WordsQuery.class);
        when(currentResult.getQuery()).thenReturn(wordsQuery);

        UserReference queryUser = mock(UserReference.class, "queryUser");
        when(wordsQuery.getUserReference()).thenReturn(queryUser);

        String query = "my query";
        when(wordsQuery.getQuery()).thenReturn(query);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentVersionReference, version)).thenReturn(document);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(document.getAuthors()).thenReturn(documentAuthors);

        UserReference contentAuthor = mock(UserReference.class);
        when(documentAuthors.getOriginalMetadataAuthor()).thenReturn(contentAuthor);

        String queryUserStr = "queryUser";
        when(this.userReferenceSerializer.serialize(queryUser)).thenReturn(queryUserStr);

        MentionedWordsRecordableEvent expectedEvent =
            new MentionedWordsRecordableEvent(Collections.singleton(queryUserStr), occurences, previousOccurrences,
                query, contentAuthor);

        this.mentionedWordsEventListener.processLocalEvent(null, null, Pair.of(previousResult, currentResult));
        verify(this.observationManager).notify(expectedEvent, MentionedWordsEventListener.NOTIFIER_SOURCE, document);
    }

    @Test
    void processLocalEventWithRemovedWordAnalysisResult() throws XWikiException
    {
        WordsAnalysisResults currentResult = mock(WordsAnalysisResults.class, "currentResult");
        WordsAnalysisResults previousResult = mock(WordsAnalysisResults.class, "previousResult");
        long occurences = 6;
        long previousOccurrences = 12;
        when(currentResult.getOccurrences()).thenReturn(occurences);
        when(previousResult.getOccurrences()).thenReturn(previousOccurrences);

        DocumentReference docReference = new DocumentReference("xwiki", "Foo", "Bar");
        String version = "4.2";
        DocumentVersionReference documentVersionReference = new DocumentVersionReference(docReference, version);
        when(currentResult.getReference()).thenReturn(documentVersionReference);

        WordsQuery wordsQuery = mock(WordsQuery.class);
        when(currentResult.getQuery()).thenReturn(wordsQuery);

        UserReference queryUser = mock(UserReference.class, "queryUser");
        when(wordsQuery.getUserReference()).thenReturn(queryUser);

        String query = "my query";
        when(wordsQuery.getQuery()).thenReturn(query);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentVersionReference, version)).thenReturn(document);
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(document.getAuthors()).thenReturn(documentAuthors);

        UserReference contentAuthor = mock(UserReference.class);
        when(documentAuthors.getOriginalMetadataAuthor()).thenReturn(contentAuthor);

        String queryUserStr = "queryUser";
        when(this.userReferenceSerializer.serialize(queryUser)).thenReturn(queryUserStr);

        RemovedWordsRecordableEvent expectedEvent =
            new RemovedWordsRecordableEvent(Collections.singleton(queryUserStr), occurences, previousOccurrences,
                query, contentAuthor);

        this.mentionedWordsEventListener.processLocalEvent(
            new RemovedWordsEvent(), null, Pair.of(previousResult, currentResult));
        verify(this.observationManager).notify(expectedEvent, MentionedWordsEventListener.NOTIFIER_SOURCE, document);
    }
}