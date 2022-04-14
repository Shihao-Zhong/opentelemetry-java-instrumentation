package io.opentelemetry.instrumentation.api.instrumenter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Executors;

public class VAIFConfigWatch {

  private final String serviceName;
  private final String contextPropagationPath;
  private final String configPath;
  private JSONObject config;
  private HashMap<String, HashSet<String>> contextPropagationConfig;
  public boolean running;

  VAIFConfigWatch(String serviceName, String configPath, String contextPropagationPath) {
    this.running = true;
    this.serviceName = serviceName;
    this.configPath = configPath;
    this.contextPropagationPath = contextPropagationPath;
    this.readJsonConfig();
    this.getContextPropagationConfig();
    //this.printPropagationConfig();

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
    try {
      this.printConfig();
      boolean result = 1 == (long) this.config.get(spanName);
      //System.out.println("SpanName" + spanName + " set enable to " + result);
      return result;
    } catch (NullPointerException e) {
      //System.out.println("SpanName not exist in config, default enable");
      return true;
    }
  }

  public boolean isContextPropagation() {
    StackTraceElement[] classes = Thread.currentThread().getStackTrace();
    for (int i = 0; i < classes.length; i++) {

      //System.out.println("current class = " + classes[i].getClassName());
      //System.out.println(this.contextPropagationConfig.toString());

      if (this.contextPropagationConfig.containsKey(classes[i].getClassName())) {
        HashSet<String> methodSet = this.contextPropagationConfig.get(classes[i].getClassName());
        //System.out.println("get set + " + methodSet + classes[i].getMethodName() + methodSet.contains(classes[i].getMethodName()));
        if (methodSet.contains(classes[i].getMethodName())) {
          return true;
        }
      }
    }
    return false;
  }

  public void printConfig() {
    for (Object element : this.config.keySet())  {
      String key = (String) element;
      boolean value = 1 == (long) this.config.get(key);
      System.out.println("key = " + key + " val = " + value);
    }
  }

  public void stop() {
    this.running = false;
  }

  public void watchFile(String fullPath) {
    Path path = FileSystems.getDefault().getPath(fullPath);
    try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
      WatchKey watchKey = path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
      while (this.running) {
        WatchKey wk = watchService.take();
        for (WatchEvent<?> event : wk.pollEvents()) {
          //we only register "ENTRY_MODIFY" so the context is always a Path.
          Path changed = (Path) event.context();
          if (changed.endsWith(path.getFileName())) {
            this.readJsonConfig();
          }
        }
        // reset the key
        boolean valid = wk.reset();
        if (!valid) {
          System.out.println("Key has been unregistered");
        }
      }
    } catch (IOException | InterruptedException e) {
      System.out.println("watch VAIF config error" + e);
    }
  }

  public void readJsonConfig() {
    JSONParser parser = new JSONParser();
    try {
      JSONObject obj = (JSONObject) parser.parse(
          Files.newBufferedReader(Paths.get(this.configPath), Charset.defaultCharset()));
      this.config = (JSONObject) obj.get(this.serviceName);
    } catch (Exception e) {
      System.out.println("read VAIF config error" + e);;
    }
  }

  public void getContextPropagationConfig() {
    JSONParser parser = new JSONParser();
    this.contextPropagationConfig = new HashMap<>();
    try {
      JSONObject obj = (JSONObject) parser.parse(
          Files.newBufferedReader(Paths.get(this.contextPropagationPath), Charset.defaultCharset()));

      for(Object key : obj.keySet()) {
        String className = (String) key;
        HashSet<String> methodSet = new HashSet<>();
        if (obj.get(className) instanceof JSONArray) {
          JSONArray arr = (JSONArray) obj.get(className);
          for (int i = 0; i < arr.size(); i++) {
            methodSet.add(arr.get(i).toString());
          }
        }
        this.contextPropagationConfig.put(className, methodSet);
      }

    } catch (Exception e) {
      System.out.println("read VAIF context propagation config error" + e);;
    }
  }

  public void printPropagationConfig() {
    for (String name: this.contextPropagationConfig.keySet()) {
      String key = name.toString();
      String value = this.contextPropagationConfig.get(name).toString();
      System.out.println(key + " " + value);
    }
  }
}
