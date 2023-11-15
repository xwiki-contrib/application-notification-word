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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Helper component for performing analysis of a text.
 * Specifically this component implementation should perform all needed transformation of the query to properly analyze
 * the different lines of text and creates the localization result with found regions matching the query.
 * Wildcards, jokers and escape should be handled in the query.
 *
 * @version $Id$
 * @since 1.1
 */
@Role
public interface PatternAnalysisHelper
{
    /**
     * Perform analysis and return the regions of the text matching the query.
     *
     * @param query the query to use for performing the analysis
     * @param textsToAnalyze the text to analyze
     * @param localization the localization used for creation of the {@link WordsMentionLocalization}
     * @return a list of localization where the query has been found, or an empty list if it has not been found
     */
    List<WordsMentionLocalization> getRegions(String query, List<String> textsToAnalyze, EntityReference localization);
}
