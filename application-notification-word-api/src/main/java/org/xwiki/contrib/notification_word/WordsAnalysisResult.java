package org.xwiki.contrib.notification_word;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class WordsAnalysisResult
{
    private final WordsQuery query;
    private final AnalyzedElementReference analyzedElement;
    private final Set<Pair<Integer, Integer>> regions;
    private int occurences;

    public WordsAnalysisResult(WordsQuery query, AnalyzedElementReference analyzedElement)
    {
        this.query = query;
        this.analyzedElement = analyzedElement;
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
}
