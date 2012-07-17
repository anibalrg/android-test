package com.accelerometer.test;

import java.lang.String;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;



public class AccelerometerTestActivity extends Activity implements SensorEventListener {
	// Accelerometer
	private final int X = 0;
	private final int Y = 1;
	private final int Z = 2;

	private float[] mLast = { 0f, 0f, 0f };
	private float[] mAcceleration = { 0f, 0f, 0f };
	private float[] mDelta = {0f, 0f, 0f };

	private float[] mLast2 = { 0f, 0f, 0f };
	private float[] mDelta2 = {0f, 0f, 0f };

	private boolean mInitialized = false;
	private SensorManager mSensorManager = null;
	private Sensor mAccelerometer = null;

	private TextView mTextX1 = null;
	private TextView mTextY1 = null;
	private TextView mTextZ1 = null;
	private ImageView mImage1 = null;

	private TextView mTextX2 = null;
	private TextView mTextY2 = null;
	private TextView mTextZ2 = null;
	private ImageView mImage2 = null;

	private static final int[] IMAGE_ID = { R.drawable.x, R.drawable.y, R.drawable.z };

	// Gyroscope
	private static final float NS2S = 1.0f / 1000000000.0f;
	private final float[] deltaRotationVector = new float[4];
	private float mTimestamp = 0;

	private static final float[] THRESHOLD = { 0.3f, 0.3f, 0.3f };
	private static final String TAG = "AccelerometerTestActivity";
	private static final float EPSILON = 1.0f;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mInitialized = false;
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

		setContentView(R.layout.main);

		mTextX1 = (TextView)findViewById(R.id.x_axis);
		mTextY1 = (TextView)findViewById(R.id.y_axis);
		mTextZ1 = (TextView)findViewById(R.id.z_axis);
		mImage1 = (ImageView)findViewById(R.id.image);

