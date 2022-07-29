package org.xwiki.contrib.notification_word.internal;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.notification_word.ChangeAnalyzer;
import org.xwiki.contrib.notification_word.WordsAnalysisException;
import org.xwiki.contrib.notification_word.WordsAnalysisResult;
import org.xwiki.contrib.notification_word.WordsQuery;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

public class WordsSearchTaskConsumer implements TaskConsumer
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManager;

    @Inject
    private UsersWordsQueriesManager usersWordsQueriesManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        // FIXME: how to handle UC when an update is performed on subwiki and the user belongs to main wiki?
        List<UserReference> userList =
            this.getUserReferenceWithWordsQuery(documentReference.getWikiReference());

        if (!userList.isEmpty()) {
            try {
                List<ChangeAnalyzer> analyzers =
                    this.contextComponentManager.get().getInstanceList(ChangeAnalyzer.class);

                for (UserReference userReference : userList) {
                    Set<WordsQuery> queries = this.usersWordsQueriesManager.getQueries(userReference);

                    for (ChangeAnalyzer analyzer : analyzers) {
                        // FIXME: the analyzer should probably take a set of query and return a consolidated result
                        for (WordsQuery query : queries) {
                            WordsAnalysisResult analyze = analyzer.analyze(documentReference, version, query);

                        }
                    }
                }
            } catch (ComponentLookupException e) {
                throw new IndexException("Error when trying to load the list of analyzers", e);
            } catch (WordsAnalysisException e) {
                throw new IndexException("Error when trying to perform word analysis", e);
            }
        }
    }

    public List<UserReference> getUserReferenceWithWordsQuery(WikiReference wikiReference) throws IndexException
    {
        String query = String.format(", BaseObject as obj, BaseObject as obj2 "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' "
            + "and doc.fullName=obj2.name and obj2.className='%s'",
            this.entityReferenceSerializer.serialize(WordsQueryXClassInitializer.XCLASS_REFERENCE));

        try {
            List<String> usersList = this.queryManager.createQuery(query, Query.HQL)
                .setWiki(wikiReference.getName())
                .execute();

            return usersList.stream().map(this.userReferenceResolver::resolve).collect(Collectors.toList());
        } catch (QueryException e) {
            throw new IndexException("Error while trying to get the list of users with a words query", e);
        }
    }
}
