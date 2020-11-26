/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.igniterealtime.openfire.customiqhandler;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.container.PluginServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author deepak
 */
class ModuleManager {

    private static final Logger Log = LoggerFactory.getLogger(ModuleManager.class);
    private final Plugin plugin;

    private Map<String, Module> modulesByClassName;
    private Map<Module, ModuleClassLoader> classLoaderByModule;

    private PluginManager manager;
    private File pluginDirectory;
    private ClassLoader parent;

    public ModuleManager(final Plugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void start(final ClassLoader parent, final PluginManager manager, final File pluginDirectory) {
        Log.debug("Starting manager.");
        modulesByClassName = new ConcurrentHashMap<>();
        classLoaderByModule = new ConcurrentHashMap<>();

        this.parent = parent;
        this.manager = manager;
        this.pluginDirectory = pluginDirectory;
    }

    public synchronized void stop() {
        Log.debug("Stopping manager.");
        for (String moduleName : modulesByClassName.keySet()) {
            try {
                unloadModule(moduleName);
            } catch (Exception e) {
                Log.error("Unable to unload module '{}'.", moduleName, e);
            }
        }

        modulesByClassName.clear();
        classLoaderByModule.clear();
        parent = null;
        manager = null;
        pluginDirectory = null;
    }

    public synchronized void loadModule(String moduleClassname, Path path) throws ClassNotFoundException, IllegalAccessException, InstantiationException, ServletException, NoSuchFieldException {
        Log.debug("Loading module '{}' from '{}'.", moduleClassname, path);

        Log.trace("Initialize the class loader dedicated to this module.");
        final ModuleClassLoader classLoader = new ModuleClassLoader(parent);
        classLoader.addDirectory(path.toFile(), false);

        Log.trace("Instantiate the module, using its own classloader.");
        final Module module = (Module) classLoader.loadClass(moduleClassname).newInstance();

        modulesByClassName.put(moduleClassname, module);
        classLoaderByModule.put(module, classLoader);

        Log.trace("Initialize the module in its own classloader.");
        final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            module.initialize(manager, pluginDirectory);

            Log.trace("Registering module servlets (if any).");
            final Map<String, String> servlets = module.getServlets();
            if (servlets != null) {
                for (final Map.Entry<String, String> entry : servlets.entrySet()) {
                    final GenericServlet servlet = instantiateServlet(entry.getValue());
                    PluginServlet.registerServlet(
                            XMPPServer.getInstance().getPluginManager(),
                            plugin,
                            servlet,
                            entry.getKey()
                    );
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    public synchronized void unloadModule(String moduleClassname) throws ServletException {
        Log.debug("Unloading module '{}'.", moduleClassname);
        final Module module = modulesByClassName.remove(moduleClassname);
        if (module == null) {
            return;
        }

        Log.trace("Deregistering module servlets (if any).");
        final Map<String, String> servlets = module.getServlets();
        if (servlets != null) {
            for (final String url : servlets.keySet()) {
                PluginServlet.unregisterServlet(
                        plugin,
                        url
                );
            }
        }

        Log.trace("Destroying the module.");
        module.destroy();

        Log.trace("Tearing down the class loader dedicated to this module.");
        classLoaderByModule.remove(module).unloadJarFiles();
    }

    public synchronized void reloadAllConfiguration() {
        final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            for (final Map.Entry<Module, ModuleClassLoader> entry : classLoaderByModule.entrySet()) {
                final Module module = entry.getKey();
                final ClassLoader moduleClassLoader = entry.getValue();

                Thread.currentThread().setContextClassLoader(moduleClassLoader);
                try {
                    module.reloadConfiguration();
                } catch (Exception e) {
                    Log.warn("An exception occurred while reloading the configuration of module '{}'.", module, e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    private static GenericServlet instantiateServlet(final String servletClassname) throws ClassNotFoundException, IllegalAccessException, InstantiationException, ServletException, NoSuchFieldException {
        final Class<?> theClass = Thread.currentThread().getContextClassLoader().loadClass(servletClassname);

        final Object instance = theClass.newInstance();
        if (!(instance instanceof GenericServlet)) {
            throw new IllegalArgumentException("Could not load servlet instance");
        }

        // TODO find better way than using reflection to get the servletConfig instance.
        Field field = PluginServlet.class.getDeclaredField("servletConfig");
        field.setAccessible(true);
        try {
            ((GenericServlet) instance).init((ServletConfig) field.get(null));
        } finally {
            field.setAccessible(false);
        }

        return ((GenericServlet) instance);
    }
}
