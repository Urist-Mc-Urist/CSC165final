package myGame;

import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;

public class Border extends GameObject{
    private Vector3f ballLoc;

    public Border(ObjShape shape, TextureImage skin) {
        super(GameObject.root(), shape, skin);
    }

    public void trackingWall(GameObject ball) {
        ballLoc = ball.getWorldLocation();
        float x = this.getWorldLocation().x();

        // Tentative
        this.setLocalLocation(new Vector3f(x, ballLoc.y(), ballLoc.z()));
    }

    public void trackingCeil(GameObject ball) {
        ballLoc = ball.getWorldLocation();
        float y = this.getWorldLocation().y();

        // Tentative
        this.setLocalLocation(new Vector3f(ballLoc.x(), y, ballLoc.z()));
    }
}
