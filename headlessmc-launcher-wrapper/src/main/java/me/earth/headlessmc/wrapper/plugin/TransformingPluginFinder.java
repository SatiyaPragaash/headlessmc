package me.earth.headlessmc.wrapper.plugin;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

@RequiredArgsConstructor
public class TransformingPluginFinder extends PluginFinder {
    private final Path transformerPluginsDirectory;

    public TransformingClassloader build(Path launcherJar, Path pluginsDirectory) throws IOException {
        URLClassLoader transformerClassloader = new URLClassLoader(find(transformerPluginsDirectory).toArray(new URL[0]), getClass().getClassLoader());
        // while we load the TransformerPlugins I want this to be the context classloader
        //Thread.currentThread().setContextClassLoader(transformerClassloader);
        List<TransformerPlugin> plugins = new ArrayList<>();
        ServiceLoader.load(TransformerPlugin.class, transformerClassloader).forEach(plugins::add);
        ServiceLoader.load(TransformerPlugin.class).forEach(plugins::add);
        System.out.println(plugins);
        Collections.sort(plugins);

        List<URL> classpath = find(pluginsDirectory);
        classpath.add(launcherJar.toUri().toURL());
        System.out.println(classpath);
        return new TransformingClassloader(classpath.toArray(new URL[0]), getClass().getClassLoader(), transformerClassloader, plugins);
    }

}
