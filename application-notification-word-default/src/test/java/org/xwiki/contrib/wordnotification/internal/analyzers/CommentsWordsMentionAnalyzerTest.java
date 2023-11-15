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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiCommentsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CommentsWordsMentionAnalyzer}.
 *
 * @version $Id$
 */
@ComponentTest
class CommentsWordsMentionAnalyzerTest
{
    @InjectMockComponents
    private CommentsWordsMentionAnalyzer analyzer;

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

        when(wordsQuery.getQuery()).thenReturn(query);
        BaseObject object1 = mock(BaseObject.class, "object1");
        BaseObject object2 = mock(BaseObject.class, "object2");
        BaseObject object3 = mock(BaseObject.class, "object3");

        List<BaseObject> objectList = new ArrayList<>();
        objectList.add(null);
        objectList.add(object1);
        objectList.add(object2);
        objectList.add(null);
        objectList.add(object3);
        objectList.add(null);

        when(document.getXObjects(XWikiCommentsDocumentInitializer.LOCAL_REFERENCE)).thenReturn(objectList);
        String comment1 = "Something completely different.";
        String comment2 = "This foo is FOO.";
        String comment3 = "foo\nBut is thisnotfOO.\nfOO!!";
        when(object1.getStringValue("comment")).thenReturn(comment1);
        when(object2.getStringValue("comment")).thenReturn(comment2);
        when(object3.getStringValue("comment")).thenReturn(comment3);

        BaseObjectReference object1Reference = mock(BaseObjectReference.class, "object1Ref");
        when(object1.getReference()).thenReturn(object1Reference);

        BaseObjectReference object2Reference = mock(BaseObjectReference.class, "object2Ref");
        when(object2.getReference()).thenReturn(object2Reference);

        BaseObjectReference object3Reference = mock(BaseObjectReference.class, "object3Ref");
        when(object3.getReference()).thenReturn(object3Reference);

        WordsMentionLocalization localization1 = mock(WordsMentionLocalization.class);
        WordsMentionLocalization localization2 = mock(WordsMentionLocalization.class);
        WordsMentionLocalization localization3 = mock(WordsMentionLocalization.class);
        when(this.patternAnalysisHelper.getRegions(query, List.of(comment1), object1Reference))
            .thenReturn(List.of(localization1));
        when(this.patternAnalysisHelper.getRegions(query, List.of(comment2), object2Reference))
            .thenReturn(List.of(localization2));
        when(this.patternAnalysisHelper.getRegions(query, List.of(comment3), object3Reference))
            .thenReturn(List.of(localization3));

        PartAnalysisResult expectedResult = new PartAnalysisResult(CommentsWordsMentionAnalyzer.HINT);
        expectedResult.addRegion(localization1);
        expectedResult.addRegion(localization2);
        expectedResult.addRegion(localization3);

        assertEquals(expectedResult, this.analyzer.analyze(document, wordsQuery));
        verify(this.patternAnalysisHelper, times(3)).getRegions(eq(query), any(), any());
    }
}