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
package org.xwiki.contrib.wordnotification.test.po;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.PreviewableEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Helper class to allow waiting on a tasks from TaskConsumer to be done.
 *
 * @version $Id$
 * @since 1.0
 */
public final class TaskConsumerUtil extends BaseElement
{
    /**
     * Default instance.
     */
    public static final TaskConsumerUtil INSTANCE = new TaskConsumerUtil();

    private TaskConsumerUtil()
    {
    }

    /**
     * Wait until all tasks for WordsSearch are processed.
     * Be aware that this method needs a user with script right to be executed.
     * No page is saved by this method: it relies entirely on preview.
     */
    public void waitUntilWordsNotificationTaskIsDone()
    {
        String uuid = UUID.randomUUID().toString();
        String checkingScript = String.format("{{velocity}}"
            + "Task check %s $!services.taskConsumer.getQueueSizePerType.get('WordsSearch')"
            + "{{/velocity}}", uuid);
        DocumentReference fakePage = new DocumentReference("xwiki", "NotExistingSpace", "TaskConsumerUtil");
        String expectedPrefix = String.format("Task check %s", uuid);

        getDriver().waitUntilCondition(condition -> {
            getUtil().gotoPage(fakePage, "edit", "editor=wiki");
            WikiEditPage editPage = new WikiEditPage();
            editPage.setContent(checkingScript);
            editPage.clickPreview();
            PreviewEditPage previewEditPage = new PreviewEditPage(new PreviewableEditPage());
            String content = StringUtils.strip(previewEditPage.getContent());
            boolean result = StringUtils.equals(content, expectedPrefix)
                || StringUtils.equals(content, expectedPrefix + " 0");

            ViewPage viewPage = previewEditPage.clickCancel();
            viewPage.waitUntilPageIsReady();
            return result;
        });
    }
}
