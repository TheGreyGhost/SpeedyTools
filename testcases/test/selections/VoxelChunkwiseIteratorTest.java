package test.selections;

import org.junit.Test;
import speedytools.common.selections.VoxelChunkwiseIterator;

import static org.junit.Assert.*;

/* test the iterator to make sure it works as expected:
1) covers all the blocks in the range, once only: in both xpos,ypos,zpos and wx,wy,wz
2) hasEnteredNewChunk() is correct
3) visits each chunk once only
4) estimatedFractionComplete() increases monotonically, starts from 0, ends near 1

*/
public class VoxelChunkwiseIteratorTest
{

  @Test
  public void testNext() throws Exception {

    for (int xmin = -20; xmin < 20; ++xmin) {
       testRange(xmin, 35, 0, 19, 12, 1);
    }

    for (int xsize = 1; xsize < 63; ++xsize) {
      testRange(13, 0, -347, xsize, 12, 3);
    }

    for (int ymin = 1; ymin < 63; ++ymin) {
      testRange(-135, ymin, 352, 19, 12, 41);
    }

    for (int ysize = 1; ysize < 63; ++ysize) {
      testRange(1356, 0, 23, 19, ysize, 17);
    }

    for (int zmin = -20; zmin < 63; ++zmin) {
      testRange(0, 19, zmin, 19, 12, 21);
    }

    for (int zsize = 1; zsize < 63; ++zsize) {
      testRange(1, 160, 128, 19, 12, zsize);
    }
  }

  /** run the tests on the given region; return true for success
   *
   * @param wxOrigin
   * @param wyOrigin
   * @param wzOrigin
   * @param xSize
   * @param ySize
   * @param zSize
   * @return
   */
  public boolean testRange(int wxOrigin, int wyOrigin, int wzOrigin, int xSize, int ySize, int zSize)
  {
    VoxelChunkwiseIterator vci = new VoxelChunkwiseIterator(wxOrigin, wyOrigin, wzOrigin, xSize, ySize, zSize);

    int cxMin = wxOrigin >> 4;
    int cxMax = (wxOrigin + xSize-1) >> 4;
    int czMin = wzOrigin >> 4;
    int czMax = (wzOrigin + zSize-1) >> 4;

    int [][][] posCount = new int[xSize][ySize][zSize];
    int [][][] worldCount = new int[xSize][ySize][zSize];
    int [][] chunkCount = new int[cxMax-cxMin+1][czMax-czMin+1];

    final int ARBITRARY_INT = -2362362;
    int cx = ARBITRARY_INT;
    int cz = ARBITRARY_INT;
    float progress = 0;
    final float SMALL_DELTA = 0.01F;
    assert vci.estimatedFractionComplete() < SMALL_DELTA;
    while (!vci.isAtEnd()) {
      ++posCount[vci.getXpos()][vci.getYpos()][vci.getZpos()];
      ++worldCount[vci.getWX()-wxOrigin][vci.getWY()-wyOrigin][vci.getWZ()-wzOrigin];
      if (vci.hasEnteredNewChunk()) {
        assert vci.getChunkX() != cx;
        assert vci.getChunkZ() != cz;
        cx = vci.getChunkX();
        cz = vci.getChunkZ();
        ++chunkCount[cx-cxMin][cz-czMin];
      } else {
        assert vci.getChunkX() == cx;
        assert vci.getChunkZ() == cz;
      }
      vci.next();
      assert vci.estimatedFractionComplete() > progress;
      progress= vci.estimatedFractionComplete();
    }
    assert progress <= 1 + SMALL_DELTA;

    for (int [][] i : posCount) {
      for (int [] j : i) {
        for (int k : j) {
          assert k == 1;
        }
      }
    }

    for (int [][] i : worldCount) {
      for (int [] j : i) {
        for (int k : j) {
          assert k == 1;
        }
      }
    }

    for (int [] i : chunkCount) {
      for (int j : i) {
        assert j == 1;
      }
    }

    return true;
  }

}