package org.xwiki.contrib.notification_word.internal;

import java.util.Collections;
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
import org.xwiki.contrib.notification_word.WordsQuery;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

@Component(roles = WordsQueryCache.class)
@Singleton
public class WordsQueryCache implements Initializable, Disposable
{
    private Cache<Set<WordsQuery>> queryCache;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration cacheConfiguration = new LRUCacheConfiguration("application-notification-word.query");
        try {
            this.queryCache = this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            throw new InitializationException("Error while creating the cache for the words queries", e);
        }
    }

    private String getCacheKey(UserReference userReference)
    {
        return this.userReferenceSerializer.serialize(userReference);
    }

    public Optional<Set<WordsQuery>> getWordsQueries(UserReference userReference)
    {
        Optional<Set<WordsQuery>> result = Optional.empty();
        Set<WordsQuery> wordsQueries = this.queryCache.get(this.getCacheKey(userReference));
        if (wordsQueries != null) {
            result = Optional.of(wordsQueries);
        }
        return result;
    }

    public void setWordsQueries(UserReference userReference, Set<WordsQuery> wordsQueries)
    {
        this.queryCache.set(getCacheKey(userReference), wordsQueries);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.queryCache.dispose();
    }
}
