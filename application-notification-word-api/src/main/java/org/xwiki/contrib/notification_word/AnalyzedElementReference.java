package org.xwiki.contrib.notification_word;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

public class AnalyzedElementReference
{
    private final DocumentReference documentReference;
    private final String documentVersion;
    private final EntityReference entityReference;
    private final String analyzerHint;

    public AnalyzedElementReference(DocumentReference documentReference, String version, EntityReference entityReference,
        String analyzerHint)
    {
        this.documentReference = documentReference;
        this.documentVersion = version;
        this.entityReference = entityReference;
        this.analyzerHint = analyzerHint;
    }
}
