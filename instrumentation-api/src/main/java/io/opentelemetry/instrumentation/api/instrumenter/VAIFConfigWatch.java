package io.opentelemetry.instrumentation.api.instrumenter;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.nio.file.*;
import java.util.Iterator;
import java.util.concurrent.Executors;

public class VAIFConfigWatch {

    private String serviceName;
    private String configPath;
    private JSONObject config;
    public boolean running;

    VAIFConfigWatch(String serviceName, String configPath) {
        this.running = true;
        this.serviceName = serviceName;
        this.configPath = configPath;
        this.readJsonConfig();
    }

    public void startWatch() {
        Executors.newCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                watchFile(configPath);
            }
        });
    }

    public JSONObject getConfig() {
        return this.config;
    }

    public boolean isEnable(String spanName) {
        return 1 == (long) this.config.get(spanName);
    }

    public void printConfig() {
        for(Iterator iterator = this.config.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            boolean value = 1 == (long) this.config.get(key);
            System.out.println("key = " + key + " val = " + value);
        }
    }

    public void stop() {
        this.running = false;
    }

    public void watchFile(String fullPath) {
        final Path path = FileSystems.getDefault().getPath(fullPath);
        try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
            final WatchKey watchKey = path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            while (this.running) {
                final WatchKey wk = watchService.take();
                for (WatchEvent<?> event : wk.pollEvents()) {
                    //we only register "ENTRY_MODIFY" so the context is always a Path.
                    final Path changed = (Path) event.context();
                    if (changed.endsWith(path.getFileName())) {
                        this.readJsonConfig(fullPath);
                    }
                }
                // reset the key
                boolean valid = wk.reset();
                if (!valid) {
                    System.out.println("Key has been unregisterede");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readJsonConfig() {
        JSONParser parser = new JSONParser();
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(this.configPath));
            this.config = (JSONObject) obj.get(this.serviceName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
