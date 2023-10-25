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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TitleWordsMentionAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class TitleWordsMentionAnalyzerTest
{
    @InjectMockComponents
    private TitleWordsMentionAnalyzer analyzer;

    @Test
    void analyze() throws WordsAnalysisException
    {
        DocumentModelBridge document = mock(DocumentModelBridge.class);
        WordsQuery wordsQuery = mock(WordsQuery.class);
        DocumentReference reference = new DocumentReference("xwiki", "Foo", "Bar");
        when(document.getDocumentReference()).thenReturn(reference);

        String query = "Foo";
        String documentTitle = "A text with foo.";
        when(wordsQuery.getQuery()).thenReturn(query);
        when(document.getTitle()).thenReturn(documentTitle);

        PartAnalysisResult expectedResult = new PartAnalysisResult(TitleWordsMentionAnalyzer.HINT);
        expectedResult.addRegion(new WordsMentionLocalization(reference, 0, 12, 15));

        assertEquals(expectedResult, this.analyzer.analyze(document, wordsQuery));
    }
}