		mTextX2 = (TextView)findViewById(R.id.x_axis2);
		mTextY2 = (TextView)findViewById(R.id.y_axis2);
		mTextZ2 = (TextView)findViewById(R.id.z_axis2);
		mImage2 = (ImageView)findViewById(R.id.image2);
	}

	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	protected void onStop() {
		super.onStop();
		mSensorManager.unregisterListener(this);
	}

	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	private void initializeValues() {
		if (!mInitialized) {
			copyCoordArray(mAcceleration, mLast	);
			initializeView();
			mInitialized = true;
		}
	}

	// Calculate delta in function of the input from the accelerometer
	private void setDelta() {	       

		for (int index = X; index <= Z	; ++index)
		{
			mDelta[index] = Math.abs(mLast[index] - mAcceleration[index]);

			if (mDelta[index] < THRESHOLD[index])
				mDelta[index] = (float)0.0;
		}

		Log.v(TAG, "delta(" + mDelta[X] + ", " + mDelta[Y] + ", " + mDelta[Z] + ")");
	}


	private void copyCoordArray(float[] s, float[] d) {
		System.arraycopy(s, 0, d, 0, 3);
	}

	private void updateValuesDeltaMethod() {
		setDelta();
		copyCoordArray(mAcceleration, mLast); // mLast = mAcceleration
		Log.v(TAG, "last(" + mLast[X] + ", " + mLast[Y] + ", " + mLast[Z] + ")");
	}

	private void updateValuesLinearAcceleration() {
		// alpha is calculated as t / (t + dT)
		// with t, the low-pass filter's time-constant
		// and dT, the event delivery rate

		final float alpha = 0.5f;

		mLast2[X] = alpha * mLast2[X] + (1 - alpha) * mAcceleration[X];
		mLast2[Y] = alpha * mLast2[Y] + (1 - alpha) * mAcceleration[Y];
		mLast2[Z] = alpha * mLast2[Z] + (1 - alpha) * mAcceleration[Z];
		Log.v(TAG, "last2(" + mLast2[X] + ", " + mLast2[Y] + ", " + mLast2[Z] + ")");

		mDelta2[X] = mAcceleration[X] - mLast2[X];
		mDelta2[Y] = mAcceleration[Y] - mLast2[Y];
		mDelta2[Z] = mAcceleration[Z] - mLast2[Z];
		Log.v(TAG, "delta2(" + mDelta2[X] + ", " + mDelta2[Y] + ", " + mDelta2[Z] + ")");
	}

	private void onAccelerometerChanged(SensorEvent event) {
		// Accelerometer returns three values: (acceleration)x - Gx, (acceleration)y - Gy and (acceleration)z - Gz
		// All values are measured in m/s^2 

		// acceleration = event.values
		copyCoordArray(event.values, mAcceleration);
		Log.v(TAG, "acceleration(" + mAcceleration[X] + ", " + mAcceleration[Y] + ", " + mAcceleration[Z] + ")");

		if (!mInitialized) {
			initializeValues();
		} 

		// Update values
		updateValuesDeltaMethod();
		updateValuesLinearAcceleration();

		updateMethod1();
		updateMethod2();
	}

	private void onGyroscopeChanged(SensorEvent event) {
		// Gyroscope returns three values: angular speed aroud x-axis, around y-axis and aroud z-axis
		// All values are in radian/s.
		// NOTE: Rotation is positive in the counter-clockwise direction. That is, an observer looking from some
		// positive location on the x, y or z axis at a device on the origin would report positive rotation if the
		// device appeared to be rotating counter clockwise. This is the standar mathematical definition of positive
		// rotation an doesn't agree with the definition of roll given earlier.
		// Typically the output of the gyroscope is integrated over time to calculate a rotation describing the change
		// of angles over the timestep.
		// This timestep's delta rotation to be multiplied by the current rotation
		// after computing it from the gyro sample data.

		if (mTimestamp != 0) {
			final float dT = (event.timestamp - mTimestamp) * NS2S;
			// Axis of the rotation sample, not normalized yet.
			float axisX = event.values[0];
			float axisY = event.values[1];
			float axisZ = event.values[2];
			Log.v(TAG, "rotation(" + axisX + ", " + axisY + ", " + axisZ + ")");

			// Calculate the angular speed of the sample
			float omegaMagnitude = (float)Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
			Log.v(TAG, "omegaMagnitude = " + omegaMagnitude);

			// Normalize the rotation vector if it's big enough to get the axis
			if (omegaMagnitude > EPSILON) {
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the timestep
			// We will convert this axis-angle representation of the delta rotation
			// into a quaternion before turning it into the rotation matrix.
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;
			float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
			deltaRotationVector[0] = sinThetaOverTwo * axisX;
			deltaRotationVector[1] = sinThetaOverTwo * axisY;
			deltaRotationVector[2] = sinThetaOverTwo * axisZ;
			deltaRotationVector[3] = cosThetaOverTwo;
		}

		mTimestamp = event.timestamp;
		float[] deltaRotationMatrix = new float[9];
		SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
		// User code should concatenate the delta rotation we computed with the current rotation
		// in order to get the updated rotation.
		// rotationCurrent = rotationCurrent * deltaRotationMatrix;

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch(event.sensor.getType()) {
		case Sensor.TYPE_GYROSCOPE:
			onGyroscopeChanged(event);
			break;
		case Sensor.TYPE_ACCELEROMETER:
		default:
			onAccelerometerChanged(event);
		}
	}

	public void initializeView() {
		mTextX1.setText("0.0");
		mTextY1.setText("0.0");
		mTextZ1.setText("0.0");
		mImage1.setVisibility(View.INVISIBLE);

		mTextX2.setText("0.0");
		mTextY2.setText("0.0");
		mTextZ2.setText("0.0");
		mImage2.setVisibility(View.INVISIBLE);
	}


	private int maxIndexOfArray(float[] array) {
		int index = 0;
		float value = array[index];

		for (int i = 1; i < array.length; i++) {
			if (value < array[i]) {
				index = i;
				value = array[index];
			}
		}

		return index;
	}

	public void updateMethod1() {
		mTextX1.setText(Float.toString(mDelta[0]));
		mTextY1.setText(Float.toString(mDelta[1]));
		mTextZ1.setText(Float.toString(mDelta[2]));

		mImage1.setVisibility(View.VISIBLE);
		mImage1.setImageResource(IMAGE_ID[maxIndexOfArray(mDelta)]);
	}

	public void updateMethod2() {
		mTextX2.setText(Float.toString(mDelta2[0]));
		mTextY2.setText(Float.toString(mDelta2[1]));
		mTextZ2.setText(Float.toString(mDelta2[2]));

		mImage2.setVisibility(View.VISIBLE);
		mImage2.setImageResource(IMAGE_ID[maxIndexOfArray(mDelta2)]);
	}	
}
