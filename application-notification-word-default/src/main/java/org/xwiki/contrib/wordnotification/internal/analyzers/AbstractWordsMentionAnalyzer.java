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

import java.util.Map;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.wordnotification.PatternAnalysisHelper;
import org.xwiki.contrib.wordnotification.WordsMentionAnalyzer;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.EntityReference;

/**
 * Abstract implementation of {@link WordsMentionAnalyzer}.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractWordsMentionAnalyzer implements WordsMentionAnalyzer
{
    @Inject
    private PatternAnalysisHelper patternAnalysisHelper;

    @Override
    public PartAnalysisResult analyze(DocumentModelBridge document, WordsQuery wordsQuery)
        throws WordsAnalysisException
    {
        PartAnalysisResult result = new PartAnalysisResult(this.getHint());

        this.getTextToAnalyze(document).forEach((key, value) -> analyzeText(wordsQuery.getQuery(), value, key, result));

        return result;
    }

    private void analyzeText(String query, List<String> textsToAnalyze, EntityReference localization,
        PartAnalysisResult result)
    {
        result.addRegions(this.patternAnalysisHelper.getRegions(query, textsToAnalyze, localization));
    }

    /**
     * @return the hint of the analyzer
     */
    public abstract String getHint();

    /**
     * Retrieve and return the text to actually analyze.
     * The output of this method aims at matching the information needed for {@link WordsMentionLocalization}: each key
     * of the map will be used as {@link WordsMentionLocalization#getEntityReference()} and the index in the provided
     * list will be used as {@link WordsMentionLocalization#getPositionInList()}.
     *
     * @param document the document instance where to perform the analysis
     * @return a map whose keys are the specific reference of each analyzed elements, and whose values are the list of
     * strings to analyze.
     * @throws WordsAnalysisException in case of problem to retrieve the part of the document to analyze
     */
    public abstract Map<EntityReference, List<String>> getTextToAnalyze(DocumentModelBridge document)
        throws WordsAnalysisException;
}
