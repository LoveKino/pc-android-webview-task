package com.ddchen.bridge.adbpc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;


/**
 * Created by yuer on 10/19/16.
 */

public class AdbSender {

    private String outputDir = null;

    public AdbSender(String outputDir) {
        this.outputDir = outputDir;
        new File(outputDir).mkdirs();
    }

    /**
     * 1. write data into a json file
     * 2. send command to PC
     *
     * @param data
     */
    public void send(Object data) {
        String id = UUID.randomUUID().toString();
        String str = data.toString();

        String outputFile = outputDir + File.separator + id;
        File file = new File(outputFile);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(str.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
