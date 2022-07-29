package org.xwiki.contrib.notification_word;

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
