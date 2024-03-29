package advanced.drawing;

import org.mt4j.MTApplication;

public class MTProject extends MTApplication
{
  private static final long serialVersionUID = 1L;
  public static void main(String args[])
  {
    initialize();
  }
  @Override
  public void startUp()
  {
    this.addScene(new MainDrawingScene(this, "Main drawing scene"));
  }
}
