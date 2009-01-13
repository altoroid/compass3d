package com.danielwright.compass3d;

import android.opengl.Matrix;
import android.util.Log;

class VMath {
	public static float[] crossProduct(float[] a, float[] b) {
		return new float[] {
				a[1] * b[2] - b[1] * a[2],
				a[2] * b[0] - b[2] * a[0],
				a[0] * b[1] - b[0] * a[1]
		};
	}
	
	public static float dotProduct(float[] a, float[] b) {
		return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
	}
	
	public static float[] normalize(float[] a) {
		float len = length(a);
		return new float[] { a[0] / len, a[1] / len, a[2] / len };
	}
	
	public static float length(float[] v) {
		return Matrix.length(v[0], v[1], v[2]);
	}
}
