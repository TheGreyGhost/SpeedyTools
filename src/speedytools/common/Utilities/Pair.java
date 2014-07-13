package speedytools.common.utilities;

/**
 * User: The Grey Ghost
 * Date: 12/07/2014
 * very simple class to group two objects into a pair
 */
public final class Pair<A, B>
{
  public Pair(A i_first, B i_second) {
    first = i_first;
    second = i_second;
  }

  public A getFirst() {
    return first;
  }

  public B getSecond() {
    return second;
  }

  private A first;
  private B second;
}