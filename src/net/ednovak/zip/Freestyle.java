package net.ednovak.zip;

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
	private double curFreq = 400;
	private AudioTrack audioTrack;
	private final int sampleRate = 8000;
	private boolean fingerDown;
	private byte[] myBuffer;
	
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
		Log.d("Freestyle:onCreate", "bufferSize: " + ATBufferSize);
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
		
		// Set an initial tone simlpy
		changeTone(100, 200);

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
	}
	
	
	protected void changeTone(double y, double max){
		// first number + (percentage of X)  hz range of [firstnumber, firstnumber+lastNumber]
		curFreq = toneMin + (y / max) * (toneMax - toneMin);
		Log.d("Freestyle:changeTone", "tone: " + curFreq);
		// I set the buffer to this length so it fits the length of the wave exactly twice.
		// This means the wave starts and ends at 0 which removes the clicking noise that
		// arises between two waves
		int length = (int) (Math.round(sampleRate / curFreq) + 1);
		myBuffer = new byte[length*2];

		for(int i = 0; i < length; i++){
			double val = sampleSin(i);
			short sVal = (short)(val * (Short.MAX_VALUE));
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
	
	
	private double sampleSin(int index){
		return Math.sin( 2 * Math.PI * index * (curFreq/sampleRate));
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
			
			if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN){ // First finger down
				fingerDown = true;
				startSoundThread();
			}
			
			if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP){ // Last finger up
				fingerDown = false;
				stopSoundThread();
			}
			
			
			Canvas c = null;
			while (c == null){
				c = surfaceHolder.lockCanvas();
			}
			changeTone(c.getHeight() - event.getY(), c.getWidth());
			reDraw(c, event);
			surfaceHolder.unlockCanvasAndPost(c);
			
			return true;
			//return super.onTouchEvent(event);
		}
		
		
	}
}
