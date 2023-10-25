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
package org.xwiki.contrib.wordnotification;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Define the specific localization of a mentioned word inside a part of an analyzed document.
 *
 * @version $Id$
 * @since 1.0
 */
public class WordsMentionLocalization
{
    private final int regionStart;
    private final int regionEnd;
    private final EntityReference entityReference;
    private final int positionInList;

    /**
     * Default constructor.
     *
     * @param entityReference the actual specific reference of the part of the document analyzed (e.g. specific
     * object reference)
     * @param positionInList if the analyzer returns a list of element, the position of the element in the list (e.g.
     * position in the list if the xobject property is a list of string, or position in a content if it's split by
     * lines)
     * @param regionStart the specific start offset where a match has been found
     * @param regionEnd the specific end offset where a match has been found
     */
    public WordsMentionLocalization(EntityReference entityReference, int positionInList, int regionStart, int regionEnd)
    {
        this.entityReference = entityReference;
        this.positionInList = positionInList;
        this.regionStart = regionStart;
        this.regionEnd = regionEnd;
    }

    /**
     * @return the specific start offset where a match has been found
     */
    public int getRegionStart()
    {
        return regionStart;
    }

    /**
     * @return the specific end offset where a match has been found
     */
    public int getRegionEnd()
    {
        return regionEnd;
    }

    /**
     * @return the actual specific reference of the part of the document analyzed (e.g. specific
     * object reference)
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return if the analyzer returns a list of element, the position of the element in the list (e.g.
     * position in the list if the xobject property is a list of string, or position in a content if it's split by
     * lines)
     */
    public int getPositionInList()
    {
        return positionInList;
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

        WordsMentionLocalization that = (WordsMentionLocalization) o;

        return new EqualsBuilder()
            .append(positionInList, that.positionInList)
            .append(regionStart, that.regionStart)
            .append(regionEnd, that.regionEnd)
            .append(entityReference, that.entityReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 85)
            .append(regionStart)
            .append(regionEnd)
            .append(entityReference)
            .append(positionInList)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("regionStart", regionStart)
            .append("regionEnd", regionEnd)
            .append("entityReference", entityReference)
            .append("positionInList", positionInList)
            .toString();
    }
}
