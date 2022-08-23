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
package org.xwiki.contrib.wordnotification.internal.wordsquery;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.wordnotification.WordsQuery;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

@Component(roles = WordsQueryCache.class)
@Singleton
public class WordsQueryCache implements Initializable, Disposable
{
    private Cache<Set<WordsQuery>> queryCache;

    private Cache<Set<UserReference>> usersWithQueriesCache;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration queryCacheConfiguration = new LRUCacheConfiguration("application-notification-word.query");
        CacheConfiguration usersCacheConfiguration = new LRUCacheConfiguration("application-notification-word.users");
        try {
            this.queryCache = this.cacheManager.createNewCache(queryCacheConfiguration);
            this.usersWithQueriesCache = this.cacheManager.createNewCache(usersCacheConfiguration);
        } catch (CacheException e) {
            throw new InitializationException("Error while creating the cache for the words queries", e);
        }
    }

    private String getQueryCacheKey(UserReference userReference)
    {
        return this.userReferenceSerializer.serialize(userReference);
    }

    public Optional<Set<WordsQuery>> getWordsQueries(UserReference userReference)
    {
        Optional<Set<WordsQuery>> result = Optional.empty();
        Set<WordsQuery> wordsQueries = this.queryCache.get(this.getQueryCacheKey(userReference));
        if (wordsQueries != null) {
            result = Optional.of(wordsQueries);
        }
        return result;
    }

    public void setWordsQueries(UserReference userReference, Set<WordsQuery> wordsQueries)
    {
        this.queryCache.set(getQueryCacheKey(userReference), wordsQueries);
    }

    public Optional<Set<UserReference>> getUserReferences(WikiReference wikiReference)
    {
        Optional<Set<UserReference>> result = Optional.empty();
        Set<UserReference> userReferences = this.usersWithQueriesCache.get(wikiReference.getName());
        if (userReferences != null) {
            result = Optional.of(userReferences);
        }
        return result;
    }

    public void setUserReferences(Set<UserReference> userReferences, WikiReference wikiReference)
    {
        this.usersWithQueriesCache.set(wikiReference.getName(), userReferences);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.queryCache.dispose();
        this.usersWithQueriesCache.dispose();
    }

    public void invalidateQueriesFrom(UserReference userReference)
    {
        this.queryCache.remove(getQueryCacheKey(userReference));
    }

    public void invalidateUsersWithQueriesFrom(WikiReference wikiReference)
    {
        this.usersWithQueriesCache.remove(wikiReference.getName());
    }
}
