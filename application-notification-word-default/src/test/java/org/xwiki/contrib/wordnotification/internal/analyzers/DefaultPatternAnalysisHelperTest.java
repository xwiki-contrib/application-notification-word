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


import org.junit.jupiter.api.Test;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DefaultPatternAnalysisHelper}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultPatternAnalysisHelperTest
{
    @InjectMockComponents
    private DefaultPatternAnalysisHelper patternAnalysisHelper;

    @Test
    void getRegions_withJokers_simpleCases()
    {
        String query = "Fo* Ba?";
        List<String> strings = List.of("Fo Ba",
            "Foo Bar",
            "Fo Bar",
            "Foooooooooo Bar",
            "FoBa",
            "Foo Barrrrr");
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");

        List<WordsMentionLocalization> expectedResult = List.of(
            new WordsMentionLocalization(entityReference, 0, 0, 5),
            new WordsMentionLocalization(entityReference, 1, 0, 7),
            new WordsMentionLocalization(entityReference, 2, 0, 6),
            new WordsMentionLocalization(entityReference, 3, 0, 15));
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }

    @Test
    void getRegions_noJokers()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        String query = "foo";
        List<String> strings = List.of(
            "Something completely different.",
            "This foo is FOO.",
            "foo\nBut is thisnotfOO.\nfOO!!"
        );

        List<WordsMentionLocalization> expectedResult =
            List.of(new WordsMentionLocalization(entityReference, 1, 5, 8),
                new WordsMentionLocalization(entityReference, 1, 12, 15),
                new WordsMentionLocalization(entityReference, 2, 0, 3),
                new WordsMentionLocalization(entityReference, 2, 23, 26));
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }

    @Test
    void getRegions_noJokers2()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        String query = "Foo";
        List<String> strings = List.of("A text with foo. \n",
            "Another line with FOO\n",
            "Something else with f*oo\n",
            "And foo finally Foo.");

        List<WordsMentionLocalization> expectedResult =
            List.of(new WordsMentionLocalization(entityReference, 0, 12, 15),
                new WordsMentionLocalization(entityReference, 1, 18, 21),
                new WordsMentionLocalization(entityReference, 3, 4, 7),
                new WordsMentionLocalization(entityReference, 3, 16, 19)
            );
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }

    @Test
    void getRegions_noJokers3()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        String query = "Foo";
        List<String> strings = List.of("foO", "bar", "buz", "other", "isfoo", "FOO");

        List<WordsMentionLocalization> expectedResult =
            List.of(new WordsMentionLocalization(entityReference, 0, 0, 3),
                new WordsMentionLocalization(entityReference, 5, 0, 3)
            );
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }

    @Test
    void getRegions_jokers()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        String query = "http?://*xwiki.org*";
        List<String> strings = List.of(
            "Check out https://xwiki.org ! ",
            "You can also visit: http://extensions.xwiki.org",
            "Document of the extension is on https://extensions.xwiki.org/xwiki/bin/view/Extension/"
                + "Word-Based%20Notification%20Application%20-%20Default/ so don't hesitate to have a look",
            "http:xwiki.org is not a valid http address",
            "http://xwiki.com is not related",
            "https://www.xwiki.org"
            );

        List<WordsMentionLocalization> expectedResult =
            List.of(
                new WordsMentionLocalization(entityReference, 0, 10, 27),
                new WordsMentionLocalization(entityReference, 1, 20, 47),
                new WordsMentionLocalization(entityReference, 2, 32, 60),
                new WordsMentionLocalization(entityReference, 5, 0, 21)
            );
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }

    @Test
    void getRegions_wildcard()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        List<String> strings = List.of("You can find here the list of queries to look for in pages. "
            + "When an expression matching the query is found, you'll receive a notification. "
            + "Don't forget to ensure the notification type is enabled in your notification settings.");
        String query = "quer*";

        List<WordsMentionLocalization> expectedResult =
            List.of(
                new WordsMentionLocalization(entityReference, 0, 30, 34),
                new WordsMentionLocalization(entityReference, 0, 92, 96)
            );
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }

    @Test
    void getRegions_jokersAndEscape()
    {
        EntityReference entityReference = new DocumentReference("xwiki", "Foo", "Bar");
        String query = "Expression with \\* *\\\\ character ?\\?";
        List<String> strings = List.of(
            "Expression with * \\ character ?",
            "Expression with * AAAAAAAAAA \\ character ?",
            "Expression with \\ character ?",
            "Expression with * \\\\ character ?",
            "Expression with * \\ character ??"
        );

        List<WordsMentionLocalization> expectedResult =
            List.of(
                new WordsMentionLocalization(entityReference, 0, 0, 31),
                new WordsMentionLocalization(entityReference, 1, 0, 42),
                new WordsMentionLocalization(entityReference, 3, 0, 32),
                new WordsMentionLocalization(entityReference, 4, 0, 32)
            );
        List<WordsMentionLocalization> regions = this.patternAnalysisHelper.getRegions(query, strings, entityReference);
        assertEquals(expectedResult, regions);
    }
}