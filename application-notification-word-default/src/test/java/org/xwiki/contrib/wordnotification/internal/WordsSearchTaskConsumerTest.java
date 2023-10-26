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
package org.xwiki.contrib.wordnotification.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.wordnotification.MentionedWordsEvent;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.UsersWordsQueriesManager;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResults;
import org.xwiki.contrib.wordnotification.WordsMentionAnalyzer;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.contrib.wordnotification.internal.storage.AnalysisResultStorageManager;
import org.xwiki.index.IndexException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WordsSearchTaskConsumer}.
 *
 * @version $Id$
 * @since 1.0
 */
@ComponentTest
class WordsSearchTaskConsumerTest
{
    @InjectMockComponents
    private WordsSearchTaskConsumer searchTaskConsumer;

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @MockComponent
    private UsersWordsQueriesManager usersWordsQueriesManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @MockComponent
    private AnalysisResultStorageManager storageManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @Test
    void consume()
        throws WordsAnalysisException, XWikiException, ComponentLookupException, IndexException
    {
        DocumentReference documentReference = new DocumentReference("mywiki", "Foo", "Document");
        String version = "3.43";
        WikiReference wikiReference = new WikiReference("mywiki");
        DocumentVersionReference documentVersionReference = new DocumentVersionReference(documentReference, version);

        UserReference user1 = mock(UserReference.class, "user1");
        UserReference user2 = mock(UserReference.class, "user2");
        UserReference user3 = mock(UserReference.class, "user3");

        when(this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(wikiReference))
            .thenReturn(Set.of(user1, user2, user3));

        DocumentReference userDoc1 = mock(DocumentReference.class, "userDoc1");
        DocumentReference userDoc2 = mock(DocumentReference.class, "userDoc2");
        DocumentReference userDoc3 = mock(DocumentReference.class, "userDoc3");

        when(this.documentReferenceUserReferenceSerializer.serialize(user1)).thenReturn(userDoc1);
        when(this.documentReferenceUserReferenceSerializer.serialize(user2)).thenReturn(userDoc2);
        when(this.documentReferenceUserReferenceSerializer.serialize(user3)).thenReturn(userDoc3);

        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc1, documentReference)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc2, documentReference)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.VIEW, userDoc3, documentReference)).thenReturn(true);

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentReference, version)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getVersion()).thenReturn(version);

        WordsMentionAnalyzer analyzer1 = mock(WordsMentionAnalyzer.class, "analyzer1");
        WordsMentionAnalyzer analyzer2 = mock(WordsMentionAnalyzer.class, "analyzer2");
        WordsMentionAnalyzer analyzer3 = mock(WordsMentionAnalyzer.class, "analyzer3");
        when(this.contextComponentManager.getInstanceList(WordsMentionAnalyzer.class))
            .thenReturn(List.of(analyzer1, analyzer2, analyzer3));

        WordsQuery wordsQuery1User1 = mock(WordsQuery.class, "wordsQuery1User1");
        WordsQuery wordsQuery2User1 = mock(WordsQuery.class, "wordsQuery2User1");

        // This one is "shared" between user1 and 3
        WordsQuery wordsQuery3 = mock(WordsQuery.class, "wordsQuery3");
        when(this.usersWordsQueriesManager.getQueries(user1))
            .thenReturn(Set.of(wordsQuery1User1, wordsQuery2User1, wordsQuery3));

        WordsQuery wordsQuery1User3 = mock(WordsQuery.class, "wordsQuery1User3");
        WordsQuery wordsQuery2User3 = mock(WordsQuery.class, "wordsQuery2User3");
        when(this.usersWordsQueriesManager.getQueries(user3))
            .thenReturn(Set.of(wordsQuery1User3, wordsQuery2User3, wordsQuery3));

        when(this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery1User1))
            .thenReturn(Optional.empty());
        when(this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery2User1))
            .thenReturn(Optional.empty());
        when(this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery1User3))
            .thenReturn(Optional.empty());
        when(this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery2User3))
            .thenReturn(Optional.empty());

        WordsAnalysisResults wordsAnalysisResults3 = mock(WordsAnalysisResults.class);
        when(this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery3))
            .thenReturn(Optional.of(wordsAnalysisResults3));

        PartAnalysisResult wordsQuery1User1Analyser1 = mock(PartAnalysisResult.class, "wordsQuery1User1Analyser1");
        PartAnalysisResult wordsQuery1User1Analyser2 = mock(PartAnalysisResult.class, "wordsQuery1User1Analyser2");
        PartAnalysisResult wordsQuery1User1Analyser3 = mock(PartAnalysisResult.class, "wordsQuery1User1Analyser3");

        when(analyzer1.analyze(document, wordsQuery1User1)).thenReturn(wordsQuery1User1Analyser1);
        when(analyzer2.analyze(document, wordsQuery1User1)).thenReturn(wordsQuery1User1Analyser2);
        when(analyzer3.analyze(document, wordsQuery1User1)).thenReturn(wordsQuery1User1Analyser3);

        PartAnalysisResult wordsQuery2User1Analyser1 = mock(PartAnalysisResult.class, "wordsQuery2User1Analyser1");
        PartAnalysisResult wordsQuery2User1Analyser2 = mock(PartAnalysisResult.class, "wordsQuery2User1Analyser2");
        PartAnalysisResult wordsQuery2User1Analyser3 = mock(PartAnalysisResult.class, "wordsQuery2User1Analyser3");

        when(analyzer1.analyze(document, wordsQuery2User1)).thenReturn(wordsQuery2User1Analyser1);
        when(analyzer2.analyze(document, wordsQuery2User1)).thenReturn(wordsQuery2User1Analyser2);
        when(analyzer3.analyze(document, wordsQuery2User1)).thenReturn(wordsQuery2User1Analyser3);

        PartAnalysisResult wordsQuery1User3Analyser1 = mock(PartAnalysisResult.class, "wordsQuery1User3Analyser1");
        PartAnalysisResult wordsQuery1User3Analyser2 = mock(PartAnalysisResult.class, "wordsQuery1User3Analyser2");
        PartAnalysisResult wordsQuery1User3Analyser3 = mock(PartAnalysisResult.class, "wordsQuery1User3Analyser3");

        when(analyzer1.analyze(document, wordsQuery1User3)).thenReturn(wordsQuery1User3Analyser1);
        when(analyzer2.analyze(document, wordsQuery1User3)).thenReturn(wordsQuery1User3Analyser2);
        when(analyzer3.analyze(document, wordsQuery1User3)).thenReturn(wordsQuery1User3Analyser3);

        PartAnalysisResult wordsQuery2User3Analyser1 = mock(PartAnalysisResult.class, "wordsQuery2User3Analyser1");
        PartAnalysisResult wordsQuery2User3Analyser2 = mock(PartAnalysisResult.class, "wordsQuery2User3Analyser2");
        PartAnalysisResult wordsQuery2User3Analyser3 = mock(PartAnalysisResult.class, "wordsQuery2User3Analyser3");

        when(analyzer1.analyze(document, wordsQuery2User3)).thenReturn(wordsQuery2User3Analyser1);
        when(analyzer2.analyze(document, wordsQuery2User3)).thenReturn(wordsQuery2User3Analyser2);
        when(analyzer3.analyze(document, wordsQuery2User3)).thenReturn(wordsQuery2User3Analyser3);

        doAnswer(invocationOnMock -> {
            WordsAnalysisResults result = invocationOnMock.getArgument(0);
            assertEquals(documentVersionReference, result.getReference());
            assertNotNull(result.getDate());

            WordsQuery query = result.getQuery();

            if (query == wordsQuery1User1) {
                assertEquals(List.of(wordsQuery1User1Analyser1, wordsQuery1User1Analyser2, wordsQuery1User1Analyser3),
                    result.getResults());
            } else if (query == wordsQuery1User3) {
                assertEquals(List.of(wordsQuery1User3Analyser1, wordsQuery1User3Analyser2, wordsQuery1User3Analyser3),
                    result.getResults());
            } else if (query == wordsQuery2User1) {
                assertEquals(List.of(wordsQuery2User1Analyser1, wordsQuery2User1Analyser2, wordsQuery2User1Analyser3),
                    result.getResults());
            } else if (query == wordsQuery2User3) {
                assertEquals(List.of(wordsQuery2User3Analyser1, wordsQuery2User3Analyser2, wordsQuery2User3Analyser3),
                    result.getResults());
            } else {
                fail("Query is not expected: " + query);
            }

            return null;
        }).when(this.storageManager).saveAnalysisResults(any());

        this.searchTaskConsumer.consume(documentReference, version);
        verify(this.storageManager, times(4)).saveAnalysisResults(any());
        verify(this.storageManager, times(6)).loadAnalysisResults(eq(documentVersionReference), any());

        // right now result returns 0 occurrences so nothing should happen.
        verifyNoInteractions(this.observationManager);

        // We have now one occurence of wordsQuery1User1
        when(wordsQuery1User1Analyser3.getOccurrences()).thenReturn(1L);

        doAnswer(invocationOnMock -> {
            Event event = invocationOnMock.getArgument(0);
            assertInstanceOf(MentionedWordsEvent.class, event);
            WordsAnalysisResults result = invocationOnMock.getArgument(2);
            assertEquals(List.of(wordsQuery1User1Analyser1, wordsQuery1User1Analyser2, wordsQuery1User1Analyser3),
                result.getResults());
            assertEquals(wordsQuery1User1, result.getQuery());
            return null;
        }).when(this.observationManager).notify(any(), any(), any(WordsAnalysisResults.class));

        this.searchTaskConsumer.consume(documentReference, version);
        verify(this.observationManager).notify(
            any(MentionedWordsEvent.class),
            eq(documentVersionReference),
            any());

        // Adding more occurrences doesn't change the result
        when(wordsQuery1User1Analyser1.getOccurrences()).thenReturn(5L);

        this.searchTaskConsumer.consume(documentReference, version);
        verify(this.observationManager, times(2)).notify(
            any(MentionedWordsEvent.class),
            eq(documentVersionReference),
            any());

        String previousVersion = "2.1";
        when(document.getPreviousVersion()).thenReturn(previousVersion);

        // Right now the previous doc cannot be loaded, and there's no previous results, so we just keep acting as
        // if there was no previous version
        this.searchTaskConsumer.consume(documentReference, version);
        verify(this.observationManager, times(3)).notify(
            any(MentionedWordsEvent.class),
            eq(documentVersionReference),
            any());

        DocumentVersionReference documentPreviousVersionReference =
            new DocumentVersionReference(documentReference, previousVersion);

        WordsAnalysisResults previousResultQuery1User1 = mock(WordsAnalysisResults.class, "previousQuery1User1");
        WordsAnalysisResults previousResultQuery2User1 = mock(WordsAnalysisResults.class, "previousQuery2User1");

        when(this.storageManager.loadAnalysisResults(documentPreviousVersionReference, wordsQuery1User1))
            .thenReturn(Optional.of(previousResultQuery1User1));
        when(this.storageManager.loadAnalysisResults(documentPreviousVersionReference, wordsQuery2User1))
            .thenReturn(Optional.of(previousResultQuery2User1));

        WordsAnalysisResults previousResultQuery1User3 = mock(WordsAnalysisResults.class, "previousQuery1User3");
        WordsAnalysisResults previousResultQuery2User3 = mock(WordsAnalysisResults.class, "previousQuery2User3");

        when(this.storageManager.loadAnalysisResults(documentPreviousVersionReference, wordsQuery1User3))
            .thenReturn(Optional.of(previousResultQuery1User3));
        when(this.storageManager.loadAnalysisResults(documentPreviousVersionReference, wordsQuery2User3))
            .thenReturn(Optional.of(previousResultQuery2User3));

        // Actual result is of 6 occurrences, so it's now less than previous result: we won't trigger a notif anymore.
        when(previousResultQuery1User1.getOccurrences()).thenReturn(8L);
        // This is gonna be ignored as the actual result for that query is 0.
        when(previousResultQuery2User1.getOccurrences()).thenReturn(8L);

        this.searchTaskConsumer.consume(documentReference, version);
        verify(this.observationManager, times(3)).notify(
            any(MentionedWordsEvent.class),
            eq(documentVersionReference),
            any());

        when(previousResultQuery1User1.getOccurrences()).thenReturn(4L);
        doAnswer(invocationOnMock -> {
            Event event = invocationOnMock.getArgument(0);
            assertInstanceOf(MentionedWordsEvent.class, event);
            Pair<WordsAnalysisResults, WordsAnalysisResults> data = invocationOnMock.getArgument(2);
            WordsAnalysisResults previousResult = data.getLeft();
            WordsAnalysisResults newResult = data.getRight();
            assertEquals(List.of(wordsQuery1User1Analyser1, wordsQuery1User1Analyser2, wordsQuery1User1Analyser3),
                newResult.getResults());
            assertEquals(wordsQuery1User1, newResult.getQuery());
            assertSame(previousResultQuery1User1, previousResult);
            return null;
        }).when(this.observationManager).notify(any(), any(), any(Pair.class));

        this.searchTaskConsumer.consume(documentReference, version);
        verify(this.observationManager, times(3)).notify(
            any(MentionedWordsEvent.class),
            eq(documentVersionReference),
            any(WordsAnalysisResults.class));

        verify(this.observationManager).notify(
            any(MentionedWordsEvent.class),
            eq(documentVersionReference),
            any(Pair.class));


    }
}