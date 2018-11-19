package com.example.divided.signalgathering;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.apache.commons.lang3.time.StopWatch;

import java.text.SimpleDateFormat;
import java.util.Date;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;


public class MainActivity extends AppCompatActivity implements SensorEventListener, DialogInterface.OnClickListener {
    private final static float g = 9.8123f; // gravity acceleration value in Warsaw [m/s^2]
    private final static float radToDegreesConst = 57.2957795131f;

    Menu toolbarMenu;
    SeekBar samplingPeriodSeekBar;
    AlertDialog saveFileDialog;
    TextView xAxisAccelerometerTextView;
    TextView yAxisAccelerometerTextView;
    TextView zAxisAccelerometerTextView;
    TextView textViewMeasuredFrequency;
    TextView xAxisGyroTextView;
    TextView yAxisGyroTextView;
    TextView zAxisGyroTextView;
    TextView textViewSetFrequency;
    TextView textViewAccSamplesCount;
    TextView textViewGyroSampelsCount;
    TextView textViewTimeFromStart;
    EditText saveFileDialogEditText;
    LineChart accelerationChartX;
    LineChart accelerationChartY;
    LineChart accelerationChartZ;
    LineChart accelerationChartSum;
    LineChart gyroscopeChartX;
    LineChart gyroscopeChartY;
    LineChart gyroscopeChartZ;
    LineChart gyroscopeChartSum;
    SoundPool soundPool;
    BluetoothSPP bt;
    int soundId;
    private float SAMPLE_PERIOD_MS = 33.333f;
    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorGyroscope;
    private boolean isRunning = true;
    private ChartPoint previousAccelerationPoint = new ChartPoint(0f, 0f);
    private ChartPoint previousGyroscopePoint = new ChartPoint(0f, 0f);
    private OnSensorMeasurement accelerometerListener;
    private OnSensorMeasurement gyroscopeListener;
    private int accSamplesCount = 0;
    private int gyroSamplesCount = 0;
    private StopWatch stopWatch;

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float currentSensorMeasurementTime = event.timestamp;
        final float accelerationTimePeriod = (currentSensorMeasurementTime - previousAccelerationPoint.getTimeStamp()) / 1000000.0f;
        final float gyroscopeTimePeriod = (currentSensorMeasurementTime - previousGyroscopePoint.getTimeStamp()) / 1000000.0f;

