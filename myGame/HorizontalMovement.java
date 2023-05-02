package myGame;

import tage.input.action.AbstractInputAction;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class HorizontalMovement extends AbstractInputAction {
    private MyGame game;
    private PhysicsEngine physicsEngine;
    private Avatar avatar;
    private PhysicsObject physicalAvatar;
    private Vector3f oldPosition, newPosition;
    private float speed = 0.3f;

    public HorizontalMovement(MyGame g) {
        game = g;
        physicsEngine = game.getPhysicsEngine();
        physicalAvatar = game.getPhysicsAvatar();
    }

    @Override
    public void performAction(float time, Event e) {
        String keyValue = e.getComponent().toString();
        avatar = game.getAvatar();
        oldPosition = avatar.getWorldLocation();

        if (keyValue.equals("A")) {
            if (game.getPlayerNum() == 1) { newPosition = oldPosition.add(speed, 0.0f, 0.0f); }
            else if (game.getPlayerNum() == 2) { newPosition = oldPosition.add(-speed, 0.0f, 0.0f); }
            avatar.setLocalLocation(newPosition);
            game.getProtocolClient().sendMoveMessage(newPosition);
        } else if (keyValue.equals("D")) {
            if (game.getPlayerNum() == 1) { newPosition = oldPosition.add(-speed, 0.0f, 0.0f); }
            else if (game.getPlayerNum() == 2) { newPosition = oldPosition.add(speed, 0.0f, 0.0f); }
            avatar.setLocalLocation(newPosition);
            game.getProtocolClient().sendMoveMessage(newPosition);
        }
        
        rebuildAvatar();
    }

    public void rebuildAvatar() {
        double[ ] tempTransform;
		Matrix4f translation;
        
        translation = new Matrix4f(avatar.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(game.vals));
		physicalAvatar = physicsEngine.addSphereObject(physicsEngine.nextUID(), 0f, tempTransform, 0.75f);

		physicalAvatar.setBounciness(1.0f);
		avatar.setPhysicsObject(physicalAvatar);
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
}