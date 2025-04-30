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

import java.util.Collections;
import java.util.Set;

import org.xwiki.user.UserReference;

/**
 * Recordable events for {@link org.xwiki.contrib.wordnotification.RemovedWordsEvent}.
 *
 * @version $Id$
 * @since 1.1
 */
public class RemovedWordsRecordableEvent extends AbstractMentionedWordsRecordableEvent
{
    /**
     * Default empty constructor.
     */
    public RemovedWordsRecordableEvent()
    {
        this(Collections.emptySet(), -1, -1, "", null);
    }

    /**
     * Default constructor.
     *
     * @param targets the actual people the notification should be triggered to
     * @param newOccurrences the number of new occurrences found in the analysis
     * @param oldOccurrences the number of old occurrences found in previous analysis
     * @param query the actual query for which we send a notification
     * @param author the author responsible of the event
     */
    public RemovedWordsRecordableEvent(Set<String> targets, long newOccurrences, long oldOccurrences, String query,
        UserReference author)
    {
        super(targets, newOccurrences, oldOccurrences, query, author);
    }
}
