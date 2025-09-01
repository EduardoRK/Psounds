package com.example.psounds_v4;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LungSoundClassifier {

    private final Interpreter interpreter;

    public LungSoundClassifier(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context, "modelo_pulmones.tflite"));
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Simulación de espectrograma
    public String classify(InputStream audioInputStream) {
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 128 * 1 * 4).order(ByteOrder.nativeOrder());


        for (int i = 0; i < 128; i++) {
            inputBuffer.putFloat(0.1f); // Valor de prueba
        }

        float[][] output = new float[1][3]; // ← 3 clases
        interpreter.run(inputBuffer, output);

        int maxIndex = 0;
        float maxValue = output[0][0];
        for (int i = 1; i < output[0].length; i++) {
            if (output[0][i] > maxValue) {
                maxIndex = i;
                maxValue = output[0][i];
            }
        }

        String[] labels = {"Normal", "Crackle", "Wheeze"};
        return labels[maxIndex] + " (" + maxValue + ")";
    }

}