        Sensor mySensor = event.sensor;

        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (accelerationTimePeriod >= SAMPLE_PERIOD_MS) {
                new ChartDrawTask(accelerometerListener, accelerationChartX, accelerationChartY, accelerationChartZ, accelerationChartSum).execute(accelerationTimePeriod, event.values[0] / g, event.values[1] / g, event.values[2] / g, currentSensorMeasurementTime);
                previousAccelerationPoint.setTimeStamp(currentSensorMeasurementTime);
                textViewMeasuredFrequency.setText("Measured: " + String.format("%.7f", accelerationTimePeriod) + "ms");
            }
        }
        if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (gyroscopeTimePeriod >= SAMPLE_PERIOD_MS) {
                new ChartDrawTask(gyroscopeListener, gyroscopeChartX, gyroscopeChartY, gyroscopeChartZ, gyroscopeChartSum).execute(gyroscopeTimePeriod, event.values[0] * radToDegreesConst, event.values[1] * radToDegreesConst, event.values[2] * radToDegreesConst, currentSensorMeasurementTime);
                previousGyroscopePoint.setTimeStamp(currentSensorMeasurementTime);
            }
        }


               /*


                ChartPoint chartPointX = new ChartPoint(event.values[0] / g, currentSensorMeasurementTime);
                ChartPoint chartPointY = new ChartPoint(event.values[1] / g, currentSensorMeasurementTime);
                ChartPoint chartPointZ = new ChartPoint(event.values[2] / g, currentSensorMeasurementTime);
                ChartPoint chartPointSum = new ChartPoint(((float) (Math.sqrt(Math.pow(chartPointX.getValue(), 2) + Math.pow(chartPointY.getValue(), 2) + Math.pow(chartPointZ.getValue(), 2)))), currentSensorMeasurementTime);

                xAxisTextView.setText("X: " + String.format("%.3f", chartPointX.getValue()));
                yAxisTextView.setText("Y: " + String.format("%.3f", chartPointY.getValue()));
                zAxisTextView.setText("Z: " + String.format("%.3f", chartPointY.getValue()));

                addEntry(chartPointX, accelerationChartX, "Axis X");
                addEntry(chartPointY, accelerationChartY, "Axis Y");
                addEntry(chartPointZ, accelerationChartZ, "Axis Z");
                addEntry(chartPointSum, accelerationChartSum, "Sum");
*/


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt = new BluetoothSPP(this);
        stopWatch = new StopWatch();

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .build();

        soundId = soundPool.load(this, R.raw.start_button_sound, 1);

        accelerometerListener = new OnSensorMeasurement() {
            @Override
            public void onMeasurement(float x, float y, float z, float sum, float measuredFrequency) {
                //textViewMeasuredFrequency.setText("Measured: " + String.format("%.7f", measuredFrequency) + "Hz");
                xAxisAccelerometerTextView.setText("X: " + String.format("%.3f", x));
                yAxisAccelerometerTextView.setText("Y: " + String.format("%.3f", y));
                zAxisAccelerometerTextView.setText("Z: " + String.format("%.3f", z));
                accSamplesCount++;
                textViewAccSamplesCount.setText(String.format("Accel: %d Samples", accSamplesCount));
                textViewTimeFromStart.setText(Utils.getTime(stopWatch.getTime()));
                if(bt!=null){
                    if(bt.isServiceAvailable()){
                        bt.send("params_"+accSamplesCount+"_"+gyroSamplesCount+"_"+Utils.getTime(stopWatch.getTime()),true);
                    }
                }
            }
        };

        gyroscopeListener = new OnSensorMeasurement() {
            @Override
            public void onMeasurement(float x, float y, float z, float sum, float measuredFrequency) {
                xAxisGyroTextView.setText("X: " + String.format("%.3f", x));
                yAxisGyroTextView.setText("Y: " + String.format("%.3f", y));
                zAxisGyroTextView.setText("Z: " + String.format("%.3f", z));
                gyroSamplesCount++;
                textViewGyroSampelsCount.setText(String.format("Gyro: %d Samples", gyroSamplesCount));
                if(bt!=null){
                    if(bt.isServiceAvailable()){
                        bt.send("params_"+accSamplesCount+"_"+gyroSamplesCount+"_"+Utils.getTime(stopWatch.getTime()),true);
                    }
                }
            }
        };
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        samplingPeriodSeekBar = findViewById(R.id.seek_bar_sampling_period);
        samplingPeriodSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            float progressMinimum = 10;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float progressValue = progress + progressMinimum;
                SAMPLE_PERIOD_MS = progressValue;
                final float setFrequency = (1000.0f / SAMPLE_PERIOD_MS);
                textViewSetFrequency.setText("Set: " + String.format("%.1f", setFrequency) + "Hz");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        textViewAccSamplesCount = findViewById(R.id.tv_acc_samples_count);
        textViewGyroSampelsCount = findViewById(R.id.tv_gyro_samples_count);
        textViewTimeFromStart = findViewById(R.id.tv_time);

        xAxisAccelerometerTextView = findViewById(R.id.text_view_axis_x_accelerometer);
        yAxisAccelerometerTextView = findViewById(R.id.text_view_axis_y_accelerometer);
        zAxisAccelerometerTextView = findViewById(R.id.text_view_axis_z_accelerometer);
        xAxisGyroTextView = findViewById(R.id.text_view_axis_x_gyroscope);
        yAxisGyroTextView = findViewById(R.id.text_view_axis_y_gyroscope);
        zAxisGyroTextView = findViewById(R.id.text_view_axis_z_gyroscope);
        textViewMeasuredFrequency = findViewById(R.id.text_view_measured_freq);
        textViewSetFrequency = findViewById(R.id.text_view_set_freq);

        accelerationChartX = findViewById(R.id.acceleration_chart_x);
        accelerationChartY = findViewById(R.id.acceleration_chart_y);
        accelerationChartZ = findViewById(R.id.acceleration_chart_z);
        accelerationChartSum = findViewById(R.id.acceleration_chart_sum);
        gyroscopeChartX = findViewById(R.id.gyro_chart_x);
        gyroscopeChartY = findViewById(R.id.gyro_chart_y);
        gyroscopeChartZ = findViewById(R.id.gyro_chart_z);
        gyroscopeChartSum = findViewById(R.id.gyro_chart_sum);


        setupChart(accelerationChartX, -6, 6);
        setupChart(accelerationChartY, -6, 6);
        setupChart(accelerationChartZ, -6, 6);
        setupChart(accelerationChartSum, 0, 12);
        setupChart(gyroscopeChartX, -360 * 4, 360 * 4);
        setupChart(gyroscopeChartY, -360 * 4, 360 * 4);
        setupChart(gyroscopeChartZ, -360 * 4, 360 * 4);
        setupChart(gyroscopeChartSum, 0, 360 * 8);
        setupData(accelerationChartX);
        setupData(accelerationChartY);
        setupData(accelerationChartZ);
        setupData(accelerationChartSum);
        setupData(gyroscopeChartX);
        setupData(gyroscopeChartY);
        setupData(gyroscopeChartZ);
        setupData(gyroscopeChartSum);

        View saveDialogView = getLayoutInflater().inflate(R.layout.dialog_layout, null);
        saveFileDialog = new AlertDialog.Builder(this).create();
        saveFileDialog.setTitle("Enter filename");
        //alertDialog.setIcon(getResources().getDrawable(R.drawable.ic_baseline_save_24px).setColorFilter(new
        //       PorterDuffColorFilter(0xffff00,PorterDuff.Mode.SRC_ATOP)));
        saveFileDialog.setCancelable(true);
        saveFileDialogEditText = saveDialogView.findViewById(R.id.edit_text_filename);
        saveFileDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save", this);
        saveFileDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", this);
        saveFileDialog.setView(saveDialogView);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) : null;
        sensorGyroscope = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) : null;

        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        stopWatch.start();


    }


    protected void onPause() {
        super.onPause();
        //sensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        if (soundPool != null) {
            soundPool.release();
        }
        if (bt != null) {
            bt.stopService();
        }
    }

    private void setupData(LineChart lineChart) {
        LineData data = new LineData();
        //data.setValueTextColor(Color.WHITE);
        lineChart.setData(data);
    }

    private void setupChart(LineChart lineChart, float min, float max) {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(false);
        lineChart.setPinchZoom(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(true);
        //lineChart.setAutoScaleMinMaxEnabled(true);
        //lineChart.getXAxis().setDrawLabels(false);
        lineChart.getXAxis().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setAxisMinimum(min);
        lineChart.getAxisLeft().setAxisMaximum(max);
        //lineChart.getAxisLeft().setTypeface(Typeface.createFromAsset(this.getAssets(), "product_sans_bold.ttf"));
        lineChart.getAxisLeft().enableGridDashedLine(8f, 8f, 0);
    }

    private LineDataSet createSet(String label, @ColorInt int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setMode(LineDataSet.Mode.LINEAR);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS[0]);
        set.setLineWidth(0.5f);
        set.setColor(color);
        //set.setColor(Color.BLACK);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        //set.setDrawFilled(true);
        //set.setFillAlpha(70);
        //set.setFillColor(color);

        return set;
    }

    private void clearChart(LineChart lineChart) {
        if (lineChart.getData() != null) {
            lineChart.getData().clearValues();
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        }
    }

    private void addEntry(ChartPoint point, LineChart lineChart, String label, @ColorInt int color) {
        LineData data = lineChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet(label, color);
                data.addDataSet(set);
            }

            data.addEntry(new Entry(point.getTimeStamp(), point.getValue()), 0);
            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            //lineChart.setVisibleXRangeMaximum(1000);
            lineChart.moveViewToX(data.getEntryCount());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.toolbarMenu = menu;
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_charts:
                Toast.makeText(getApplicationContext(), "Charts have been cleared", Toast.LENGTH_SHORT).show();
                clearChart(accelerationChartX);
                clearChart(accelerationChartY);
                clearChart(accelerationChartZ);
                clearChart(accelerationChartSum);
                clearChart(gyroscopeChartX);
                clearChart(gyroscopeChartY);
                clearChart(gyroscopeChartZ);
                clearChart(gyroscopeChartSum);
                accSamplesCount = 0;
                gyroSamplesCount = 0;

                textViewAccSamplesCount.setText(String.format("Accel: %d Samples",accSamplesCount));
                textViewGyroSampelsCount.setText(String.format("Gyro: %d Samples", gyroSamplesCount));
                textViewTimeFromStart.setText(Utils.getTime(0));
                if(bt!=null){
                    if(bt.isServiceAvailable()){
                        bt.send("params_"+0+"_"+0+"_"+Utils.getTime(0),true);
                    }
                }
                if(stopWatch.isStarted()) {
                    stopWatch.reset();
                    stopWatch.start();
                }
                return true;

            case R.id.run_pause:
                isRunning = !isRunning;
                soundPool.play(soundId, 1, 1, 0, 0, 1);
                if (isRunning) {
                    if (sensorManager != null) {
                        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                        sensorManager.registerListener(this, sensorGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                        item.setIcon(getResources().getDrawable(R.drawable.ic_baseline_pause_24px));
                        if (stopWatch.isSuspended()) {
                            stopWatch.resume();
                        } else if (stopWatch.isStopped()) {
                            stopWatch.start();
                        }
                    }
                } else {
                    if (sensorManager != null) {
                        sensorManager.unregisterListener(this);
                        item.setIcon(getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24px));
                        if (stopWatch.isStarted()) {
                            stopWatch.suspend();
                        }
                    }
                }
                return true;

            case R.id.save:
                if (isRunning) {
                    soundPool.play(soundId, 1, 1, 0, 0, 1);
                    if (sensorManager != null) {
                        sensorManager.unregisterListener(this);
                        toolbarMenu.findItem(R.id.run_pause).setIcon(getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24px));
                    }
                    isRunning = !isRunning;
                }
                saveFileDialog.show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_ANDROID);
                bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
                    @Override
                    public void onDataReceived(byte[] data, String message) {
                        if (message.equals("action_run")) {
                            Log.e("Bt receive", message);
                            MainActivity.this.onOptionsItemSelected(toolbarMenu.findItem(R.id.run_pause));
                        } else if (message.equals("action_clear")) {
                            Log.e("Bt receive", message);
                            MainActivity.this.onOptionsItemSelected(toolbarMenu.findItem(R.id.clear_charts));
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                String filename = saveFileDialogEditText.getText().toString().trim();
                final String date = new SimpleDateFormat("dd_MM_yyy_HH_mm_ss").format(new Date());
                if (!filename.isEmpty()) {
                    filename = filename + "_";
                }
                Utils.saveAsCsv(MainActivity.this, accelerationChartX.getData().getDataSetByIndex(0),accelerationChartY.getData().getDataSetByIndex(0),accelerationChartZ.getData().getDataSetByIndex(0),accelerationChartSum.getData().getDataSetByIndex(0), filename + "accel_" + date);

                Utils.saveAsCsv(MainActivity.this, gyroscopeChartX.getData().getDataSetByIndex(0),gyroscopeChartY.getData().getDataSetByIndex(0),gyroscopeChartZ.getData().getDataSetByIndex(0),gyroscopeChartSum.getData().getDataSetByIndex(0), filename + "gyro_" + date);
                break;

            case AlertDialog.BUTTON_NEGATIVE:

                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            this.onOptionsItemSelected(toolbarMenu.findItem(R.id.run_pause));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public interface OnSensorMeasurement {
        void onMeasurement(float x, float y, float z, float sum, float measuredFrequency);
    }

    private class ChartDrawTask extends AsyncTask<Float, Void, Void> {

        float frequency;
        ChartPoint chartPointX;
        ChartPoint chartPointY;
        ChartPoint chartPointZ;
        ChartPoint chartPointSum;
        OnSensorMeasurement listener;
        LineChart chartX;
        LineChart chartY;
        LineChart chartZ;
        LineChart chartSum;


        ChartDrawTask(OnSensorMeasurement listener, LineChart chartX, LineChart chartY, LineChart chartZ, LineChart chartSum) {
            this.listener = listener;
            this.chartX = chartX;
            this.chartY = chartY;
            this.chartZ = chartZ;
            this.chartSum = chartSum;
        }

        @Override
        protected Void doInBackground(Float... params) {
            frequency = (1000.0f / params[0]); // Current sampling frequency in Hz
            chartPointX = new ChartPoint(params[1], params[4]);
            chartPointY = new ChartPoint(params[2], params[4]);
            chartPointZ = new ChartPoint(params[3], params[4]);
            chartPointSum = new ChartPoint(((float) (Math.sqrt(Math.pow(chartPointX.getValue(), 2) + Math.pow(chartPointY.getValue(), 2) + Math.pow(chartPointZ.getValue(), 2)))), params[4]);
            addEntry(chartPointX, chartX, "Axis X", Color.RED);
            addEntry(chartPointY, chartY, "Axis Y", Color.GREEN);
            addEntry(chartPointZ, chartZ, "Axis Z", Color.BLUE);
            addEntry(chartPointSum, chartSum, "Sum", Color.MAGENTA);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            listener.onMeasurement(chartPointX.getValue(), chartPointY.getValue(), chartPointZ.getValue(), chartPointSum.getValue(), frequency);
        }

    }
}