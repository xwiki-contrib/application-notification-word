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

import java.util.ArrayList;
import java.util.List;

public class WordsAnalysisResults
{
    private final WordsQuery query;
    private final AnalyzedElementReference reference;

    private final List<PartAnalysisResult> results;

    public WordsAnalysisResults(AnalyzedElementReference reference, WordsQuery query)
    {
        this.reference = reference;
        this.query = query;
        this.results = new ArrayList<>();
    }

    public void addResult(PartAnalysisResult result)
    {
        this.results.add(result);
    }

    public WordsQuery getQuery()
    {
        return query;
    }

    public AnalyzedElementReference getReference()
    {
        return reference;
    }

    public long getOccurrences()
    {
        return this.results.stream().map(PartAnalysisResult::getOccurrences).reduce(0L, Long::sum);
    }

    public List<PartAnalysisResult> getResults()
    {
        return new ArrayList<>(results);
    }
}
