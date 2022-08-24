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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;

@Component
@Singleton
@Named(ContentChangeAnalyzer.HINT)
public class ContentChangeAnalyzer extends AbstractChangeAnalyzer
{
    static final String HINT = "content";

    @Override
    public String getHint()
    {
        return HINT;
    }

    @Override
    public String getTextToAnalyze(DocumentModelBridge document) throws WordsAnalysisException
    {
        // FIXME: this probably needs to be improved to avoid performing matching on wiki syntax.
        return document.getContent();
    }
}
