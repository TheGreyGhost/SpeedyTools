package speedytools.common.utilities;

import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
* User: The Grey Ghost
* Date: 1/03/14
* wrapper to interchangably replace static FMLLog calls with a logger for unit testing
* usage:
* ErrorLog.defaultLog().log(etc) instead of FMLLog.log(etc)
* ErrorLog.defaultLog().severe (etc)
* Defaults to FMLLog.  To change this, call ErrorLog.setDefaultErrorLogger
*/
public class ErrorLog
{
  public static ErrorLog defaultLog() {
    return defaultErrorLog;
  }

  public static void setDefaultErrorLogger(Logger i_logger)
  {
    defaultErrorLog = new ErrorLog(i_logger);
  }

  /** sends all messages to the given logfile, does not copy to console
   *
   * @param logfilename
   */
  public static void setLogFileAsDefault(String logfilename)
  {
    Logger logger = LogManager.getLogger("Default");
//    logger.setLevel(Level.INFO);
//    FileHandler fileTxt;
//    try {
//      fileTxt = new FileHandler(logfilename);
//    } catch (IOException e) {
//      setDefaultErrorLogger(logger);
//      return;
//    }
//    fileTxt.setFormatter(new SimpleFormatter());
//    for (Handler handler : logger.getHandlers()) {
//      logger.removeHandler(handler);
//    }
//    logger.addHandler(fileTxt);
//    logger.setUseParentHandlers(false);
    setDefaultErrorLogger(logger);
  }

  private static ErrorLog defaultErrorLog = new ErrorLog(null);

  public ErrorLog(Logger i_logger) {
   if (i_logger == null) {
     useFMLLog = true;
   } else {
     useFMLLog = false;
     logger = i_logger;
   }
  }

  private boolean useFMLLog;
  private Logger logger;

  public  void log(String logChannel, Level level, String format, Object... data)
    {
      if (useFMLLog) {
        FMLLog.log(logChannel, level, format, data);
      } else {
        logger.log(level, String.format(format, data));
      }
    }

    public  void log(Level level, String format, Object... data)
    {
      if (useFMLLog) {
        FMLLog.log(level, format, data);
      } else {
        logger.log(level, String.format(format, data));
      }
    }

    public  void log(String logChannel, Level level, Throwable ex, String format, Object... data)
    {
      if (useFMLLog) {
        FMLLog.log(logChannel, level, ex, format, data);
      } else {
        logger.log(level, String.format(format, data), ex);
      }
    }

    public  void log(Level level, Throwable ex, String format, Object... data)
    {
      if (useFMLLog) {
        FMLLog.log(level, ex, format, data);
      } else {
        logger.log(level, String.format(format, data), ex);
      }
    }

    public void severe(String format, Object... data)
    {
      log(Level.FATAL, format, data);
    }

    public void debug(String format, Object... data)
    {
      log(Level.DEBUG, format, data);
    }

    public  void info(String format, Object... data)
    {
      log(Level.INFO, format, data);
    }

}
