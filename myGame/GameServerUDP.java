package myGame;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {
  public GameServerUDP(int localPort) throws IOException {
    super(localPort, ProtocolType.UDP);
  }
  
  @Override
  public void processPacket(Object o, InetAddress senderIP, int senderPort) {
    String message = (String)o;
    System.out.println("Server received message: " + message);
    String[] msgTokens = message.split(",");

    if(msgTokens.length > 0){

      // case where server receives a JOIN message
      // format: join,localid
      if(msgTokens[0].compareTo("join") == 0){
        try{
          IClientInfo ci;
          ci = getServerSocket().createClientInfo(senderIP, senderPort);
          UUID clientID = UUID.fromString(msgTokens[1]);
          addClient(ci, clientID);
          sendJoinedMessage(clientID, true);
        }
        catch (IOException e){
          e.printStackTrace();
        }
      }

      // case where server receives a CREATE message
      // format: create,localid,x,y,z
      if(msgTokens[0].compareTo("create") == 0)
      { 
        UUID clientID = UUID.fromString(msgTokens[1]);
        String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
        sendCreateMessages(clientID, pos);
        sendWantsDetailsMessages(clientID);
      }

      // case where server receives a BYE message
      // format: bye,localid
      if(msgTokens[0].compareTo("bye") == 0)
      { 
        UUID clientID = UUID.fromString(msgTokens[1]);
        sendByeMessages(clientID);
        removeClient(clientID);
      }

      // case where server receives a DETAILS-FOR message
      if(msgTokens[0].compareTo("dsfr") == 0)
      { 
        // etc..... 
      }

      // case where server receives a MOVE message
      if(msgTokens[0].compareTo("move") == 0)
      { 
        // etc..... 
      }
    }
  }

  @Override
  public void acceptClient(IClientInfo client, Object o) {
    // TODO Auto-generated method stub
    
  }

  private void sendJoinedMessage(UUID clientID, boolean success) {
    // format: join, success or join, failure
    try{
      String message = new String("join,");
      if (success) message += "success";
      else message += "failure";
      sendPacket(message, clientID);
    }
    catch (IOException e){
      e.printStackTrace(); 
    }
  }

  private void sendCreateMessages(UUID clientID, String[] pos){
    // format: create,localid,x,y,z
    try{
      String message = new String("create,");
      message += clientID.toString() + ",";
      message += pos[0] + ",";
      message += pos[1] + ",";
      message += pos[2];
      sendPacket(message, clientID);
    }
    catch (IOException e){
      e.printStackTrace(); 
    }
  }

  private void sendWantsDetailsMessages(UUID clientID){
    // format: wtdt,localid
    try{
      String message = new String("wtdt,");
      message += clientID.toString();
      sendPacket(message, clientID);
    }
    catch (IOException e){
      e.printStackTrace(); 
    }
  }

  private void sendByeMessages(UUID clientID){
    // format: bye,localid
    try{
      String message = new String("bye,");
      message += clientID.toString();
      sendPacket(message, clientID);
    }
    catch (IOException e){
      e.printStackTrace(); 
    }
  }
}
