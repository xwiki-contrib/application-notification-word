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

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ContentWordsMentionAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class ContentWordsMentionAnalyzerTest
{
    @InjectMockComponents
    private ContentWordsMentionAnalyzer analyzer;

    @Test
    void analyze() throws WordsAnalysisException
    {
        DocumentModelBridge document = mock(DocumentModelBridge.class);
        WordsQuery wordsQuery = mock(WordsQuery.class);
        DocumentReference reference = new DocumentReference("xwiki", "Foo", "Bar");
        when(document.getDocumentReference()).thenReturn(reference);

        String query = "Foo";
        String documentContent = "A text with foo. \n"
            + "Another line with FOO\n"
            + "Something else with f*oo\n"
            + "And foo finally Foo.";
        when(wordsQuery.getQuery()).thenReturn(query);
        when(document.getContent()).thenReturn(documentContent);

        PartAnalysisResult expectedResult = new PartAnalysisResult(ContentWordsMentionAnalyzer.HINT);
        expectedResult.addRegion(new WordsMentionLocalization(reference, 0, 12, 15));
        expectedResult.addRegion(new WordsMentionLocalization(reference, 1, 18, 21));
        expectedResult.addRegion(new WordsMentionLocalization(reference, 3, 4, 7));
        expectedResult.addRegion(new WordsMentionLocalization(reference, 3, 16, 19));

        assertEquals(expectedResult, this.analyzer.analyze(document, wordsQuery));
    }
}