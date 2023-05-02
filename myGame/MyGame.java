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

	private GameObject moon, astroid;
	private Avatar avatar;
	public Avatar getAvatar() { return avatar; }
  	private NPC opponent;

	private int playerNum = 0;
	public void setPlayerNum(int num) { playerNum = num; }
	public int getPlayerNum() { return playerNum; }
	final private Vector3f AVATAR_ONE_POS = new Vector3f(0f,0f,-50f);
	final private Vector3f AVATAR_TWO_POS = new Vector3f(0f,0f,50f);
	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

	private ObjShape avatarShape, ghostS, moonTShape, AIShape, astroShape;
	private TextureImage ghostT, avatarSkin, moonSkin, moonTerrain, AISkin, astroSkin;
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
	{	
    	avatarShape = new ImportedModel("Paddle.obj");
		AIShape = new ImportedModel("Paddle.obj");
    	astroShape = new ImportedModel("astroid.obj");
		ghostS = new ImportedModel("Paddle.obj");
    	moonTShape = new TerrainPlane(1000);
	}

	@Override
	public void loadTextures()
	{	
		ghostT = new TextureImage("Paddle.png");
    	avatarSkin = new TextureImage("Paddle.png");
		AISkin = new TextureImage("Paddle.png");
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


		// build avatar 
		avatar = new Avatar(avatarShape, avatarSkin);
		initialTranslation = (new Matrix4f()).translation(0,0,-50);
		initialScale = (new Matrix4f()).scaling(1.5f);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(-90), 1, 0, 0);
		avatar.setLocalTranslation(initialTranslation);
		//avatar.setLocalRotation(initialRotation);
		avatar.setLocalScale(initialScale);

		// build opponent
		opponent = new NPC(AIShape, AISkin);
		initialTranslation = (new Matrix4f()).translation(0,0,50);
		initialScale = (new Matrix4f()).scaling(1.5f);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(-90), 1, 0, 0);
		opponent.setLocalTranslation(initialTranslation);
		//opponent.setLocalRotation(initialRotation);
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

    	// Up/Down
		VertMovement vertMovement = new VertMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// Left/Right
		HorizontalMovement horMovement = new HorizontalMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);


		setupNetworking();

		while (playerNum == 0) {
			//protClient.processPackets();
			System.out.println("waiting");
		}

		if (playerNum == 1) {
			System.out.println("assigning player to player 1");
			avatar.setLocalLocation(AVATAR_ONE_POS);
		} else if (playerNum == 2) {
			System.out.println("assigning player to player 2");
			avatar.setLocalLocation(AVATAR_TWO_POS);
			//cam.lookAt(0f, 0f, 0f);
		}
	}

	

	@Override
	public void update()
	{	elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt = elapsedTime * 0.03;
		
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
		positionCamToAvatar();
		processNetworking((float)elapsedTime);

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
		
		// Check for Win/Loose Scenario
		if (astroid.getWorldLocation().z() < -51) {
			//System.out.println("You Loose");
		} else if (astroid.getWorldLocation().z() > 51) {
			//System.out.println("You Win");
		}
	}

  private void positionCamToAvatar() {
		Vector3f loc = avatar.getWorldLocation();
		Vector3f fwd = avatar.getWorldForwardVector();
		Vector3f up = avatar.getWorldUpVector();
		Vector3f right = avatar.getWorldRightVector();
		if (playerNum == 1){ 
			cam.setU(new Vector3f(-1.0f, 0.0f, 0.0f));
			cam.setV(new Vector3f(0.0f, 1.0f, 0.0f));
			cam.setN(new Vector3f(0.0f, 0.0f, 1.0f));
			cam.setLocation(loc.add(fwd.mul(-8.0f)).add(up.mul(2.5f))); 
		}
		else if (playerNum == 2) { 
			cam.setU(new Vector3f(1.0f, 0.0f, 0.0f));
			cam.setV(new Vector3f(0.0f, 1.0f, 0.0f));
			cam.setN(new Vector3f(0.0f, 0.0f, -1.0f));
			cam.setLocation(loc.add(fwd.mul(8.0f)).add(up.mul(2.5f))); 
		}
	}

  private void detectCollision() {
		// detect player
		if ((avatar.getWorldLocation().x() + 1.0f) >= astroid.getWorldLocation().x() &&
			(avatar.getWorldLocation().x() - 1.0f) <= astroid.getWorldLocation().x() &&
			(avatar.getWorldLocation().y() + 1.0f) >= astroid.getWorldLocation().y() &&
			(avatar.getWorldLocation().y() - 1.0f) <= astroid.getWorldLocation().y() &&
			(avatar.getWorldLocation().z() + 1.0f) >= astroid.getWorldLocation().z() &&
			(avatar.getWorldLocation().z() - 1.0f) <= astroid.getWorldLocation().z()) {
				System.out.println("Detected collision with avatar");
				numColls++;
				north = true;
		}
		// detect NPC
		if ((opponent.getWorldLocation().x() + 1.0f) >= astroid.getWorldLocation().x() &&
			(opponent.getWorldLocation().x() - 1.0f) <= astroid.getWorldLocation().x() &&
			(opponent.getWorldLocation().y() + 1.0f) >= astroid.getWorldLocation().y() &&
			(opponent.getWorldLocation().y() - 1.0f) <= astroid.getWorldLocation().y() &&
			(opponent.getWorldLocation().z() + 1.0f) >= astroid.getWorldLocation().z() &&
			(opponent.getWorldLocation().z() - 1.0f) <= astroid.getWorldLocation().z()) {
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