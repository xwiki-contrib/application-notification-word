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

import org.xwiki.eventstream.TargetableEvent;

public class MentionedWordsRecordableEvent implements TargetableEvent
{
    static final String NEW_OCCURRENCES_FIELD = "newOccurrences";
    static final String OLD_OCCURRENCES_FIELD = "oldOccurrences";
    static final String QUERY_FIELD = "query";
    static final String IS_NEW_FIELD = "isNew";

    private final Set<String> targets;
    private boolean isNew = false;
    private final long newOccurrences;
    private final long oldOccurrences;
    private final String query;

    public MentionedWordsRecordableEvent()
    {
        this(Collections.emptySet(), -1, -1, "");
    }

    public MentionedWordsRecordableEvent(Set<String> targets, long newOccurrences, long oldOccurrences, String query)
    {
        this.targets = targets;
        this.newOccurrences = newOccurrences;
        this.oldOccurrences = oldOccurrences;
        this.query = query;
    }

    @Override
    public Set<String> getTarget()
    {
        return this.targets;
    }

    public boolean isNew()
    {
        return isNew;
    }

    public long getNewOccurrences()
    {
        return newOccurrences;
    }

    public long getOldOccurrences()
    {
        return oldOccurrences;
    }

    public String getQuery()
    {
        return query;
    }

    public void setNew(boolean aNew)
    {
        isNew = aNew;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof MentionedWordsRecordableEvent;
    }
}
