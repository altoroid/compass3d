package com.danielwright.compass3d;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.opengles.GL10;

import android.util.Log;

public class Arrow {
	class Face {
		Face(int begin, int end, int type) {
			mBegin = begin; mEnd = end; mType = type;
		}
		public int mBegin;
		public int mEnd;
		public int mType;
	}
	
	public Arrow() {
		ArrayList<Float> vertices = new ArrayList<Float>();
		ArrayList<Float> normals = new ArrayList<Float>();
		ArrayList<Face> faces = new ArrayList<Face>();
		
		addCone(1.0f, 2.0f, 0.2f, 0.0f, 20,
				vertices, normals, faces);
		addCircle(1.0f, 0.2f, true, 20,
				vertices, normals, faces);
		addCone(0.0f, 1.0f, 0.1f, 0.1f, 20,
				vertices, normals, faces);
		addCircle(0.0f, 0.1f, true, 20,
				vertices, normals, faces);

		
		mVertices = toFloatBuffer(vertices);
		mNormals = toFloatBuffer(normals);
		mFaces = faces.toArray(new Face[faces.size()]);
	}
	
	FloatBuffer toFloatBuffer(ArrayList<Float> al) {
		ByteBuffer bb = ByteBuffer.allocateDirect(al.size() * 4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		for (float f : al) {
			fb.put(f);
		}
		return fb;
	}
	
	void addCone(float zStart, float zEnd, float rStart, float rEnd, int segments,
			ArrayList<Float> vertices, ArrayList<Float> normals, ArrayList<Face> faces) {
		float normalZ = -(rEnd - rStart) / (zEnd - zStart);
		
		int beginSize = vertices.size();
		for (int i = 0; i <= segments; ++i) {
			double angle = i * Math.PI * 2.0 / segments;
			float cos = (float)Math.cos(angle);
			float sin = (float)Math.sin(angle);
			vertices.add(cos * rStart);
			vertices.add(sin * rStart);
			vertices.add(zStart);
			normals.add(cos);
			normals.add(sin);
			normals.add(normalZ);
			vertices.add(cos * rEnd);
			vertices.add(sin * rEnd);
			vertices.add(zEnd);
			normals.add(cos);
			normals.add(sin);
			normals.add(normalZ);
		}
		faces.add(new Face(beginSize / 3, vertices.size() / 3, GL10.GL_TRIANGLE_STRIP));
	}

	void addCircle(float z, float r, boolean pointsPositive, int segments,
			ArrayList<Float> vertices, ArrayList<Float> normals, ArrayList<Face> faces) {
		int beginSize = vertices.size();
		vertices.add(0.0f);
		vertices.add(0.0f);
		vertices.add(z);
		normals.add(0.0f);
		normals.add(0.0f);
		normals.add(pointsPositive ? -1f : 1f);
		for (int i = 0; i <= segments; ++i) {
			double angle = i * Math.PI * 2.0 / segments;
			float cos = (float)Math.cos(angle);
			if (!pointsPositive) cos = -cos;
			float sin = (float)Math.sin(angle);
			vertices.add(cos * r);
			vertices.add(sin * r);
			vertices.add(z);
			normals.add(0.0f);
			normals.add(0.0f);
			normals.add(pointsPositive ? -1f : 1f);
		}
		faces.add(new Face(beginSize / 3, vertices.size() / 3, GL10.GL_TRIANGLE_FAN));
	}

	public void draw(GL10 gl) {
		mVertices.position(0);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertices);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		mNormals.position(0);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormals);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
	
		for (Face face : mFaces) {
			gl.glDrawArrays(face.mType, face.mBegin, face.mEnd - face.mBegin);
		}
	}

	FloatBuffer mVertices;
	FloatBuffer mNormals;
	Face[] mFaces;
}
