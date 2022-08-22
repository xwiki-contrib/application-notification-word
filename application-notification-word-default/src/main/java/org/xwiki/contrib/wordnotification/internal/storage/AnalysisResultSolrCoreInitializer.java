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
package org.xwiki.contrib.wordnotification.internal.storage;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

@Component
@Named(AnalysisResultSolrCoreInitializer.ANALYSIS_RESULT_SOLR_CORE)
@Singleton
public class AnalysisResultSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    public static final String ANALYSIS_RESULT_SOLR_CORE = "wordsAnalysis";

    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String WORDS_QUERY_FIELD = "wordsQuery";
    public static final String ANALYZER_HINT = "hint";
    public static final String USER_FIELD = "user";
    public static final String DOCUMENT_FIELD = "document";
    public static final String DOCUMENT_VERSION_FIELD = "documentVersion";
    public static final String OCCURENCES_FIELD = "occurences";
    public static final String REGIONS_FIELD = "regions";

    private static final long CURRENT_VERSION = 10000000;

    @Override
    protected void createSchema() throws SolrException
    {
        this.addPDateField(CREATED_DATE_FIELD, false, false);
        this.addStringField(WORDS_QUERY_FIELD, false, false);
        this.addStringField(ANALYZER_HINT, false, false);
        this.addStringField(USER_FIELD, false, false);
        this.addStringField(DOCUMENT_FIELD, false, false);
        this.addStringField(DOCUMENT_VERSION_FIELD, false, false);
        this.addPIntField(OCCURENCES_FIELD, false, false);
        this.addStringField(REGIONS_FIELD, true, false);
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        // Nothing to do for now.
    }

    @Override
    protected long getVersion()
    {
        return CURRENT_VERSION;
    }

    @Override
    public boolean isCache()
    {
        return true;
    }
}
