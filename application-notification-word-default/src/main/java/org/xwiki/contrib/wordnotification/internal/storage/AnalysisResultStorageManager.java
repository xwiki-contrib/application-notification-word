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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.wordnotification.AnalyzedElementReference;
import org.xwiki.contrib.wordnotification.PartAnalysisResult;
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResults;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.user.UserReference;

@Component(roles = AnalysisResultStorageManager.class)
@Singleton
public class AnalysisResultStorageManager implements Initializable
{
    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private SolrClient solrClient;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.solrClient = this.solr.getClient(AnalysisResultSolrCoreInitializer.ANALYSIS_RESULT_SOLR_CORE);
        } catch (SolrException e) {
            throw new InitializationException("Error while getting the solr client", e);
        }
    }

    public void saveAnalysisResults(WordsAnalysisResults wordsAnalysisResult) throws WordsAnalysisException
    {
        List<SolrInputDocument> documents = this.getInputDocumentsFromResult(wordsAnalysisResult);
        try {
            this.solrClient.add(documents);
            this.solrClient.commit();
        } catch (SolrServerException | IOException e) {
            throw new WordsAnalysisException("Error while trying to add documents to Solr core.", e);
        }
    }

    private List<SolrInputDocument> getInputDocumentsFromResult(WordsAnalysisResults wordsAnalysisResult)
    {
        List<SolrInputDocument> result = new ArrayList<>();
        AnalyzedElementReference reference = wordsAnalysisResult.getReference();

        // Common fields
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, new Date(), solrInputDocument);
        this.solrUtils.setString(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, reference.getDocumentReference(),
            DocumentReference.class, solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, reference.getDocumentVersion(),
            solrInputDocument);

        WordsQuery query = wordsAnalysisResult.getQuery();
        this.solrUtils.setString(AnalysisResultSolrCoreInitializer.USER_FIELD, query.getUserReference(),
            UserReference.class, solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query.getQuery(), solrInputDocument);

        String commonIdentifier =
            String.format("%s_%s_%s", this.entityReferenceSerializer.serialize(reference.getDocumentReference()),
                reference.getDocumentVersion(), query.getQuery());

        for (PartAnalysisResult partAnalysisResult : wordsAnalysisResult.getResults()) {
            result.add(
                this.getInputDocumentFromPartAnalysisResult(partAnalysisResult, solrInputDocument, commonIdentifier));
        }

        return result;
    }

    private SolrInputDocument getInputDocumentFromPartAnalysisResult(PartAnalysisResult partAnalysisResult,
        SolrInputDocument commonFields, String commonIdentifier)
    {
        SolrInputDocument inputDocument = new SolrInputDocument(commonFields);

        this.solrUtils.set(AnalysisResultSolrCoreInitializer.REGIONS_FIELD, partAnalysisResult.getRegions()
            .stream().map(this::transformRegionToString)
            .collect(Collectors.toList()), inputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.ENTITY_REFERENCE_FIELD,
            partAnalysisResult.getEntityReference(), inputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.ANALYZER_HINT,
            partAnalysisResult.getAnalyzerHint(), inputDocument);

        String identifier = String.format("%s_%s", commonIdentifier, partAnalysisResult.getAnalyzerHint());
        this.solrUtils.set("id", identifier, inputDocument);

        return inputDocument;
    }

    private String transformRegionToString(Pair<Integer, Integer> region)
    {
        return String.format("(%s,%s)", region.getLeft(), region.getRight());
    }

    private String mapToQuery(Map<String, Object> queryMap)
    {
        StringBuilder result = new StringBuilder();
        Iterator<Map.Entry<String, Object>> iterator = queryMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            result.append("filter(");
            result.append(entry.getKey());
            result.append(":");

            Object value = entry.getValue();
            if (value instanceof UserReference) {
                result.append(this.solrUtils.toFilterQueryString(value, UserReference.class));
            } else if (value instanceof EntityReference) {
                result.append(this.solrUtils.toFilterQueryString(value, EntityReference.class));
            } else if (value != null) {
                result.append(this.solrUtils.toFilterQueryString(value));
            }
            result.append(")");
            if (iterator.hasNext()) {
                result.append(" AND ");
            }
        }

        return result.toString();
    }

    private WordsAnalysisResults transformDocumentsToWordsAnalysisResult(SolrDocumentList solrDocumentList)
        throws WordsAnalysisException
    {
        WordsAnalysisResults results = null;

        for (SolrDocument solrDocument : solrDocumentList) {
            results = this.transformDocumentToPartAnalysisResult(solrDocument, results);
        }
        return results;
    }

    private WordsAnalysisResults transformDocumentToPartAnalysisResult(SolrDocument solrDocument,
        WordsAnalysisResults aggregator) throws WordsAnalysisException
    {
        WordsAnalysisResults wordsAnalysisResult = aggregator;
        if (aggregator == null) {
            DocumentReference documentReference = this.solrUtils.get(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD,
                solrDocument, DocumentReference.class);
            String version = this.solrUtils.get(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, solrDocument);
            String query = this.solrUtils.get(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, solrDocument);
            UserReference userReference =
                this.solrUtils.get(AnalysisResultSolrCoreInitializer.USER_FIELD, solrDocument, UserReference.class);
            WordsQuery wordsQuery = new WordsQuery(query, userReference);
            AnalyzedElementReference reference = new AnalyzedElementReference(documentReference, version);
            wordsAnalysisResult = new WordsAnalysisResults(reference, wordsQuery);
        }

        String analyzerHint = this.solrUtils.get(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, solrDocument);
        EntityReference entityReference = this.solrUtils.get(AnalysisResultSolrCoreInitializer.ENTITY_REFERENCE_FIELD,
            solrDocument, EntityReference.class);
        List<String> serializedRegions =
            this.solrUtils.getList(AnalysisResultSolrCoreInitializer.REGIONS_FIELD, solrDocument);

        PartAnalysisResult partAnalysisResult = new PartAnalysisResult(analyzerHint, entityReference);

        for (String serializedRegion : serializedRegions) {
            partAnalysisResult.addRegion(this.parseSerializedRegion(serializedRegion));
        }

        wordsAnalysisResult.addResult(partAnalysisResult);
        return wordsAnalysisResult;
    }

    private Pair<Integer, Integer> parseSerializedRegion(String serializedRegion) throws WordsAnalysisException
    {
        Pattern pattern = Pattern.compile("^\\((?<left>\\d+),(?<right>\\d+)\\)$");
        Matcher matcher = pattern.matcher(serializedRegion);
        if (matcher.matches()) {
            int left = Integer.parseInt(matcher.group("left"));
            int right = Integer.parseInt(matcher.group("right"));
            return Pair.of(left, right);
        } else {
            throw new WordsAnalysisException(
                String.format("Cannot parse regions from [%s]", serializedRegion));
        }
    }

    public Optional<WordsAnalysisResults> loadAnalysisResults(DocumentReference documentReference, String version,
        WordsQuery wordsQuery) throws WordsAnalysisException
    {
        Map<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, documentReference);
        queryMap.put(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        queryMap.put(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, wordsQuery.getQuery());
        queryMap.put(AnalysisResultSolrCoreInitializer.USER_FIELD, wordsQuery.getUserReference());

        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(this.mapToQuery(queryMap))
            .setStart(0)
            // FIXME: We assume that we won't have more than 100 analyzers but that's not necessarily true:
            // in the future we should loop over pages
            .setRows(100);
        try {
            QueryResponse queryResponse = this.solrClient.query(solrQuery);
            SolrDocumentList results = queryResponse.getResults();
            if (results.getNumFound() > 0) {
                return Optional.of(this.transformDocumentsToWordsAnalysisResult(results));
            }
        } catch (SolrServerException | IOException e) {
            throw new WordsAnalysisException("Error while searching for analysis result", e);
        }
        return Optional.empty();
    }
}
