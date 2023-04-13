package myGame;

import tage.*;
import tage.shapes.*;

import tage.input.InputManager;
import tage.input.action.*;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.nodeControllers.*;
import java.lang.Math;
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

  //script variables
	private File testScript;
	private long fileLastModifiedTime = 0;
	ScriptEngine jsEngine;

  //gameobject variables
  	private GameObject avatar;
  	public GameObject getAvatar() { return avatar; }
  	private ObjShape avatarShape;
  	private TextureImage avatarSkin;
	private Light light1;

  //skybox
	private int fluffyClouds;

	public MyGame() { super(); }

	public static void main(String[] args)
	{	MyGame game = new MyGame();
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	avatarShape = new Cube();
	}

	@Override
	public void loadTextures()
	{	avatarSkin = new TextureImage("ice.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialScale;

		// build dolphin in the center of the window
		avatar = new GameObject(GameObject.root(), avatarShape, avatarSkin);
		initialTranslation = (new Matrix4f()).translation(0,0,0);
		initialScale = (new Matrix4f()).scaling(0.5f);
		avatar.setLocalTranslation(initialTranslation);
		avatar.setLocalScale(initialScale);
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
		cam.setLocation(new Vector3f(0,0,5));

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
	{	// rotate dolphin if not paused
		lastFrameTime = currFrameTime;
		currFrameTime = System.currentTimeMillis();
		if (!paused) elapsTime += (currFrameTime - lastFrameTime) / 1000.0;
		avatar.setLocalRotation((new Matrix4f()).rotation((float)elapsTime, 0, 1, 0));

		im.update((float)elapsTime);

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
	}

	@Override
	public void loadSkyBoxes()
	{
		fluffyClouds = (engine.getSceneGraph()).loadCubeMap("fluffyClouds");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(fluffyClouds);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
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
}