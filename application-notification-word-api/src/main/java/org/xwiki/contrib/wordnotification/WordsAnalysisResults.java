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

import org.xwiki.extension.xar.job.diff.DocumentVersionReference;
import org.xwiki.stability.Unstable;

/**
 * Global results of an analysis of a document for a given query.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class WordsAnalysisResults
{
    private final WordsQuery query;
    private final DocumentVersionReference reference;
    private final List<PartAnalysisResult> results;

    /**
     * Default constructor.
     *
     * @param reference the reference of the analyzed document referring to a specific version of the document
     * @param query the query to look for in the document
     */
    public WordsAnalysisResults(DocumentVersionReference reference, WordsQuery query)
    {
        this.reference = reference;
        this.query = query;
        this.results = new ArrayList<>();
    }

    /**
     * Add the given partial result to the global results.
     *
     * @param result the partial result to add.
     */
    public void addResult(PartAnalysisResult result)
    {
        this.results.add(result);
    }

    /**
     * @return the query used in the analysis
     */
    public WordsQuery getQuery()
    {
        return query;
    }

    /**
     * @return the version reference of the analyzed document
     */
    public DocumentVersionReference getReference()
    {
        return reference;
    }

    /**
     * @return the total number of occurrences found, computed from all partial analysis results
     */
    public long getOccurrences()
    {
        return this.results.stream().map(PartAnalysisResult::getOccurrences).reduce(0L, Long::sum);
    }

    /**
     * @return the list of all partial analysis results
     */
    public List<PartAnalysisResult> getResults()
    {
        return new ArrayList<>(results);
    }
}
