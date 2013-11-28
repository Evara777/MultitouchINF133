package advanced.drawing;

import java.util.HashMap;
import java.util.Random;

import org.mt4j.AbstractMTApplication;
import org.mt4j.components.TransformSpace;
import org.mt4j.components.visibleComponents.shapes.AbstractShape;
import org.mt4j.input.IMTInputEventListener;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.sceneManagement.IPreDrawAction;
import org.mt4j.util.MTColor;
import org.mt4j.util.math.ToolsMath;
import org.mt4j.util.math.Vector3D;

import processing.core.PApplet;

public class DrawSurfaceScene extends AbstractScene
{
  private AbstractMTApplication mtApp;
  private AbstractShape drawShape;
  private float stepDistance;
  private Vector3D localBrushCenter;
  private float brushWidthHalf;
  private HashMap<InputCursor, Vector3D> cursorToLastDrawnPoint;
  private float brushHeightHalf;
  private float brushScale;
  private MTColor brushColor;
  private boolean dynamicBrush;

  // Our addition begin

  // Last time since a new touch was added
  private long lastTouchTime;
  // How many touches are currently active.
  private short touchCount;
  // Milliseconds from one finger down to another. If the current time is less
  // than lastTouchTime plus this, it's considered part of a gesture, and not
  // just a new finger. This allows us to use three or more fingers without it
  // being interpreted as a "randomize color" gesture. For debugging on the
  // simulator it's set to 1 seconds (1000 milliseconds), but on the real unit
  // it could be lower.
  private final int THREEFINGERTHRESHOLD = 1000;
  // Initially true, set to false when it takes longer than the threshold for
  // all the fingers to interact. Reset when no fingers are on the board.
  // Without this check a user could hold two fingers on the board and double
  // tap the third to randomize colors, but this is a very different gesture.
  private boolean isValidGesture;
  // Used for random colors.
  Random rand;

  // Our addition end

