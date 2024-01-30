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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MentionedWordsEventDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
class MentionedWordsEventDisplayerTest
{
    @InjectMockComponents
    private MentionedWordsEventDisplayer eventDisplayer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @Test
    void renderNotification() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);

        Event event1Query1 = mock(Event.class, "event1query1");
        Event event2Query1 = mock(Event.class, "event2query1");
        Event event3Query1 = mock(Event.class, "event3query1");

        Event event1Query2 = mock(Event.class, "event1query2");
        Event event2Query2 = mock(Event.class, "event2query2");

        Event event1Query3 = mock(Event.class, "event1query3");
        Event event2Query3 = mock(Event.class, "event2query3");
        Event event3Query3 = mock(Event.class, "event3query3");

        Event event1Query4 = mock(Event.class, "event1query4");

        String query1 = "query1";
        when(event1Query1.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query1));
        when(event2Query1.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query1));
        when(event3Query1.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query1));

        String query2 = "query2";
        when(event1Query2.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query2));
        when(event2Query2.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query2));

        String query3 = "query3";
        when(event1Query3.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query3));
        when(event2Query3.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query3));
        when(event3Query3.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, query3));

        when(event1Query4.getCustom()).thenReturn(Map.of(AbstractMentionedWordsRecordableEvent.QUERY_FIELD, "query4"));

        when(compositeEvent.getEvents()).thenReturn(List.of(
            event1Query3,
            event1Query1,
            event2Query1,
            event1Query4,
            event1Query2,
            event2Query2,
            event2Query3,
            event3Query1,
            event3Query3
        ));

        when(event1Query1.getDate()).thenReturn(new Date(23));
        when(event2Query1.getDate()).thenReturn(new Date(24));
        when(event3Query1.getDate()).thenReturn(new Date(78));

        when(event1Query2.getDate()).thenReturn(new Date(29));
        when(event2Query2.getDate()).thenReturn(new Date(48));

        when(event1Query3.getDate()).thenReturn(new Date(15));
        when(event2Query3.getDate()).thenReturn(new Date(52));
        when(event3Query3.getDate()).thenReturn(new Date(96));

        when(event1Query4.getDate()).thenReturn(new Date(26));

        CompositeEvent expectedComposite1 = new CompositeEvent(event1Query1);
        expectedComposite1.add(event2Query1, 10);
        expectedComposite1.add(event3Query1, 10);

        CompositeEvent expectedComposite2 = new CompositeEvent(event1Query2);
        expectedComposite2.add(event2Query2, 10);

        CompositeEvent expectedComposite3 = new CompositeEvent(event1Query3);
        expectedComposite3.add(event2Query3, 10);
        expectedComposite3.add(event3Query3, 10);

        CompositeEvent expectedComposite4 = new CompositeEvent(event1Query4);

        when(compositeEvent.getType()).thenReturn(RemovedWordsRecordableEvent.class.getCanonicalName());
        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        Template template = mock(Template.class);
        when(this.templateManager.getTemplate("notificationWord/notification.vm")).thenReturn(template);
        XDOM xdom = mock(XDOM.class);
        when(this.templateManager.execute(template)).thenReturn(xdom);
        Block block1 = mock(Block.class, "block1");
        Block block2 = mock(Block.class, "block2");
        Block block3 = mock(Block.class, "block3");
        Block block4 = mock(Block.class, "block4");
        when(xdom.getChildren())
            .thenReturn(List.of(block1))
            .thenReturn(List.of(block2))
            .thenReturn(List.of(block3))
            .thenReturn(List.of(block4));

        GroupBlock groupBlock = new GroupBlock();
        groupBlock.addChildren(List.of(block1, block2, block3, block4));

        assertEquals(groupBlock.getChildren(), this.eventDisplayer.renderNotification(compositeEvent).getChildren());

        verify(scriptContext).setAttribute("compositeEvent", expectedComposite1, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("compositeEvent", expectedComposite2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("compositeEvent", expectedComposite3, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("compositeEvent", expectedComposite4, ScriptContext.ENGINE_SCOPE);

        verify(scriptContext, times(4)).setAttribute("isRemoval", true, ScriptContext.ENGINE_SCOPE);

        verify(scriptContext, times(4)).removeAttribute("compositeEvent", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext, times(4)).removeAttribute("isRemoval", ScriptContext.ENGINE_SCOPE);
    }
}