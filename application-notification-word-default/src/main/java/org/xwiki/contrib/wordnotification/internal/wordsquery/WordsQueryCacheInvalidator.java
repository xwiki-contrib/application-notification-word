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
package org.xwiki.contrib.wordnotification.internal.wordsquery;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

@Component
@Named(WordsQueryCacheInvalidator.NAME)
@Singleton
public class WordsQueryCacheInvalidator extends AbstractEventListener
{
    static final String NAME = "WordsQueryCacheInvalidator";

    private static final RegexEntityReference WORDS_QUERY_XCLASS =
        BaseObjectReference.any(WordsQueryXClassInitializer.XCLASS_REFERENCE.toString());

    private static final List<Event> EVENT_LIST = List.of(
        new XObjectDeletedEvent(WORDS_QUERY_XCLASS),
        new XObjectAddedEvent(WORDS_QUERY_XCLASS),
        new XObjectUpdatedEvent(WORDS_QUERY_XCLASS)
    );

    @Inject
    private WordsQueryCache wordsQueryCache;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    public WordsQueryCacheInvalidator()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument sourceDoc = (XWikiDocument) source;
        if (sourceDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE) != null) {
            DocumentReference documentReference = sourceDoc.getDocumentReference();
            UserReference userReference =
                this.documentReferenceUserReferenceResolver.resolve(documentReference);
            this.wordsQueryCache.invalidateQueriesFrom(userReference);

            if (!(event instanceof XObjectUpdatedEvent)) {
                this.wordsQueryCache.invalidateUsersWithQueriesFrom(documentReference.getWikiReference());
            }
        }
    }
}
