package org.igniterealtime.openfire.customiqhandler;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;

import java.io.File;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.cluster.ClusterEventListener;
import org.jivesoftware.openfire.cluster.ClusterManager;
import org.jivesoftware.openfire.container.PluginListener;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyCustomQueryPlugin implements Plugin, SessionEventListener, ClusterEventListener, PluginListener {

    private static final Logger Log = LoggerFactory.getLogger(MyCustomQueryPlugin.class);
    public File pluginDirectory;
    private MyCustomHandler customHandler;
    private IQRouter iqRouter;
    private CustomIQHandler filter;
    
    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory) {

        manager.addPluginListener(this);

        this.pluginDirectory = pluginDirectory;
       
        filter = new CustomIQHandler("customiqhandler", XMPPServer.getInstance().getPacketRouter());
        iqRouter = XMPPServer.getInstance().getIQRouter();
        iqRouter.addHandler(filter);
//        customHandler = new MyCustomHandler(XMPPServer.getInstance().getPacketRouter());
//        InterceptorManager.getInstance().addInterceptor(customHandler);
        SessionEventDispatcher.addListener(this);

        ClusterManager.addListener(this);

        Log.info("customquery Plugin - Initialize email listener");

    }

    @Override
    public void destroyPlugin() {
        try {
            SessionEventDispatcher.removeListener(this);
        } catch (Exception ex) {
            Log.error("An exception occurred while trying to destroy the customiqhandler IQ Handler.", ex);
        }
        if (customHandler != null) {
            InterceptorManager.getInstance().removeInterceptor(customHandler);
            customHandler = null;
        }
        iqRouter.removeHandler(filter);
        ClusterManager.removeListener(this);
        XMPPServer.getInstance().getPluginManager().removePluginListener(this);

    }

    @Override
    public void anonymousSessionCreated(Session session) {
        Log.debug("customquery Plugin -  anonymousSessionCreated " + session.getAddress().toString() + "\n" + ((ClientSession) session).getPresence().toXML());
    }

    @Override
    public void anonymousSessionDestroyed(Session session) {
        Log.debug("customquery Plugin -  anonymousSessionDestroyed " + session.getAddress().toString() + "\n" + ((ClientSession) session).getPresence().toXML());
    }

    @Override
    public void resourceBound(Session session) {
        Log.debug("customquery Plugin -  resourceBound " + session.getAddress().toString() + "\n" + ((ClientSession) session).getPresence().toXML());
    }

    @Override
    public void sessionCreated(Session session) {
        Log.debug("customquery Plugin -  sessionCreated " + session.getAddress().toString() + "\n" + ((ClientSession) session).getPresence().toXML());
    }

    @Override
    public void sessionDestroyed(Session session) {
        Log.debug("customquery Plugin -  sessionDestroyed " + session.getAddress().toString() + "\n" + ((ClientSession) session).getPresence().toXML());
    }

    @Override
    public void pluginCreated(final String pluginName, final Plugin plugin) {
        System.out.println("pluginName " + pluginName);
    }

    @Override
    public void leftCluster(byte[] arg0) {
    }

    @Override
    public void markedAsSeniorClusterMember() {
        Log.info("This instance was marked as senior member of an Openfire cluster. Loading all OFMeet functionality.");
        try {

        } catch (Exception ex) {
            Log.error("An exception occurred while trying to initialize the Jitsi Plugin.", ex);
        }
    }

    @Override
    public void pluginDestroyed(final String pluginName, final Plugin plugin) {
    }

    @Override
    public void joinedCluster() {
        Log.info("An Openfire cluster was joined. Unloading customquery functionality (as only the senior cluster node will provide this.");
    }

    @Override
    public void joinedCluster(byte[] bytes) {
    }

    @Override
    public void leftCluster() {
    }

}
