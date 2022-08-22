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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.xwiki.contrib.wordnotification.WordsAnalysisException;
import org.xwiki.contrib.wordnotification.WordsAnalysisResult;
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

    public void saveAnalysisResults(Set<WordsAnalysisResult> analysisResults) throws WordsAnalysisException
    {
        for (WordsAnalysisResult wordsAnalysisResult : analysisResults) {
            SolrInputDocument document =
                this.getInputDocumentFromResult(wordsAnalysisResult);
            try {
                this.solrClient.add(document);
            } catch (SolrServerException | IOException e) {
                throw new WordsAnalysisException(
                    String.format("Error while trying to add document [%s] to Solr core.", document), e);
            }
        }
        try {
            this.solrClient.commit();
        } catch (SolrServerException | IOException e) {
            throw new WordsAnalysisException("Error when committing changes to solr core", e);
        }
    }

    private SolrInputDocument getInputDocumentFromResult(WordsAnalysisResult wordsAnalysisResult)
    {
        AnalyzedElementReference reference = wordsAnalysisResult.getReference();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.CREATED_DATE_FIELD, new Date(), solrInputDocument);
        this.solrUtils.setString(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, reference.getDocumentReference(),
            DocumentReference.class, solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, reference.getDocumentVersion(),
            solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, wordsAnalysisResult.getAnalyzerHint(),
            solrInputDocument);
        WordsQuery query = wordsAnalysisResult.getQuery();
        this.solrUtils.setString(AnalysisResultSolrCoreInitializer.USER_FIELD, query.getUserReference(),
            UserReference.class, solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, query.getQuery(), solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.OCCURENCES_FIELD, wordsAnalysisResult.getOccurences(),
            solrInputDocument);
        this.solrUtils.set(AnalysisResultSolrCoreInitializer.REGIONS_FIELD, wordsAnalysisResult.getRegions()
            .stream().map(this::transformRegionToString)
            .collect(Collectors.toList()), solrInputDocument);

        String identifier =
            String.format("%s_%s_%s", this.entityReferenceSerializer.serialize(reference.getDocumentReference()),
                reference.getDocumentVersion(), query.getQuery());
        this.solrUtils.set("id", identifier, solrInputDocument);

        return solrInputDocument;
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

    private WordsAnalysisResult transformDocumentToWordsAnalysisResult(SolrDocument solrDocument)
        throws WordsAnalysisException
    {
        DocumentReference documentReference =
            this.solrUtils.get(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, solrDocument, DocumentReference.class);
        String version = this.solrUtils.get(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, solrDocument);
        String analyzerHint = this.solrUtils.get(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, solrDocument);
        String query = this.solrUtils.get(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, solrDocument);
        UserReference userReference =
            this.solrUtils.get(AnalysisResultSolrCoreInitializer.USER_FIELD, solrDocument, UserReference.class);
        int occurences = this.solrUtils.get(AnalysisResultSolrCoreInitializer.OCCURENCES_FIELD, solrDocument);
        List<String> serializedRegions =
            this.solrUtils.getList(AnalysisResultSolrCoreInitializer.REGIONS_FIELD, solrDocument);
        Set<Pair<Integer,Integer>> regions = new HashSet<>();
        for (String serializedRegion : serializedRegions) {
            regions.add(this.parseSerializedRegion(serializedRegion));
        }

        WordsQuery wordsQuery = new WordsQuery(query, userReference);
        AnalyzedElementReference reference = new AnalyzedElementReference(documentReference, version, null);
        WordsAnalysisResult wordsAnalysisResult = new WordsAnalysisResult(reference, wordsQuery, analyzerHint);
        wordsAnalysisResult.setOccurences(occurences);
        if (!regions.isEmpty()) {
            wordsAnalysisResult.setRegions(regions);
        }
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

    public Optional<WordsAnalysisResult> loadAnalysisResults(DocumentReference documentReference, String version,
        String hint, String wordQuery) throws WordsAnalysisException
    {
        Map<String, Object> queryMap = new LinkedHashMap<>();
        queryMap.put(AnalysisResultSolrCoreInitializer.DOCUMENT_FIELD, documentReference);
        queryMap.put(AnalysisResultSolrCoreInitializer.DOCUMENT_VERSION_FIELD, version);
        queryMap.put(AnalysisResultSolrCoreInitializer.ANALYZER_HINT, hint);
        queryMap.put(AnalysisResultSolrCoreInitializer.WORDS_QUERY_FIELD, wordQuery);

        SolrQuery solrQuery = new SolrQuery()
            .addFilterQuery(this.mapToQuery(queryMap))
            .setStart(0)
            .setRows(1);
        try {
            QueryResponse queryResponse = this.solrClient.query(solrQuery);
            SolrDocumentList results = queryResponse.getResults();
            if (results.getNumFound() > 0) {
                SolrDocument solrDocument = results.get(0);
                return Optional.of(this.transformDocumentToWordsAnalysisResult(solrDocument));
            }
        } catch (SolrServerException | IOException e) {
            throw new WordsAnalysisException("Error while searching for analysis result", e);
        }
        return Optional.empty();
    }
}
