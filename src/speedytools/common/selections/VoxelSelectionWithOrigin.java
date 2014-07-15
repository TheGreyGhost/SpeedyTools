package speedytools.common.selections;

import speedytools.common.utilities.ErrorLog;

import java.io.*;

/**
 * Created by TheGreyGhost on 16/06/14.
 * Has the same functions as a VoxelSelection, but also adds an absolute location in world coordinates, i.e.
 *   the world coordinates corresponding to the [0,0,0] of the VoxelSelection
 */
public class VoxelSelectionWithOrigin extends VoxelSelection
{
  public VoxelSelectionWithOrigin(int i_wxOrigin, int i_wyOrigin, int i_wzOrigin, int xSize, int ySize, int zSize) {
    super(xSize, ySize, zSize);
    wxOrigin = i_wxOrigin;
    wyOrigin = i_wyOrigin;
    wzOrigin = i_wzOrigin;
  }

  public void setOrigin(int newWxOrigin, int newWyOrigin, int newWzOrigin)
  {
    wxOrigin = newWxOrigin; wyOrigin = newWyOrigin; wzOrigin = newWzOrigin;
  }

  public int getWxOrigin() {
    return wxOrigin;
  }

  public int getWyOrigin() {
    return wyOrigin;
  }

  public int getWzOrigin() {
    return wzOrigin;
  }

  /** serialise the VoxelSelectionWithOrigin to a byte array
   * @return the serialised VoxelSelection, or null for failure
   */
  @Override
  public ByteArrayOutputStream writeToBytes()
  {
    ByteArrayOutputStream bos = null;
    try {
      bos = new ByteArrayOutputStream();
      DataOutputStream outputStream = new DataOutputStream(bos);
      outputStream.writeInt(wxOrigin);
      outputStream.writeInt(wyOrigin);
      outputStream.writeInt(wzOrigin);
      ByteArrayOutputStream bosVoxelSelection = super.writeToBytes();
      bosVoxelSelection.writeTo(bos);
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while converting VoxelSelectionWithOrigin writeToBytes:" + ioe);
      bos = null;
    }

    return bos;
  }

  /** fill this VoxelSelectionWithOrigin using the serialised VoxelSelection byte array
   * @param byteArrayInputStream the bytearray containing the serialised VoxelSelection
   * @return true for success, false for failure (leaves selection untouched)
   */
  @Override
  public boolean readFromBytes(ByteArrayInputStream byteArrayInputStream) {
    try {
      DataInputStream inputStream = new DataInputStream(byteArrayInputStream);

      int newWxOrigin = inputStream.readInt();
      int newWyOrigin = inputStream.readInt();
      int newWzOrigin = inputStream.readInt();
//      if (newWxOrigin < 1 || newWxOrigin > MAX_X_SIZE || newWyOrigin < 1 || newWyOrigin > MAX_Y_SIZE || newWzOrigin < 1 || newWzOrigin > MAX_Z_SIZE) {
//        return false;
//      }
      super.readFromBytes(byteArrayInputStream);

      wxOrigin = newWxOrigin;
      wyOrigin = newWyOrigin;
      wzOrigin = newWzOrigin;
    } catch (IOException ioe) {
      ErrorLog.defaultLog().warning("Exception while VoxelSelectionWithOrigin.readFromBytes: " + ioe);
      return false;
    }
    return true;
  }

  private int wxOrigin;
  private int wyOrigin;
  private int wzOrigin;
}
