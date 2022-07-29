package org.xwiki.contrib.notification_word;

public class WordsAnalysisException extends Exception
{
    public WordsAnalysisException(String msg)
    {
        super(msg);
    }

    public WordsAnalysisException(String msg, Throwable e)
    {
        super(msg, e);
    }
}
