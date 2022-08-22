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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;

public class AnalyzedElementReference
{
    private final DocumentReference documentReference;
    private final String documentVersion;

    private final String previousVersion;

    public AnalyzedElementReference(DocumentReference documentReference, String version, String previousVersion)
    {
        this.documentReference = documentReference;
        this.documentVersion = version;
        this.previousVersion = previousVersion;
    }

    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    public String getDocumentVersion()
    {
        return documentVersion;
    }

    public String getPreviousVersion()
    {
        return previousVersion;
    }

    public boolean isFirstVersion()
    {
        return StringUtils.isEmpty(previousVersion);
    }
}
