package myGame;

import tage.input.action.AbstractInputAction;
import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class VertMovement extends AbstractInputAction {
    private MyGame game;
    private PhysicsEngine physicsEngine;
    private Avatar avatar;
    private PhysicsObject physicalAvatar;
    private Vector3f oldPosition, newPosition;
    private float speed = 0.5f;

    public VertMovement(MyGame g) {
        game = g;
        physicsEngine = game.getPhysicsEngine();
        physicalAvatar = game.getPhysicsAvatar();
    }

    @Override
    public void performAction(float time, Event e) {
        String keyValue = e.getComponent().toString();
        avatar = game.getAvatar();
        oldPosition = avatar.getWorldLocation();

        physicsEngine.removeObject(game.avaID);

        if (keyValue.equals("W")) {
            newPosition = oldPosition.add(0.0f, speed, 0.0f);
            avatar.setLocalLocation(newPosition);
            game.getProtocolClient().sendMoveMessage(newPosition);
        } else if (keyValue.equals("S")) {
            newPosition = oldPosition.add(0.0f, -speed, 0.0f);
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
        game.avaID = physicsEngine.nextUID();
		physicalAvatar = physicsEngine.addSphereObject(game.avaID, 0f, tempTransform, 0.75f);

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