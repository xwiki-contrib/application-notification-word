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

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResults;
import org.xwiki.contrib.wordnotification.WordsMentionLocalization;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentVersionReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.Converter;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.internal.DefaultSolrUtils;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AnalysisResultStorageManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@ComponentList({ DefaultSolrUtils.class })
@ComponentTest
class AnalysisResultStorageManagerTest
{
    @InjectMockComponents
    private AnalysisResultStorageManager storageManager;

    @MockComponent
    private Solr solr;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @MockComponent
    private ConverterManager converterManager;

    private SolrClient solrClient;

    @BeforeEach
    void beforeEach() throws SolrException, InitializationException
    {
        this.solrClient = mock(SolrClient.class);
        when(this.solr.getClient(AnalysisResultSolrCoreInitializer.ANALYSIS_RESULT_SOLR_CORE))
            .thenReturn(this.solrClient);
        this.storageManager.initialize();
        Converter<Object> documentReferenceConverter = mock(Converter.class);
        when(this.converterManager.getConverter(DocumentReference.class)).thenReturn(documentReferenceConverter);
        when(this.converterManager.getConverter(EntityReference.class)).thenReturn(documentReferenceConverter);
        when(documentReferenceConverter.convert(eq(String.class), any()))
            .then(invocationOnMock -> invocationOnMock.getArgument(1).toString());
    }

