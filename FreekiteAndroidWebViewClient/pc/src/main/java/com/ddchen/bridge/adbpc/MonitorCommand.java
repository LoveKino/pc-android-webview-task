package com.ddchen.bridge.adbpc;

import android.os.FileObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuer on 10/12/16.
 * <p>
 * used to monitor command json file
 */

public class MonitorCommand {
    private static FileObserver observer = null;

    // handler queue
    // TODO clear or remove monitor API
    private static Map<String, ArrayList<ExecuteCommand>> executeHandlersMap = new HashMap<>();
    private static Map<String, FileObserver> observerMap = new HashMap<>();

    public interface ExecuteCommand {
        void execute(String command);
    }

    public static void monitor(String commandFile, final ExecuteCommand executeCommand) {
        if (observer != null) return;
        final File file = new File(commandFile);
        file.getParentFile().mkdirs();
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<ExecuteCommand> handlers = executeHandlersMap.get(commandFile);
        if (handlers == null) {
            handlers = new ArrayList<>();
            executeHandlersMap.put(commandFile, handlers);
        }
        handlers.add(executeCommand);

        FileObserver observer = observerMap.get(commandFile);

        if (observer == null) {
            final ArrayList<ExecuteCommand> finalHandlers = handlers;
            observer = new FileObserver(commandFile) {
                @Override
                public void onEvent(int event, String f) {
                    if (event == FileObserver.CLOSE_WRITE) {
                        try {
                            FileInputStream ins = new FileInputStream(file);
                            byte[] buffer = new byte[(int) file.length()];
                            ins.read(buffer);
                            String command = new String(buffer);

                            for (ExecuteCommand handler : finalHandlers) {
                                handler.execute(command);
                            }

                            ins.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            observerMap.put(commandFile, observer);

            observer.startWatching();
        }
    }
}