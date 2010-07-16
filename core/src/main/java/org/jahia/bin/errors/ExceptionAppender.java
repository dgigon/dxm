package org.jahia.bin.errors;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: loom
 * Date: Jul 16, 2010
 * Time: 3:40:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExceptionAppender extends AppenderSkeleton {

    public ExceptionAppender() {
    }

    @Override
    protected void append(LoggingEvent event) {
        //To change body of implemented methods use File | Settings | File Templates.
        if (event.getThrowableInformation() != null) {
            try {
                ErrorFileDumper.dumpToFile(event.getThrowableInformation().getThrowable(), null);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean requiresLayout() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
