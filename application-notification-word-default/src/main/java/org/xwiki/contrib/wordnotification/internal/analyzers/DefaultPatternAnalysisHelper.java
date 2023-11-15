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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.PatternAnalysisHelper;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.model.reference.EntityReference;

/**
 * Default implementation of {@link PatternAnalysisHelper}.
 * This classes performs the analysis using Pattern matching.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
public class DefaultPatternAnalysisHelper implements PatternAnalysisHelper
{
    private static final String SPACE_PREFIX_GROUP_NAME = "querySpacePrefix";
    private static final String SPACE_SUFFIX_GROUP_NAME = "querySpaceSuffix";
    private static final String ALONE_GROUP_NAME = "querySpaceAlone";
    private static final String START_ESCAPING = "\\Q";
    private static final String END_ESCAPING = "\\E";

    private String transformQuery(String query)
    {
        String result;
        if (query.contains("*") || query.contains("?")) {
            StringBuilder resultBuilder = new StringBuilder();
            boolean backslashEscape = false;
            resultBuilder.append(START_ESCAPING);
            for (int currentIndex = 0; currentIndex < query.length(); currentIndex++) {
                char currentChar = query.charAt(currentIndex);
                // we're not in escape mode we find a backslash: we enter in escape mode for next char
                if (!backslashEscape && currentChar == '\\') {
                    backslashEscape = true;
                // we're in escape mode we find a backslash: we just add a backslash since everything is escaped
                // inside \Q and \E and we leave escape mode
                } else if (backslashEscape && currentChar == '\\') {
                    resultBuilder.append('\\');
                    backslashEscape = false;
                // we're in escape mode we find a question mark: same as above
                } else if (backslashEscape && currentChar == '?') {
                    resultBuilder.append('?');
                    backslashEscape = false;
                // we're in escape mode we find a star: same as above
                } else if (backslashEscape && currentChar == '*') {
                    resultBuilder.append('*');
                    backslashEscape = false;
                // we're not in escape mode we find a question mark: we stop escaping the whole string, and we want
                // to detect a single character so we add it in the query and we start again escaping everything
                } else if (!backslashEscape && currentChar == '?') {
                    resultBuilder.append(END_ESCAPING);
                    resultBuilder.append(".?");
                    resultBuilder.append(START_ESCAPING);
                // we're not in escape mode we find a star: same as above
                } else if (!backslashEscape && currentChar == '*') {
                    resultBuilder.append(END_ESCAPING);
                    resultBuilder.append(".*");
                    resultBuilder.append(START_ESCAPING);
                // we're in backslash escape and we find any other character: it was just a backslash, not an escape
                } else if (backslashEscape) {
                    resultBuilder.append('\\');
                    resultBuilder.append(Character.toLowerCase(currentChar));
                    backslashEscape = false;
                } else {
                    resultBuilder.append(Character.toLowerCase(currentChar));
                }
            }
            resultBuilder.append(END_ESCAPING);
            result = resultBuilder.toString();
        } else {
            result = query.toLowerCase();
        }
        return result;
    }

    private Pattern getPattern(String query)
    {
        String transformedQuery = transformQuery(query);
        String regex = String.format("(\\s(?<%2$s>%1$s))|"
                + "((?<%3$s>%1$s)\\s)|"
                + "(^(?<%4$s>%1$s)$)",
            transformedQuery,
            SPACE_PREFIX_GROUP_NAME,
            SPACE_SUFFIX_GROUP_NAME,
            ALONE_GROUP_NAME);
        return Pattern.compile(regex);
    }

    @Override
    public List<WordsMentionLocalization> getRegions(String query, List<String> textsToAnalyze,
        EntityReference localization)
    {
        int counter = 0;
        List<WordsMentionLocalization> result = new ArrayList<>();
        Pattern pattern = getPattern(query);
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
                result.add(new WordsMentionLocalization(
                    localization,
                    counter,
                    matcher.start(groupName),
                    matcher.end(groupName)));
            }
            counter++;
        }
        return result;
    }
}
