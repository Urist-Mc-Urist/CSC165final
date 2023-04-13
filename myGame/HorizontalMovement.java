package myGame;

import tage.input.action.AbstractInputAction;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class HorizontalMovement extends AbstractInputAction {
    private MyGame game;
    private GameObject avatar;
    private Vector3f oldPosition, newPosition;
    private float speed = 0.2f;

    public HorizontalMovement(MyGame g) {
        game = g;
    }
    @Override
    public void performAction(float time, Event e) {
        String keyValue = e.getComponent().toString();

        avatar = game.getAvatar();

        oldPosition = avatar.getWorldLocation();

        if (keyValue.equals("D")) {
            newPosition = oldPosition.add(speed, 0.0f, 0.0f);
            avatar.setLocalLocation(newPosition);
        } else if (keyValue.equals("A")) {
            newPosition = oldPosition.add(-speed, 0.0f, 0.0f);
            avatar.setLocalLocation(newPosition);
        }
    }
}
