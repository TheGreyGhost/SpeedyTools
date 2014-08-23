package speedytools.clientside.rendering;

/**
 * User: The Grey Ghost
 * Date: 17/04/2014
 * Contains all the Speedy Sound effects
 */
public enum SpeedySoundTypes
{
  BOUNDARY_GRAB("speedytools:boundarygrab.ogg"),
  BOUNDARY_UNGRAB("speedytools:boundaryungrab.ogg"),
  BOUNDARY_PLACE_1ST("speedytools:boundaryplace1st.ogg"),
  BOUNDARY_PLACE_2ND("speedytools:boundaryplace2nd.ogg"),
  BOUNDARY_UNPLACE("speedytools:boundaryunplace.ogg"),
  STRONGWAND_PLACE("speedytools:wandplace.ogg"),
  STRONGWAND_UNPLACE("speedytools:wandunplace.ogg"),
  WEAKWAND_PLACE("speedytools:wandplace.ogg"),
  WEAKWAND_UNPLACE("speedytools:wandunplace.ogg"),
  ORB_PLACE("speedytools:orbplace.ogg"),
  ORB_UNPLACE("speedytools:orbunplace.ogg"),
  SCEPTRE_UNPLACE("speedytools:sceptreunplace.ogg"),
  SCEPTRE_PLACE("speedytools:sceptreplace.ogg");


  public final String getFilename() {return filename;}

  private SpeedySoundTypes(String i_filename) {
    filename = i_filename;
  }
  private final String filename;
}
