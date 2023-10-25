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
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiCommentsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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
        when(object1.getStringValue("comment")).thenReturn("Something completely different.");
        when(object2.getStringValue("comment")).thenReturn("This foo is FOO.");
        when(object3.getStringValue("comment")).thenReturn("foo\nBut is thisnotfOO.\nfOO!!");

        BaseObjectReference object2Reference = mock(BaseObjectReference.class, "object2Ref");
        when(object2.getReference()).thenReturn(object2Reference);

        BaseObjectReference object3Reference = mock(BaseObjectReference.class, "object3Ref");
        when(object3.getReference()).thenReturn(object3Reference);

        PartAnalysisResult expectedResult = new PartAnalysisResult(CommentsWordsMentionAnalyzer.HINT);
        expectedResult.addRegion(new WordsMentionLocalization(object2Reference, 0, 5, 8));
        expectedResult.addRegion(new WordsMentionLocalization(object2Reference, 0, 12, 15));
        expectedResult.addRegion(new WordsMentionLocalization(object3Reference, 0, 0, 3));
        expectedResult.addRegion(new WordsMentionLocalization(object3Reference, 0, 23, 26));

        assertEquals(expectedResult, this.analyzer.analyze(document, wordsQuery));
    }
}