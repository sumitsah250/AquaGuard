package com.boss.aquaguardv1;

import android.content.Context;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class OnnxHelper {
    private OrtEnvironment env;
    private OrtSession session;

    public OnnxHelper(Context context) throws Exception {
        env = OrtEnvironment.getEnvironment();

        // Load ONNX model from assets
        InputStream is = context.getAssets().open("gaussian_nb.onnx");
        byte[] modelBytes = new byte[is.available()];
        is.read(modelBytes);
        is.close();

        session = env.createSession(modelBytes);
    }

    public float[] predict(float[] inputData, int inputSize) throws Exception {
        // Define input shape (batch=1, features=inputSize)
        long[] shape = new long[]{1, inputSize};

        // Wrap input data in FloatBuffer
        FloatBuffer fb = FloatBuffer.wrap(inputData);

        // Create tensor
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, fb, shape);

        // Run inference
        OrtSession.Result results = session.run(
                Collections.singletonMap(session.getInputNames().iterator().next(), inputTensor)
        );

        // Get output object
        Object outputObj = results.get(0).getValue();

        // Handle long[] (class labels) output
        if (outputObj instanceof long[]) {
            long[] longOutput = (long[]) outputObj;
            float[] floatOutput = new float[longOutput.length];
            for (int i = 0; i < longOutput.length; i++) {
                floatOutput[i] = (float) longOutput[i];
            }
            return floatOutput;
        }

        // Handle float[][] (probabilities) output
        if (outputObj instanceof float[][]) {
            return ((float[][]) outputObj)[0];
        }

        throw new IllegalStateException("Unsupported output type: " + outputObj.getClass());
    }
}
