package kz.codingwolves.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import kz.codingwolves.framework.Api;
import kz.codingwolves.framework.QueryParam;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Created by sagynysh on 2/16/17.
 */
public class Runner {

    public static void main(String[] args) throws Exception {

        String application = System.getProperty("application");
        if (application == null) {
            noApplication();
        }

        File file = new File(application);
        if (!file.exists()) {
            noApplication();
        }

        ClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
        JarFile jarFile = new JarFile(file);
        Map<String, Method> methodsAndValues = new HashMap<>();

        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {

            String entryName = entries.nextElement().getName();
            if (entryName.endsWith(".class")) {
                entryName = entryName.replace('/', '.');
                entryName = entryName.substring(0, entryName.length() - ".class".length());
                System.out.println(entryName + " was loaded");
                Class loadedClass = classLoader.loadClass(entryName);
                for (int i = 0; i < loadedClass.getDeclaredMethods().length; i++) {
                    Api api = loadedClass.getDeclaredMethods()[i].getAnnotation(Api.class);
                    if (api != null) {
                        System.out.println(api.value() + " path was registered");
                        methodsAndValues.put(api.value(), loadedClass.getDeclaredMethods()[i]);
                    }
                }
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                try {
                    List<String> contentTypes = new ArrayList<>();
                    contentTypes.add("text/plain");
                    httpExchange.getResponseHeaders().put("content-type", contentTypes);

                    String path = httpExchange.getRequestURI().getPath();
                    if (methodsAndValues.containsKey(path)) {
                        Method method = methodsAndValues.get(path);
                        Map<String, String> parameters = queryToMap(httpExchange.getRequestURI().getQuery());
                        Object[] arguments = substituteParams(method, parameters);
                        try {
                            String result = (String) method.invoke(method.getDeclaringClass().newInstance(), arguments);
                            sendMessage(200, result, httpExchange);
                        } catch (Exception e) {
                            e.printStackTrace();
                            sendMessage(400, "Parameters are not correct", httpExchange);
                        }
                    } else {
                        sendMessage(404, "Method not found", httpExchange);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendMessage(500, "Internal server error", httpExchange);
                }
            }
        });
        server.setExecutor(null);
        server.start();
    }

    private static void sendMessage(int code, String message, HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(code, 0);
        httpExchange.getResponseBody().write(message.getBytes());
        httpExchange.getResponseBody().close();
        httpExchange.close();
    }

    private static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String pair[] = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], pair[1]);
                } else {
                    result.put(pair[0], "");
                }
            }
        }
        return result;
    }

    private static Object[] substituteParams(Method method, Map<String, String> parameters) {
        Object[] result = new Object[method.getParameters().length];
        for (int i = 0; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            String name = parameter.getAnnotation(QueryParam.class).value();
            if (parameters.containsKey(name)) {
                result[i] = parameters.get(name);
            } else {
                return null;
            }
        }
        return result;
    }

    private static void noApplication() {
        System.out.println("No application deployed");
        System.exit(0);
    }
}
