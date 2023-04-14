package myGame;

import tage.*;
import tage.shapes.*;
import tage.nodeControllers.*;
import tage.rml.Vector3;
import tage.input.InputManager;
import tage.input.action.*;
import tage.networking.IGameConnection.ProtocolType;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;

import java.lang.Math;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.net.ssl.HostnameVerifier;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.joml.*;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	public static Engine getEngine() { return engine; }

	private Camera cam;
	public Camera getCam() { return cam; }
	private InputManager im;

  //engine variables
	private boolean paused=false;
	private int counter=0;
	private double lastFrameTime, currFrameTime, elapsTime;

  //networking variables
  private GhostManager gm;
  private String serverAddress;
  private int serverPort;
  private ProtocolType serverProtocol;
  private ProtocolClient protClient;
  private boolean isClientConnected = false;

  //script variables
	private File testScript;
	private long fileLastModifiedTime = 0;
	ScriptEngine jsEngine;

  //gameobject variables
  private GameObject avatar, moon, testObj;
  public GameObject getAvatar() { return avatar; }
  private ObjShape avatarShape, moonTShape, testShape;
  private TextureImage avatarSkin, moonSkin, moonTerrain, testText;
	private Light light1;

  //skybox
	private int fluffyClouds;

	public MyGame(String serverAddress, int serverPort, String protocol) { 
    super();
    gm = new GhostManager(this);
    this.serverAddress = serverAddress;
    this.serverPort = serverPort;
    if (protocol.toUpperCase().compareTo("TCP") == 0)
    this.serverProtocol = ProtocolType.TCP;
    else
    this.serverProtocol = ProtocolType.UDP;
  }

	public static void main(String[] args)
	{	
    MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

  private void setupNetworking(){
    isClientConnected = false;
    try{
      protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
    } 
    catch (UnknownHostException e)
    { 
      e.printStackTrace();
    } 
    catch (IOException e){
      e.printStackTrace(); 
    }
    if (protClient == null){
      System.out.println("missing protocol host"); 
    }
    else{
      // ask client protocol to send initial join message
      // to server, with a unique identifier for this client
      protClient.sendJoinMessage();
    } 
  }

  protected void processNetworking(float elapsTime){
    //process packets recieved by the client from the server
    if (protClient != null){
      protClient.processPackets();
    }
  }

	@Override
	public void loadShapes()
	{	avatarShape = new Cube();
		moonTShape = new TerrainPlane(1000);
		testShape = new Cube();
	}

	@Override
	public void loadTextures()
	{	avatarSkin = new TextureImage("ice.jpg");
		moonSkin = new TextureImage("checkerboardSmall.jpg");
		moonTerrain = new TextureImage("moonHM.jpg");
		testText = new TextureImage("checkerboardSmall.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build avatar in the center of the window
		avatar = new GameObject(GameObject.root(), avatarShape, avatarSkin);
		initialTranslation = (new Matrix4f()).translation(0,0,-100);
		initialScale = (new Matrix4f()).scaling(0.5f);
		avatar.setLocalTranslation(initialTranslation);
		avatar.setLocalScale(initialScale);

		// build moon terrain
		moon = new GameObject(GameObject.root(), moonTShape, moonSkin);
		initialTranslation = (new Matrix4f()).translation(0,-50,0);
		initialScale = (new Matrix4f()).scaling(100.0f);
		moon.setLocalTranslation(initialTranslation);
		moon.setLocalScale(initialScale);
		moon.setHeightMap(moonTerrain);

		// build test object
		testObj = new GameObject(GameObject.root(), testShape, testText);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		initialScale = (new Matrix4f()).scaling(5f);
		testObj.setLocalTranslation(initialTranslation);
		testObj.setLocalScale(initialScale);
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0.5f, 0.5f, 0.5f);
		light1 = new Light();
		light1.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		(engine.getSceneGraph()).addLight(light1);
	}

	@Override
	public void initializeGame()
	{	
    //setup networking
    setupNetworking();

		//initialize script engine
		ScriptEngineManager factory = new ScriptEngineManager();
		jsEngine = factory.getEngineByName("js");

		//test script
		testScript = new File ("assets/scripts/testScript.js");
		this.runScript(testScript);
		
		lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		im = engine.getInputManager();

		// ------------- positioning the camera -------------
		cam = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		cam.setLocation(new Vector3f(0,0,-5));

		// ---------------------- Action Controls -----------------------------
		// Up/Down
		VertMovement vertMovement = new VertMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		//Left/Right
		HorizontalMovement horMovement = new HorizontalMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}


	@Override
	public void update()
	{	// calculate elapsed time
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		elapsTime += (currFrameTime - lastFrameTime) / 1000.0;
		// rotate test object
		testObj.setLocalRotation((new Matrix4f()).rotation((float)elapsTime, 0, 1, 0));

		// Update the input manager
		im.update((float)elapsTime);

		// set cam to follow the avatar
		positionCamToAvatar();

		// build and set HUD
		int elapsTimeSec = Math.round((float)elapsTime);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "Keyboard hits = " + counterStr;
		Vector3f hud1Color = new Vector3f(1,0,0);
		Vector3f hud2Color = new Vector3f(0,0,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

    processNetworking((float)elapsTime);
	}

	@Override
	public void loadSkyBoxes()
	{
		fluffyClouds = (engine.getSceneGraph()).loadCubeMap("fluffyClouds");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(fluffyClouds);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	private void positionCamToAvatar() {
		Vector3f loc = avatar.getWorldLocation();
		Vector3f fwd = avatar.getWorldForwardVector();
		Vector3f up = avatar.getWorldUpVector();
		Vector3f right = avatar.getWorldRightVector();
		cam.setU(right);
		cam.setV(up);
		cam.setN(fwd);
		cam.setLocation(loc.add(up.mul(1.3f)).add(fwd.mul(-5.0f)));
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	
		super.keyPressed(e);
	}

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

  // Ghost methods
  public GhostManager getGhostManager() {
    return gm;
  }

  public ObjShape getGhostShape(){
    return testShape;
  }

  public TextureImage getGhostTexture(){
    return testText;
  }

  public Vector3f getPlayerPosition(){
    return avatar.getWorldLocation();
  }

  private class SendCloseConnectionPacketAction extends AbstractInputAction{ 
    // for leaving the game... need to attach to an input device
    @Override
    public void performAction(float time, net.java.games.input.Event evt){
      if(protClient != null && isClientConnected == true){
        protClient.sendByeMessage();
      } 
    } 
  }
}