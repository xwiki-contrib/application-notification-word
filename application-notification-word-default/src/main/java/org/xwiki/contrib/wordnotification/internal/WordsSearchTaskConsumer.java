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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.wordnotification.WordsMentionAnalyzer;
import org.xwiki.contrib.wordnotification.MentionedWordsEvent;
import org.xwiki.contrib.wordnotification.UsersWordsQueriesManager;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResults;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.contrib.wordnotification.internal.storage.AnalysisResultStorageManager;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Dedicated task consumer for performing document analysis for finding words.
 *
 * @version $Id$
 * @since 1.0
 */
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

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Logger logger;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        Set<UserReference> userList;
        try {
            userList =
                this.usersWordsQueriesManager.getUserReferenceWithWordsQuery(documentReference.getWikiReference());
        } catch (WordsAnalysisException e) {
            throw new IndexException("Error when trying to get list of users with words queries", e);
        }

        // We only perform analysis if the user is authorized to see the doc.
        userList = this.filterUsersAuthorizedToSee(documentReference, userList);

        if (!userList.isEmpty()) {
            try {
                XWikiDocument document = this.documentRevisionProvider.getRevision(documentReference, version);
                List<WordsMentionAnalyzer> analyzers =
                    this.contextComponentManager.get().getInstanceList(WordsMentionAnalyzer.class);

                for (UserReference userReference : userList) {
                    this.performAnalysis(document, analyzers, userReference);
                }
            } catch (ComponentLookupException e) {
                throw new IndexException("Error when trying to load the list of analyzers", e);
            } catch (XWikiException e) {
                throw new IndexException(
                    String.format("Error when loading document [%s] on version [%s]", documentReference, version), e);
            }
        }
    }

    private void performAnalysis(XWikiDocument document, List<WordsMentionAnalyzer> analyzers,
        UserReference userReference) throws IndexException
    {
        DocumentReference documentReference = document.getDocumentReference();
        Set<WordsQuery> queries = null;
        try {
            queries = this.usersWordsQueriesManager.getQueries(userReference);
        } catch (WordsAnalysisException e) {
            throw new IndexException(String.format(
                "Error when trying to load the list of queries for user [%s]", userReference), e);
        }

        for (WordsQuery query : queries) {
            WordsAnalysisResults wordsAnalysisResults = this.performAnalysis(document, analyzers, query);
            WordsAnalysisResults previousResult = null;

            if (!document.isNew() && document.getPreviousVersion() != null) {
                previousResult = getPreviousResult(document, analyzers, query, documentReference);
            }
            if (previousResult == null) {
                this.observationManager.notify(new MentionedWordsEvent(), wordsAnalysisResults.getReference(),
                    wordsAnalysisResults);
            } else if (wordsAnalysisResults.getOccurrences() > previousResult.getOccurrences()) {
                this.observationManager.notify(new MentionedWordsEvent(), wordsAnalysisResults.getReference(),
                    Pair.of(previousResult, wordsAnalysisResults));
            }
        }
    }

    private WordsAnalysisResults getPreviousResult(XWikiDocument document, List<WordsMentionAnalyzer> analyzers,
        WordsQuery query, DocumentReference documentReference)
        throws IndexException
    {
        WordsAnalysisResults previousResult = null;
        String previousVersion = document.getPreviousVersion();
        Optional<WordsAnalysisResults> previousResultOpt = Optional.empty();
        try {
            previousResultOpt =
                this.storageManager.loadAnalysisResults(
                    new DocumentVersionReference(documentReference, previousVersion), query);
        } catch (WordsAnalysisException e) {
            // We don't throw an exception here since we're always able to compute back previous result.
            this.logger.error("Error when trying to load previous analysis result for document [{}] on "
                    + "version [{}] with query [{}]. Exception: [{}]", documentReference, previousVersion, query,
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug("Full error was: ", e);
        }

        if (previousResultOpt.isEmpty()) {
            try {
                XWikiDocument previousDoc =
                    this.documentRevisionProvider.getRevision(documentReference, previousVersion);
                if (previousDoc != null) {
                    previousResult = this.performAnalysis(previousDoc, analyzers, query);
                }
            } catch (XWikiException e) {
                throw new IndexException(
                    String.format("Cannot load document [%s] with revision [%s] for comparing results",
                        documentReference, previousVersion), e);
            }
        } else {
            // FIXME: We should probably check if the previous result had the exact same hints
            // and perform some more analysis if some hints were missing.
            previousResult = previousResultOpt.get();
        }
        return previousResult;
    }

    private WordsAnalysisResults performAnalysis(XWikiDocument document, List<WordsMentionAnalyzer> analyzers,
        WordsQuery query)
    {
        DocumentReference documentReference = document.getDocumentReference();
        String version = document.getVersion();
        DocumentVersionReference documentVersionReference =
            new DocumentVersionReference(documentReference, version);

        // We try first to load the results as they might have been computed already for another user.
        WordsAnalysisResults wordsAnalysisResults = null;
        try {
            Optional<WordsAnalysisResults> wordsAnalysisResultsOpt =
                this.storageManager.loadAnalysisResults(documentVersionReference, query);
            if (wordsAnalysisResultsOpt.isPresent()) {
                wordsAnalysisResults = wordsAnalysisResultsOpt.get();
            }
        } catch (WordsAnalysisException e) {
            this.logger.error("Error while trying to load analysis results for document [{}] and query [{}]",
                documentVersionReference, query, e);
        }

        if (wordsAnalysisResults == null) {
            wordsAnalysisResults =
                new WordsAnalysisResults(documentVersionReference, query);
            for (WordsMentionAnalyzer analyzer : analyzers) {
                try {
                    wordsAnalysisResults.addResult(analyzer.analyze(document, query));
                } catch (WordsAnalysisException e) {
                    // we avoid throwing an IndexException here since other analyzers could work.
                    this.logger.error("Error during analysis performed by [{}] on document [{}] on "
                            + "version [{}]. Root cause is: [{}]",
                        analyzer.getClass(),
                        documentReference,
                        version,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
            try {
                this.storageManager.saveAnalysisResults(wordsAnalysisResults);
            } catch (WordsAnalysisException e) {
                // We don't throw an exception since the persistency is not strictly needed.
                this.logger.error("Error while persisting the results of analysis of [{}] with query [{}]. "
                    + "Root cause: [{}]", documentReference, query, ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return wordsAnalysisResults;
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
