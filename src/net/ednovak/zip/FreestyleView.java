package net.ednovak.zip;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

public class FreestyleView extends View{
	public FreestyleView(Context context, AttributeSet attrs){
		super(context, attrs);
	}
		
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		canvas.drawColor(Color.BLUE);
	}
}
