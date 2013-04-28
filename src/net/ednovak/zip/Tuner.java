package net.ednovak.zip;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Tuner extends Activity implements Runnable{
	
	private EditText freq_text;
	private final int sampleRate = 8000;
	private double curFreq;
	private boolean playing = false;
	private AudioTrack audioTrack;
	private byte[] myBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tuner_layout);

		// Set up the spinner to choose notes (musical notes)
		final Spinner notes_spin = (Spinner) findViewById(R.id.note);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.notes_array, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		notes_spin.setAdapter(adapter);
		
		notes_spin.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int pos, long id){
				Object note = parent.getItemAtPosition(pos);
				freq_text.setText(resolveNote(note.toString()));		
			}
			
			@Override
			public void onNothingSelected(AdapterView<?> parent){
				freq_text.setText("0");
			}
		});
		
		// The frequency text box
		freq_text = (EditText)findViewById(R.id.freq);
		Log.d("tuner:onCreate", "Just testing");
		
		// Play button
		final ToggleButton play_butt = (ToggleButton) findViewById(R.id.play_stop);
		play_butt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v){
				if (playing){
					stopTone();
				}
				else{
					try{
						curFreq = Double.valueOf(freq_text.getText().toString());
					}
					catch(NumberFormatException e){
						e.printStackTrace();
						curFreq = 440;
					}
					checkFreq();
					playTone();
				}
			}
		});
	}
	
	
	private double sampleSin(int i, double freq){
		return Math.sin(2 * Math.PI * i * (freq/sampleRate));
	}
	
	
	private void genTone(){
		int length = (int) (Math.round( sampleRate / curFreq) + 1);
		myBuffer = new byte[length*2];

		for(int i = 0; i < length; i++){
			double val = sampleSin(i, curFreq);
			short sVal = (short)(val * (Short.MAX_VALUE));
			myBuffer[i*2] = (byte) (sVal & 0x00ff);
			myBuffer[i*2 + 1] = (byte) ((sVal & 0xff00) >>> 8);
		}
	}
	
	
	// This function translates from the string note name to a
	// string frequency.
	// I had to use if statements and not a switch because the java switch only
	// supports ints (which is stupid).  It returns a string because it actually 
	// is used to write the value into the edittext
	private String resolveNote(String note){
		 String freq = "0";
		 if (note.equals("A")){
			 freq = "440";
		 }
		 if (note.equals("B")){
			 freq = "493.88";
		 }
		 if (note.equals("C")){
			 freq = "523.25";
		 }
		 if (note.equals("D")){
			 freq = "587.33";
		 }
		 if (note.equals("E")){
			 freq = "659.26";
		 }
		 if (note.equals("F")){
			 freq = "698.46";
		 }
		 if (note.equals("G")){
			 freq = "783.99";
		 }
		 return freq;
	}
	
	
	// Makes sure the frequency input is acceptable
	private void checkFreq(){
		double maxFreq = (double)sampleRate / 2.0;
		if (curFreq > maxFreq){
			Toast.makeText(this, "Maximum frequency allowed: " + String.valueOf(maxFreq), Toast.LENGTH_LONG).show();
			freq_text.setText("440");
			curFreq = 440;
		}
		if(curFreq < 20){
			Toast.makeText(this, "Minimum frequency allowed: 20", Toast.LENGTH_LONG).show();
			freq_text.setText("20");
			curFreq = 20;
		}
	}
	
	
	private void playTone(){
		playing = true;
		final int ATBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, 
				 AudioFormat.CHANNEL_CONFIGURATION_MONO, 
				 AudioFormat.ENCODING_PCM_16BIT, ATBufferSize,
				 AudioTrack.MODE_STREAM);
		// I am using a streaming mode audiotrack because I do not know the duration of the tone needed
		genTone();
		Thread t = new Thread(this);
		t.start();
	}
	
	private void stopTone(){
		playing = false;
		audioTrack.pause();
		audioTrack.flush();
		// Actually, pause and flush is enough but I wanna be thorough
		audioTrack.stop();
		audioTrack.release();
	}
	
	@Override
	public void run(){
		audioTrack.play();
		while(playing){
			audioTrack.write(myBuffer, 0, myBuffer.length);
		}
	}
	
}
