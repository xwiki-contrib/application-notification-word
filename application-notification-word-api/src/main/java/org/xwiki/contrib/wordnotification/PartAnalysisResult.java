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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a partial result of an analysis performed on a document: each analyzer will return a partial analysis.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class PartAnalysisResult
{
    private final EntityReference entityReference;
    private final Set<Pair<Integer, Integer>> regions;
    private final String analyzerHint;

    /**
     * Default constructor.
     *
     * @param analyzerHint the hint of the {@link WordsMentionAnalyzer} which returns this result
     * @param entityReference the specific reference of the element of the document which have been analyzed
     */
    public PartAnalysisResult(String analyzerHint, EntityReference entityReference)
    {
        this.entityReference = entityReference;
        this.analyzerHint = analyzerHint;
        this.regions = new LinkedHashSet<>();
    }

    /**
     * Add a region where the query has been found in the results.
     *
     * @param region the coordinate where a query has been found, represented as the start and end offset of the
     *               matching expression.
     */
    public void addRegion(Pair<Integer, Integer> region)
    {
        this.regions.add(region);
    }

    /**
     * @return the total number of occurrences found
     */
    public long getOccurrences()
    {
        return this.regions.size();
    }

    /**
     * @return the specific reference of the analyzed part of the document
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return the set of regions corresponding to each occurrence: a region is represented by the start and end offsets
     *         of the matching expression
     */
    public Set<Pair<Integer, Integer>> getRegions()
    {
        return regions;
    }

    /**
     * @return the hint of the {@link WordsMentionAnalyzer} that produces this result
     */
    public String getAnalyzerHint()
    {
        return analyzerHint;
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

        PartAnalysisResult that = (PartAnalysisResult) o;

        return new EqualsBuilder()
            .append(entityReference, that.entityReference)
            .append(regions, that.regions)
            .append(analyzerHint, that.analyzerHint)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 67)
            .append(entityReference)
            .append(regions)
            .append(analyzerHint)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("entityReference", entityReference)
            .append("regions", regions)
            .append("analyzerHint", analyzerHint)
            .toString();
    }
}
