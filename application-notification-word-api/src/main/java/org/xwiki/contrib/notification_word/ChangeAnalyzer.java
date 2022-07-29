package org.xwiki.contrib.notification_word;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

@Role
public interface ChangeAnalyzer
{
    WordsAnalysisResult analyze(DocumentReference documentReference, String version, WordsQuery wordsQuery)
        throws WordsAnalysisException;
}
