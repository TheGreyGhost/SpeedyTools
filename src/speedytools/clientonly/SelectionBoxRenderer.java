package speedytools.clientonly;


import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import speedytools.common.UsefulConstants;

public class SelectionBoxRenderer {

  public static void drawConnectingLine(double x1, double y1, double z1, double x2, double y2, double z2)
  {
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawing(GL11.GL_LINES);
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x2, y2, z2);
    tessellator.draw();
  }

  public static void drawCube(AxisAlignedBB cube) {
    double xa = cube.minX;
    double xb = cube.maxX;
    double ya = cube.minY;
    double yb = cube.maxY;
    double za = cube.minZ;
    double zb = cube.maxZ;

    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawing(GL11.GL_LINE_STRIP);
    tessellator.addVertex(xa, ya, za);
    tessellator.addVertex(xa, yb, za);
    tessellator.addVertex(xb, yb, za);
    tessellator.addVertex(xb, ya, za);
    tessellator.addVertex(xa, ya, za);

    tessellator.addVertex(xa, ya, zb);
    tessellator.addVertex(xa, yb, zb);
    tessellator.addVertex(xb, yb, zb);
    tessellator.addVertex(xb, ya, zb);
    tessellator.addVertex(xa, ya, zb);
    tessellator.draw();

    tessellator.startDrawing(GL11.GL_LINES);
    tessellator.addVertex(xa, ya, za);
    tessellator.addVertex(xa, ya, zb);

    tessellator.addVertex(xa, yb, za);
    tessellator.addVertex(xa, yb, zb);

    tessellator.addVertex(xb, ya, za);
    tessellator.addVertex(xb, ya, zb);

    tessellator.addVertex(xb, yb, za);
    tessellator.addVertex(xb, yb, zb);
    tessellator.draw();
  }

  public static void drawBoxWithCross(double x1, double x2, double x3, double x4,
                                      double y1, double y2, double y3, double y4,
                                      double z1, double z2, double z3, double z4)
  {
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawing(GL11.GL_LINE_STRIP);
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x4, y4, z4);
    tessellator.addVertex(x1, y1, z1);
    tessellator.draw();

    tessellator.startDrawing(GL11.GL_LINES);
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x4, y4, z4);
    tessellator.draw();
  }

  public static void drawBoxWithCrossHighlight(double x1, double x2, double x3, double x4,
                                               double y1, double y2, double y3, double y4,
                                               double z1, double z2, double z3, double z4,
                                               boolean highlight)
  {
    Tessellator tessellator = Tessellator.instance;

    if (highlight) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.4F);
      GL11.glLineWidth(6.0F);
    } else {
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
      GL11.glLineWidth(2.0F);
    }

    tessellator.startDrawing(GL11.GL_LINE_STRIP);
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x4, y4, z4);
    tessellator.addVertex(x1, y1, z1);
    tessellator.draw();

    tessellator.startDrawing(GL11.GL_LINES);
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x4, y4, z4);
    tessellator.draw();
  }

  /**
   *  Draw the given cube, rendering the selectedSide differently
   * @param cube
   * @param selectedSide (as per UsefulConstants.FACE_XPOS etc) - if no match (eg -1) doesn't draw any side selected
   */
  public static void drawFilledCubeWithSelectedSide(AxisAlignedBB cube, int selectedSide)
  {
    double xa = cube.minX;
    double xb = cube.maxX;
    double ya = cube.minY;
    double yb = cube.maxY;
    double za = cube.minZ;
    double zb = cube.maxZ;

    drawBoxWithCrossHighlight(xa, xa, xa, xa, ya, ya, yb, yb, za, zb, zb, za, selectedSide == UsefulConstants.FACE_XNEG);
    drawBoxWithCrossHighlight(xb, xb, xb, xb, ya, ya, yb, yb, za, zb, zb, za, selectedSide == UsefulConstants.FACE_XPOS);
    drawBoxWithCrossHighlight(xa, xa, xb, xb, ya, ya, ya, ya, za, zb, zb, za, selectedSide == UsefulConstants.FACE_YNEG);
    drawBoxWithCrossHighlight(xa, xa, xb, xb, yb, yb, yb, yb, za, zb, zb, za, selectedSide == UsefulConstants.FACE_YPOS);
    drawBoxWithCrossHighlight(xa, xa, xb, xb, ya, yb, yb, ya, za, za, za, za, selectedSide == UsefulConstants.FACE_ZNEG);
    drawBoxWithCrossHighlight(xa, xa, xb, xb, ya, yb, yb, ya, zb, zb, zb, zb, selectedSide == UsefulConstants.FACE_ZPOS);
  }

  public static void drawFilledCube(AxisAlignedBB cube)
  {
    double xa = cube.minX;
    double xb = cube.maxX;
    double ya = cube.minY;
    double yb = cube.maxY;
    double za = cube.minZ;
    double zb = cube.maxZ;

    drawBoxWithCross(xa, xa, xa, xa, ya, ya, yb, yb, za, zb, zb, za);
    drawBoxWithCross(xb, xb, xb, xb, ya, ya, yb, yb, za, zb, zb, za);
    drawBoxWithCross(xa, xa, xb, xb, ya, ya, ya, ya, za, zb, zb, za);
    drawBoxWithCross(xa, xa, xb, xb, yb, yb, yb, yb, za, zb, zb, za);
    drawBoxWithCross(xa, xa, xb, xb, ya, yb, yb, ya, za, za, za, za);
    drawBoxWithCross(xa, xa, xb, xb, ya, yb, yb, ya, zb, zb, zb, zb);
/*
    Tessellator tessellator = Tessellator.instance;
    tessellator.startDrawing(GL11.GL_LINE_STRIP);
    tessellator.addVertex(cube.minX, cube.minY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.minY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.minY, cube.maxZ);
    tessellator.addVertex(cube.minX, cube.minY, cube.maxZ);
    tessellator.addVertex(cube.minX, cube.minY, cube.minZ);
    tessellator.draw();

    tessellator.startDrawing(GL11.GL_LINE_STRIP);
    tessellator.addVertex(cube.minX, cube.maxY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.maxY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.maxY, cube.maxZ);
    tessellator.addVertex(cube.minX, cube.maxY, cube.maxZ);
    tessellator.addVertex(cube.minX, cube.maxY, cube.minZ);
    tessellator.draw();

    tessellator.startDrawing(GL11.GL_LINES);
    tessellator.addVertex(cube.minX, cube.minY, cube.minZ);
    tessellator.addVertex(cube.minX, cube.maxY, cube.minZ);

    tessellator.addVertex(cube.maxX, cube.minY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.maxY, cube.minZ);

    tessellator.addVertex(cube.maxX, cube.minY, cube.maxZ);
    tessellator.addVertex(cube.maxX, cube.maxY, cube.maxZ);

    tessellator.addVertex(cube.minX, cube.minY, cube.maxZ);
    tessellator.addVertex(cube.minX, cube.maxY, cube.maxZ);

    tessellator.addVertex(cube.minX, cube.minY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.maxY, cube.maxZ);

    tessellator.addVertex(cube.maxX, cube.minY, cube.minZ);
    tessellator.addVertex(cube.minX, cube.maxY, cube.maxZ);

    tessellator.addVertex(cube.minX, cube.maxY, cube.minZ);
    tessellator.addVertex(cube.maxX, cube.minY, cube.maxZ);

    tessellator.addVertex(cube.minX, cube.minY, cube.maxZ);
    tessellator.addVertex(cube.maxX, cube.maxY, cube.minZ);
    tessellator.draw();
*/
  }



}
