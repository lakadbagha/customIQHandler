/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.igniterealtime.openfire.customiqhandler;

import org.dom4j.Element;
import org.jivesoftware.openfire.PacketRouter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

/**
 *
 * @author deepak
 */
class MyCustomHandler implements PacketInterceptor {


    private Element probeResult;
    private final PacketRouter router;

    MyCustomHandler(PacketRouter packetRouter) {
        this.router = packetRouter;
    }

    @Override
    public void interceptPacket(Packet packet,
            Session session,
            boolean incoming,
            boolean processed) throws PacketRejectedException {
        // OF-1591 - I can't quite explain these. Maybe the session has been closed before the outgoing server promise timed out?
        if (session == null) {
            return;
        }
        if (packet.getFrom() != null
                && !packet.getFrom().asBareJID().equals(session.getAddress().asBareJID())
                && !packet.getFrom().toBareJID().equals(XMPPServer.getInstance().getServerInfo().getXMPPDomain())) {
            return;
        }

        final Element filter = filterQuery(packet);

        if (filter == null) {
            return;
        } else {
            IQ reply = IQ.createResultIQ((IQ) packet);
            router.route(reply);
        }
    }

    private Element filterQuery(Packet packet) {
        if (packet instanceof IQ) {
            final IQ iq = (IQ) packet;
            final Element childElement = iq.getChildElement();
            System.out.println("iq " + iq.getChildElement().getNamespaceURI() + " xml=" + iq.getElement().getNamespaceURI());
            if (childElement == null || iq.getType() != IQ.Type.result) {
                return null;
            }
            System.out.println("childElement " + childElement.getName() + " qualifiied name " + childElement.getStringValue() + " ,text=, " + childElement.getText() + " " + childElement.attributeValue("type"));
            switch (childElement.getNamespaceURI()) {
                case "com:via:call#request":
                    System.out.println("com:via:call#request invoked");
                    return childElement;

            }
        }
        System.out.println("not IQ instance");
        return null;
    }

}
