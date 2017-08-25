/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.opmdc.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.OpmdcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpmdcProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OpmdcProvider.class);
    
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;

    private ListenerRegistration<NotificationListener> registration = null;// registration for PacketProcessingListener
    private RpcRegistration<OpmdcService> opmdcService = null;
    
    public OpmdcProvider(final DataBroker dataBroker,
                final RpcProviderRegistry rpcRegistry,
                final NotificationPublishService notificationPublishService,
                final NotificationService notificationService) {
        this.rpcRegistry = rpcRegistry;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("OpmdcProvider Session Initiated");
        // for packetout RPC handle
        PacketProcessingService packetProcessingService = rpcRegistry.getRpcService(PacketProcessingService.class);
        
        // handle set parameter rpc 
        opmdcService = rpcRegistry.addRpcImplementation(OpmdcService.class, new MemoryManager());
        
        // catch notification
        if (notificationService != null) {
        LOG.info("NotificationService is: " + notificationService.toString());
        // create packet handler
        PacketHandler mypacketHandler = new PacketHandler(dataBroker,packetProcessingService);
        // create monitor 
        Monitor monitor = new Monitor(dataBroker);
        
        registration = notificationService.registerNotificationListener(mypacketHandler);
        }
        LOG.info("OpmdcProvider Session Ended");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("OpmdcProvider Closed");
        if( registration != null){
            registration.close();           
        }
    }
}