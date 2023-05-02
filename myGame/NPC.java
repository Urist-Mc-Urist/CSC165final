package myGame;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import org.joml.*;

public class NPC extends GameObject{
    private Vector3f ballLoc;

    public NPC(ObjShape shape, TextureImage skin) {
        super(GameObject.root(), shape, skin);
    }

    public void trackingAI(GameObject ball) {
        ballLoc = ball.getWorldLocation();
        float z = this.getWorldLocation().z();

        // Tentative
        this.setLocalLocation(new Vector3f(ballLoc.x(), ballLoc.y(), z));
    }

}