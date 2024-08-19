package net.code.btalk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.RecordControl;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

public class AudioRecorderThread extends Thread implements PlayerListener
{
	private Player _player;
	private RecordControl _rcontrol;
	private ByteArrayOutputStream _output;
	private byte _data[];

	public AudioRecorderThread() {}

	private int getSize()
	{
		return (_output != null ? _output.size() : 0);
	}

	public byte[] getVoice()
	{
		return _data;
	}

	public void run() {
		try {
			// Create a Player that captures live audio.
			_player = Manager.createPlayer("capture://audio?encoding=audio/amr");
			_player.realize();

			// Get the RecordControl, set the record stream,
			_rcontrol = (RecordControl)_player.getControl("RecordControl");

			//Create a ByteArrayOutputStream to capture the audio stream.
			_output = new ByteArrayOutputStream();
			_rcontrol.setRecordStream(_output);
			_rcontrol.startRecord();
			_player.start();

		} catch (final Exception e) {
		}
	}

	public void stop() {
		try {
			//Stop recording, capture data from the OutputStream,
			//close the OutputStream and player.
			_rcontrol.commit();
			_data = _output.toByteArray();

			//saveRecordedFile(_data);

			_output.close();
			_player.close();

		} catch (Exception e) {
			synchronized (UiApplication.getEventLock()) {
				Dialog.inform(e.toString());
			}
		}
	}

	public void playerUpdate(Player player, String event, Object eventData) 
	{
		Dialog.alert("Player " + player.hashCode() + " got event " + event + ": " + eventData);
	}
	
	public static boolean saveRecordedFile(byte[] data) {
	    try {
	        String filePath1 = "file:///SDCard/Blackberry/";
	        String fileName = "sample" + System.currentTimeMillis();
	        boolean existed = true;
	        for (int i = 0; i < Integer.MAX_VALUE; i++) {
	            try {
	                FileConnection fc = (FileConnection) Connector.open(filePath1 + fileName+ ".amr");
	                if (!fc.exists()) {
	                    existed = false;
	                }
	                fc.close();
	            } catch (IOException e) {
	                Dialog.alert("unable to save");
	                return existed;
	            }
	            if (!existed) {
	                fileName += i + ".amr";
	                filePath1 += fileName;
	                break;
	            }
	        }
	        System.out.println(filePath1);
	        System.out.println("");
	        FileConnection fconn = (FileConnection) javax.microedition.io.Connector .open(filePath1, javax.microedition.io.Connector.READ_WRITE);
	        if (fconn.exists())
	            fconn.delete();
	        fconn.create();

	        OutputStream outputStream = fconn.openOutputStream();
	        outputStream.write(data);
	        outputStream.close();
	        fconn.close();
	        return true;
	    } catch (Exception e) {
	    }
	    return false;
	}
}
