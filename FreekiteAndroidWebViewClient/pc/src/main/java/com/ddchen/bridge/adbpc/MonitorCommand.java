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

    public static void monitor(final String commandDir, final ExecuteCommand executeCommand) {
        if (observer != null) return;
        new File(commandDir).mkdirs();

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
                    System.out.println("***********************************");
                    System.out.println(event);
                    System.out.println(f);
                    if (f != null &&
                            f.indexOf(commandFileName) != -1 &&
                            (event == FileObserver.CLOSE_WRITE || event == FileObserver.MOVED_TO)) {
                        try {
                            final String commandFilePath = commandDir + File.separator + f;
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