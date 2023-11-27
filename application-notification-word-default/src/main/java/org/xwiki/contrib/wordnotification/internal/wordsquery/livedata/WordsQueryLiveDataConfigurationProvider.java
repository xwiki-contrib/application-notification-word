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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.wordnotification.internal.wordsquery.WordsQueryXClassInitializer;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataActionDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Configuration of the {@link WordsQueryLiveDataSource}.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
@Named(WordsQueryLiveDataSource.NAME)
public class WordsQueryLiveDataConfigurationProvider implements Provider<LiveDataConfiguration>
{
    static final String IS_EDITABLE_FIELD = "isEditable";
    static final String OBJECT_REFERENCE_FIELD = "objectReference";

    static final String REMOVE_OBJECT_URL_FIELD = "removeObjectUrl";
    private static final String ACTIONS_FIELD = "actions";

    private static final String REMOVE = "remove";
    private static final String STRING_TYPE = "String";

    @Inject
    private ContextualLocalizationManager l10n;

    @Inject
    private IconManager iconManager;

    @Inject
    private Logger logger;

    @Override
    public LiveDataConfiguration get()
    {
        LiveDataConfiguration input = new LiveDataConfiguration();
        LiveDataMeta meta = new LiveDataMeta();
        LiveDataPaginationConfiguration pagination = new LiveDataPaginationConfiguration();
        pagination.setShowPageSizeDropdown(true);
        meta.setPagination(pagination);
        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty(OBJECT_REFERENCE_FIELD);
        input.setMeta(meta);
        meta.setEntryDescriptor(entryDescriptor);

        LiveDataActionDescriptor removeAction = new LiveDataActionDescriptor();
        removeAction.setName(this.l10n.getTranslationPlain("wordsNotification.settings.remove"));
        removeAction.setId(REMOVE);
        removeAction.setAllowProperty(IS_EDITABLE_FIELD);
        removeAction.setUrlProperty(REMOVE_OBJECT_URL_FIELD);
        try {
            removeAction.setIcon(this.iconManager.getMetaData(REMOVE));
        } catch (IconException e) {
            this.logger.error("Error while getting icon for the remove action", e);
        }
        meta.setActions(List.of(removeAction));

        meta.setPropertyDescriptors(List.of(
            getObjectReferenceDescriptor(),
            getQueryDescriptor(),
            getIsEditableDescriptor(),
            getRemoveObjectURLDescriptor(),
            getActionDescriptor()
        ));

        return input;
    }

    private LiveDataPropertyDescriptor getObjectReferenceDescriptor()
    {
        LiveDataPropertyDescriptor objectReferenceDescriptor = new LiveDataPropertyDescriptor();
        objectReferenceDescriptor.setId(OBJECT_REFERENCE_FIELD);
        objectReferenceDescriptor.setType(STRING_TYPE);
        objectReferenceDescriptor.setVisible(false);
        objectReferenceDescriptor.setEditable(false);
        objectReferenceDescriptor.setSortable(false);
        objectReferenceDescriptor.setFilterable(false);

        return objectReferenceDescriptor;
    }

    private LiveDataPropertyDescriptor getRemoveObjectURLDescriptor()
    {
        LiveDataPropertyDescriptor removeObjectUrlDescriptor = new LiveDataPropertyDescriptor();
        removeObjectUrlDescriptor.setId(REMOVE_OBJECT_URL_FIELD);
        removeObjectUrlDescriptor.setType(STRING_TYPE);
        removeObjectUrlDescriptor.setVisible(false);
        removeObjectUrlDescriptor.setEditable(false);
        removeObjectUrlDescriptor.setSortable(false);
        removeObjectUrlDescriptor.setFilterable(false);

        return removeObjectUrlDescriptor;
    }

    private LiveDataPropertyDescriptor getQueryDescriptor()
    {
        LiveDataPropertyDescriptor queryDescriptor = new LiveDataPropertyDescriptor();
        queryDescriptor.setName(this.l10n.getTranslationPlain("wordsNotification.livedata.query"));
        queryDescriptor.setId(WordsQueryXClassInitializer.QUERY_FIELD);
        queryDescriptor.setType(STRING_TYPE);
        queryDescriptor.setVisible(true);
        queryDescriptor.setEditable(true);
        queryDescriptor.setSortable(true);
        queryDescriptor.setFilterable(true);

        return queryDescriptor;
    }

    private LiveDataPropertyDescriptor getIsEditableDescriptor()
    {
        LiveDataPropertyDescriptor isEditableDescriptor = new LiveDataPropertyDescriptor();
        isEditableDescriptor.setName(IS_EDITABLE_FIELD);
        isEditableDescriptor.setId(IS_EDITABLE_FIELD);
        isEditableDescriptor.setType("Boolean");
        isEditableDescriptor.setVisible(false);
        isEditableDescriptor.setEditable(false);
        isEditableDescriptor.setSortable(false);
        isEditableDescriptor.setFilterable(false);

        return isEditableDescriptor;
    }

    private LiveDataPropertyDescriptor getActionDescriptor()
    {
        LiveDataPropertyDescriptor actionDescriptor = new LiveDataPropertyDescriptor();
        actionDescriptor.setName(this.l10n.getTranslationPlain("wordsNotification.livedata.action"));
        actionDescriptor.setId(ACTIONS_FIELD);
        LiveDataPropertyDescriptor.DisplayerDescriptor displayer =
            new LiveDataPropertyDescriptor.DisplayerDescriptor(ACTIONS_FIELD);
        displayer.setParameter(ACTIONS_FIELD, List.of(REMOVE));
        actionDescriptor.setDisplayer(displayer);
        actionDescriptor.setVisible(true);
        actionDescriptor.setEditable(false);
        actionDescriptor.setSortable(false);
        actionDescriptor.setFilterable(false);
        return actionDescriptor;
    }
}
