package es.csic.cnb.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CustomFormatter extends Formatter {
  private static final MessageFormat MSGFORMAT = 
          new MessageFormat("{0,date,dd/MM/yy hh:mm:ss.SSS} [{1} - {2}] {3}: {4} {5}\n");
  
  public CustomFormatter() {
    super();
  }
  
  @Override
  public synchronized String format(LogRecord record) {
//    String source;
//    if (record.getSourceClassName() != null) {
//      source = record.getSourceClassName();
//      source = source.substring(source.lastIndexOf('.') + 1);
//      if (record.getSourceMethodName() != null) {
//        source += " " + record.getSourceMethodName();
//      }
//    } else {
//      source = record.getLoggerName();
//    }
    
    String source;
    if (record.getSourceClassName() != null) {
      source = record.getSourceClassName();
      source = source.substring(source.lastIndexOf('.') + 1);
    } else {
      source = record.getLoggerName();
    }
    
    String method = "";
    if (record.getSourceMethodName() != null) {
      method = record.getSourceMethodName();
    }

    String throwable = "";
    if (record.getThrown() != null) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println();
      record.getThrown().printStackTrace(pw);
      pw.close();
      throwable = sw.toString();
    }

    Object[] arguments = new Object[6];
    arguments[0] = new Date(record.getMillis());
    arguments[1] = record.getLevel();
    arguments[2] = source; //record.getLoggerName(); Thread.currentThread().getName();
    arguments[3] = method;
    arguments[4] = formatMessage(record); //record.getMessage();
    arguments[5] = throwable;
    
    return MSGFORMAT.format(arguments);
  }

}
