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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Helper to analyze a specific field of all objects of a specific class contained in a document.
 * Note that this helper only works with String properties.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractXObjectPropertyWordsMentionAnalyzer extends AbstractWordsMentionAnalyzer
{
    private final EntityReference xclassReference;

    private final String xclassProperty;

    /**
     * Constructor taking the reference of the xclass of the objects to analyze and the property to check.
     * @param xclassReference the xclass reference of the xobjects to analyze
     * @param xclassProperty the string property that needs to be checked
     */
    public AbstractXObjectPropertyWordsMentionAnalyzer(EntityReference xclassReference,  String xclassProperty)
    {
        this.xclassReference = xclassReference;
        this.xclassProperty = xclassProperty;
    }

    @Override
    public Map<EntityReference, List<String>> getTextToAnalyze(DocumentModelBridge document)
        throws WordsAnalysisException
    {
        XWikiDocument xWikiDocument = (XWikiDocument) document;
        Map<EntityReference, List<String>> result = new HashMap<>();
        for (BaseObject xObject : xWikiDocument.getXObjects(this.xclassReference)) {
            if (xObject != null) {
                result.put(xObject.getReference(), List.of(xObject.getStringValue(xclassProperty)));
            }
        }
        return result;
    }
}
