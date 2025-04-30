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

import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.UserReference;

/**
 * Abstract recordable events for {@link org.xwiki.contrib.wordnotification.MentionedWordsEvent} and
 * {@link org.xwiki.contrib.wordnotification.RemovedWordsEvent}.
 *
 * @version $Id$
 * @since 1.1
 */
public abstract class AbstractMentionedWordsRecordableEvent implements TargetableEvent
{
    static final String NEW_OCCURRENCES_FIELD = "newOccurrences";
    static final String OLD_OCCURRENCES_FIELD = "oldOccurrences";
    static final String QUERY_FIELD = "query";
    static final String IS_NEW_FIELD = "isNew";

    private final Set<String> targets;
    private boolean isNew;
    private final long newOccurrences;
    private final long oldOccurrences;
    private final String query;
    private final UserReference author;

    /**
     * Default constructor.
     *
     * @param targets the actual people the notification should be triggered to
     * @param newOccurrences the number of new occurrences found in the analysis
     * @param oldOccurrences the number of old occurrences found in previous analysis
     * @param query the actual query for which we send a notification
     * @param author the author responsible of the event
     */
    protected AbstractMentionedWordsRecordableEvent(Set<String> targets, long newOccurrences, long oldOccurrences,
        String query, UserReference author)
    {
        this.targets = targets;
        this.newOccurrences = newOccurrences;
        this.oldOccurrences = oldOccurrences;
        this.query = query;
        this.author = author;
    }

    @Override
    public Set<String> getTarget()
    {
        return this.targets;
    }

    /**
     * @return whether the event concerns a new document or not
     */
    public boolean isNew()
    {
        return isNew;
    }

    /**
     * @return the number of occurrences counted in new analysis
     */
    public long getNewOccurrences()
    {
        return newOccurrences;
    }

    /**
     * @return the number of occurrences counted in previous analysis
     */
    public long getOldOccurrences()
    {
        return oldOccurrences;
    }

    /**
     * @return the query for which an event is triggered
     */
    public String getQuery()
    {
        return query;
    }

    /**
     * @return the author responsible of the event.
     */
    public UserReference getAuthor()
    {
        return author;
    }

    /**
     * Set whether the document for which the event is triggered is new or not.
     *
     * @param aNew {@code true} if it's a new document
     */
    public void setNew(boolean aNew)
    {
        isNew = aNew;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof AbstractMentionedWordsRecordableEvent;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractMentionedWordsRecordableEvent that = (AbstractMentionedWordsRecordableEvent) o;

        return new EqualsBuilder()
            .append(isNew, that.isNew)
            .append(newOccurrences, that.newOccurrences)
            .append(oldOccurrences, that.oldOccurrences)
            .append(targets, that.targets)
            .append(query, that.query)
            .append(author, that.author)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 87)
            .append(targets)
            .append(isNew)
            .append(newOccurrences)
            .append(oldOccurrences)
            .append(query)
            .append(author)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("targets", targets)
            .append(IS_NEW_FIELD, isNew)
            .append(NEW_OCCURRENCES_FIELD, newOccurrences)
            .append(OLD_OCCURRENCES_FIELD, oldOccurrences)
            .append(QUERY_FIELD, query)
            .append("author", author)
            .toString();
    }
}
