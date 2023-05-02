package myGame;

import tage.input.action.AbstractInputAction;
import tage.GameObject;
import net.java.games.input.Event;
import org.joml.*;

public class HorizontalMovement extends AbstractInputAction {
    private MyGame game;
    private Avatar avatar;
    private Vector3f oldPosition, newPosition;
    private float speed = 0.3f;

    public HorizontalMovement(MyGame g) {
        game = g;
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
    }
}