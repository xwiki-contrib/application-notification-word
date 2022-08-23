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
package org.xwiki.contrib.wordnotification.internal.ui;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

@Component
@Singleton
@Named(UserProfileUIExtension.ID)
public class UserProfileUIExtension implements UIExtension
{
    static final String ID = "notification.word.default.userprofile";

    @Inject
    private ContextualLocalizationManager localization;

    @Inject
    private TemplateManager templates;

    @Inject
    private IconManager iconManager;

    @Inject
    private Logger logger;

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.plaftorm.user.profile.menu";
    }

    @Override
    public Map<String, String> getParameters()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("id", ID);
        try {
            parameters.put("icon", this.iconManager.renderHTML("eye"));
        } catch (IconException e) {
            this.logger.error("Error when rendering eye icon: [{}].", ExceptionUtils.getRootCauseMessage(e));
        }
        parameters.put("name", this.localization.getTranslationPlain("notificationWord.user.menu"));
        parameters.put("priority", "1000");
        return parameters;
    }

    @Override
    public Block execute()
    {
        return this.templates.executeNoException("notificationWord/userprofile.vm");
    }
}
