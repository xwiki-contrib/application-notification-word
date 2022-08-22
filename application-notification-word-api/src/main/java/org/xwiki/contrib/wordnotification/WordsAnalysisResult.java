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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class WordsAnalysisResult
{
    private final WordsQuery query;
    private final AnalyzedElementReference reference;

    private final String analyzerHint;
    private final Set<Pair<Integer, Integer>> regions;
    private int occurences;

    public WordsAnalysisResult(AnalyzedElementReference reference, WordsQuery query, String analyzerHint)
    {
        this.reference = reference;
        this.query = query;
        this.analyzerHint = analyzerHint;
        this.regions = new HashSet<>();
    }

    public void setRegions(Set<Pair<Integer, Integer>> regions)
    {
        this.regions.clear();
        this.regions.addAll(regions);
        this.occurences = this.regions.size();
    }

    public void setOccurences(int occurences)
    {
        this.regions.clear();
        this.occurences = occurences;
    }

    public boolean hasOccurences()
    {
        return this.occurences > 0;
    }

    public WordsQuery getQuery()
    {
        return query;
    }

    public Set<Pair<Integer, Integer>> getRegions()
    {
        return regions;
    }

    public int getOccurences()
    {
        return occurences;
    }

    public AnalyzedElementReference getReference()
    {
        return reference;
    }

    public String getAnalyzerHint()
    {
        return analyzerHint;
    }
}
