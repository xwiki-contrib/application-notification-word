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

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.PatternAnalysisHelper;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TagsWordsMentionAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class TagsWordsMentionAnalyzerTest
{
    @InjectMockComponents
    private TagsWordsMentionAnalyzer analyzer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private PatternAnalysisHelper patternAnalysisHelper;

    @Test
    void analyze() throws WordsAnalysisException
    {
        XWikiDocument document = mock(XWikiDocument.class);
        WordsQuery wordsQuery = mock(WordsQuery.class);
        DocumentReference reference = new DocumentReference("xwiki", "Foo", "Bar");
        when(document.getDocumentReference()).thenReturn(reference);

        String query = "Foo";
        List<String> tags = List.of("foO", "bar", "buz", "other", "isfoo", "FOO");
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);

        when(wordsQuery.getQuery()).thenReturn(query);
        when(document.getTagsList(context)).thenReturn(tags);

        WordsMentionLocalization localization1 = mock(WordsMentionLocalization.class);
        WordsMentionLocalization localization2 = mock(WordsMentionLocalization.class);
        when(patternAnalysisHelper.getRegions(query, tags, reference))
            .thenReturn(List.of(localization1, localization2));

        PartAnalysisResult expectedResult = new PartAnalysisResult(TagsWordsMentionAnalyzer.HINT);
        expectedResult.addRegion(localization1);
        expectedResult.addRegion(localization2);

        assertEquals(expectedResult, this.analyzer.analyze(document, wordsQuery));
    }
}