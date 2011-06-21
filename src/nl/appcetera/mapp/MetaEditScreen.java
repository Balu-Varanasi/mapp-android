package nl.appcetera.mapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;

/**
 * De klasse het editscherm van metadata (naam, omschrijving, kleur etc)
 * @author Joost
 */
public class MetaEditScreen extends Activity {
	
	private int polyID;
	private int polyColor;
	private EditText nameField;
	private EditText descriptionField;
	private MetaEditScreen metaEditScreen;
	public static final String ID_KEY = "ID";
	public static final String COLOR_KEY = "COLOR";
	public static final String NAME_KEY = "NAME";
	public static final String DESCRIPTION_KEY = "DESCRIPTION";
	public static final int RESULT_SAVE = 42;
	public static final int RESULT_CANCEL = 41;
	public static final int RESULT_DELETE = 40;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.metascreen);      
		//de bundle wordt uitgelezen zodat we de gegevens van de polygoon in kunnen vullen
        Bundle bundle = getIntent().getExtras();
        polyColor = bundle.getInt(COLOR_KEY);
        polyID = bundle.getInt(ID_KEY);
		String name = bundle.getString(NAME_KEY);
		String description = bundle.getString(DESCRIPTION_KEY);
		Log.v(Mapp.TAG, "Incoming description: "+description);
		//we vullen de naam in in het editscherm, als we een naam hebben
		nameField = (EditText) findViewById(R.id.edtInputName);
		if (name != null && name != "")
		{
			nameField.setText(name);
		}
		//en we vullen de description in, als er een description is
		descriptionField = (EditText) findViewById(R.id.edtInputDescription);
		if (description != null && description != "")
		{
			descriptionField.setText(description);
		}
		//we zetten een listener klaar die reageert op een verandering van kleur in de colorpicker
		OnColorChangedListener l = new OnColorChangedListener() {
            public void colorChanged(int color) {
            	polyColor = color;
            }
        };
        //Ook voegen we een nieuwe colorpicker toe aan een layout die daarvoor klaar staat
		ColorPickerView colorPickerView = new ColorPickerView(getApplicationContext(), l, polyColor);
		LinearLayout layout = (LinearLayout) findViewById(R.id.colorpickerlayout);
		layout.addView(colorPickerView);
		
		metaEditScreen = this;
		
		//listener toevoegen aan de savebutton
		final Button savebutton = (Button) findViewById(R.id.savebutton);
		savebutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Wordt aangeroepen wanneer er op de savebutton wordt getapt
			 * @param v de savebutton
			 */
            public void onClick(View v) {
            	Bundle bundle = new Bundle();

            	bundle.putInt(ID_KEY, polyID);
        		bundle.putInt(COLOR_KEY, polyColor);
        		bundle.putString(NAME_KEY, nameField.getText().toString());
        		bundle.putString(DESCRIPTION_KEY,descriptionField.getText().toString());
        		
            	Intent mIntent = new Intent();
            	mIntent.putExtras(bundle);
            	setResult(RESULT_SAVE, mIntent);
            	finish();
            }
        });
		
		//listener toevoegen aan de cancelbutton
		final Button cancelbutton = (Button) findViewById(R.id.cancelbutton);
		cancelbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Wordt aangeroepen wanneer er op de cancelbutton wordt getapt
			 * @param v de deletebutton
			 */
            public void onClick(View v) {
            	Bundle bundle = new Bundle();
            	Intent mIntent = new Intent();
            	mIntent.putExtras(bundle);
            	setResult(RESULT_CANCEL, mIntent);
            	finish();
            }
        });
		
		//listener toevoegen aan de deletebutton
		final Button deletebutton = (Button) findViewById(R.id.deletebutton);
		deletebutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Wordt aangeroepen wanneer er op de deletebutton wordt getapt
			 * @param v de cancelbutton
			 */
            public void onClick(View v) {
            	//vraag de gebruiker of hij/zij het zeker weet
                new AlertDialog.Builder(metaEditScreen)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.meta_deletedialogtitle)
                .setMessage(R.string.meta_deletedialogtext)
                .setPositiveButton(R.string.meta_deleteyesbutton, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                    	Bundle bundle = new Bundle();
                    	
                    	bundle.putInt(ID_KEY, polyID);
                    	
                    	Intent mIntent = new Intent();
                    	mIntent.putExtras(bundle);
                    	setResult(RESULT_DELETE, mIntent);
                    	finish();
                    }

                })
                .setNegativeButton(R.string.meta_deletenobutton, null)
                .show();
            }
        });
	}
	
	/**
	 * Interface die een listener beschrijft die reageert op een colorchange-event van de colorpickerview
	 */
	public interface OnColorChangedListener {
		 void colorChanged(int color);
	}

	/**
	 * De klasse die een colorpicker tekent en beheert
	 * @author Joost
	 * Deze klasse is gebasseerd op delen van de ColorPickerDialog, zoals aangeleverd in de Android SDK
	 * http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/graphics/ColorPickerDialog.html
	 */
	/*
	 * Copyright (C) 2007 The Android Open Source Project
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 *
	 *      http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 */
    private static class ColorPickerView extends View {
        private Paint mPaint;
        private Paint mCenterPaint;
        private final int[] mColors;
        private OnColorChangedListener mListener;

        /**
         * Constructor
         * @param c de context waarin de colorpicker zich begeeft
         * @param l de listener die moet worden aangeroepen wanneer er een kleur gekozen wordt
         * @param color de initi‘le kleur
         */
        ColorPickerView(Context c, OnColorChangedListener l, int color) {
            super(c);
            mListener = l;
            mColors = new int[] {
                0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
                0xFFFFFF00, 0xFFFF0000
            };
            Shader s = new SweepGradient(0, 0, mColors, null);

            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mPaint.setShader(s);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(32);

            mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mCenterPaint.setColor(color);
            mCenterPaint.setStrokeWidth(5);
        }

        /**
         * De functie die wordt aangeroepen wanneer het midden colorpicker opnieuw getekend dient te worden
         * @param canvas het canvas waarop de colorpicker wordt getekend
         */
        @Override
        protected void onDraw(Canvas canvas) {
            float r = CENTER_X - mPaint.getStrokeWidth()*0.5f;
            canvas.translate(CENTER_X, CENTER_X);
            canvas.drawOval(new RectF(-r, -r, r, r), mPaint);
            canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);
        }

        /**
         * Een override van de onMeasure functie die een View verplicht moet implementeren
         */
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(CENTER_X*2, CENTER_Y*2);
        }

        private static final int CENTER_X = 100;
        private static final int CENTER_Y = 100;
        private static final int CENTER_RADIUS = 32;

        private int ave(int s, int d, float p) {
            return s + java.lang.Math.round(p * (d - s));
        }

        /**
         * Functie die een punt tussen twee basiskleuren omzet in een enkele kleur 
         * @param colors de basiskleuren op het colorwheel waartussen getapt is
         * @param unit de afstand tot de basiskleuren
         * @return de gecombineerde kleur
         */
        private int interpColor(int colors[], float unit) {
            if (unit <= 0) {
                return colors[0];
            }
            if (unit >= 1) {
                return colors[colors.length - 1];
            }

            float p = unit * (colors.length - 1);
            int i = (int)p;
            p -= i;

            // now p is just the fractional part [0...1) and i is the index
            int c0 = colors[i];
            int c1 = colors[i+1];
            int a = ave(Color.alpha(c0), Color.alpha(c1), p);
            int r = ave(Color.red(c0), Color.red(c1), p);
            int g = ave(Color.green(c0), Color.green(c1), p);
            int b = ave(Color.blue(c0), Color.blue(c1), p);

            return Color.argb(a, r, g, b);
        }

        private static final float PI = 3.1415926f;
        private boolean mTrackingCenter;
        
        /**
         * Wanneer er op de buitenste ring getapd wordt, moet de kleur binnenin verspringen
         * Als je over de ring sleept moet dit ook blijven gebeuren
         * De onColorChangedListener wordt hier aangeroepen, zodra de kleur verandert
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX() - CENTER_X;
            float y = event.getY() - CENTER_Y;
            boolean inCenter = java.lang.Math.sqrt(x*x + y*y) <= CENTER_RADIUS*2;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTrackingCenter = inCenter;
                case MotionEvent.ACTION_MOVE:
                    if (!mTrackingCenter) {
                        float angle = (float)java.lang.Math.atan2(y, x);
                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle/(2*PI);
                        if (unit < 0) {
                            unit += 1;
                        }
                        mCenterPaint.setColor(interpColor(mColors, unit));
                        mListener.colorChanged(mCenterPaint.getColor());
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    }	
}