    @Test
    void saveAnalysisResults() throws WordsAnalysisException, SolrServerException, IOException
    {
        UserReference userReference = mock(UserReference.class, "userReference");
        String query = "myQuery";
        WordsQuery wordsQuery = new WordsQuery(query, userReference);

        DocumentReference documentReference = new DocumentReference("xwiki", "Foo", "Bar");
        String version = "2.3";
        Date creationDate = new Date(489);
        DocumentVersionReference documentVersionReference = new DocumentVersionReference(documentReference, version);
        WordsAnalysisResults wordsAnalysisResults =
            new WordsAnalysisResults(documentVersionReference, wordsQuery, creationDate);

        String serializedReference = "xwiki:Foo.Bar";
        when(this.entityReferenceSerializer.serialize(documentVersionReference)).thenReturn(serializedReference);
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn(serializedReference);

        PartAnalysisResult titleAnalysis = new PartAnalysisResult("title");
        titleAnalysis.addRegion(new WordsMentionLocalization(documentReference, 0, 12, 15));
        wordsAnalysisResults.addResult(titleAnalysis);

        PartAnalysisResult contentAnalysis = new PartAnalysisResult("content");
        contentAnalysis.addRegion(new WordsMentionLocalization(documentReference, 3, 4, 7));
        contentAnalysis.addRegion(new WordsMentionLocalization(documentReference, 3, 16, 19));
        wordsAnalysisResults.addResult(contentAnalysis);

        EntityReference xobject1 = new EntityReference("xobject1", EntityType.OBJECT);
        when(this.entityReferenceSerializer.serialize(xobject1)).thenReturn("xobject1");
        EntityReference xobject2 = new EntityReference("xobject2", EntityType.OBJECT);
        when(this.entityReferenceSerializer.serialize(xobject2)).thenReturn("xobject2");

        PartAnalysisResult commentsAnalysis = new PartAnalysisResult("comments");
        commentsAnalysis.addRegion(new WordsMentionLocalization(xobject1, 0, 0, 745));
        commentsAnalysis.addRegion(new WordsMentionLocalization(xobject2, 14, 16, 19878));
        wordsAnalysisResults.addResult(commentsAnalysis);

        String expectedCommonIdentifier = String.format("%s_%s_%s", serializedReference, version, query);

        SolrInputDocument solrInputDocument1 = new SolrInputDocument();
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, creationDate);
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, serializedReference);
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query);
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(DOCUMENT," + serializedReference + ",0,12,15)");
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, "title");
        solrInputDocument1.addField(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID,
            expectedCommonIdentifier + "_title");

        SolrInputDocument solrInputDocument2 = new SolrInputDocument();
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, creationDate);
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, serializedReference);
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query);
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(DOCUMENT," + serializedReference + ",3,4,7)");
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(DOCUMENT," + serializedReference + ",3,16,19)");
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, "content");
        solrInputDocument2.addField(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID,
            expectedCommonIdentifier + "_content");

        SolrInputDocument solrInputDocument3 = new SolrInputDocument();
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, creationDate);
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, serializedReference);
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query);
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(OBJECT,xobject1,0,0,745)");
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(OBJECT,xobject2,14,16,19878)");
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, "comments");
        solrInputDocument3.addField(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID,
            expectedCommonIdentifier + "_comments");

        when(this.solrClient.add(any(Collection.class))).then(invocationOnMock -> {
            List<SolrInputDocument> documents = invocationOnMock.getArgument(0);
            assertEquals(3, documents.size());

            // We rely on toString since there's no proper equals method to rely on in SolrInputDocument
            assertEquals(solrInputDocument1.toString(), documents.get(0).toString());
            assertEquals(solrInputDocument2.toString(), documents.get(1).toString());
            assertEquals(solrInputDocument3.toString(), documents.get(2).toString());
            return null;
        });
        this.storageManager.saveAnalysisResults(wordsAnalysisResults);
        verify(this.solrClient).add(any(Collection.class));
        verify(this.solrClient).commit();
    }

    @Test
    void loadAnalysisResults() throws WordsAnalysisException, SolrServerException, IOException
    {
        UserReference userReference = mock(UserReference.class, "userReference");
        String query = "myQuery";
        WordsQuery wordsQuery = new WordsQuery(query, userReference);

        DocumentReference documentReference = new DocumentReference("xwiki", "Foo", "Bar");
        String version = "2.3";
        Date creationDate = new Date(489);
        DocumentVersionReference documentVersionReference = new DocumentVersionReference(documentReference, version);
        String serializedReference = "xwiki:Foo.Bar";
        when(this.entityReferenceResolver.resolve(serializedReference, EntityType.DOCUMENT))
            .thenReturn(documentReference);

        String expectedQuery =
            "filter(document:xwiki\\:Foo.Bar) AND filter(documentVersion:2.3) AND filter(wordsQuery:myQuery)";

        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrQuery expectedSolrQuery = new SolrQuery()
            .addFilterQuery(expectedQuery)
            .setStart(0)
            .setRows(100);

        when(this.solrClient.query(any())).then(invocationOnMock -> {
            SolrQuery solrQuery = invocationOnMock.getArgument(0);
            assertEquals(expectedSolrQuery.toString(), solrQuery.toString());
            return queryResponse;
        });

        SolrDocumentList result = mock(SolrDocumentList.class);
        when(queryResponse.getResults()).thenReturn(result);
        when(result.getNumFound()).thenReturn(0L);

        assertEquals(Optional.empty(), this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery));
        verify(this.solrClient).query(any());

        String commonIdentifier = String.format("%s_%s_%s", serializedReference, version, query);

        List<SolrDocument> inputDocuments = new LinkedList<>();
        SolrDocument solrDocument1 = new SolrDocument();
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, creationDate);
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, serializedReference);
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query);
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(DOCUMENT," + serializedReference + ",0,12,15)");
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, "title");
        solrDocument1.addField(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID,
            commonIdentifier + "_title");
        inputDocuments.add(solrDocument1);

        SolrDocument solrDocument2 = new SolrDocument();
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, creationDate);
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, serializedReference);
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query);
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(DOCUMENT," + serializedReference + ",3,4,7)");
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(DOCUMENT," + serializedReference + ",3,16,19)");
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, "content");
        solrDocument2.addField(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID,
            commonIdentifier + "_content");
        inputDocuments.add(solrDocument2);

        EntityReference xobject1 = new EntityReference("xobject1", EntityType.OBJECT);
        when(this.entityReferenceResolver.resolve("xobject1", EntityType.OBJECT)).thenReturn(xobject1);
        EntityReference xobject2 = new EntityReference("xobject2", EntityType.OBJECT);
        when(this.entityReferenceResolver.resolve("xobject2", EntityType.OBJECT)).thenReturn(xobject2);

        SolrDocument solrDocument3 = new SolrDocument();
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, creationDate);
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, serializedReference);
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query);
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(OBJECT,xobject1,0,0,745)");
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.REGIONS_FIELD,
            "(OBJECT,xobject2,14,16,19878)");
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, "comments");
        solrDocument3.addField(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID,
            commonIdentifier + "_comments");
        inputDocuments.add(solrDocument3);

        result = new SolrDocumentList();
        result.addAll(inputDocuments);
        result.setNumFound(inputDocuments.size());
        when(queryResponse.getResults()).thenReturn(result);

        WordsAnalysisResults expectedWordsAnalysisResult =
            new WordsAnalysisResults(documentVersionReference, wordsQuery, creationDate);

        PartAnalysisResult titleAnalysis = new PartAnalysisResult("title");
        titleAnalysis.addRegion(new WordsMentionLocalization(documentReference, 0, 12, 15));
        expectedWordsAnalysisResult.addResult(titleAnalysis);

        PartAnalysisResult contentAnalysis = new PartAnalysisResult("content");
        contentAnalysis.addRegion(new WordsMentionLocalization(documentReference, 3, 4, 7));
        contentAnalysis.addRegion(new WordsMentionLocalization(documentReference, 3, 16, 19));
        expectedWordsAnalysisResult.addResult(contentAnalysis);

        PartAnalysisResult commentsAnalysis = new PartAnalysisResult("comments");
        commentsAnalysis.addRegion(new WordsMentionLocalization(xobject1, 0, 0, 745));
        commentsAnalysis.addRegion(new WordsMentionLocalization(xobject2, 14, 16, 19878));
        expectedWordsAnalysisResult.addResult(commentsAnalysis);

        assertEquals(Optional.of(expectedWordsAnalysisResult),
            this.storageManager.loadAnalysisResults(documentVersionReference, wordsQuery));
    }
}