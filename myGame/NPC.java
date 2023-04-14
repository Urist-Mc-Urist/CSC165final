package myGame;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import org.joml.*;

public class NPC {
    private GameObject npc;

    private Vector3f ballLoc;

    public NPC(ObjShape shape, TextureImage skin) {
        npc = new GameObject(GameObject.root(), shape, skin);
    }

    public void trackingAI(GameObject ball) {
        ballLoc = ball.getWorldLocation();
        float z = npc.getLocalLocation().z();

        // Tentative
        npc.setLocalLocation(new Vector3f(ballLoc.x(), ballLoc.y(), z));
    }

    public void setLocalTranslation(Matrix4f initialTranslation) { npc.setLocalTranslation(initialTranslation); }

    public void setLocalRotation(Matrix4f initialRotation) { npc.setLocalRotation(initialRotation); }

    public void setLocalScale(Matrix4f initialScale) { npc.setLocalScale(initialScale); }

    public Vector3f getLocalLocation() { return npc.getLocalLocation(); }

}