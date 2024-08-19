package net.code.btalk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.file.FileConnection;
import javax.microedition.media.Control;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import javax.microedition.media.protocol.ContentDescriptor;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;

import net.rim.device.api.crypto.CFBDecryptor;
import net.rim.device.api.crypto.CryptoTokenException;
import net.rim.device.api.crypto.CryptoUnsupportedOperationException;
import net.rim.device.api.crypto.DESEncryptorEngine;
import net.rim.device.api.crypto.DESKey;
import net.rim.device.api.crypto.InitializationVector;
import net.rim.device.api.media.control.AudioPathControl;

public class RecvStream implements  PlayerListener {
	private Player mPlayer;
	
   private SendStream mSendStream;

	private long mStartTime=0;
	public boolean mRunning;
	private boolean mBuffering=true;
	private long mPlayerTs=-1;
	public static BTalk btalk;	

	private void reset() {
		mPlayer=null;
		mStartTime=0;
		mBuffering=true;
		mPlayerTs=-1;
	}
	public SourceStream mInput= new SourceStream(){
		 byte [] sSilentAmr= {  (byte)0x3c, (byte)0x48, (byte)0xf5, (byte)0x1f,
			        			(byte)0x96, (byte)0x66, (byte)0x79, (byte)0xe1,
			        			(byte)0xe0, (byte)0x01, (byte)0xe7, (byte)0x8a,
			        			(byte)0xf0, (byte)0x00, (byte)0x00, (byte)0x00,
			        			(byte)0xc0, (byte)0x00, (byte)0x00, (byte)0x00,
			        			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
			        			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
			        			(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 }; 

		private boolean priority_set=false;
		

		ContentDescriptor mContentDescriptor=new ContentDescriptor("audio/amr");
		 /* (non-Javadoc)
		 * @see java.io.InputStream#read(byte[], int, int)
		 */
		public int read(byte[] b, int offset, int length) throws IOException {
				
		    int bytesRead = 0;
		    int available = 0;

		    if (mPlayer.getState() == mPlayer.UNREALIZED)
				return 0;		        	
				 
			try {
				if (!priority_set && Thread.currentThread().getPriority() != Thread.MAX_PRIORITY) {
					Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
					priority_set=true;
				}
				
				if(mRunning)
				{
				    bytesRead = btalk.bufferIStream.read(b, offset, length);
				}else
				{
				   return -1;
				}
				
				return bytesRead;
			} catch (Throwable e) {
				return -1;
			}
		}



		public ContentDescriptor getContentDescriptor() {
			return mContentDescriptor;
		}

		public long getContentLength() {
			return -1;
		}

		public int getSeekType() {
			return SourceStream.SEEKABLE_TO_START; 
		}

		public int getTransferSize() {
			return 160;
		}

		public long seek(long where) throws IOException {
			return where;
		}

		public long tell() {
			if (mStartTime==0) return 0;
			return (System.currentTimeMillis() - mStartTime);
		}

		public Control getControl(String controlType) {
			return null;
		}

		public Control[] getControls() {
			return null;
		}
	};

	public RecvStream(SendStream sendstream) {
	    mSendStream = sendstream;
	}

	public void writeBuffer(byte[] data)
	{
	    
	}

	public void stop() {
		if (mPlayer == null) return;//nothing to stop		
		mRunning=false;
				
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
	}

	public void start() {
		mRunning=true;
		try{
			mPlayer = Manager.createPlayer(new DataSource (null) {
				SourceStream[] mStream = {mInput};
				public void connect() throws IOException {					
				}

				public void disconnect() {
				
				}

				public String getContentType() {
					return "audio/amr";
				}

				public SourceStream[] getStreams() {
					return mStream;
				}

				public void start() throws IOException {
					
				}

				public void stop() throws IOException {
					
				}

				public Control getControl(String controlType) {
					return null;
				}

				public Control[] getControls() {
					return null;
				}
				
			});
	
			mPlayer.addPlayerListener(this);
			mPlayer.realize();
			AudioPathControl  lPathCtr = (AudioPathControl) mPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
			lPathCtr.setAudioPath(AudioPathControl.AUDIO_PATH_HANDSET);
			
	         try {
	                mPlayer.prefetch();
	                mPlayer.start();
	            } catch (MediaException e) {
	            }
			
		}catch (Throwable e){
		}

	}

	public void playerUpdate(Player arg0, String event, Object eventData) {
		if (event==PlayerListener.BUFFERING_STARTED){
			mBuffering=true;
		} else if (event == PlayerListener.BUFFERING_STOPPED) {
			mBuffering=false;
		} else if (event == PlayerListener.DEVICE_UNAVAILABLE) {
            //pausing both player and recorder
            try {
                //already stoped mPlayer.stop(); 
                 mSendStream.getPlayer().stop();
            } catch (Throwable e) {
            }
		} else if (event == PlayerListener.DEVICE_AVAILABLE
		        || event == "com.rim.externalStop") {
		    //starting both player and recorder
		    if(mRunning)
		    {
		    try {
		        if (mSendStream.getPlayer().getState()==Player.PREFETCHED)
		            mSendStream.getPlayer().start();
		        stop();
		        reset();
		        start();
		    } catch (Throwable e) {
		    }
		    }

		} else if ((event == PlayerListener.STOPPED) && mRunning) {
		    //starting player
		    try {
		        stop();
		        reset();
		        start();
		    } catch (Throwable e) {
		    }			
		}
	}
	
	public int getPlayLevel() {
		if (mPlayer !=null) {
			return ((VolumeControl)mPlayer.getControl("VolumeControl")).getLevel();
		} else {
			return 0;
		}
	}
	
	public void setPlayLevel(int level) {
		if (mPlayer !=null) {
			((VolumeControl)mPlayer.getControl("VolumeControl")).setLevel(level);
		}
	}
	
	private int getCurTs(){
		if (mStartTime==0) {
			mStartTime=System.currentTimeMillis();
		}
		return (int)((System.currentTimeMillis() - mStartTime)*8);
	}
	public void enableSpeaker(boolean value) {
		if (mPlayer == null) return;//just ignore
		AudioPathControl  lPathCtr = (AudioPathControl) mPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
		try {
			lPathCtr.setAudioPath(value?AudioPathControl.AUDIO_PATH_HANDSFREE:AudioPathControl.AUDIO_PATH_HANDSET);
		} catch (Throwable e) {
		}		
	}
	
	public boolean isSpeakerEnabled() {
		if (mPlayer == null) return false;//just ignore
		AudioPathControl  lPathCtr = (AudioPathControl) mPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
		return	lPathCtr.getAudioPath()==AudioPathControl.AUDIO_PATH_HANDSFREE;
	}
	public long getPlayerTs() {
		return mPlayerTs;
	}
	
	private static byte[] DESCFBDEncryption( byte[] data )
	{
			byte[] output = {  (byte)0x3c, (byte)0x48, (byte)0xf5, (byte)0x1f,
							   (byte)0x96, (byte)0x66, (byte)0x79, (byte)0xe1,
							   (byte)0xe0, (byte)0x01, (byte)0xe7, (byte)0x8a,
							   (byte)0xf0, (byte)0x00, (byte)0x00, (byte)0x00,
							   (byte)0xc0, (byte)0x00, (byte)0x00, (byte)0x00,
							   (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
							   (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
							   (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
			
			byte[] mask = {  (byte)0xcc, (byte)0xcc, (byte)0xcc, (byte)0xcc,
							 (byte)0x96, (byte)0xff, (byte)0xff, (byte)0xff,
							 (byte)0xe0, (byte)0x01, (byte)0xe7, (byte)0x8a,
							 (byte)0xfd, (byte)0x00, (byte)0xee, (byte)0x33,
							 (byte)0xfd, (byte)0x11, (byte)0xdd, (byte)0x33,
							 (byte)0xfd, (byte)0x11, (byte)0xff, (byte)0x33,
							 (byte)0xfd, (byte)0x11, (byte)0x22, (byte)0x33,
							 (byte)0xfd, (byte)0x11, (byte)0x55, (byte)0x33 };
	  
	        // Create a new DES key based on the 8 bytes in the secretKey array
			byte[] keyData = { (byte)0x01, (byte)0x23, (byte)0x45,(byte)0x67,
	                (byte)0x89, (byte)0x01, (byte)0x23, (byte)0x45 };

	        // Set up the DESEngine.
	        DESEncryptorEngine engine2;

		try {		
	    	
			DESKey key = new DESKey( keyData);
			
			engine2 = new DESEncryptorEngine( key );

	    // Set up the output stream.
	    ByteArrayInputStream inStream = new ByteArrayInputStream( data,0,data.length );

	    // Set up the encryptor.
	    CFBDecryptor decryptor = new CFBDecryptor( engine2, new InitializationVector( keyData ), inStream, true );

	    // Read in the data to be decrypted.
			decryptor.read( output, 0, output.length );

			
	    // Close the encryptor stream.
			decryptor.close();
			inStream.close();

		}catch( IOException e )
			{
			return mask;
			}
			catch( CryptoTokenException e )
			{ 
				return mask;
			} catch (CryptoUnsupportedOperationException e) {
				
				return mask;
			}

	    return output;
	}

}
