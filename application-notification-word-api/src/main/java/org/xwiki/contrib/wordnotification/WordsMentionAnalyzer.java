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

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Dedicated role for performing an analysis of a document.
 * Various component might implement this role to perform analysis of different parts of the documents. Each analyzer
 * should document how it will produce a {@link WordsMentionLocalization}.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
@Role
public interface WordsMentionAnalyzer
{
    /**
     * Perform analysis of a part of the document.
     *
     * @param document the document to analyze
     * @param wordsQuery the query to look for in the document
     * @return the result of the analysis
     * @throws WordsAnalysisException if something went wrong during the analysis
     */
    PartAnalysisResult analyze(DocumentModelBridge document, WordsQuery wordsQuery) throws WordsAnalysisException;
}
