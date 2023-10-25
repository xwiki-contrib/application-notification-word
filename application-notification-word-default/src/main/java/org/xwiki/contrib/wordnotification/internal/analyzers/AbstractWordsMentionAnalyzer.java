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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
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
    private static final String SPACE_PREFIX_GROUP_NAME = "querySpacePrefix";
    private static final String SPACE_SUFFIX_GROUP_NAME = "querySpaceSuffix";
    private static final String ALONE_GROUP_NAME = "querySpaceAlone";

    @Override
    public PartAnalysisResult analyze(DocumentModelBridge document, WordsQuery wordsQuery)
        throws WordsAnalysisException
    {
        PartAnalysisResult result = new PartAnalysisResult(this.getHint(), document.getDocumentReference());
        String query = wordsQuery.getQuery();

        // FIXME: we need to escape some characters to avoid problem with the regex
        String regex = String.format("(\\s(?<%2$s>%1$s))|"
                + "((?<%3$s>%1$s)\\s)|"
                + "(^(?<%4$s>%1$s)$)",
            query.toLowerCase(),
            SPACE_PREFIX_GROUP_NAME,
            SPACE_SUFFIX_GROUP_NAME,
            ALONE_GROUP_NAME);
        Pattern pattern = Pattern.compile(regex);

        this.getTextToAnalyze(document).forEach((key, value) -> analyzeText(pattern, value, key, result));

        return result;
    }

    private void analyzeText(Pattern pattern, List<String> textsToAnalyze, EntityReference localization,
        PartAnalysisResult result)
    {
        int counter = 0;
        for (String textToAnalyze : textsToAnalyze) {
            // Note that for now it seems better for perf to transform the content and the query to perform
            // case insensitive matching instead of using the case insensitive flag as the javadoc indicates that
            // it might involve some performance penalty.
            Matcher matcher = pattern.matcher(textToAnalyze.toLowerCase());

            while (matcher.find()) {
                String groupName;
                if (!StringUtils.isEmpty(matcher.group(SPACE_PREFIX_GROUP_NAME))) {
                    groupName = SPACE_PREFIX_GROUP_NAME;
                } else if (!StringUtils.isEmpty(matcher.group(SPACE_SUFFIX_GROUP_NAME))) {
                    groupName = SPACE_SUFFIX_GROUP_NAME;
                } else {
                    groupName = ALONE_GROUP_NAME;
                }
                result.addRegion(new WordsMentionLocalization(
                    localization,
                    counter,
                    matcher.start(groupName),
                    matcher.end(groupName)));
            }
            counter++;
        }
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
