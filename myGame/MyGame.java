package myGame;

import tage.*;
import tage.Light.LightType;
import tage.audio.AudioManagerFactory;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;
import tage.audio.joal.JOALAudioManager;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;
import tage.input.InputManager;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.nodeControllers.*;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsEngineFactory;
import tage.physics.PhysicsObject;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

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
import javax.swing.*;

import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

import com.jogamp.openal.sound3d.Vec3f;

public class MyGame extends VariableFrameRateGame
{
	// engines
	private static Engine engine;
	private PhysicsEngine physicsEngine;
	public PhysicsEngine getPhysicsEngine() { return physicsEngine; }
	public float vals[] = new float[16];

	// camera
	private Camera cam;
	public Camera getCam() { return cam; }

	//
	private JPanel menu;

	// managers
	private InputManager im;
	private GhostManager gm;

	// update variables
	private int counter=0;
	private Vector3f currentPosition;
	private Matrix4f initialTranslation, initialRotation, initialScale;
	private double startTime, prevTime, elapsedTime, amt;

	// objects and avatars
	private GameObject moon, asteroid;
	private Border rightWall, leftWall, ceiling;
	private PhysicsObject asteroidP, avatarP, opponentP, moonP, rwP, lwP, ceilP;
	public PhysicsObject getPhysicsAvatar() { return avatarP; }
	public int astroID, oppID, avaID, rwID, lwID, ceilID;
	//public float left[ ], right[ ], down[ ];
	private Avatar avatar;
	public Avatar getAvatar() { return avatar; }
  	private NPC opponent;

	// shapes and textures
  	private AnimatedShape avatarShape;
	private ObjShape ghostS, moonTShape, AIShape, astroShape, plane, cube, floatingWall;
	private TextureImage ghostT, avatarSkin, moonSkin, border, moonTerrain, AISkin, astroSkin;

	// light
	private Light light, spotlight1, spotlight2, astroLight;
	private boolean togglableLight = true;
	private float spotlightHeight = 10000f;
	public boolean isLightOn() { return togglableLight; }
	public void turnLightOn() { 
		togglableLight = true; 
		spotlight1.setLocation(new Vector3f(0f, spotlightHeight, 0f)); 
		astroLight.setDiffuse(0, 0, 0);
		astroLight.setSpecular(0, 0, 0);
	}
	public void turnLightOff() { 
		togglableLight = false; 
		spotlight1.setLocation(new Vector3f(0f, -200f, 0f)); 
		astroLight.setDiffuse(1, 0, 0);
		astroLight.setSpecular(1, 0, 0);
	}

	// sound
	private IAudioManager audioMgr;
	private AudioResource resource1, resource2, resource3;
	private Sound backgroundMusic1, backgroundMusic2, astroSound;
	public void setBackgroundMusic(int i) {
		if (i == 0) {
			backgroundMusic1.play();
		} else if (i == 1) {
			backgroundMusic1.stop();
			backgroundMusic2.play();
		} else {
			backgroundMusic2.stop();
		}
	}

	// multiplayer tracking
	private int playerNum = 0;
	public void setPlayerNum(int num) { playerNum = num; }
	public int getPlayerNum() { return playerNum; }
	final private Vector3f AVATAR_ONE_POS = new Vector3f(0f,0f,-50f);
	final private Vector3f AVATAR_TWO_POS = new Vector3f(0f,0f,50f);
	public Vector3f getPlayerPosition() { return avatar.getWorldLocation(); }

  	Vector3f avatarUp, avatarFwd, avatarRight; // possibly unneeded

