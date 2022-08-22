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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.AnalyzedElementReference;
import org.xwiki.contrib.wordnotification.MentionedWordsEvent;
import org.xwiki.contrib.wordnotification.WordsAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Named(MentionedWordsEventListener.NAME)
@Singleton
public class MentionedWordsEventListener extends AbstractLocalEventListener
{
    static final String NAME = "MentionedWordsEventListener";

    @Inject
    private Logger logger;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    public MentionedWordsEventListener()
    {
        super(NAME, Collections.singletonList(new MentionedWordsEvent()));
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        if (data instanceof WordsAnalysisResult) {
            this.notifyAbout((WordsAnalysisResult) data, null);
        } else if (data instanceof Pair) {
            Pair<WordsAnalysisResult, WordsAnalysisResult> results =
                (Pair<WordsAnalysisResult, WordsAnalysisResult>) data;
            this.notifyAbout(results.getRight(), results.getLeft());
        } else {
            this.logger.error("Cannot process the following data class for mentioned words event: [{}]",
                data.getClass());
        }
    }

    public void notifyAbout(WordsAnalysisResult currentResult, WordsAnalysisResult previousResult)
    {
        int nbOccurences;
        boolean isNew = false;
        if (previousResult == null) {
            nbOccurences = currentResult.getOccurences();
            isNew = true;
        } else {
            nbOccurences = currentResult.getOccurences() - previousResult.getOccurences();
        }

        AnalyzedElementReference reference = currentResult.getReference();
        try {
            XWikiDocument document = this.documentRevisionProvider.getRevision(reference.getDocumentReference(),
                reference.getDocumentVersion());

            WordsQuery query = currentResult.getQuery();
            String userTarget = this.userReferenceSerializer.serialize(query.getUserReference());
            MentionedWordsRecordableEvent event =
                new MentionedWordsRecordableEvent(Collections.singleton(userTarget), nbOccurences, query.getQuery());
            event.setNew(isNew);

            this.observationManager
                .notify(event, "org.xwiki.contrib.wordnotification:application-notification-word-default", document);
        } catch (XWikiException e) {
            throw new RuntimeException(e);
        }
    }
}
