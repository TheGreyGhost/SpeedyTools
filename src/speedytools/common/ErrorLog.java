package speedytools.common;

import cpw.mods.fml.common.FMLLog;

import java.io.IOException;
import java.util.logging.*;

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
    Logger logger = Logger.getLogger("Default");
    logger.setLevel(Level.INFO);
    FileHandler fileTxt;
    try {
      fileTxt = new FileHandler(logfilename);
    } catch (IOException e) {
      setDefaultErrorLogger(logger);
      return;
    }
    fileTxt.setFormatter(new SimpleFormatter());
    logger.addHandler(fileTxt);
    logger.setUseParentHandlers(false);
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

    public  void severe(String format, Object... data)
    {
      log(Level.SEVERE, format, data);
    }

    public  void warning(String format, Object... data)
    {
      log(Level.WARNING, format, data);
    }

    public  void info(String format, Object... data)
    {
      log(Level.INFO, format, data);
    }

    public  void fine(String format, Object... data)
    {
      log(Level.FINE, format, data);
    }

    public  void finer(String format, Object... data)
    {
      log(Level.FINER, format, data);
    }

    public  void finest(String format, Object... data)
    {
      log(Level.FINEST, format, data);
    }
}
