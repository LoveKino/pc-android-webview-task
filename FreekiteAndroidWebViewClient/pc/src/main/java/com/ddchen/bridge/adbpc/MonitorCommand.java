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

    private static String commandFileName = "command.json";

    // handler queue
    // TODO clear or remove monitor API
    private static Map<String, ArrayList<ExecuteCommand>> executeHandlersMap = new HashMap<>();
    private static Map<String, FileObserver> observerMap = new HashMap<>();

    public interface ExecuteCommand {
        void execute(String command);
    }

    public static void monitor(String commandDir, final ExecuteCommand executeCommand) {
        if (observer != null) return;
        new File(commandDir).mkdirs();

        final String commandFilePath = commandDir + File.separator + commandFileName;

        ArrayList<ExecuteCommand> handlers = executeHandlersMap.get(commandDir);
        if (handlers == null) {
            handlers = new ArrayList<>();
            executeHandlersMap.put(commandDir, handlers);
        }
        handlers.add(executeCommand);

        FileObserver observer = observerMap.get(commandDir);

        if (observer == null) {
            final ArrayList<ExecuteCommand> finalHandlers = handlers;
            observer = new FileObserver(commandDir) {
                @Override
                public void onEvent(int event, String f) {
                    if (commandFileName.equals(f) &&
                            (event == FileObserver.CLOSE_WRITE || event == FileObserver.MOVED_TO)) {
                        try {
                            File commandFile = new File(commandFilePath);
                            FileInputStream ins = new FileInputStream(commandFile);
                            byte[] buffer = new byte[(int) commandFile.length()];
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

            observerMap.put(commandDir, observer);

            observer.startWatching();
        }
    }
}