  public DrawSurfaceScene(AbstractMTApplication mtApplication, String name)
  {
    super(mtApplication, name);
    this.mtApp = mtApplication;
    // Our addition begin
    rand = new Random();
    isValidGesture = true;
    lastTouchTime = Long.MAX_VALUE - THREEFINGERTHRESHOLD;
    // Our addition end
    this.getCanvas().setDepthBufferDisabled(true);
    this.brushColor = new MTColor(0, 0, 0);
    this.brushScale = 1.0f;
    this.dynamicBrush = true;
    this.cursorToLastDrawnPoint = new HashMap<InputCursor, Vector3D>();
    this.getCanvas().addInputListener(new IMTInputEventListener()
    {
      public boolean processInputEvent(MTInputEvent input)
      {
        if(input instanceof AbstractCursorInputEvt)
        {
          final AbstractCursorInputEvt position = (AbstractCursorInputEvt) input;
          final InputCursor cursor = position.getCursor();
          if(position.getId() == AbstractCursorInputEvt.INPUT_STARTED)
          {
            // Our addition begin
            ++touchCount;
            if(isValidGesture)
            {
              final long touchTime = System.currentTimeMillis();
              System.out.println("Touch Count " + Integer.toString(touchCount));
              System.out.println("Current Time " + Long.toString(touchTime));
              System.out.println("Threshold Time "
                  + Long.toString(lastTouchTime + THREEFINGERTHRESHOLD));
              if(lastTouchTime + THREEFINGERTHRESHOLD >= touchTime)
              {
                System.out.println("Valid Input.");
                if(touchCount == 3)
                {
                  System.out.println("Three hits!");
                  setBrushColor(new MTColor(rand.nextInt(256), rand
                      .nextInt(256), rand.nextInt(256)));
                  isValidGesture = false;
                }
                else if(touchCount > 3)
                {
                  System.out.println("More than three touches active.");
                }
              }
              else
              {
                System.out.println("Too Slow");
                isValidGesture = false;
              }
              lastTouchTime = System.currentTimeMillis();
            }
            else
              System.out.println("Not a valid gesture.");
            // Our addition end
          }
          else if(position.getId() == AbstractCursorInputEvt.INPUT_UPDATED)
          {
            registerPreDrawAction(new IPreDrawAction()
            {
              public void processAction()
              {
                boolean firstPoint = false;
                Vector3D lastDrawnPoint = cursorToLastDrawnPoint.get(cursor);
                Vector3D pos = new Vector3D(position.getX(), position.getY(), 0);

                if(lastDrawnPoint == null)
                {
                  lastDrawnPoint = new Vector3D(pos);
                  cursorToLastDrawnPoint.put(cursor, lastDrawnPoint);
                  firstPoint = true;
                }
                else
                {
                  if(lastDrawnPoint.equalsVector(pos))
                    return;
                }

                float scaledStepDistance = stepDistance * brushScale;

                Vector3D direction = pos.getSubtracted(lastDrawnPoint);
                float distance = direction.length();
                direction.normalizeLocal();
                direction.scaleLocal(scaledStepDistance);

                float howManySteps = distance / scaledStepDistance;
                int stepsToTake = Math.round(howManySteps);

                // Force draw at 1st point
                if(firstPoint && stepsToTake == 0)
                {
                  stepsToTake = 1;
                }
                // System.out.println("Steps: " + stepsToTake);

                // GL gl = Tools3D.getGL(mtApp);
                // gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA,
                // GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE,
                // GL.GL_ONE_MINUS_SRC_ALPHA);

                mtApp.pushMatrix();
                // We would have to set up a default view here
                // for stability? (default cam etc?)
                getSceneCam().update();

                Vector3D currentPos = new Vector3D(lastDrawnPoint);
                for(int i = 0; i < stepsToTake; i++)
                { // start i at 1? no, we add first step at 0
                  // already
                  currentPos.addLocal(direction);
                  // Draw new brush into FBO at correct
                  // position
                  Vector3D diff = currentPos.getSubtracted(localBrushCenter);

                  mtApp.pushMatrix();
                  mtApp.translate(diff.x, diff.y);

                  // NOTE: works only if brush upper left at
                  // 0,0
                  mtApp.translate(brushWidthHalf, brushHeightHalf);
                  mtApp.scale(brushScale);

                  if(dynamicBrush)
                  {
                    // Rotate brush randomly
                    // mtApp.rotateZ(PApplet.radians(Tools3D.getRandom(0,
                    // 179)));
                    // mtApp.rotateZ(PApplet.radians(Tools3D.getRandom(-85,
                    // 85)));
                    mtApp.rotateZ(PApplet.radians(ToolsMath.getRandom(-25, 25)));
                    // mtApp.rotateZ(PApplet.radians(Tools3D.getRandom(-9,
                    // 9)));
                    mtApp.translate(-brushWidthHalf, -brushHeightHalf);
                  }

                  /*
                   * //Use random brush from brushes int brushIndex =
                   * Math.round(Tools3D.getRandom(0, brushes.length-1));
                   * AbstractShape brushToDraw = brushes[brushIndex];
                   */
                  AbstractShape brushToDraw = drawShape;

                  // Draw brush
                  brushToDraw.drawComponent(mtApp.g);

                  mtApp.popMatrix();
                }
                mtApp.popMatrix();

                cursorToLastDrawnPoint.put(cursor, currentPos);
              }

              public boolean isLoop()
              {
                return false;
              }
            });
          }
          else
          {
            // Our addition begin
            // We have to decrease touchCount here as otherwise you could
            // quickly tap three times to change colors, which isn't
            // multi-touch.
            if(touchCount > 0)
            {
              --touchCount;
              System.out.println("Touches down to "
                  + Integer.toString(touchCount));
            }
            // If there are no more fingers on the board then we can start
            // accepting our three finger gesture again.
            if(touchCount == 0)
            {
              System.out.println("No more fingers on board.");
              isValidGesture = true;
              lastTouchTime = Long.MAX_VALUE - THREEFINGERTHRESHOLD;
            }
            // Our addition end
            cursorToLastDrawnPoint.remove(cursor);
          }
        }
        return false;
      }
    });

  }

  public void setBrush(AbstractShape brush)
  {
    this.drawShape = brush;
    this.localBrushCenter = drawShape.getCenterPointLocal();
    this.brushWidthHalf = drawShape.getWidthXY(TransformSpace.LOCAL) / 2f;
    this.brushHeightHalf = drawShape.getHeightXY(TransformSpace.LOCAL) / 2f;
    this.stepDistance = brushWidthHalf / 2.8f;
    this.drawShape.setFillColor(this.brushColor);
    this.drawShape.setStrokeColor(this.brushColor);
  }

  public void setBrushColor(MTColor color)
  {
    this.brushColor = color;
    if(this.drawShape != null)
    {
      drawShape.setFillColor(color);
      drawShape.setStrokeColor(color);
    }
  }

  public void setBrushScale(float scale)
  {
    this.brushScale = scale;
  }

  public void onEnter()
  {
  }

  public void onLeave()
  {
  }
}
