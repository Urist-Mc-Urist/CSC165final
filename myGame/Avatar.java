package myGame;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import org.joml.*;

public class Avatar extends GameObject{
    
    //private GameObject avatar;

    public Avatar(ObjShape shape, TextureImage skin) {
        //avatar = new GameObject(GameObject.root(), shape, skin);
        super(GameObject.root(), shape, skin);
    }
}
