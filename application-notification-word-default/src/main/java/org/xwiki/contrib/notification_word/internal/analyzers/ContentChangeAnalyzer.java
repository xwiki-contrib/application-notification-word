package org.xwiki.contrib.notification_word.internal.analyzers;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.notification_word.AnalyzedElementReference;
import org.xwiki.contrib.notification_word.ChangeAnalyzer;
import org.xwiki.contrib.notification_word.WordsAnalysisException;
import org.xwiki.contrib.notification_word.WordsAnalysisResult;
import org.xwiki.contrib.notification_word.WordsQuery;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Singleton
@Named(ContentChangeAnalyzer.HINT)
public class ContentChangeAnalyzer implements ChangeAnalyzer
{
    static final String HINT = "content";

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Override
    public WordsAnalysisResult analyze(DocumentReference documentReference, String version, WordsQuery wordsQuery)
        throws WordsAnalysisException
    {
        try {
            XWikiDocument document = this.documentRevisionProvider.getRevision(documentReference, version);

            AnalyzedElementReference reference =
                new AnalyzedElementReference(documentReference, version, documentReference, HINT);
            WordsAnalysisResult result = new WordsAnalysisResult(wordsQuery, reference);

            String query = wordsQuery.getQuery();
            Matcher matcher = Pattern.compile(query).matcher(document.getContent());
            Set<Pair<Integer, Integer>> regions = new HashSet<>();
            while (matcher.find()) {
                regions.add(Pair.of(matcher.start(), matcher.end()));
            }
            result.setRegions(regions);

            return result;
        } catch (XWikiException e) {
            throw new WordsAnalysisException(
                String.format("Error when loading the document [%s] with version"
                    + " [%s] to analyze its content", documentReference, version), e);
        }
    }
}
