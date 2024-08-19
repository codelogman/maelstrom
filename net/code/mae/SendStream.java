
package net.code.btalk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.RecordControl;

import net.rim.device.api.crypto.CFBEncryptor;
import net.rim.device.api.crypto.CryptoException;
import net.rim.device.api.crypto.DESEncryptorEngine;
import net.rim.device.api.crypto.DESKey;
import net.rim.device.api.crypto.InitializationVector;
import net.rim.device.api.io.Base64OutputStream;
import net.rim.device.api.media.control.AudioPathControl;


public class SendStream implements PlayerListener{
	
    public static BTalk btalk;
	private Player mPlayer;
	public boolean isStarted=false;
	private String _name = "";
    private OutputStream mOutput= new OutputStream(){

        
        public void close() throws IOException {
        }

        public void flush() throws IOException {

        }

        public void write(byte[] buffer, int offset, int count) {
            try {
                if(isStarted)
                {                  
                    String snd = encodeBase64(buffer,offset,count);
                    btalk.sendCallSound("$_jsr;" + "<:" + _name + ":>;" + "[:" + snd + ":]", _name);
                }else
                {
                	//stop();
                }
            } catch (Throwable e) {
            }
        }

        public void write(byte[] buffer) throws IOException {
            write( buffer, 0, buffer.length);
            }

        public void write(int arg0) throws IOException {
        }
        
    };

    public SendStream(String name) {

        _name = name;
    }

/*    public void stop() {
        try {
            isStarted=false;
            RecordControl recordControl = (RecordControl) mPlayer.getControl("RecordControl");
            recordControl.stopRecord();
            recordControl.commit();
            mOutput.close();            
            try {
                if (mPlayer.getState() == Player.STARTED) {
                    mPlayer.stop();
                } 
                if (mPlayer.getState() != Player.CLOSED) {
                    mPlayer.close();
                }
            } catch (MediaException e) {
                e.printStackTrace();
            }
            recordControl = null;
        }  catch (Exception e) {
        } 

    }*/
    
    public void stop() {
        try {
            isStarted=false;
            
            try {
                if (mPlayer != null) {
                    mPlayer.close();
                    mOutput.close();
                    mPlayer = null;
                } 
            } catch (Exception e) {
                e.printStackTrace();
            }

            try
            {
            	((RecordControl) mPlayer.getControl("RecordControl")).commit();
            }
            catch(Exception e)
            {
            	
            }                                
        }  catch (Exception e) {
        }     	
    }

    public void start() {
        try {
            mPlayer = Manager.createPlayer("capture://audio?encoding=audio/amr&updateMethod=size&updateThreshold=160&rate=7950");
            mPlayer.addPlayerListener(this);
            mPlayer.realize();
            RecordControl recordControl = (RecordControl) mPlayer.getControl("RecordControl");
            recordControl.setRecordStream(mOutput);

            recordControl.startRecord();
            AudioPathControl  lPathCtr = (AudioPathControl) mPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
            lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSET);
            mPlayer.prefetch();
            mPlayer.start();
            isStarted=true;

        } catch (Throwable e) {
        } 

    }

    private String encodeBase64( byte[] toEncode, int offset, int length ) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(length);
        Base64OutputStream base64OutputStream = new Base64OutputStream( byteArrayOutputStream );
        try{
            base64OutputStream.write( toEncode, offset, length );
            base64OutputStream.flush();
            base64OutputStream.close();
            
        }
        catch (IOException ioe){
            System.out.println("Error in encodeBase64() : "+ioe.toString());
            return null;
        }
        return byteArrayOutputStream.toString();
}//fn encodeBase64

    public void playerUpdate(Player arg0, String event, Object eventData) {
            //sLogger.warn("Got event " + event + "[" + (eventData == null ? "" : eventData.toString()) + "]");
        if (isStarted && event.equals(PlayerListener.RECORD_STOPPED )) {
            //sLogger.warn("Unexpected recorder stop");
            //RecordControl recordControl = (RecordControl) mPlayer.getControl("RecordControl");
            //recordControl.startRecord();
        }
    }

    public void muteMic(boolean value) {
        try {
        if (mPlayer!= null) 
            if (value) {
                mPlayer.stop();
            } else {
                mPlayer.start();
            }
        //sLogger.info("Mic "+(value?"muted":"unmuted"));
        } catch (Throwable e) {
            //sLogger.error("Cannot "+(value?"mute":"unmute")+" mic", e);
        }
        
    }
    public boolean isMicMuted() {
        if (mPlayer!= null) 
            return mPlayer.getState()!=Player.STARTED;
        else
            return false;
    }       
    
    public void enableSpeaker(boolean value) {
        if (mPlayer == null) return;//just ignore
        AudioPathControl  lPathCtr = (AudioPathControl) mPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
        try {
            lPathCtr.setAudioPath(value?AudioPathControl.AUDIO_PATH_HANDSFREE:AudioPathControl.AUDIO_PATH_HANDSET);
            //sLogger.info("Send stream has speaker is "+(lPathCtr.getAudioPath()==AudioPathControl.AUDIO_PATH_HANDSFREE?"enabled":"disabled"));
        } catch (Throwable e) {
            //sLogger.error("Cannot "+(value?"enable":"disable")+" speaker", e);
        }       
    }
    protected Player getPlayer() {
        return mPlayer;
    }
    
    private static byte[] DESCFBEncryption( byte[] data )
    throws CryptoException, IOException
{  
    // Create a new DES key based on the 8 bytes in the secretKey array
    byte[] keyData = { (byte)0x01, (byte)0x23, (byte)0x45,(byte)0x67,
            (byte)0x89, (byte)0x01, (byte)0x23, (byte)0x45 };
    
    // Setup the Initialization vector.
    byte[] iv1 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
    
    DESKey key = new DESKey( keyData);
    
    // Set up the DESEngine.
    DESEncryptorEngine engine1 = new DESEncryptorEngine( key );


    // Set up the output stream.
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    // Setup the encryptor.
    CFBEncryptor encryptor = new CFBEncryptor( engine1, new InitializationVector( keyData ), outStream, true );

    // Write out the data to be encrypted.
    encryptor.write( data, 0, data.length );

    // Close the encryptor stream.
    encryptor.close();

    // Get the information from the underlying data stream.
    byte[] output = outStream.toByteArray();
    outStream.close();


    return output;
}

}
