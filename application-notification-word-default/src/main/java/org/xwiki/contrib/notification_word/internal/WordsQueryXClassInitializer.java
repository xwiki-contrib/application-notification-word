package org.xwiki.contrib.notification_word.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

@Component
@Named("WordsQueryXClassInitializer")
@Singleton
public class WordsQueryXClassInitializer extends AbstractMandatoryClassInitializer
{
    static final LocalDocumentReference XCLASS_REFERENCE = new LocalDocumentReference(
        List.of("NotificationWords", "Code"), "WordsQueryXClass"
    );

    static final String QUERY_FIELD = "query";

    public WordsQueryXClassInitializer()
    {
        super(XCLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(QUERY_FIELD, QUERY_FIELD, 255);
    }
}
