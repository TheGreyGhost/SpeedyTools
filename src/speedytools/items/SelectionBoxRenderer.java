package speedytools.items;


import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

public class SelectionBoxRenderer {
  public static void drawFilledCube(AxisAlignedBB cube)
  {
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

  }



}
