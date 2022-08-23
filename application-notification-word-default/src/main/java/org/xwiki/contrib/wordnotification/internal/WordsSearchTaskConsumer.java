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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.wordnotification.AnalyzedElementReference;
import org.xwiki.contrib.wordnotification.ChangeAnalyzer;
import org.xwiki.contrib.wordnotification.MentionedWordsEvent;
import org.xwiki.contrib.wordnotification.UsersWordsQueriesManager;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.contrib.wordnotification.internal.storage.AnalysisResultStorageManager;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

@Component
@Named(WordsSearchTaskConsumer.WORDS_SEARCH_TASK_HINT)
@Singleton
public class WordsSearchTaskConsumer implements TaskConsumer
{
    static final String WORDS_SEARCH_TASK_HINT = "WordsSearch";

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @Inject
    private UsersWordsQueriesManager usersWordsQueriesManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @Inject
    private AnalysisResultStorageManager storageManager;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        // TODO: this should be refactored.
        Set<WordsAnalysisResult> analysisResults = this.performAnalysis(documentReference, version);

        if (!analysisResults.isEmpty()) {
            try {
                this.storageManager.saveAnalysisResults(analysisResults);
            } catch (WordsAnalysisException e) {
                throw new IndexException("Error while trying to save words analysis result", e);
            }

            AnalyzedElementReference reference = analysisResults.iterator().next().getReference();
            if (reference.isFirstVersion()) {
                this.handleResultsForNewDocument(analysisResults);
            } else {
                try {
                    this.handleResultsForUpdatedDocument(analysisResults);
                } catch (WordsAnalysisException e) {
                    throw new IndexException("Error while trying to handle words analysis result", e);
                }
            }
        }
    }

    private Set<WordsAnalysisResult> performAnalysis(DocumentReference documentReference, String version)
        throws IndexException
    {
        Set<WordsAnalysisResult> results = new LinkedHashSet<>();
        Set<UserReference> userList =
            null;
        try {
            userList =
                this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(documentReference.getWikiReference());
        } catch (WordsAnalysisException e) {
            throw new IndexException("Error when trying to get list of users with words queries", e);
        }
        userList = this.filterUsersAuthorizedToSee(documentReference, userList);
        if (!userList.isEmpty()) {
            try {
                List<ChangeAnalyzer> analyzers =
                    this.contextComponentManager.get().getInstanceList(ChangeAnalyzer.class);

                for (UserReference userReference : userList) {
                    Set<WordsQuery> queries = this.usersWordsQueriesManager.getQueries(userReference);

                    for (ChangeAnalyzer analyzer : analyzers) {
                        results.addAll(analyzer.analyze(documentReference, version, queries));
                    }
                }
            } catch (ComponentLookupException e) {
                throw new IndexException("Error when trying to load the list of analyzers", e);
            } catch (WordsAnalysisException e) {
                throw new IndexException("Error when trying to perform word analysis", e);
            }
        }
        return results;
    }

    private WordsAnalysisResult performAnalysis(DocumentReference documentReference, String version,
        String analyzerHint, WordsQuery wordsQuery) throws WordsAnalysisException
    {
        try {
            ChangeAnalyzer analyzer =
                this.contextComponentManager.get().getInstance(ChangeAnalyzer.class, analyzerHint);
            return analyzer.analyze(documentReference, version, wordsQuery);
        } catch (ComponentLookupException e) {
            throw new WordsAnalysisException(
                String.format("Cannot find the analyzer with hint [%s]", analyzerHint), e);
        }
    }

    private void handleResultsForNewDocument(Set<WordsAnalysisResult> analysisResults)
    {
        for (WordsAnalysisResult analysisResult : analysisResults) {
            this.observationManager.notify(new MentionedWordsEvent(), analysisResult.getReference(), analysisResult);
        }
    }

    private void handleResultsForUpdatedDocument(Set<WordsAnalysisResult> analysisResults) throws WordsAnalysisException
    {
        for (WordsAnalysisResult analysisResult : analysisResults) {
            String query = analysisResult.getQuery().getQuery();
            AnalyzedElementReference reference = analysisResult.getReference();
            DocumentReference documentReference = reference.getDocumentReference();
            String previousVersion = reference.getPreviousVersion();
            String analyzerHint = analysisResult.getAnalyzerHint();
            Optional<WordsAnalysisResult> wordsAnalysisResult =
                this.storageManager.loadAnalysisResults(documentReference, previousVersion, analyzerHint, query);

            WordsAnalysisResult previousResult;
            if (wordsAnalysisResult.isPresent()) {
                previousResult = wordsAnalysisResult.get();
            } else {
                previousResult =
                    this.performAnalysis(documentReference, previousVersion, analyzerHint, analysisResult.getQuery());
                this.storageManager.saveAnalysisResults(Collections.singleton(previousResult));
            }

            if (previousResult.getOccurences() < analysisResult.getOccurences()) {
                this.observationManager.notify(new MentionedWordsEvent(), analysisResult.getReference(),
                    Pair.of(previousResult, analysisResult));
            }
        }
    }

    private Set<UserReference> filterUsersAuthorizedToSee(DocumentReference documentReference,
        Set<UserReference> userList)
    {
        return userList.stream().filter(userReference -> {
            DocumentReference userDoc = this.documentReferenceUserReferenceSerializer.serialize(userReference);
            return this.authorizationManager.hasAccess(Right.VIEW, userDoc, documentReference);
        }).collect(Collectors.toSet());
    }
}
