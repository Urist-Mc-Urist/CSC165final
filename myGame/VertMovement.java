package myGame;

import tage.input.action.AbstractInputAction;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class VertMovement extends AbstractInputAction {
    private MyGame game;
    private GameObject avatar;
    private Vector3f oldPosition, newPosition;
    private float speed = 0.3f;

    public VertMovement(MyGame g) {
        game = g;
    }
    @Override
    public void performAction(float time, Event e) {
        String keyValue = e.getComponent().toString();

        avatar = game.getAvatar();

        oldPosition = avatar.getWorldLocation();

        if (keyValue.equals("W")) {
            newPosition = oldPosition.add(0.0f, speed, 0.0f);
            avatar.setLocalLocation(newPosition);
            game.getProtocolClient().sendMoveMessage(newPosition);
        } else if (keyValue.equals("S")) {
            newPosition = oldPosition.add(0.0f, -speed, 0.0f);
            avatar.setLocalLocation(newPosition);
            game.getProtocolClient().sendMoveMessage(newPosition);
        }
    }
}