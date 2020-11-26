/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.igniterealtime.openfire.customiqhandler;

import java.io.File;
import java.util.Map;
import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

/**
 *
 * @author deepak
 */
public class CustomIQHandler extends IQHandler{

    private final String childElementName = "query";
    private final String childElementNameSpace = "com:via:call#request";
    private IQHandlerInfo info;
    private final PacketRouter router;

    public CustomIQHandler(String moduleName, PacketRouter router) {
        super(moduleName);
        info = new IQHandlerInfo(childElementName, childElementNameSpace);
        this.router = router;
        System.out.println("CustomIQHandler invoked ");
    }

    @Override
    public IQ handleIQ(IQ iq) throws UnauthorizedException {
        IQ reply = IQ.createResultIQ(iq);
        System.out.println("handleIQ invoked");
        final Element childElement = iq.getChildElement();
        System.out.println("CustomIQHandler " + iq.getChildElement().getNamespaceURI() + " xml=" + iq.getElement().getNamespaceURI());
        if (childElement == null || iq.getType() != IQ.Type.result) {
            reply.setChildElement(childElement.createCopy());
            reply.setError(PacketError.Condition.bad_request);
            return reply;
        }
        System.out.println("CustomIQHandler " + childElement.getName() + " qualifiied name " + childElement.getStringValue() + " ,text=, " + childElement.getText() + " " + childElement.attributeValue("type"));
        switch (childElement.getNamespaceURI()) {
            case "com:via:call#request":
                System.out.println("com:via:call#request CustomIQHandler invoked");
                router.route(reply);
                return reply;

        }
        System.out.println("ouch error coiming");
        throw new UnauthorizedException();
    }

    @Override
    public IQHandlerInfo getInfo() {
        return info;
    }
}
