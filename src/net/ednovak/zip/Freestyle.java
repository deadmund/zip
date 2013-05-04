package net.ednovak.zip;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class Freestyle extends Activity implements Runnable{
	
	private ZipSurfaceView SV;
	private AudioTrack audioTrack;
	private final int sampleRate = 8000;
	private boolean fingerDown;
	private byte[] myBuffer = new byte[1024]; // This is an arbitrary length
	final private float[] pointers = new float[3];
	private float canvasHeight;
	
	private int ballMin;
	private int ballMax;
	private int toneMin;
	private int toneMax;
	

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    SV = new ZipSurfaceView(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(SV);
		
		final int ATBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		//Log.d("Freestyle:onCreate", "bufferSize: " + ATBufferSize);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				 AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				 AudioFormat.ENCODING_PCM_16BIT, ATBufferSize,
				 AudioTrack.MODE_STREAM);
		// I'm using a streaming mode audiotrack because of the dynamic tone changing
		// and because I don't know the duration (or even the future) of the tone
		
		// I pull these out here because they cannot be change outside of this app
		final SharedPreferences SP  = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		ballMin = Integer.valueOf(SP.getString("free_ball_min", "20"));
		ballMax = Integer.valueOf(SP.getString("free_ball_max", "100"));
		toneMin = Integer.valueOf(SP.getString("free_tone_min", "100"));
		toneMax = Integer.valueOf(SP.getString("free_tone_max", "900"));

	}
	
	@Override
	protected void onResume(){
		super.onResume();
		SV.onResumeSV();
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		SV.onPauseSV();
		fingerDown = false;
		stopSoundThread();
	}
	
	
	
	protected float getFreq(float y){
		y = canvasHeight - y;
		return (toneMin + (y / canvasHeight) * (toneMax - toneMin));
	}
	
	
	
	// Sad that I have to write this function
	protected double getMax(double[] input){
		double max = Double.NEGATIVE_INFINITY;
		for(int i = 0; i < input.length; i++){
			if (input[i] > max){
				max = input[i];
			}
		}
		return max;
	}
	
	
	// Combine all the tones together, it might be better to sometimes remove
	// tones than always rebuild the entire waveform
	protected void reBuildTone(){
		
		// Collect all  the samples together in tmp (doubles)
		double l = sampleRate / pointers[0];
		for (int i = 1; i < pointers.length; i++){
			if (pointers[i] != 0.0){
				l = l * sampleRate/getFreq(pointers[i]);
			}
		}
		Log.d("main:reBuildTone", "length : "+ l);
		int length = (int) (Math.round(l) + 1);
		myBuffer = new byte[length*2];
		double[] tmp = new double[length];
		for(int i = 0; i < pointers.length; i++){
			double freq = getFreq(pointers[i]);
			for(int j = 0; j < tmp.length; j++){
				tmp[j] += sampleSin(j, freq);
			}
		}
		
		// Convert to PCM and normalize all in one step, place in myBuffer
		double max = getMax(tmp);
		for(int i = 0; i < tmp.length; i++){
			short sVal = (short)(tmp[i]/max * Short.MAX_VALUE ); 
			myBuffer[i*2] = (byte) (sVal & 0x00ff);
			myBuffer[i*2 + 1] = (byte) ((sVal & 0xff00) >>> 8);
		}
	}
	
	private void startSoundThread(){
		Thread t = new Thread(this);
		t.start();
	}
	
	private void stopSoundThread(){
		audioTrack.pause();
		audioTrack.flush();
		audioTrack.stop();
		// don't release the audio track because the user might
		// rapidly stop generating and then start generating sound
	}
	
	
	private double sampleSin(int index, double freq){
		return Math.sin( 2 * Math.PI * index * (freq/sampleRate));
	}
	
	@Override
	public void run(){
		audioTrack.play();
		while(fingerDown){
			audioTrack.write(myBuffer, 0, myBuffer.length);
		}
	}

	
	class ZipSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
		SurfaceHolder surfaceHolder;
		private Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		public ZipSurfaceView(Context ctx){
			super(ctx);
			surfaceHolder = getHolder();
			surfaceHolder.addCallback(this);
			p.setColor(Color.BLACK);
		}
		
		// Maybe this should be removed?  IDK
		public void onResumeSV(){

		}
		
		public void onPauseSV(){
			fingerDown = false;
		}
		
		
		protected void reDraw(Canvas c, MotionEvent e){
			
			// I need to come up with a better way to set the frequency!
			double freq = .02;
			int red =(int) (Math.sin(freq*e.getX() + 0) * 127) + 128;
			int green =(int) (Math.sin(freq*e.getX() + 2) * 127) + 128;
			int blue =(int) (Math.sin(freq*e.getX() + 4) * 127) + 128;

			c.drawColor(Color.rgb(red, green, blue));
			float rad = (float) ( ballMin + ((e.getY() / c.getHeight()) * (ballMax - ballMin)) );
			c.drawCircle(e.getX(), e.getY(), rad, p);
		}
		
		// This function is necessary to be a surfaceView but I dont' need it
		public void surfaceDestroyed(SurfaceHolder surfaceHolder){
			
		}
		
		// This function is necessary to be a surfaceview but I don't need it
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){
			//Log.d("ZipSurfaceView:surfaceChange", "the surface changed for some reason?");
		}
		
		
		public void surfaceCreated(SurfaceHolder holder){
			surfaceHolder = holder;
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event){
			//Log.d("zipSurfaceView:onTouch", "touched!");
			
			// Get the canvas
			Canvas c = null;
			while (c == null){
				c = surfaceHolder.lockCanvas();
			}
			canvasHeight = c.getHeight();
			
			// Get the "Action Event"
			int action = MotionEventCompat.getActionMasked(event);
			int index = MotionEventCompat.getActionIndex(event);
			float y = MotionEventCompat.getY(event, index);
			float x = MotionEventCompat.getX(event, index);
			int id = MotionEventCompat.getPointerId(event, index);
			
			switch (action){
			case MotionEvent.ACTION_DOWN: // First finger down
				pointers[0] = y;
				fingerDown = true;
				startSoundThread();
				break;
				
			case MotionEvent.ACTION_POINTER_DOWN: // Another finger down				
				pointers[id] = y;
				break;
				
			case MotionEvent.ACTION_POINTER_UP:
				pointers[id] = 0.0f;
				break;
				
			case MotionEvent.ACTION_MOVE: // Change in frequency(ies)
				pointers[id] = y;
				break;
				
			case MotionEvent.ACTION_UP:
				fingerDown = false;
				stopSoundThread();
				pointers[0] = 0.0f;
				pointers[1] = 0.0f;
				pointers[2] = 0.0f;
				break;
			}
			
			for(int i = 0; i < pointers.length; i++){
				Log.d("freestyle:onTouchEvent", "pointers[i]: " + pointers[i]);
			}
			
			reBuildTone();
			reDraw(c, event);
			surfaceHolder.unlockCanvasAndPost(c);
			
			return true;
			//return super.onTouchEvent(event);
		}
	}
}
