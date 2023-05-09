package myGame;

import javax.swing.AbstractAction;

import net.java.games.input.Event;
import tage.input.action.AbstractInputAction;

public class ToggleMusic extends AbstractInputAction{
    MyGame game;
    int soundCount = 0;

    public ToggleMusic(MyGame g) {
        game = g;
    }

    @Override
    public void performAction(float time, Event e) {
        if (soundCount == 0) {
            soundCount++;
            game.setBackgroundMusic(soundCount);
        } else if (soundCount == 1) {
            soundCount++;
            game.setBackgroundMusic(soundCount);
        } else {
            soundCount = 0;
            game.setBackgroundMusic(soundCount);
        }
    }
    
}
