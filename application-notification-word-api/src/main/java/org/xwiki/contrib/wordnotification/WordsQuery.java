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
package org.xwiki.contrib.wordnotification;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.user.UserReference;

public class WordsQuery
{
    private final String query;
    private final UserReference userReference;

    public WordsQuery(String query, UserReference userReference)
    {
        this.query = query;
        this.userReference = userReference;
    }

    public String getQuery()
    {
        return query;
    }

    public UserReference getUserReference()
    {
        return userReference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WordsQuery that = (WordsQuery) o;

        return new EqualsBuilder().append(query, that.query)
            .append(userReference, that.userReference).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(query).append(userReference).toHashCode();
    }
}
