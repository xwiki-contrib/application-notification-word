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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Custom {@link RecordableEventConverter} mainly aiming to register the custom fields of
 * {@link AbstractMentionedWordsRecordableEvent}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named("MentionedWordsRecordableEventConverter")
public class MentionedWordsRecordableEventConverter implements RecordableEventConverter
{
    @Inject
    private RecordableEventConverter defaultConverter;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceDocSerializer;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event event = this.defaultConverter.convert(recordableEvent, source, data);

        AbstractMentionedWordsRecordableEvent mentionedWordsEvent =
            (AbstractMentionedWordsRecordableEvent) recordableEvent;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(MentionedWordsRecordableEvent.IS_NEW_FIELD, mentionedWordsEvent.isNew());
        parameters.put(MentionedWordsRecordableEvent.NEW_OCCURRENCES_FIELD, mentionedWordsEvent.getNewOccurrences());
        parameters.put(MentionedWordsRecordableEvent.OLD_OCCURRENCES_FIELD, mentionedWordsEvent.getOldOccurrences());
        parameters.put(MentionedWordsRecordableEvent.QUERY_FIELD, mentionedWordsEvent.getQuery());

        event.setCustom(parameters);
        event.setUser(this.userReferenceDocSerializer.serialize(mentionedWordsEvent.getAuthor()));
        return event;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return List.of(
            new MentionedWordsRecordableEvent(),
            new RemovedWordsRecordableEvent()
        );
    }
}
