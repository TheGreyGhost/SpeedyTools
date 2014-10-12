package speedytools.clientside.sound;

/**
 * User: The Grey Ghost
 * Date: 17/04/2014
 * Contains all the Speedy Sound effects
 */
public enum SoundEffectNames
{
  BOUNDARY_GRAB("complex.boundarygrab"),
  BOUNDARY_UNGRAB("complex.boundaryungrab"),
  BOUNDARY_PLACE_1ST("complex.boundaryplace1st"),
  BOUNDARY_PLACE_2ND("complex.boundaryplace2nd"),
  BOUNDARY_UNPLACE("complex.boundaryunplace"),
  BOUNDARY_HUM("complex.boundaryfieldhumloop"),
  STRONGWAND_PLACE("simple.wandplace"),
  STRONGWAND_UNPLACE("simple.wandunplace"),
  WEAKWAND_PLACE("simple.wandplace"),
  WEAKWAND_UNPLACE("simple.wandunplace"),
  ORB_PLACE("simple.orbplace"),
  ORB_UNPLACE("simple.orbunplace"),
  SCEPTRE_UNPLACE("simple.sceptreunplace"),
  SCEPTRE_PLACE("simple.sceptreplace"),
  POWERUP("complex.powerup"),
  POWERUPHOLD("complex.poweruphold"),
  UNDOPOWERUP("complex.undopowerup"),
  UNDOPOWERUPHOLD("complex.underpoweruphold"),
  PERFORMINGACTION("complex.performingaction"),
  PERFORMINGUNDO("complex.performingundo"),
  CREATESELECTION("complex.createselection");


  public final String getJsonName() {return "speedytoolsmod:" + jsonName;}

  private SoundEffectNames(String i_jsonName) {
    jsonName = i_jsonName;
  }
  private final String jsonName;
}
