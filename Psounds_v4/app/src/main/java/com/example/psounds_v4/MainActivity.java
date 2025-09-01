package com.example.psounds_v4;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.UniversalAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.util.fft.FFT;

public class MainActivity extends AppCompatActivity {

    private Button btnSeleccionar, btnReproducir, btnDetener,
            btnToggleWaveform, btnToggleSpectrogram, btnClasificar, btnGenerarEspectrograma;
    private Uri audioUri;
    private MediaPlayer mediaPlayer;
    private WaveformView waveformView;
    private SpectrogramView spectrogramView;
    private Handler handler = new Handler();
    private LungSoundClassifier classifier;

    private final ActivityResultLauncher<Intent> audioPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            audioUri = result.getData().getData();
                            btnReproducir.setEnabled(true);
                            btnClasificar.setEnabled(true);
                            btnGenerarEspectrograma.setEnabled(true);
                            procesarFormaDeOnda(audioUri);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        waveformView = findViewById(R.id.waveformView);
        spectrogramView = findViewById(R.id.spectrogramView);
        btnSeleccionar = findViewById(R.id.btnSeleccionar);
        btnReproducir = findViewById(R.id.btnReproducir);
        btnDetener = findViewById(R.id.btnDetener);
        btnToggleWaveform = findViewById(R.id.btnToggleWaveform);
        btnToggleSpectrogram = findViewById(R.id.btnToggleSpectrogram);
        btnClasificar = findViewById(R.id.btnClasificar);
        btnGenerarEspectrograma = findViewById(R.id.btnGenerarEspectrograma);

        btnReproducir.setEnabled(false);
        btnDetener.setEnabled(false);
        btnClasificar.setEnabled(false);
        btnGenerarEspectrograma.setEnabled(false);

        try {
            classifier = new LungSoundClassifier(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar modelo", Toast.LENGTH_LONG).show();
        }

        btnSeleccionar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            audioPickerLauncher.launch(intent);
        });

        btnReproducir.setOnClickListener(v -> {
            if (audioUri != null) {
                try {
                    if (mediaPlayer != null) {
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }

                    mediaPlayer = MediaPlayer.create(this, audioUri);
                    int duration = mediaPlayer.getDuration();
                    int startMs = (int) (waveformView.getStartRatio() * duration);
                    int endMs = (int) (waveformView.getEndRatio() * duration);

                    mediaPlayer.seekTo(startMs);
                    mediaPlayer.start();
                    btnDetener.setEnabled(true);

                    handler.postDelayed(() -> {
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            mediaPlayer.pause();
                            mediaPlayer.seekTo(0);
                            btnDetener.setEnabled(false);
                            btnReproducir.setEnabled(true);
                        }
                    }, endMs - startMs);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnDetener.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                btnDetener.setEnabled(false);
                btnReproducir.setEnabled(true);
                handler.removeCallbacksAndMessages(null);
            }
        });

        btnToggleWaveform.setOnClickListener(v -> {
            if (waveformView.getVisibility() == View.VISIBLE) {
                waveformView.setVisibility(View.GONE);
                btnToggleWaveform.setText("Mostrar Ondas");
            } else {
                waveformView.setVisibility(View.VISIBLE);
                btnToggleWaveform.setText("Ocultar Ondas");
            }
        });

        btnToggleSpectrogram.setOnClickListener(v -> {
            if (spectrogramView.getVisibility() == View.VISIBLE) {
                spectrogramView.setVisibility(View.GONE);
                btnToggleSpectrogram.setText("Mostrar Espectrograma");
            } else {
                spectrogramView.setVisibility(View.VISIBLE);
                btnToggleSpectrogram.setText("Ocultar Espectrograma");
            }
        });

        btnClasificar.setOnClickListener(v -> {
            if (audioUri != null && classifier != null) {
                clasificarSonido(audioUri);
            }
        });

        btnGenerarEspectrograma.setOnClickListener(v -> {
            if (audioUri != null) {
                generarEspectrograma(audioUri);
            }
        });
    }

    private void procesarFormaDeOnda(Uri uri) {
        new Thread(() -> {
            List<Float> amplitudes = new ArrayList<>();
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream == null) return;

                TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(44100, 16, 1, true, false);
                UniversalAudioInputStream universalStream = new UniversalAudioInputStream(inputStream, format);
                AudioDispatcher dispatcher = new AudioDispatcher(universalStream, 1024, 512);

                dispatcher.addAudioProcessor(new AudioProcessor() {
                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        float[] buffer = audioEvent.getFloatBuffer();
                        float sum = 0;
                        for (float sample : buffer) sum += Math.abs(sample);
                        amplitudes.add(sum / buffer.length);
                        return true;
                    }

                    @Override
                    public void processingFinished() {
                        runOnUiThread(() -> waveformView.setAmplitudes(amplitudes));
                    }
                });

                dispatcher.run();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void generarEspectrograma(Uri uri) {
        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream == null) return;

                TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(44100, 16, 1, true, false);
                UniversalAudioInputStream universalStream = new UniversalAudioInputStream(inputStream, format);
                AudioDispatcher dispatcher = new AudioDispatcher(universalStream, 1024, 512);

                List<float[]> espectros = new ArrayList<>();

                dispatcher.addAudioProcessor(new AudioProcessor() {
                    @Override
                    public boolean process(AudioEvent audioEvent) {
                        float[] buffer = audioEvent.getFloatBuffer().clone();
                        int fftSize = 1024;
                        float[] spectrum = new float[fftSize / 2];
                        FFT fft = new FFT(fftSize);
                        fft.forwardTransform(buffer);
                        fft.modulus(buffer, spectrum);
                        espectros.add(spectrum);
                        return true;
                    }

                    @Override
                    public void processingFinished() {
                        runOnUiThread(() -> {
                            spectrogramView.clearSpectrogram();
                            for (float[] spectrum : espectros) {
                                spectrogramView.addSpectrum(spectrum);
                            }
                            spectrogramView.setVisibility(View.VISIBLE);
                        });
                    }
                });

                dispatcher.run();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void clasificarSonido(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return;

            String resultado = classifier.classify(inputStream);
            runOnUiThread(() -> Toast.makeText(this, "Resultado: " + resultado, Toast.LENGTH_LONG).show());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al procesar audio", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
    }
}
