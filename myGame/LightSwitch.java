package myGame;

import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class LightSwitch extends AbstractInputAction{
    private MyGame game;

    public LightSwitch(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        if (game.isLightOn()) {
            game.turnLightOff();
        } else { game.turnLightOn(); }
    }
    
}
