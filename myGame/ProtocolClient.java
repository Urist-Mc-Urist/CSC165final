package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient {
  private MyGame game;
  private UUID id;
  private GhostManager ghostManager;
  

  public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException{ 
    super(remAddr, remPort, pType);
    this.game = game;
    this.id = UUID.randomUUID();
    ghostManager = game.getGhostManager();
  }
  
  @Override
  public void processPacket(Object o) {
    String message = (String)o;
    System.out.println("Client received message: " + message);
    String[] msgTokens = message.split(",");
    
    if(msgTokens.length > 0){
      
      // case where client receives a JOINED message
      // format: joined,success
      if(msgTokens[0].compareTo("joined") == 0){
        boolean success = Boolean.parseBoolean(msgTokens[1]);
        if(success){
          System.out.println("Successfully joined the game");
        }
        else{
          System.out.println("Failed to join the game");
        }
      }
      
      // case where client receives a CREATE message
      // format: create,localid,x,y,z
      if(msgTokens[0].compareTo("create") == 0)
      { 
        // etc..... 
      }
      
      // case where client receives a BYE message
      // format: bye,localid
      if(msgTokens[0].compareTo("bye") == 0)
      { 
        // etc..... 
      }
      
      // case where client receives a DETAILS-FOR message
      if(msgTokens[0].compareTo("dsfr") == 0)
      { 
        // etc..... 
      }
      
      // case where client receives a DETAILS message
      if(msgTokens[0].compareTo("ds") == 0)
      { 
        // etc..... 
      }
      
      // case where client receives a MOVE message
      if(msgTokens[0].compareTo("move") == 0)
      { 
        // etc..... 
      }
      
      // case where client receives a WANT-DETAILS message
      if(msgTokens[0].compareTo("wd") == 0)
      { 
        // etc..... 
      }
      
      // case where client receives a WANTS-DETAILS-FOR message
      if(msgTokens[0].compareTo("wsfr") == 0)
      { 
        // etc..... 
      }
    }
  }

  public void sendJoinMessage(){
    String message = "join," + id.toString();
    try{
      sendPacket(message);
    }
    catch(IOException e){
      e.printStackTrace();
    }
  }
  
  public void sendByeMessage(){
    String message = "bye," + id.toString();
    try{
      sendPacket(message);
    }
    catch(IOException e){
      e.printStackTrace();
    }
  }
}
