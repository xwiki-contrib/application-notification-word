package org.xwiki.contrib.notification_word;

import org.xwiki.observation.event.Event;

public class MentionedWordsEvent implements Event
{
    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof MentionedWordsEvent;
    }
}
