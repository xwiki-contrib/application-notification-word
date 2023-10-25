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
package org.xwiki.contrib.wordnotification.internal.analyzers;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default analyzer for document's tags.
 * Note that the analyzer uses the actual API of XWikiDocument for retrieving tags and doesn't use the xobjects, so the
 * {@link org.xwiki.contrib.wordnotification.WordsMentionLocalization} won't contain the reference of the objects but
 * only the reference of the document.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named(TagsWordsMentionAnalyzer.HINT)
public class TagsWordsMentionAnalyzer extends AbstractWordsMentionAnalyzer
{
    static final String HINT = "tags";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public String getHint()
    {
        return HINT;
    }

    @Override
    public Map<EntityReference, List<String>> getTextToAnalyze(DocumentModelBridge document)
        throws WordsAnalysisException
    {
        XWikiDocument xWikiDocument = (XWikiDocument) document;
        List<String> tagsList = xWikiDocument.getTagsList(this.contextProvider.get());

        return Map.of(document.getDocumentReference(), tagsList);
    }
}
