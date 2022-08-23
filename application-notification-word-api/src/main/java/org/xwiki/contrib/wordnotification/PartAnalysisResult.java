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

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.model.reference.EntityReference;

public class PartAnalysisResult
{
    private final EntityReference entityReference;
    private final Set<Pair<Integer, Integer>> regions;
    private final String analyzerHint;

    public PartAnalysisResult(String analyzerHint, EntityReference entityReference)
    {
        this.entityReference = entityReference;
        this.analyzerHint = analyzerHint;
        this.regions = new LinkedHashSet<>();
    }

    public void addRegion(Pair<Integer, Integer> region)
    {
        this.regions.add(region);
    }

    public long getOccurences()
    {
        return this.regions.size();
    }

    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    public Set<Pair<Integer, Integer>> getRegions()
    {
        return regions;
    }

    public String getAnalyzerHint()
    {
        return analyzerHint;
    }
}
