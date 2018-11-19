package com.example.divided.signalgathering;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Utils {
    public static boolean saveAsCsv(Context context, ILineDataSet dataSet, String filename) {
        File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "Acceleration signals");

        if (!directory.exists()) {
            directory.mkdir();
        }
        File newFile = new File(directory, filename + ".csv");
        try (CSVWriter writer = new CSVWriter(new FileWriter(newFile))) {
            final float timeStampOffset = dataSet.getEntryForIndex(0).getX();
            List<String[]> data = new ArrayList<>();
            for (int i = 0; i < dataSet.getEntryCount(); i++) {
                Entry currentEntry = dataSet.getEntryForIndex(i);
                data.add(new String[]{String.format("%f", currentEntry.getX() - timeStampOffset)
                        , String.format("%f", currentEntry.getY())});
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

    public static String getTime(long time){
        long timeToEdit = time;
        String textToDraw = "";
        if(TimeUnit.MILLISECONDS.toMinutes(time) >= 10){
            textToDraw = textToDraw + TimeUnit.MILLISECONDS.toMinutes(time);
        }else{
            textToDraw = textToDraw + "0" + TimeUnit.MILLISECONDS.toMinutes(time);
        }
        timeToEdit -=        TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(timeToEdit));
        if(TimeUnit.MILLISECONDS.toSeconds(timeToEdit) >= 10){
            textToDraw = textToDraw + ":" + TimeUnit.MILLISECONDS.toSeconds(timeToEdit);
        }else{
            textToDraw = textToDraw + ":0" + TimeUnit.MILLISECONDS.toSeconds(timeToEdit);
        }
        timeToEdit -= TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(timeToEdit));
        if(TimeUnit.MILLISECONDS.toMillis(timeToEdit) >= 100){
            textToDraw = textToDraw + ":" + TimeUnit.MILLISECONDS.toMillis(timeToEdit);
        }else if(TimeUnit.MILLISECONDS.toMillis(timeToEdit) >= 10) {
            textToDraw = textToDraw + ":0" + TimeUnit.MILLISECONDS.toMillis(timeToEdit);
        }else{
            textToDraw = textToDraw + ":00" + TimeUnit.MILLISECONDS.toMillis(timeToEdit);
        }
        return textToDraw;
    }
}
