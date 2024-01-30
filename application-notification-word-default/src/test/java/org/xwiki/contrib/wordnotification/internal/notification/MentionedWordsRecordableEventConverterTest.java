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
package org.xwiki.contrib.wordnotification.internal.notification;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MentionedWordsRecordableEventConverter}.
 *
 * @version $Id$
 */
@ComponentTest
class MentionedWordsRecordableEventConverterTest
{
    @InjectMockComponents
    private MentionedWordsRecordableEventConverter recordableEventConverter;

    @MockComponent
    private RecordableEventConverter defaultConverter;

    @Test
    void convert() throws Exception
    {
        AbstractMentionedWordsRecordableEvent recordableEvent = mock(AbstractMentionedWordsRecordableEvent.class);
        String source = "mySource";
        String data = "someData";

        when(recordableEvent.isNew()).thenReturn(true);
        when(recordableEvent.getOldOccurrences()).thenReturn(38L);
        when(recordableEvent.getNewOccurrences()).thenReturn(42L);
        when(recordableEvent.getQuery()).thenReturn("theQuery");

        Map<String, Object> expectedMap = Map.of(
            MentionedWordsRecordableEvent.IS_NEW_FIELD, true,
            MentionedWordsRecordableEvent.OLD_OCCURRENCES_FIELD, 38L,
            MentionedWordsRecordableEvent.NEW_OCCURRENCES_FIELD, 42L,
            MentionedWordsRecordableEvent.QUERY_FIELD, "theQuery"
        );

        Event expectedEvent = mock(Event.class);
        when(this.defaultConverter.convert(recordableEvent, source, data)).thenReturn(expectedEvent);
        assertEquals(expectedEvent, this.recordableEventConverter.convert(recordableEvent, source, data));

        verify(expectedEvent).setCustom(expectedMap);
    }
}