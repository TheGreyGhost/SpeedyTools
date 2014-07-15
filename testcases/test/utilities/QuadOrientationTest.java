package test.utilities;

import org.junit.*;
import speedytools.common.utilitiesOld.QuadOrientation;

public class QuadOrientationTest
{

  @Test
  public void testTransformations() throws Exception {
    QuadOrientation quadOrientation = TestQuad.generateTestQuadOrientation();
    TestQuad testQuad = new TestQuad();

    Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));

    for (int i = 0; i < 4; ++i) {
      quadOrientation.rotateClockwise(1);
      testQuad.rotateClockwise();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));
    }

    quadOrientation = TestQuad.generateTestQuadOrientation();
    testQuad = new TestQuad();
    quadOrientation.flipX();
    testQuad.flipX();
    Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
    Assert.assertTrue(testQuad.testMapping(quadOrientation));
    for (int i = 0; i < 4; ++i) {
      quadOrientation.rotateClockwise(1);
      testQuad.rotateClockwise();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));
    }

    quadOrientation = TestQuad.generateTestQuadOrientation();
    testQuad = new TestQuad();
    quadOrientation.flipZ();
    testQuad.flipZ();
    Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
    Assert.assertTrue(testQuad.testMapping(quadOrientation));
    for (int i = 0; i < 4; ++i) {
      quadOrientation.rotateClockwise(1);
      testQuad.rotateClockwise();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));
    }

    quadOrientation = TestQuad.generateTestQuadOrientation();
    testQuad = new TestQuad();
    for (int i = 0; i < 4; ++i) {
      quadOrientation.rotateClockwise(1);
      testQuad.rotateClockwise();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));

      quadOrientation.flipX(); testQuad.flipX();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));
      quadOrientation.flipZ(); testQuad.flipZ();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));

      quadOrientation.flipX(); testQuad.flipX();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));
      quadOrientation.flipZ(); testQuad.flipZ();
      Assert.assertTrue(testQuad.matchesTransformation(quadOrientation));
      Assert.assertTrue(testQuad.testMapping(quadOrientation));
    }

  }

  // test quad, 4 x 4 grid, quad itself is 4 wide * 2 high, centred in the middle
  // i.e. a quad with origin 0,1 and size 4,2
  private static class TestQuad
  {
    public static QuadOrientation generateTestQuadOrientation()
    {
       return new QuadOrientation(0, 1, 4, 2);
    }

    public void flipX()
    {
      int [][] newQuad = new int[4][4];
      for (int x = 0; x < 4; ++x) {
        for (int z = 0; z < 4; ++z) {
          newQuad[x][z] = quad[3-x][z];
        }
      }
      quad = newQuad;
    }

    public void flipZ()
    {
      int [][] newQuad = new int[4][4];
      for (int x = 0; x < 4; ++x) {
        for (int z = 0; z < 4; ++z) {
          newQuad[x][z] = quad[x][3-z];
        }
      }
      quad = newQuad;
    }

    public void rotateClockwise()
    {
      int [][] newQuad = new int[4][4];
      for (int x = 0; x < 4; ++x) {
        for (int z = 0; z < 4; ++z) {
          newQuad[x][z] = quad[z][3-x];
        }
      }
      quad = newQuad;
    }

    public boolean matchesTransformation(QuadOrientation quadOrientation)
    {
     int [][] testQuad = new int[][]{ {0,0,0,0}, {0,0,0,0}, {0,0,0,0}, {0,0,0,0}};
     for (int x = 0; x < 4; ++x) {
       for (int z = 0; z < 2; ++z) {
         int wx = quadOrientation.calcWXfromXZ(x, z);
         int wz = quadOrientation.calcWZfromXZ(x, z);
         testQuad[wx][wz] = quadSource[x][z];
       }
     }

     for (int x = 0; x < 4; ++x) {
       for (int z = 0; z < 4; ++z) {
         if (testQuad[x][z] != quad[x][z]) {
           printGrids(testQuad, quad);
           return false;
         }
       }
     }
     return true;
    }

    public void printGrids(int [][] grid1, int [][] grid2)
    {
      for (int z = 0; z < 4; ++z) {
        for (int x = 0; x < 4; ++x) {
          System.out.print(grid1[x][z] + " ");
        }
        System.out.print("  :  ");
        for (int x = 0; x < 4; ++x) {
          System.out.print(grid2[x][z] + " ");
        }
        System.out.println();
      }
    }

    public boolean testMapping(QuadOrientation quadOrientation)
    {
      for (int x = 0; x < 4; ++x) {
        for (int z = 0; z < 2; ++z) {
          int wx = quadOrientation.calcWXfromXZ(x, z);
          int wz = quadOrientation.calcWZfromXZ(x, z);
          int checkx = quadOrientation.calcXfromWXZ(wx, wz);
          int checkz = quadOrientation.calcZfromWXZ(wx, wz);
          if (checkx != x || checkz != z) return false;
        }
      }
      return true;
    }

    private int [][] quad = new int[][]{ {0,1,5,0}, {0,2,6,0}, {0,3,7,0}, {0,4,8,0}};
    private final int [][] quadSource = new int[][]{ {1, 5}, {2, 6}, {3, 7}, {4, 8}};
  }

}