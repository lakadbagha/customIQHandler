/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.igniterealtime.openfire.customiqhandler;

import java.io.File;
import java.util.Map;
import org.jivesoftware.openfire.container.PluginManager;

/**
 *
 * @author deepak
 */
interface Module {

    void initialize(final PluginManager manager, final File pluginDirectory);

    void destroy();

    void reloadConfiguration();

    Map<String, String> getServlets();
}
