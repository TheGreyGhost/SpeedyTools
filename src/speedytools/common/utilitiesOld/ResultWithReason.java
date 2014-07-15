package speedytools.common.utilitiesOld;

/**
 * User: The Grey Ghost
 * Date: 22/06/2014
 * Utility class used by methods to return a success/failure status to the caller, providing a reason.
 * Usage:
 * 1) create using ResultWithReason.success() or ResultWithReason.failure(message)
 * 2) .succeeded() to check for success.  .getReason() to retrieve the reason
 */
public class ResultWithReason
{
  public static ResultWithReason success()
  {
    return new ResultWithReason(true, "");
  }

  public static ResultWithReason failure()
  {
    return new ResultWithReason(false, "");
  }

  public static ResultWithReason failure(String i_reason)
  {
    return new ResultWithReason(false, i_reason);
  }

  public boolean succeeded() { return success;}

  public String getReason()
  {
    return reason;
  }

  private ResultWithReason(boolean i_success, String i_reason)
  {
    success = i_success;
    reason = i_reason;
  }

  private boolean success;
  private String reason;
}
