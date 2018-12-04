package com.example.divided.signalgathering;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Environment;
import android.widget.Toast;

import com.example.divided.signalgathering.model.SensorDataPack;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static boolean saveAsCsv(Context context, SensorDataPack dataPack, String filename) {

        List<Double> verticalAccelerationData = calculateVerticalAccelerationData(dataPack);

        File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "Acceleration signals");

        if (!directory.exists()) {
            directory.mkdir();
        }
        File newFile = new File(directory, filename + ".csv");

        try (CSVWriter writer = new CSVWriter(new FileWriter(newFile), ';', CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER)) {
            //final float timeStampOffset = dataSetX.getEntryForIndex(0).getX();
            final double timeStampOffset = dataPack.getLinearAccelerationData().get(0).getTimestamp();
            List<String[]> data = new ArrayList<>();
            for (int i = 0; i < dataPack.getPackSize(); i++) {
                data.add(new String[]{String.format("%f", dataPack.getLinearAccelerationData().get(i).getTimestamp() - timeStampOffset) // czas w ns
                        , String.format("%f", dataPack.getLinearAccelerationData().get(i).getX())   //przyspieszenie liniowe X
                        , String.format("%f", dataPack.getLinearAccelerationData().get(i).getY())   //przyspieszenie liniowe Y
                        , String.format("%f", dataPack.getLinearAccelerationData().get(i).getZ())   //przuspieszenie liniowe Z
                        , String.format("%f", dataPack.getLinearAccelerationData().get(i).getModule())  //przyspieszenie liniowe Mod
                        , String.format("%f", dataPack.getGyroscopeData().get(i).getX()) //zyroskop x
                        , String.format("%f", dataPack.getGyroscopeData().get(i).getY()) //zyroskop y
                        , String.format("%f", dataPack.getGyroscopeData().get(i).getZ()) //zyroskop Z
                        , String.format("%f", dataPack.getGyroscopeData().get(i).getModule())    //zyroskop Mod
                        , String.format("%f", dataPack.getMagneticFieldData().get(i).getX()) //pole magnetyczne x
                        , String.format("%f", dataPack.getMagneticFieldData().get(i).getY()) //pole magnetyczne y
                        , String.format("%f", dataPack.getMagneticFieldData().get(i).getZ()) //polemagnetyczne Z
                        , String.format("%f", dataPack.getMagneticFieldData().get(i).getModule())    //zyroskop Mod
                        , String.format("%f", dataPack.getRotationVectorData().get(i).getX()) //rotacja x
                        , String.format("%f", dataPack.getRotationVectorData().get(i).getZ()) //rotacja y
                        , String.format("%f", dataPack.getRotationVectorData().get(i).getZ()) //rotacja Z
                        , String.format("%f", verticalAccelerationData.get(i)) // składowa normalna przyspieszenia (vertykalna)
                });
            }
            writer.writeAll(data);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "File save failed", Toast.LENGTH_SHORT).show();
            return false;
        }

        Toast.makeText(context, "File save successfull", Toast.LENGTH_SHORT).show();
        return true;

    }

    public static String getTime(long time) {
        long timeToEdit = time;
        String textToDraw = "";
        if (TimeUnit.MILLISECONDS.toMinutes(time) >= 10) {
            textToDraw = textToDraw + TimeUnit.MILLISECONDS.toMinutes(time);
        } else {
            textToDraw = textToDraw + "0" + TimeUnit.MILLISECONDS.toMinutes(time);
        }
        timeToEdit -= TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(timeToEdit));
        if (TimeUnit.MILLISECONDS.toSeconds(timeToEdit) >= 10) {
            textToDraw = textToDraw + ":" + TimeUnit.MILLISECONDS.toSeconds(timeToEdit);
        } else {
            textToDraw = textToDraw + ":0" + TimeUnit.MILLISECONDS.toSeconds(timeToEdit);
        }
        timeToEdit -= TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(timeToEdit));
        if (TimeUnit.MILLISECONDS.toMillis(timeToEdit) >= 100) {
            textToDraw = textToDraw + ":" + TimeUnit.MILLISECONDS.toMillis(timeToEdit);
        } else if (TimeUnit.MILLISECONDS.toMillis(timeToEdit) >= 10) {
            textToDraw = textToDraw + ":0" + TimeUnit.MILLISECONDS.toMillis(timeToEdit);
        } else {
            textToDraw = textToDraw + ":00" + TimeUnit.MILLISECONDS.toMillis(timeToEdit);
        }
        return textToDraw;
    }

    private static List<Double> calculateVerticalAccelerationData(SensorDataPack dataPack) {
        List<Double> verticalAccelerationData = new ArrayList<>();

        float[] degrees = new float[3];
        float[] rotationMatrix = new float[9];
        double thetaY;
        double thetaZ;

        for (int i = 0; i < dataPack.getPackSize(); i++) {
            android.hardware.SensorManager.getRotationMatrix(rotationMatrix, null, dataPack.getRotationVectorData().get(i).getValues(), dataPack.getMagneticFieldData().get(i).getValues());
            SensorManager.getOrientation(rotationMatrix, degrees);
            thetaY = degrees[2];
            thetaZ = degrees[0];


            final double verticalAcceleration = calculateAccelerationNormal(dataPack.getLinearAccelerationData().get(i).getX(), dataPack.getLinearAccelerationData().get(i).getY(), dataPack.getLinearAccelerationData().get(i).getZ(), thetaY, thetaZ);
            verticalAccelerationData.add(verticalAcceleration);
        }

        return verticalAccelerationData;
    }

    private static double calculateAccelerationNormal(double aX, double aY, double aZ, double thetaY, double thetaZ) {

        return Math.abs(aX * Math.sin(thetaZ) + aY * Math.sin(thetaY) - aZ * Math.cos(thetaY) * Math.cos(thetaZ));
    }
}