	// server variables
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
		avatarShape = new AnimatedShape("ship.rkm", "ship.rks");
		avatarShape.loadAnimation("ENGINE_VIBRATE", "engine_vibrate.rka");
    	//avatarShape = new ImportedModel("Ship.obj");
		AIShape = new ImportedModel("Paddle.obj");
    	astroShape = new ImportedModel("asteroid.obj");
		ghostS = new ImportedModel("Paddle.obj");
    	moonTShape = new TerrainPlane(1000);
		plane = new Plane();
		cube = new Cube();
		floatingWall = new ImportedModel("rock.obj");
	}

	@Override
	public void loadTextures()
	{	
		ghostT = new TextureImage("Paddle.png");
    	avatarSkin = new TextureImage("SpaceshipTex.png");
		AISkin = new TextureImage("Paddle.png");
    	astroSkin = new TextureImage("asteroid.png");
    	moonSkin = new TextureImage("squareMoonMap.jpg");
		moonTerrain = new TextureImage("squareMoonMap.jpg");
		border = new TextureImage("rock.png");
	}

  @Override
	public void loadSkyBoxes()
	{
		fluffyClouds = (engine.getSceneGraph()).loadCubeMap("starrySky");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(fluffyClouds);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;


		// build avatar 
		avatar = new Avatar(avatarShape, avatarSkin);
		initialTranslation = (new Matrix4f()).translation(0,-5,-50);
		initialScale = (new Matrix4f()).scaling(1.5f);
		avatar.setLocalTranslation(initialTranslation);
		avatar.setLocalScale(initialScale);
    	avatarShape.playAnimation("ENGINE_VIBRATE", 1, AnimatedShape.EndType.LOOP, 100000000);

		// build opponent
		opponent = new NPC(AIShape, AISkin);
		initialTranslation = (new Matrix4f()).translation(0,0,50);
		initialScale = (new Matrix4f()).scaling(1.5f);
		opponent.setLocalTranslation(initialTranslation);
		opponent.setLocalScale(initialScale);

		// build moon terrain
		moon = new GameObject(GameObject.root(), moonTShape, moonSkin);
		initialTranslation = (new Matrix4f()).translation(0,-50,0);
		initialScale = (new Matrix4f()).scaling(500f, 5f, 500f);
		moon.setLocalTranslation(initialTranslation);
		moon.setLocalScale(initialScale);
		moon.setHeightMap(moonTerrain);

		float scale = 8.0f;

		// build right wall
		rightWall = new Border(floatingWall, border);
		initialTranslation = (new Matrix4f()).translation(50,0,0);
		initialScale = (new Matrix4f()).scaling(scale);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(90), 0, 1, 0);
		rightWall.setLocalTranslation(initialTranslation);
		rightWall.setLocalScale(initialScale);
		rightWall.setLocalRotation(initialRotation);

		// build left wall
		leftWall = new Border(floatingWall, border);
		initialTranslation = (new Matrix4f()).translation(-50,0,0);
		initialScale = (new Matrix4f()).scaling(scale);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(-90), 0, 1, 0);
		leftWall.setLocalTranslation(initialTranslation);
		leftWall.setLocalScale(initialScale);
		leftWall.setLocalRotation(initialRotation);

		// build ceiling
		ceiling = new Border(floatingWall, border);
		initialTranslation = (new Matrix4f()).translation(0,50,0);
		initialScale = (new Matrix4f()).scaling(scale);
		initialRotation = (new Matrix4f()).rotation((float)Math.toRadians(-90), 1, 0, 0);
		ceiling.setLocalTranslation(initialTranslation);
		ceiling.setLocalScale(initialScale);
		ceiling.setLocalRotation(initialRotation);

    	// build asteroid
		asteroid = new GameObject(GameObject.root(), astroShape, astroSkin);
		initialTranslation = (new Matrix4f()).translation(0, 0, 0);
		initialScale = (new Matrix4f()).scaling(2.0f);
		asteroid.setLocalTranslation(initialTranslation);
		asteroid.setLocalScale(initialScale);

	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(0f, 0f, 0f);

		light = new Light();
		light.setLocation(new Vector3f(0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(light);

		spotlight1 = new Light();
		spotlight1.setType(LightType.SPOTLIGHT);
		spotlight1.setLocation(new Vector3f(0f, spotlightHeight, 0f));
		spotlight1.setDirection(new Vector3f(0, -1, 0));
		spotlight1.setAmbient(0.5f, 0.5f, 0.5f);
		(engine.getSceneGraph()).addLight(spotlight1);

		//spotlight2 = new Light();
		//spotlight2.setType(LightType.SPOTLIGHT);
		//spotlight2.setLocation(new Vector3f(0f, 100f, 50f));
		//spotlight2.setDirection(new Vector3f(0, -1, -1));
		//(engine.getSceneGraph()).addLight(spotlight2);

		astroLight = new Light();
		astroLight.setLocation(new Vector3f(0,0,0));
		astroLight.setSpecular(0, 0, 0);
		astroLight.setDiffuse(0, 0, 0);
		astroLight.setRange(0.1f);
		(engine.getSceneGraph()).addLight(astroLight);
	}

	public void initAudio()
	{ 
		audioMgr = AudioManagerFactory.createAudioManager("tage.audio.joal.JOALAudioManager");
		if (!audioMgr.initialize())
		{ 
			System.out.println("Audio Manager failed to initialize!");
			return;
		}

		// https://pixabay.com/music/upbeat-space-120280/ - space.wav
		// https://pixabay.com/music/techno-trance-background-loop-melodic-techno-04-3822/ - zenman.wav
		// https://pixabay.com/sound-effects/054883-bounce-38937/ - bounce.wav
		resource1 = audioMgr.createAudioResource("assets/sounds/space.wav", AudioResourceType.AUDIO_SAMPLE);
		resource2 = audioMgr.createAudioResource("assets/sounds/zenman.wav", AudioResourceType.AUDIO_SAMPLE);
		resource3 = audioMgr.createAudioResource("assets/sounds/bounce.wav", AudioResourceType.AUDIO_SAMPLE);
		
		backgroundMusic1 = new Sound(resource1,SoundType.SOUND_MUSIC, 5, true);
		backgroundMusic1.initialize(audioMgr);
		backgroundMusic1.setMaxDistance(100.0f);
		backgroundMusic1.setMinDistance(0.5f);
		backgroundMusic1.setRollOff(1000f);
		backgroundMusic1.setLocation(ceiling.getWorldLocation());

		backgroundMusic2 = new Sound(resource2,SoundType.SOUND_MUSIC, 5, true);
		backgroundMusic2.initialize(audioMgr);
		backgroundMusic2.setMaxDistance(100.0f);
		backgroundMusic2.setMinDistance(0.5f);
		backgroundMusic2.setRollOff(1000f);
		backgroundMusic2.setLocation(ceiling.getWorldLocation());

		astroSound = new Sound(resource3, SoundType.SOUND_EFFECT, 50, false);
		astroSound.initialize(audioMgr);
		astroSound.setMaxDistance(100.0f);
		astroSound.setMinDistance(0.5f);
		astroSound.setRollOff(0.1f);
		astroSound.setLocation(asteroid.getWorldLocation());
		
		setEarParameters();
		backgroundMusic1.play();
	}

	
	@Override
	public void initializeGame()
	{
		//initialize script engine
		ScriptEngineManager factory = new ScriptEngineManager();
		jsEngine = factory.getEngineByName("js");

		// initialize physics engine
		String pEngine = "tage.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0f, 0f, 0f};
		physicsEngine = PhysicsEngineFactory.createPhysicsEngine(pEngine);
		physicsEngine.initSystem();
		physicsEngine.setGravity(gravity);

		//test script
		testScript = new File ("assets/scripts/testScript.js");
		this.runScript(testScript);

		//initialize audio
		initAudio();

		// menu panel
		menu = new JPanel();
		menu.setBounds(50, 50, 1000, 1000);
		menu.setBackground(Color.gray);
		JButton b1=new JButton("Button 1");     
        b1.setBounds(50,100,80,30);    
        b1.setBackground(Color.yellow);   
        JButton b2=new JButton("Button 2");   
        b2.setBounds(100,100,80,30);    
        b2.setBackground(Color.green);   
        menu.add(b1);
		menu.add(b2);  
		engine.getRenderSystem().add(menu);

		// system time and window dimensions
		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ----------------- initialize camera ----------------
		(engine.getRenderSystem().getViewport("MAIN").getCamera()).setLocation(new Vector3f(0,0,5));
		cam = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		cam.setLocation(new Vector3f(0,0,-5));

		// ----------------- KEYBOARD INPUTS SECTION -----------------------------
		im = engine.getInputManager();

    	// Up/Down
		VertMovement vertMovement = new VertMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.W, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.S, vertMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// Left/Right
		HorizontalMovement horMovement = new HorizontalMovement(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.A, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.D, horMovement, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

		// Light on/off
		LightSwitch lightswitch = new LightSwitch(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.L, lightswitch, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// switch music
		ToggleMusic toggleMusic = new ToggleMusic(this);
		im.associateActionWithAllKeyboards(net.java.games.input.Component.Identifier.Key.M, toggleMusic, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

		// --------------------- Create Physics World ------------------------------
		float mass = 1.0f;
		float size[ ] = {15,15,1};
		float up[ ] = {0,1,0};
		float left[ ] = {-1,0,0};
		float right[ ] = {1,0,0};
		float down[ ] = {0,-1,0};
		float velo[] = {10f,5f,35f}; // start velosity of the ball
		double[ ] tempTransform;
		Matrix4f translation;
		
		// asteroid
		translation = new Matrix4f(asteroid.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		astroID = physicsEngine.nextUID();
		asteroidP = physicsEngine.addSphereObject(astroID, 1f, tempTransform, 1.5f);

		asteroidP.setBounciness(1.01f); // minimum speedup on each bounce without loosing velosity
		asteroidP.setLinearVelocity(velo);
		asteroidP.setFriction(0);
		asteroidP.setDamping(0, 0);
		asteroid.setPhysicsObject(asteroidP);

		// avatar
		translation = new Matrix4f(avatar.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		avaID = physicsEngine.nextUID();
		avatarP = physicsEngine.addBoxObject(avaID, 0f, tempTransform, size);

		avatarP.setBounciness(1.0f);
		avatar.setPhysicsObject(avatarP);

		// opponent
		translation = new Matrix4f(opponent.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		oppID = physicsEngine.nextUID(); 
		opponentP = physicsEngine.addSphereObject(oppID, 0f, tempTransform, 0.75f);

		opponentP.setBounciness(1.0f);
		opponent.setPhysicsObject(opponentP);

		// moon
		translation = new Matrix4f(moon.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		moonP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), tempTransform, up, 0.0f);

		moonP.setBounciness(1.0f);
		moon.setPhysicsObject(moonP);

		// right wall
		translation = new Matrix4f(rightWall.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		rwID = physicsEngine.nextUID();
		rwP = physicsEngine.addStaticPlaneObject(rwID, tempTransform, left, 0.0f);

		rwP.setBounciness(1.0f);
		rightWall.setPhysicsObject(rwP);

		// left wall
		translation = new Matrix4f(leftWall.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		lwID = physicsEngine.nextUID();
		lwP = physicsEngine.addStaticPlaneObject(lwID, tempTransform, right, 0.0f);

		lwP.setBounciness(1.0f);
		leftWall.setPhysicsObject(lwP);

		// ceiling
		translation = new Matrix4f(ceiling.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		ceilID = physicsEngine.nextUID();
		ceilP = physicsEngine.addStaticPlaneObject(ceilID, tempTransform, down, 0.0f);

		ceilP.setBounciness(1.0f);
		ceiling.setPhysicsObject(ceilP);

		// ------------------------- Networking --------------------------
		// initialize multiplayer mode
		setupNetworking();
		System.out.println("waiting for server response");
		while (playerNum == 0) {
			System.out.println("waiting");
		}

		if (playerNum == 1) {
			System.out.println("assigning player to player 1");
			avatar.setLocalLocation(AVATAR_ONE_POS);
		} else if (playerNum == 2) {
			System.out.println("assigning player to player 2");
			avatar.setLocalLocation(AVATAR_TWO_POS);
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
		
		// -------------------- GAME UPDATE ---------------------

		//animations
		avatarShape.updateAnimation();

		//update sound
		astroSound.setLocation(asteroid.getWorldLocation());
		setEarParameters();

		// update inputs and camera
		im.update((float)elapsedTime);
		positionCamToAvatar();
		processNetworking((float)elapsedTime);

		// AI update
		physicsEngine.removeObject(oppID);
		physicsEngine.removeObject(rwID);
		physicsEngine.removeObject(lwID);
		physicsEngine.removeObject(ceilID);
		opponent.trackingAI(asteroid);
		rightWall.trackingWall(asteroid);
		leftWall.trackingWall(asteroid);
		ceiling.trackingCeil(asteroid);
		rebuildOpponent();
		rebuildRightWall();
		rebuildLeftWall();
		rebuildCeiling();

		// ball rotation
    	//asteroid.setLocalRotation((new Matrix4f()).rotation(0.1f*(float)elapsedTime, 0, 1, 0));
		astroLight.setLocation(asteroid.getWorldLocation());

		// colision detection
		Matrix4f mat = new Matrix4f();
		Matrix4f mat2 = new Matrix4f().identity();
		checkForCollisions();
		physicsEngine.update((float)elapsedTime);
		for (GameObject go:engine.getSceneGraph().getGameObjects())
		{ 
			if (go.getPhysicsObject() != null)
			{ 
				mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
				mat2.set(3,0,mat.m30());
				mat2.set(3,1,mat.m31());
				mat2.set(3,2,mat.m32());
				go.setLocalTranslation(mat2);
			} 
		}
		
		// Check for Win/Loose Scenario
		if (asteroid.getWorldLocation().z() < -51) {
			System.out.println("You Loose");
			restartGame();
		} else if (asteroid.getWorldLocation().z() > 51) {
			System.out.println("You Win");
			restartGame();
		}
	}

  	private void positionCamToAvatar() {
		Vector3f loc = avatar.getWorldLocation();
		Vector3f fwd = avatar.getWorldForwardVector();
		Vector3f up = avatar.getWorldUpVector();
		Vector3f right = avatar.getWorldRightVector();
      	cam.followObjFloating(avatar, -30f, 10f, 0.8f, false);
	}

	private void checkForCollisions()
	{ 
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;
		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++)
		{ 
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for (int j = 0; j < manifold.getNumContacts(); j++)
			{ 
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 1.0f)
				{ 
					System.out.println("---- hit between " + obj1 + " and " + obj2);

					astroSound.setLocation(asteroid.getWorldLocation());
					setEarParameters();
					astroSound.play();

					break;
				} 
			} 
		} 
	}

	private void rebuildAsteroid() {
		double[ ] tempTransform;
		Matrix4f translation;
		float velo[] = {10f,5f,35f}; // start velosity of the ball
		
		translation = new Matrix4f(asteroid.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		astroID = physicsEngine.nextUID();
		asteroidP = physicsEngine.addSphereObject(astroID, 1f, tempTransform, 1.5f);

		asteroidP.setBounciness(1.01f); // minimum speedup on each bounce without loosing velosity
		asteroidP.setLinearVelocity(velo);
		asteroidP.setFriction(0);
		asteroidP.setDamping(0, 0);
		asteroid.setPhysicsObject(asteroidP);
	}

	private void rebuildOpponent() {
		double[ ] tempTransform;
		Matrix4f translation;
		
		translation = new Matrix4f(opponent.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		oppID = physicsEngine.nextUID();
		opponentP = physicsEngine.addSphereObject(oppID, 0f, tempTransform, 0.75f);

		opponentP.setBounciness(1.0f);
		opponent.setPhysicsObject(opponentP);
	}

	private void rebuildRightWall() {
		double[ ] tempTransform;
		Matrix4f translation;
		float left[ ] = {-1,0,0};
		
		translation = new Matrix4f(rightWall.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		rwID = physicsEngine.nextUID();
		rwP = physicsEngine.addStaticPlaneObject(rwID, tempTransform, left, 0.0f);

		rwP.setBounciness(1.0f);
		rightWall.setPhysicsObject(rwP);
	}

	private void rebuildLeftWall() {
		double[ ] tempTransform;
		Matrix4f translation;
		float right[ ] = {1,0,0};
		
		translation = new Matrix4f(leftWall.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		lwID = physicsEngine.nextUID();
		lwP = physicsEngine.addStaticPlaneObject(lwID, tempTransform, right, 0.0f);

		lwP.setBounciness(1.0f);
		leftWall.setPhysicsObject(lwP);
	}

	private void rebuildCeiling() {
		double[ ] tempTransform;
		Matrix4f translation;
		float down[ ] = {0,-1,0};
		
		translation = new Matrix4f(ceiling.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		ceilID = physicsEngine.nextUID();
		ceilP = physicsEngine.addStaticPlaneObject(ceilID, tempTransform, down, 0.0f);

		ceilP.setBounciness(1.0f);
		ceiling.setPhysicsObject(ceilP);
	}

	private void restartGame() {
		physicsEngine.removeObject(astroID);
		asteroid.setLocalLocation(new Vector3f(0, 0, 0));
		rebuildAsteroid();
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

	// ------------------ PHYSICS UTILITY FUNCTIONS ----------------
	private float[] toFloatArray(double[] arr)
	{ 
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) { ret[i] = (float)arr[i]; }
		return ret;
	}
	private double[] toDoubleArray(float[] arr)
	{ 
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) { ret[i] = (double)arr[i]; }
		return ret;
	}

	// ----------------------SOUND FUNCTIONS ---------------------
	public void setEarParameters()
	{ 
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		audioMgr.getEar().setLocation(avatar.getWorldLocation());
		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f));
	}

}

