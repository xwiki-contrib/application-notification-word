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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.wordnotification.ChangeAnalyzer;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsQuery;

/**
 * Abstract implementation of {@link ChangeAnalyzer}.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractChangeAnalyzer implements ChangeAnalyzer
{
    @Override
    public PartAnalysisResult analyze(DocumentModelBridge document, WordsQuery wordsQuery)
        throws WordsAnalysisException
    {
        PartAnalysisResult result = new PartAnalysisResult(this.getHint(), document.getDocumentReference());
        String query = wordsQuery.getQuery();
        String textToAnalyze = this.getTextToAnalyze(document);

        // Note that for now it seems better for perf to transform the content and the query to perform
        // case insensitive matching instead of using the case insensitive flag as the javadoc indicates that
        // it might involve some performance penalty.
        Matcher matcher = Pattern
            // FIXME: we need to escape some characters to avoid problem with the regex
            .compile(String.format("(%s)", query.toLowerCase()))
            .matcher(textToAnalyze.toLowerCase());

        while (matcher.find()) {
            result.addRegion(Pair.of(matcher.start(), matcher.end()));
        }

        return result;
    }

    /**
     * @return the hint of the analyzer
     */
    public abstract String getHint();

    /**
     * Retrieve and return the text to actually analyze.
     *
     * @param document the document instance where to perform the analysis
     * @return the part of the document that needs to be analyzed
     * @throws WordsAnalysisException in case of problem to retrieve the part of the document to analyze
     */
    public abstract String getTextToAnalyze(DocumentModelBridge document) throws WordsAnalysisException;
}
