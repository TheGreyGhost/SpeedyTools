package speedytools.serverside.worldmanipulation;

/**
 * Created by TheGreyGhost on 5/08/14.
 */
public class UniqueTokenID implements Comparable<UniqueTokenID>
{
  public UniqueTokenID() {myID = uniqueID++;}

  public int hashCode() {
    return myID;
  }

  public boolean equals(Object obj) {
    if (obj instanceof UniqueTokenID) {
      return this.myID == ((UniqueTokenID)obj).myID;
    }
    return false;
  }

  public int compareTo(UniqueTokenID anotherUniqueTokenID) {
    return (this.myID < anotherUniqueTokenID.myID ) ? -1 : ( (this.myID == anotherUniqueTokenID.myID) ? 0 : 1);
  }

  private int myID;
  private static int uniqueID = 1000;  // arbitrary starting number
}
