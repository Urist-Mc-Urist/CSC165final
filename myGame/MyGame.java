package myGame;

import tage.*;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;
import tage.input.InputManager;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.nodeControllers.*;

import java.lang.Math;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;

import java.net.UnknownHostException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

import com.jogamp.openal.sound3d.Vec3f;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;

	private Camera cam;
	public Camera getCam() { return cam; }
	private InputManager im;
	private GhostManager gm;

	private int counter=0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	private GameObject tor, avatar, x, y, z, moon, astroid;
  private NPC opponent;
	private ObjShape torS, avatarShape, ghostS, dolS, linxS, linyS, linzS, moonTShape, oppShape, astroShape;
	private TextureImage doltx, ghostT, avatarSkin, moonSkin, moonTerrain, oppSkin, astroSkin ;
	private Light light;

  Vector3f avatarUp, avatarFwd, avatarRight;

	private String serverAddress;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
  public ProtocolClient getProtocolClient() { return protClient; }
	private boolean isClientConnected = false;

  //script variables
  private File testScript;
  private long fileLastModifiedTime = 0;
  ScriptEngine jsEngine;

  //skybox
	private int fluffyClouds;

  
	// test variables
	private boolean r = true;
	private float n = 0.0f;
	private int numColls = 0;
	private boolean north = true;
	private float m = 0.0f;

	public MyGame(String serverAddress, int serverPort, String protocol)
	{	super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	torS = new Torus(0.5f, 0.2f, 48);
    avatarShape = new ImportedModel("Paddle.obj");
		oppShape = new Cube();
    astroShape = new ImportedModel("astroid.obj");
		ghostS = new ImportedModel("Paddle.obj");
		dolS = new ImportedModel("dolphinHighPoly.obj");
		linxS = new Line(new Vector3f(0f,0f,0f), new Vector3f(3f,0f,0f));
		linyS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,3f,0f));
		linzS = new Line(new Vector3f(0f,0f,0f), new Vector3f(0f,0f,-3f));
    moonTShape = new TerrainPlane(1000);
	}

	@Override
	public void loadTextures()
	{	doltx = new TextureImage("Dolphin_HighPolyUV.png");
		ghostT = new TextureImage("Paddle.png");
    avatarSkin = new TextureImage("Paddle.png");
		oppSkin = new TextureImage("ice.jpg");
    astroSkin = new TextureImage("astroid.png");
    moonSkin = new TextureImage("checkerboardSmall.jpg");
		moonTerrain = new TextureImage("moonHM.jpg");
	}

  @Override
	public void loadSkyBoxes()
	{
		fluffyClouds = (engine.getSceneGraph()).loadCubeMap("fluffyClouds");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(fluffyClouds);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;


		// build avatar in the center of the window
		avatar = new GameObject(GameObject.root(), avatarShape, avatarSkin);
		initialTranslation = (new Matrix4f()).translation(0,0,-50);
		initialScale = (new Matrix4f()).scaling(1.5f);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(-90), 1, 0, 0);
		avatar.setLocalTranslation(initialTranslation);
		avatar.setLocalRotation(initialRotation);
		avatar.setLocalScale(initialScale);

		opponent = new NPC(oppShape, oppSkin);
		initialScale = (new Matrix4f()).scaling(1.5f);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(-90), 1, 0, 0);
		opponent.setLocalTranslation(initialTranslation);
		opponent.setLocalRotation(initialRotation);
		opponent.setLocalScale(initialScale);

		// build moon terrain
		moon = new GameObject(GameObject.root(), moonTShape, moonSkin);
		initialTranslation = (new Matrix4f()).translation(0,-50,0);
		initialScale = (new Matrix4f()).scaling(100.0f);
		moon.setLocalTranslation(initialTranslation);
		moon.setLocalScale(initialScale);
		moon.setHeightMap(moonTerrain);

    // build astroid
		astroid = new GameObject(GameObject.root(), astroShape, astroSkin);
		initialTranslation = (new Matrix4f()).translation(0, 0, 0);
		initialScale = (new Matrix4f()).scaling(1.0f);
		astroid.setLocalTranslation(initialRotation);
		astroid.setLocalScale(initialScale);

	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(.5f, .5f, .5f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);
	}

	@Override
	public void initializeGame()
	{
    //initialize script engine
    ScriptEngineManager factory = new ScriptEngineManager();
    jsEngine = factory.getEngineByName("js");

    //test script
    testScript = new File ("assets/scripts/testScript.js");
    this.runScript(testScript);

    prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ----------------- initialize camera ----------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		cam = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		cam.setLocation(new Vector3f(0,0,-5));

		// ----------------- INPUTS SECTION -----------------------------
		im = engine.getInputManager();

    VertMovement vertMovement = new VertMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		//Left/Right
		HorizontalMovement horMovement = new HorizontalMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
;


		setupNetworking();
	}

	public GameObject getAvatar() { return avatar; }

	@Override
	public void update()
	{	elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		
		// build and set HUD
		int elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String countColl = Integer.toString(numColls);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "Collisions = " + countColl;
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(0,0,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);
		// update inputs and camera
		im.update((float)elapsedTime);
		//positionCameraBehindAvatar();
		processNetworking((float)elapsedTime);

    // set cam to follow the avatar
		positionCamToAvatar();

    astroid.setLocalRotation((new Matrix4f()).rotation(3.0f*(float)elapsedTime, 0, 1, 0));

		// test tracking AI
		if (n >= 50) { r = false; }
		if (n <= -50) { r = true; }
		if (r) { n += 0.2f; }
		else { n -= 0.2f; }

		// Game update
		if (north == true) { m += 0.3f; }
		else { m -= 0.3f; }
		astroid.setLocalLocation(new Vector3f(n, 0, m));
		opponent.trackingAI(astroid);

		// colision detection
		detectCollision();
	}

	private void positionCameraBehindAvatar()
	{	Vector4f u = new Vector4f(-1f,0f,0f,1f);
		Vector4f v = new Vector4f(0f,1f,0f,1f);
		Vector4f n = new Vector4f(0f,0f,1f,1f);
		u.mul(avatar.getWorldRotation());
		v.mul(avatar.getWorldRotation());
		n.mul(avatar.getWorldRotation());
		Matrix4f w = avatar.getWorldTranslation();
		Vector3f position = new Vector3f(w.m30(), w.m31(), w.m32());
		position.add(-n.x()*2f, -n.y()*2f, -n.z()*2f);
		position.add(v.x()*.75f, v.y()*.75f, v.z()*.75f);
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		c.setLocation(position);
		c.setU(new Vector3f(u.x(),u.y(),u.z()));
		c.setV(new Vector3f(v.x(),v.y(),v.z()));
		c.setN(new Vector3f(n.x(),n.y(),n.z()));
	}

  private void positionCamToAvatar() {
		Vector3f loc = avatar.getWorldLocation();
		Vector3f fwd = avatar.getWorldForwardVector();
		Vector3f up = avatar.getWorldUpVector();
		Vector3f right = avatar.getWorldRightVector();
		cam.setU(right);
		cam.setV(fwd);
		cam.setN(up.mul(-1f));
		cam.setLocation(loc.add(fwd.mul(2.5f)).add(up.mul(-8.0f)));
	}

  private void detectCollision() {
		// detect player
		if ((avatar.getLocalLocation().x() + 1.0f) >= astroid.getLocalLocation().x() &&
			(avatar.getLocalLocation().x() - 1.0f) <= astroid.getLocalLocation().x() &&
			(avatar.getLocalLocation().y() + 1.0f) >= astroid.getLocalLocation().y() &&
			(avatar.getLocalLocation().y() - 1.0f) <= astroid.getLocalLocation().y() &&
			(avatar.getLocalLocation().z() + 1.0f) >= astroid.getLocalLocation().z() &&
			(avatar.getLocalLocation().z() - 1.0f) <= astroid.getLocalLocation().z()) {
				System.out.println("Detected collision with avatar");
				numColls++;
				north = true;
		}
		// detect NPC
		if ((opponent.getLocalLocation().x() + 1.0f) >= astroid.getLocalLocation().x() &&
			(opponent.getLocalLocation().x() - 1.0f) <= astroid.getLocalLocation().x() &&
			(opponent.getLocalLocation().y() + 1.0f) >= astroid.getLocalLocation().y() &&
			(opponent.getLocalLocation().y() - 1.0f) <= astroid.getLocalLocation().y() &&
			(opponent.getLocalLocation().z() + 1.0f) >= astroid.getLocalLocation().z() &&
			(opponent.getLocalLocation().z() - 1.0f) <= astroid.getLocalLocation().z()) {
				System.out.println("Detected collision with NPC");
				numColls++;
				north = false;
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		super.keyPressed(e);
	}

  // SCRIPTS
  private void runScript(File scriptFile){
    try{
      FileReader fileReader = new FileReader(scriptFile);
      jsEngine.eval(fileReader);
      fileReader.close();
    }
    catch (FileNotFoundException e1){ 
      System.out.println(scriptFile + " not found " + e1); 
    }
    catch (IOException e2){
      System.out.println("IO problem with " + scriptFile + e2); 
    }
    catch (ScriptException e3){
      System.out.println("ScriptException in " + scriptFile + e3); 
    }
    catch (NullPointerException e4){
      System.out.println ("Null ptr exception reading " + scriptFile + e4);
    } 
  }

	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostT; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	
	private void setupNetworking()
	{	isClientConnected = false;	
		try 
		{	protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		} 	catch (UnknownHostException e) 
		{	e.printStackTrace();
		}	catch (IOException e) 
		{	e.printStackTrace();
		}
		if (protClient == null)
		{	System.out.println("missing protocol host");
		}
		else
		{	// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime)
	{	// Process packets received by the client from the server
		if (protClient != null)
			protClient.processPackets();
	}

	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	public void setIsConnected(boolean value) { this.isClientConnected = value; }
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	protClient.sendByeMessage();
			}
		}
	}
}