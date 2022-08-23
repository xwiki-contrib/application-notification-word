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
package org.xwiki.contrib.wordnotification.internal.analyzers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.AnalyzedElementReference;
import org.xwiki.contrib.wordnotification.ChangeAnalyzer;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Singleton
@Named(ContentChangeAnalyzer.HINT)
public class ContentChangeAnalyzer implements ChangeAnalyzer
{
    static final String HINT = "content";

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Override
    public Set<WordsAnalysisResult> analyze(DocumentReference documentReference, String version,
        Set<WordsQuery> wordsQueries) throws WordsAnalysisException
    {
        try {
            XWikiDocument document = this.documentRevisionProvider.getRevision(documentReference, version);
            Set<WordsAnalysisResult> results = new HashSet<>();

            AnalyzedElementReference analyzedElementReference =
                new AnalyzedElementReference(documentReference, version, document.getPreviousVersion());
            for (WordsQuery wordsQuery : wordsQueries) {
                results.add(this.analyze(document, analyzedElementReference, wordsQuery));
            }

            return results;
        } catch (XWikiException e) {
            throw new WordsAnalysisException(
                String.format("Error when loading the document [%s] with version"
                    + " [%s] to analyze its content", documentReference, version), e);
        }
    }

    @Override
    public WordsAnalysisResult analyze(DocumentReference documentReference, String version, WordsQuery wordsQuery)
        throws WordsAnalysisException
    {
        return this.analyze(documentReference, version, Collections.singleton(wordsQuery)).iterator().next();
    }

    private WordsAnalysisResult analyze(XWikiDocument document, AnalyzedElementReference reference,
        WordsQuery wordsQuery)
    {
        WordsAnalysisResult result = new WordsAnalysisResult(reference, wordsQuery, HINT);

        String query = wordsQuery.getQuery();
        Matcher matcher = Pattern.compile(query).matcher(document.getContent());
        Set<Pair<Integer, Integer>> regions = new HashSet<>();
        while (matcher.find()) {
            regions.add(Pair.of(matcher.start(), matcher.end()));
        }
        result.setRegions(regions);

        return result;
    }
}
