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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.internal.ui.UserProfileUIExtension;
import org.xwiki.contrib.wordnotification.internal.wordsquery.WordsQueryXClassInitializer;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.QueryParameter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

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
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private ContextualLocalizationManager l10n;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private CSRFToken csrfToken;

    private EscapeTool escapeTool = new EscapeTool();

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        Optional<Map<String, Object>> result = Optional.empty();
        EntityReference entityReference =
            this.entityReferenceResolver.resolve(String.valueOf(entryId), EntityType.OBJECT);
        BaseObjectReference baseObjectReference = new BaseObjectReference(entityReference);
        DocumentReference holderDocReference = baseObjectReference.getDocumentReference();
        Integer objectNumber = baseObjectReference.getObjectNumber();

        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(holderDocReference, context);
            BaseObject baseObject =
                document.getXObject(baseObjectReference.getXClassReference(), objectNumber);
            if (baseObject != null) {
                boolean isEditable = this.isEditable(holderDocReference);
                result = Optional.of(Map.of(
                    WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD,
                    this.entityReferenceSerializer.serialize(baseObjectReference),
                    WordsQueryXClassInitializer.QUERY_FIELD,
                    baseObject.getStringValue(WordsQueryXClassInitializer.QUERY_FIELD),
                    WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, isEditable,
                    WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD,
                    getObjectRemoveUrl(document, objectNumber)
                ));
            }
        } catch (XWikiException e) {
            throw new LiveDataException(
                String.format("Error when loading entry with id [%s].", entryId), e);
        }
        return result;
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
        XWikiContext context = this.contextProvider.get();

        boolean isEditable = this.isEditable(documentReference);
        LiveData liveData = new LiveData();
        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);

            Query hqlQuery = this.computeQuery(query);
            hqlQuery.bindValue("className", className)
                .bindValue("propertyName", WordsQueryXClassInitializer.QUERY_FIELD)
                .bindValue("docFullName", docName);

            hqlQuery = hqlQuery.setWiki(documentReference.getWikiReference().getName());
            List<Object[]> result = hqlQuery.execute();
            List<Map<String, Object>> entries = liveData.getEntries();

            result.forEach(item -> {
                int objectNumber = (int) item[0];
                String entryValue = (String) item[1];
                BaseObjectReference baseObjectReference = new BaseObjectReference(
                    WordsQueryXClassInitializer.XCLASS_REFERENCE,
                    objectNumber,
                    documentReference
                );

                entries.add(Map.of(
                    WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD,
                    this.entityReferenceSerializer.serialize(baseObjectReference),
                    WordsQueryXClassInitializer.QUERY_FIELD, entryValue,
                    WordsQueryLiveDataConfigurationProvider.IS_EDITABLE_FIELD, isEditable,
                    WordsQueryLiveDataConfigurationProvider.REMOVE_OBJECT_URL_FIELD,
                    getObjectRemoveUrl(document, objectNumber)
                ));
            });
            liveData.setCount(result.size());
        } catch (QueryException e) {
            throw new LiveDataException("Error while performing request", e);
        } catch (XWikiException e) {
            throw new LiveDataException(
                String.format("Cannot load parent document [%s]", documentReference), e);
        }

        return liveData;
    }

    private String getObjectRemoveUrl(XWikiDocument document, int objectNumber)
    {
        XWikiContext context = this.contextProvider.get();
        String redirectUrl =
            document.getURL("view", this.escapeTool.url(Map.of("category", UserProfileUIExtension.ID)), context);
        Map<String, String> queryStringParameters = Map.of(
            "classname", this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE),
            "classid", String.valueOf(objectNumber),
            "form_token", this.csrfToken.getToken(),
            "xredirect", redirectUrl
        );
        return document.getURL("objectremove", this.escapeTool.url(queryStringParameters), context);
    }

    private Query computeQuery(LiveDataQuery query) throws QueryException
    {
        int offset = query.getOffset().intValue();
        String baseQuery = "select obj.number, prop.value from XWikiDocument doc, BaseObject obj, StringProperty prop "
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
            QueryParameter queryParameter = hqlQuery.bindValue("propValue");
            if (isMatchAll) {
                hqlQuery = queryParameter.literal(propValue).query();
            } else {
                hqlQuery = queryParameter.anyChars().literal(propValue).anyChars().query();
            }
        }
        return hqlQuery;
    }

    private boolean isEditable(DocumentReference currentDoc)
    {
        XWikiContext context = this.contextProvider.get();
        return !context.getWiki().isReadOnly()
            && this.contextualAuthorizationManager.hasAccess(Right.EDIT, currentDoc);
    }

    @Override
    public Optional<Object> save(Map<String, Object> entry) throws LiveDataException
    {
        if (!entry.containsKey(WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD)) {
            throw new LiveDataException("The entry must contain a reference.");
        }
        String serializedObjectRef = (String) entry.get(WordsQueryLiveDataConfigurationProvider.OBJECT_REFERENCE_FIELD);
        EntityReference entityReference =
            this.entityReferenceResolver.resolve(serializedObjectRef, EntityType.OBJECT);
        BaseObjectReference baseObjectReference = new BaseObjectReference(entityReference);

        DocumentReference holderDocReference = baseObjectReference.getDocumentReference();
        if (!isEditable(holderDocReference)) {
            throw new LiveDataException(
                String.format("Current user cannot edit the document [%s].", holderDocReference));
        }
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiDocument document = context.getWiki().getDocument(holderDocReference, context);
            BaseObject baseObject =
                document.getXObject(baseObjectReference.getXClassReference(), baseObjectReference.getObjectNumber());
            if (baseObject == null) {
                throw new LiveDataException(String.format("Cannot load base object with reference [%s]",
                    baseObjectReference));
            }
            baseObject.setStringValue(WordsQueryXClassInitializer.QUERY_FIELD,
                String.valueOf(entry.get(WordsQueryXClassInitializer.QUERY_FIELD)));
            context.getWiki().saveDocument(document,
                this.l10n.getTranslationPlain("wordsNotification.storage.saveQuery"), true, context);
            return Optional.of(serializedObjectRef);
        } catch (XWikiException e) {
            throw new LiveDataException(
                String.format("Error when loading holder document [%s].", holderDocReference), e);
        }
    }
}
