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
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.user.UserReference;

/**
 * Solr storage manager of the result analysis.
 * The goal of this component is to allow storing the results for better performance for comparing analysis.
 *
 * @version $Id$
 * @since 1.0
 */
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

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

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

    /**
     * Save the results of the analysis.
     *
     * @param wordsAnalysisResult the results to save.
     * @throws WordsAnalysisException in case of problem to save the results.
     */
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
        DocumentVersionReference reference = wordsAnalysisResult.getReference();

        // Common fields
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, wordsAnalysisResult.getDate(),
            solrInputDocument);
        this.solrUtils.setString(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, reference,
            DocumentReference.class, solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, reference.getVersion(),
            solrInputDocument);

        WordsQuery query = wordsAnalysisResult.getQuery();
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query.getQuery(), solrInputDocument);

        String commonIdentifier =
            String.format("%s_%s_%s", this.entityReferenceSerializer.serialize(reference),
                reference.getVersion(), query.getQuery());

        for (PartAnalysisResult partAnalysisResult : wordsAnalysisResult.getResults()) {
            result.add(
                this.getInputDocumentFromPartAnalysisResult(partAnalysisResult, solrInputDocument, commonIdentifier));
        }

        return result;
    }

    private SolrInputDocument getInputDocumentFromPartAnalysisResult(PartAnalysisResult partAnalysisResult,
        SolrInputDocument commonFields, String commonIdentifier)
    {
        SolrInputDocument inputDocument = new SolrInputDocument(new LinkedHashMap<>(commonFields));

        List<String> serializedRegions = partAnalysisResult.getRegions()
            .stream().map(this::transformRegionToString)
            .collect(Collectors.toList());
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.REGIONS_FIELD, serializedRegions, inputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.ANALYZER_HINT,
            partAnalysisResult.getAnalyzerHint(), inputDocument);

        String identifier = String.format("%s_%s", commonIdentifier, partAnalysisResult.getAnalyzerHint());
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.SOLR_FIELD_ID, identifier, inputDocument);

        return inputDocument;
    }

    private String transformRegionToString(WordsMentionLocalization localization)
    {
        return String.format("(%s,%s,%s,%s,%s)",
            localization.getEntityReference().getType().name(),
            this.entityReferenceSerializer.serialize(localization.getEntityReference()),
            localization.getPositionInList(),
            localization.getRegionStart(),
            localization.getRegionEnd());
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
            } else {
                result.append(this.solrUtils.toFilterQueryString(value));
            }
            result.append(")");
            if (iterator.hasNext()) {
                result.append(" AND ");
            }
        }

        return result.toString();
    }

    private void transformDocumentToPartAnalysisResult(SolrDocument solrDocument, WordsAnalysisResults aggregator)
        throws WordsAnalysisException
    {
        String analyzerHint = this.solrUtils.get(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, solrDocument);
        List<String> serializedRegions =
            this.solrUtils.getList(AnalysisResultSolrCoreInitializer.REGIONS_FIELD, solrDocument);

        PartAnalysisResult partAnalysisResult = new PartAnalysisResult(analyzerHint);

        for (String serializedRegion : serializedRegions) {
            partAnalysisResult.addRegion(this.parseSerializedRegion(serializedRegion));
        }

        aggregator.addResult(partAnalysisResult);
    }

    private WordsMentionLocalization parseSerializedRegion(String serializedRegion) throws WordsAnalysisException
    {
        Pattern pattern = Pattern.compile("^\\("
            + "(?<entityType>\\w+),"
            + "(?<entityReference>.+),"
            + "(?<positionInList>\\d+),"
            + "(?<regionStart>\\d+),"
            + "(?<regionEnd>\\d+)"
            + "\\)$");
        Matcher matcher = pattern.matcher(serializedRegion);
        if (matcher.matches()) {
            EntityType entityType = EntityType.valueOf(matcher.group("entityType"));
            EntityReference entityReference =
                this.entityReferenceResolver.resolve(matcher.group("entityReference"), entityType);
            int positionInList = Integer.parseInt(matcher.group("positionInList"));
            int regionStart = Integer.parseInt(matcher.group("regionStart"));
            int regionEnd = Integer.parseInt(matcher.group("regionEnd"));
            return new WordsMentionLocalization(entityReference, positionInList, regionStart, regionEnd);
        } else {
            throw new WordsAnalysisException(
                String.format("Cannot parse regions from [%s]", serializedRegion));
        }
    }

    /**
     * Search and retrieve previous result analysis from Solr.
     *
     * @param documentVersionReference the reference of the document version results to look for
     * @param wordsQuery the words query to look for
     * @return an {@link Optional#empty()} if no result have been found else an {@link Optional} containing the
     *         {@link WordsAnalysisResults} instance with all results stored
     * @throws WordsAnalysisException in case of problem when performing the query
     */
    public Optional<WordsAnalysisResults> loadAnalysisResults(DocumentVersionReference documentVersionReference,
        WordsQuery wordsQuery) throws WordsAnalysisException
    {
        Map<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, documentVersionReference);
        queryMap.put(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, documentVersionReference.getVersion());
        queryMap.put(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, wordsQuery.getQuery());

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
                Date creationDate = this.solrUtils.get(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD,
                    results.get(0));
                WordsAnalysisResults wordsAnalysisResult =
                    new WordsAnalysisResults(documentVersionReference, wordsQuery, creationDate);

                for (SolrDocument result : results) {
                    this.transformDocumentToPartAnalysisResult(result, wordsAnalysisResult);
                }

                return Optional.of(wordsAnalysisResult);
            }
        } catch (SolrServerException | IOException e) {
            throw new WordsAnalysisException("Error while searching for analysis result", e);
        }
        return Optional.empty();
    }
}
