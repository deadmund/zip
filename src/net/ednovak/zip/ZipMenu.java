package net.ednovak.zip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ZipMenu extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zip_menu_layout);
        
        Button free_butt = (Button) findViewById(R.id.freestyle);
        free_butt.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		startActivity(new Intent(v.getContext(), Freestyle.class));
        	}
        });
        
        Button tuner_butt = (Button) findViewById(R.id.tuner);
        tuner_butt.setOnClickListener(new OnClickListener() {
        	public void onClick(View v){
        		startActivity(new Intent(v.getContext(), Tuner.class));
        	}
        });
        
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.zip_menu_layout, menu);
        return true;
    }
    
    

    
    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    */

}
