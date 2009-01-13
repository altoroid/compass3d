package com.danielwright.compass3d;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.opengl.GLU;

import com.danielwright.compass3d.GLSurfaceView.Renderer;

public class CompassRenderer implements Renderer, SensorListener {

	public CompassRenderer(Activity activity) {
		SensorManager sensorManager =
			(SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
		sensorManager.registerListener(this,
				SensorManager.SENSOR_ACCELEROMETER | SensorManager.SENSOR_MAGNETIC_FIELD,
				SensorManager.SENSOR_DELAY_GAME);
	}

	public void drawFrame(GL10 gl) {
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		GLU.gluLookAt(gl, 0, 0, 4, 0, 0, 0, 0, 1, 0);

		float[] nAccel = getNewAccel();
		float[] nMag = getNewMag();
		if (nAccel != null && nMag != null) {
			nAccel = VMath.normalize(nAccel);
			nMag = VMath.normalize(nMag);
			if (mWeightedAccel == null)
				mWeightedAccel = nAccel.clone();
			if (mWeightedMag == null)
				mWeightedMag = nMag.clone();
			float alpha = 0.9f;
			for (int i = 0; i < 3; ++i) {
				mWeightedAccel[i] =
					alpha * mWeightedAccel[i] + (1f - alpha) * nAccel[i];
				mWeightedMag[i] =
					alpha * mWeightedMag[i] + (1f - alpha) * nMag[i];
			}
			mWeightedAccel = VMath.normalize(mWeightedAccel);
			mWeightedMag = VMath.normalize(mWeightedMag);

			// Distance from the mag point to the horizontal plane
			float magDist = VMath.dotProduct(mWeightedAccel, mWeightedMag);
			// Project the mag vector onto the horizontal plane
			float[] magProj = VMath.normalize(new float[] {
					mWeightedMag[0] - mWeightedAccel[0] * magDist,
					mWeightedMag[1] - mWeightedAccel[1] * magDist,
					mWeightedMag[2] - mWeightedAccel[2] * magDist });

			float[] x = VMath.crossProduct(mWeightedAccel, magProj);
			gl.glMultMatrixf(new float[] { x[0], x[1], x[2], 0,
					-mWeightedAccel[0], -mWeightedAccel[1], -mWeightedAccel[2],
					0, magProj[0], magProj[1], magProj[2], 0, 0, 0, 0, 1 }, 0);
		}

		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbient, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuse, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPos, 0);

		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE,
				red, 0);
		mArrow.draw(gl);
		gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE,
				yellow, 0);
		gl.glRotatef(90.0f, 0f, 1f, 0f);
		mArrow.draw(gl);
		gl.glRotatef(90.0f, 0f, 1f, 0f);
		mArrow.draw(gl);
		gl.glRotatef(90.0f, 0f, 1f, 0f);
		mArrow.draw(gl);
	}

	public int[] getConfigSpec() {
		// We want a depth buffer, don't care about the
		// details of the color buffer.
		int[] configSpec = { EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_NONE };
		return configSpec;
	}

	public void sizeChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		float ratio = (float) width / height;
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
	}

	public void surfaceCreated(GL10 gl) {
		gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glEnable(GL10.GL_MULTISAMPLE);
		gl.glDepthFunc(GL10.GL_LEQUAL);
		gl.glClearDepthf(1.0f);
		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
	}

	public synchronized void onSensorChanged(int sensor, float[] values) {
		switch (sensor) {
		case SensorManager.SENSOR_ACCELEROMETER:
			mNewAccel = values.clone();
			break;
		case SensorManager.SENSOR_MAGNETIC_FIELD:
			mNewMag = values.clone();
			// Values is weird -- x and y point south while z points north --
			// make all point north.
			mNewMag[0] = -mNewMag[0];
			mNewMag[1] = -mNewMag[1];
			break;
		}
	}

	public void onAccuracyChanged(int arg0, int arg1) {
		// For SensorListener -- Ignored
	}

	// Synchronized accessors for new accel an mag vectors -- always use these
	// rather than the local variables directly.
	synchronized float[] getNewAccel() {
		return mNewAccel;
	}

	synchronized float[] getNewMag() {
		return mNewMag;
	}

	float[] lightAmbient = new float[] { 0.4f, 0.4f, 0.4f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightPos = new float[] { 10f, 20f, -15f, 1f };

	float[] yellow = new float[] { 1.0f, 1.0f, 0.0f, 1.0f };
	float[] red = new float[] { 1.0f, 0.0f, 0.0f, 1.0f };

	// New accel and mag vectors -- should only be accessed when synchronized.
	// Default values are so we see the compass at an angle in the emulator.
	float[] mNewAccel = new float[] { 1f, -9f, -4f };
	float[] mNewMag = new float[] { -3f, 2f, -5f };;

	float[] mWeightedMag;
	float[] mWeightedAccel;

	Arrow mArrow = new Arrow();
}
