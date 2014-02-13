package speedytools.clientonly;


import net.minecraft.client.renderer.Tessellator;
import net.minecraft.server.integrated.IntegratedPlayerList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import speedytools.common.Colour;
import speedytools.common.UsefulConstants;

import java.util.HashMap;
import java.util.Map;

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

//    OpenGLdebugging.dumpAllIsEnabled();

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

  /**
   * Use the following order: anticlockwise from bottom right: x1, x2, x3, x4
   * @param highlightedSide
   */
  public static void drawTranslucentBoxWithHighlightedSide(double x1, double x2, double x3, double x4,
                                                           double y1, double y2, double y3, double y4,
                                                           double z1, double z2, double z3, double z4,
                                                           int highlightedSide)
  {
    Tessellator tessellator = Tessellator.instance;

    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDepthMask(true);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    tessellator.startDrawingQuads();

    tessellator.setColorRGBA_F(0.0F, 1.0F, 0.0F, 0.2F);

//    tessellator.setBrightness(this.brightnessTopLeft);
    float u1 = 1.0F;
    float u2 = 1.0F;
    float u3 = 0.0F;
    float u4 = 0.0F;
    float v1 = 1.0F;
    float v2 = 0.0F;
    float v3 = 0.0F;
    float v4 = 1.0F;
    tessellator.addVertexWithUV(x1, y1, z1, u1, v1);
    tessellator.addVertexWithUV(x2, y2, z2, u2, v2);
    tessellator.addVertexWithUV(x3, y3, z3, u3, v3);
    tessellator.addVertexWithUV(x4, y4, z4, u4, v4);
    tessellator.addVertexWithUV(x1, y1, z1, u1, v1);
    tessellator.draw();

  }

  public static void setDrawColour(Colour colour)
  {
    GL11.glColor4f(colour.R, colour.G, colour.B, colour.A);
  }

  /**
   * Draws a rectangle with a superimposed cross using the given coordinates, anticlockwise from x1 to x4.
   * @param highlight - true if this face should be highlighted
   * @param alternateHighlightColour - if true, and highlight is true, then use the alternate colour for highlighting
   */
  public static void drawTransparentRectWithCrossHighlight(double x1, double x2, double x3, double x4,
                                                           double y1, double y2, double y3, double y4,
                                                           double z1, double z2, double z3, double z4,
                                                           boolean highlight, boolean alternateHighlightColour)
  {
    Tessellator tessellator = Tessellator.instance;

    if (highlight) {
      if (alternateHighlightColour) {
        setDrawColour(Colour.YELLOW_20);
      } else {
        setDrawColour(Colour.GREENYELLOW_20);
      }
    } else {
      setDrawColour(Colour.GREEN_20);
    }
    tessellator.startDrawingQuads();
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x4, y4, z4);
    tessellator.addVertex(x1, y1, z1);
    tessellator.draw();

    if (highlight) {
      setDrawColour(Colour.WHITE_40);
      GL11.glLineWidth(4.0F);
    } else {
      setDrawColour(Colour.BLACK_40);
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
   * Draws a rectangle with a superimposed cross using the given coordinates, anticlockwise from x1 to x4.
   * @param highlight - true if this face should be highlighted
   * @param alternateHighlightColour - if true, and highlight is true, then use the alternate colour for highlighting
   * @param cross - if true, draw the cross lines in addition to the rect outline
   */
  public static void drawTransparentRect(double x1, double x2, double x3, double x4,
                                         double y1, double y2, double y3, double y4,
                                         double z1, double z2, double z3, double z4,
                                         boolean highlight, boolean alternateHighlightColour, boolean cross)
  {
    Tessellator tessellator = Tessellator.instance;

    if (highlight) {
      if (alternateHighlightColour) {
        setDrawColour(Colour.YELLOW_20);
      } else {
        setDrawColour(Colour.GREENYELLOW_20);
      }
    } else {
      setDrawColour(Colour.GREEN_20);
    }
    tessellator.startDrawingQuads();
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x4, y4, z4);
    tessellator.addVertex(x1, y1, z1);
    tessellator.draw();

    if (highlight) {
      setDrawColour(Colour.WHITE_40);
      GL11.glLineWidth(4.0F);
    } else {
      setDrawColour(Colour.BLACK_40);
      GL11.glLineWidth(2.0F);
    }
    tessellator.startDrawing(GL11.GL_LINE_STRIP);
    tessellator.addVertex(x1, y1, z1);
    tessellator.addVertex(x2, y2, z2);
    tessellator.addVertex(x3, y3, z3);
    tessellator.addVertex(x4, y4, z4);
    tessellator.addVertex(x1, y1, z1);
    tessellator.draw();

    if (cross) {
      tessellator.startDrawing(GL11.GL_LINES);
      tessellator.addVertex(x1, y1, z1);
      tessellator.addVertex(x3, y3, z3);
      tessellator.addVertex(x2, y2, z2);
      tessellator.addVertex(x4, y4, z4);
      tessellator.draw();
    }
  }

    /**
     *  Draw the given cube, rendering the selectedSide differently.  Only draw the "cross" outline on those sides facing the viewer [0,0,0] - or if the viewer is inside the box.
     * @param cube
     * @param selectedSide (as per UsefulConstants.FACE_XPOS etc) - if no match (eg -1) doesn't draw any side selected
     * @param dragging - true is the player has grabbed a side and is dragging it
     */
  public static void drawFilledCubeWithSelectedSide(AxisAlignedBB cube, int selectedSide, boolean dragging)
  {
    GL11.glDisable(GL11.GL_LIGHTING);
    GL11.glEnable(GL11.GL_BLEND);
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glDepthMask(true);
    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

    double xa = cube.minX;
    double xb = cube.maxX;
    double ya = cube.minY;
    double yb = cube.maxY;
    double za = cube.minZ;
    double zb = cube.maxZ;


    // faces must be rendered in a specific order to make sure the transparent sides correctly (further must render first otherwise depth mapping will hide the rear faces)
    //  (1) rearmost faces first
    //  (2) "middle" faces next (where the viewer is between the two sides of the cube along that axis)
    //  (3) frontmost faces last
    int [] faceRenderOrder = new int[6];
    int relativeXpos = 0;
    if (xa > 0) {
        relativeXpos = -1;
    } else if (xb < 0) {
      relativeXpos = 1;
    }
    faceRenderOrder[UsefulConstants.FACE_XNEG] = relativeXpos;
    faceRenderOrder[UsefulConstants.FACE_XPOS] = -relativeXpos;

    int relativeYpos = 0;
    if (ya > 0) {
      relativeYpos = -1;
    } else if (yb < 0) {
      relativeYpos = 1;
    }
    faceRenderOrder[UsefulConstants.FACE_YNEG] = relativeYpos;
    faceRenderOrder[UsefulConstants.FACE_YPOS] = -relativeYpos;

    int relativeZpos = 0;
    if (za > 0) {
      relativeZpos = -1;
    } else if (zb < 0) {
      relativeZpos = 1;
    }
    faceRenderOrder[UsefulConstants.FACE_ZNEG] = relativeZpos;
    faceRenderOrder[UsefulConstants.FACE_ZPOS] = -relativeZpos;

    for (int relpos = 1; relpos >= -1; --relpos) {
      boolean drawCross = (relpos == -1) || (relativeXpos == 0 && relativeYpos == 0 && relativeZpos == 0);
      for (int side = 0; side <= 5; ++side) {
        if (faceRenderOrder[side] == relpos) {
          switch(side) {
            case UsefulConstants.FACE_XNEG: {
              drawTransparentRect(xa, xa, xa, xa, ya, ya, yb, yb, za, zb, zb, za, selectedSide == UsefulConstants.FACE_XNEG, dragging, drawCross);
              break;
            }
            case UsefulConstants.FACE_XPOS: {
              drawTransparentRect(xb, xb, xb, xb, ya, ya, yb, yb, za, zb, zb, za, selectedSide == UsefulConstants.FACE_XPOS, dragging, drawCross);
              break;
            }
            case UsefulConstants.FACE_YNEG: {
              drawTransparentRect(xa, xa, xb, xb, ya, ya, ya, ya, za, zb, zb, za, selectedSide == UsefulConstants.FACE_YNEG, dragging, drawCross);
              break;
            }
            case UsefulConstants.FACE_YPOS: {
              drawTransparentRect(xa, xa, xb, xb, yb, yb, yb, yb, za, zb, zb, za, selectedSide == UsefulConstants.FACE_YPOS, dragging, drawCross);
              break;
            }
            case UsefulConstants.FACE_ZNEG: {
              drawTransparentRect(xa, xa, xb, xb, ya, yb, yb, ya, za, za, za, za, selectedSide == UsefulConstants.FACE_ZNEG, dragging, drawCross);
              break;
            }
            case UsefulConstants.FACE_ZPOS: {
              drawTransparentRect(xa, xa, xb, xb, ya, yb, yb, ya, zb, zb, zb, zb, selectedSide == UsefulConstants.FACE_ZPOS, dragging, drawCross);
              break;
            }
          }
        } // if (faceRenderOrder)
      }   // for (side)
    }  // for(relpos)
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
