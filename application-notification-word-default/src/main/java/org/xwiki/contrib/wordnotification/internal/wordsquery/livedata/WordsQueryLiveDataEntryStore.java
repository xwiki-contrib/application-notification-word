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
package org.xwiki.contrib.wordnotification.internal.wordsquery.livedata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.internal.wordsquery.WordsQueryXClassInitializer;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;

/**
 * Dedicated {@link LiveDataEntryStore} for the {@link WordsQueryLiveDataSource}.
 * This component is in charge of performing the actual HQL queries to display the live data.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(WordsQueryLiveDataSource.NAME)
public class WordsQueryLiveDataEntryStore implements LiveDataEntryStore
{
    private static final String CURRENT_DOC_PARAMETER = "currentDoc";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        if (query.getOffset() > Integer.MAX_VALUE) {
            throw new LiveDataException("Offset of the query is too large.");
        }
        Map<String, Object> sourceParameters = query.getSource().getParameters();

        if (!sourceParameters.containsKey(CURRENT_DOC_PARAMETER)) {
            throw new LiveDataException("The parameter 'currentDoc' is mandatory.");
        }

        String className = this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE);
        String currentDoc = String.valueOf(sourceParameters.get(CURRENT_DOC_PARAMETER));
        DocumentReference documentReference = this.documentReferenceResolver.resolve(currentDoc);
        // We need the compact serialization of the doc.
        String docName = this.entityReferenceSerializer.serialize(documentReference);

        LiveData liveData = new LiveData();
        try {
            Query hqlQuery = this.computeQuery(query);
            hqlQuery.bindValue("className", className)
                .bindValue("propertyName", WordsQueryXClassInitializer.QUERY_FIELD)
                .bindValue("docFullName", docName);

            hqlQuery = hqlQuery.setWiki(documentReference.getWikiReference().getName());
            List<String> result = hqlQuery.execute();

            result.forEach(item -> liveData.getEntries().add(Map.of(WordsQueryXClassInitializer.QUERY_FIELD, item)));
            liveData.setCount(result.size());
        } catch (QueryException e) {
            throw new LiveDataException("Error while performing request", e);
        }

        return liveData;
    }

    private Query computeQuery(LiveDataQuery query) throws QueryException
    {
        int offset = query.getOffset().intValue();
        String baseQuery = "select prop.value from XWikiDocument doc, BaseObject obj, StringProperty prop "
            + "where doc.fullName=obj.name and obj.className=:className and prop.id.id=obj.id "
            + "and prop.name=:propertyName and doc.fullName=:docFullName %s order by prop.value %s";

        String paramFilter = "";
        String propValue = "";
        boolean withFilter = false;
        boolean isMatchAll = false;
        if (!query.getFilters().isEmpty()) {
            // We only consider a unique filter as there's only one column
            LiveDataQuery.Filter filter = query.getFilters().get(0);
            isMatchAll = filter.isMatchAll();
            if (filter.isMatchAll()) {
                paramFilter = "and prop.value = :propValue";
            } else {
                paramFilter = "and prop.value like :propValue";
            }
            propValue = String.valueOf(filter.getConstraints().get(0).getValue());
            withFilter = true;
        }

        String sortOrder = "asc";
        if (!query.getSort().isEmpty()) {
            LiveDataQuery.SortEntry sortEntry = query.getSort().get(0);
            if (sortEntry.isDescending()) {
                sortOrder = "desc";
            }
        }

        baseQuery = String.format(baseQuery, paramFilter, sortOrder);
        Query hqlQuery = this.queryManager.createQuery(baseQuery, Query.HQL)
            .setLimit(query.getLimit())
            .setOffset(offset);
        if (withFilter) {
            QueryParameter queryParameter = hqlQuery.bindValue(propValue);
            if (isMatchAll) {
                hqlQuery = queryParameter.literal(propValue).query();
            } else {
                hqlQuery = queryParameter.anyChars().literal(propValue).anyChars().query();
            }
        }
        return hqlQuery;
    }
}
