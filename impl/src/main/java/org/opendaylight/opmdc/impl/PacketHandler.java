/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.opmdc.impl;

import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.common.collect.Lists;

import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;

//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev150105.OpmdcService;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev150105.SendPacketInput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev150105.SendPacketOutput;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev150105.SendPacketOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandler implements PacketProcessingListener  {
//public class OpmdcImpl implements OpmdcService, PacketProcessingListener  {
//public class OpmdcImpl implements OpmdcService{
    private PacketProcessingService packetProcessingService;
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    private static DataBroker dataBroker;
    private final static long FLOOD_PORT_NUMBER = 0xfffffffbL;
    private final static String multicasttype = "224.0.0.22";
    private final static String multicaststart = "224.0.0.1";
    private final static String CHANGE_TO_INCLUDE_MODE = "3";
    private final static String CHANGE_TO_EXCLUDE_MODE = "4";
    private final static String zero_number_of_source = "00";
    private final static String broadcast_dec = "255.255.255.255";
    private final static String broadcast_hex = "ff:ff:ff:ff:ff:ff";
    public final static int unicast = 0; 
    public final static int join = 1;
    public final static int leave = 2;
    public final static int multicast = 3;
    private Scheduler scheduler;
    
    public static final String OPENFLOW_NODE_PREFIX = "openflow:";
    
    public PacketHandler(DataBroker dataBroker, PacketProcessingService packetProcessingService) {
        LOG.info("PacketHandler Session Initiated");
        PacketHandler.dataBroker = dataBroker;
        this.packetProcessingService = packetProcessingService;
        this.scheduler = new Scheduler(); 
    }

    //for RPC handle
    //public PacketHandler(PacketProcessingService packetProcessingService) {
    //    this.packetProcessingService = packetProcessingService;
    //}
    
    /**
     * size of MAC address in octets (6*8 = 48 bits)
     */
    private static final int MAC_ADDRESS_SIZE = 6;

   /**
    * length of IP address in array
    */
    private static final int IP_LENGTH = 4;

    /**
     * length of source TCP/UDP Port in array
     */
    private static final int PORT_LENGTH = 2;

    /**
     * start position of destination MAC address in array
     */
    private static final int DST_MAC_START_POSITION = 0;

    /**
     * end position of destination MAC address in array
     */
    private static final int DST_MAC_END_POSITION = 6;

    /**
     * start position of source MAC address in array
     */
    private static final int SRC_MAC_START_POSITION = 6;

    /**
     * end position of source MAC address in array
     */
    private static final int SRC_MAC_END_POSITION = 12;


    /**
     * start position of ethernet type in array
     */
    private static final int ETHER_TYPE_START_POSITION = 12;

    /**
     * end position of ethernet type in array
     */
    private static final int ETHER_TYPE_END_POSITION = 14;

    private static final int IP_PROTOCOL_START_POSITION = 23;

    /**
     * start position of source IP address in array
     */
    private static final int SRC_IP_START_POSITION = 26;

    /**
     * end position of source IP address in array
     */
    private static final int SRC_IP_END_POSITION = 30;

    /**
     * start position of Destination IP address in array
     */
    private static final int DST_IP_START_POSITION = 30;

    /**
     * end position of Destination IP address in array
     */
    private static final int DST_IP_END_POSITION = 34;

    /**
     * start position of source TCP/UDP Port in array
     */
    private static final int SRC_PORT_START_POSITION = 34;

    /**
     * end position of source TCP/UDP Port  in array
     */
    private static final int SRC_PORT_END_POSITION = 36;

    /**
     * start position of Destination TCP/UDP Port  in array
     */
    private static final int DST_PORT_START_POSITION = 36;

    /**
     * end position of DestinationTCP/UDP Port in array
     */
    private static final int DST_PORT_END_POSITION = 38;

    /**
     * start position of multicast record
     */
    private static final int MUL_RECORD_START_POSITION = 46;
 
    /**
     * end position of multicast record"0"
     */
    private static final int MUL_RECORD_END_POSITION = 47;

    /**
     * start position of multicast source numbers
     */
    private static final int MUL_SRCNUM_START_POSITION = 48;

    /**
     * end position of multicast source numbers
     */
    private static final int MUL_SRCNUM_END_POSITION = 50;

    /**
     * start position of multicast group address
     */
    private static final int MUL_IP_START_POSITION = 50;

    /**
     * end position of multicast group address
     */
    private static final int MUL_IP_END_POSITION = 54;
   
    private static final int POWER = 256;

    /*
     * @param payload
     * @return destination IP address
     */
    public static byte[] extractDstIP(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_IP_START_POSITION, DST_IP_END_POSITION);
    }

    /*
     * @param payload
     * @return source IP address
     */
    public static byte[] extractSrcIP(final byte[] payload) {
        return Arrays.copyOfRange(payload, SRC_IP_START_POSITION, SRC_IP_END_POSITION);
    }

    /*
     * @param payload
     * @return multicast group IP address
     */
    public static byte[] extractMulIP(final byte[] payload) {
        return Arrays.copyOfRange(payload, MUL_IP_START_POSITION, MUL_IP_END_POSITION);
    }

    /*
     * @param payload
     * @return multicast group record type
     */
    public static byte[] extractMulrecord(final byte[] payload) {
        return Arrays.copyOfRange(payload, MUL_RECORD_START_POSITION, MUL_RECORD_END_POSITION);
    }

    /*
     * @param payload
     * @return multicast group source numbers
     */
    public static byte[] extractMulsrcnum(final byte[] payload) {
        return Arrays.copyOfRange(payload, MUL_SRCNUM_START_POSITION, MUL_SRCNUM_END_POSITION);
    }

    /*
     * @param payload
     * @return Source TCP Port
     */
    public static byte[] extractSrcPort(final byte[] payload) {
        return Arrays.copyOfRange(payload, SRC_PORT_START_POSITION, SRC_PORT_END_POSITION);
    }

    /*
     * @param payload
     * @return Destination TCP Port
     */
    public static byte[] extractDstPort(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_PORT_START_POSITION, DST_PORT_END_POSITION);
    }

    /**
     */
    public static byte[] extractDstMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, DST_MAC_START_POSITION, DST_MAC_END_POSITION);
    }

    /**
     */
    public static byte[] extractSrcMac(final byte[] payload) {
        return Arrays.copyOfRange(payload, SRC_MAC_START_POSITION, SRC_MAC_END_POSITION);
    }

    /**
     */
    public static byte[] extractEtherType(final byte[] payload) {
        return Arrays.copyOfRange(payload, ETHER_TYPE_START_POSITION, ETHER_TYPE_END_POSITION);
    }

    public static byte extractIPprotocol(final byte[] payload) {
        return payload[IP_PROTOCOL_START_POSITION];
    }

    /*
     * @param rawdata
     * @return String number
     */
    public static String rawdataToString(byte[] rawdata) {
        if (rawdata != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0 ; i<rawdata.length; i++){
                    sb.append(String.format("%d",rawdata[i] & 0xff));

            }
            return sb.substring(0,sb.length());
        }
        return null;
    }

    /*
     * @param rawIP
     * @return String IPAddress
     */
    public static String rawIPToString(byte[] rawIP) {
        if (rawIP != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0 ; i<rawIP.length; i++){
                sb.append(String.format("%d",rawIP[i] & 0xff));
                sb.append(".");
            }
            return sb.substring(0,sb.length()-1);
        }
        return null;
    }

    /*
     * @param rawPort
     * @return int TCPPort
     */
    public static int rawPortToInteger(byte[] rawPort) {
        int intOctet =0;
        int intOctetSum = 0;
        int iter = 1;
        if (rawPort != null && rawPort.length == PORT_LENGTH) {
            for (byte octet : rawPort) {
                intOctet = octet & 0xff;
                intOctetSum = (int) (intOctetSum + intOctet *  Math.pow(POWER,iter));
                iter--;
            }
            return intOctetSum;
        }
        return 0;
    }

    /**
     */
    public static String byteToHexStr(final byte[] bts, String delimit) {
        StringBuffer macStr = new StringBuffer();

        for (int i = 0; i < bts.length; i++) {
            String str = Integer.toHexString(bts[i] & 0xFF);
            if( str.length()<=1 ){
                macStr.append("0");
            }
            macStr.append(str);

            if( i < bts.length - 1 ) { //not last delimit string
                macStr.append(delimit);
            }
        } // end of for !!
        return macStr.toString();
    }

    @Override
    public void onPacketReceived(PacketReceived notification) {
        // TODO Auto-generated method stub
        LOG.debug("PacketHandler Received packet {}", InventoryUtils.getNodeConnectorId(notification.getIngress()));
        
        
        
        NodeConnectorRef ingressNodeConnectorRef = notification.getIngress();
        NodeConnectorId ingressNodeConnectorId = InventoryUtils.getNodeConnectorId(ingressNodeConnectorRef);
        NodeId ingressNodeId = InventoryUtils.getNodeId(ingressNodeConnectorRef);
        NodeRef ingressNodeRef = InventoryUtils.getNodeRef(ingressNodeConnectorRef);
        
        NodeConnectorId floodNodeConnectorId = InventoryUtils.getNodeConnectorId(ingressNodeId, FLOOD_PORT_NUMBER);
        NodeConnectorRef floodNodeConnectorRef = InventoryUtils.getNodeConnectorRef(floodNodeConnectorId);

        byte[] payload = notification.getPayload();
        byte[] dstMacRaw = extractDstMac(payload);
        byte[] srcMacRaw = extractSrcMac(payload);
        byte[] ethType   = extractEtherType(payload);
        byte[] srcIPRaw  = extractSrcIP(payload);
        byte[] dstIPRaw  = extractDstIP(payload);
        
        String dstMac = byteToHexStr(dstMacRaw, ":");
        String srcMac = byteToHexStr(srcMacRaw, ":");
        String ethStr = byteToHexStr(ethType, "");
        String srcIP  = rawIPToString(srcIPRaw);
        String dstIP  = rawIPToString(dstIPRaw);
        String tmpIP  = dstIP;
        String[] dstIPsplit = tmpIP.split("\\.");

        if (Integer.parseInt(dstIPsplit[0]) != 10){
        	if (Integer.parseInt(dstIPsplit[0]) < 224)
        		return;
        }
        
        if (dstIP.equals(broadcast_dec) || dstMac.equals(broadcast_hex) || dstIP.equals("0.0.0.0") ){
            LOG.debug("Ignore broadcast");
            
        }
        else{
            // multicast join or leave msg
            if (dstIP.equals(multicasttype)){
                byte[] mulIPRaw  = extractMulIP(payload);
                byte[] mulrecordRaw = extractMulrecord(payload);
                byte[] mulsrcnumRaw = extractMulsrcnum(payload);
                
                String mulIP  = rawIPToString(mulIPRaw);
                String mulrecord = rawIPToString(mulrecordRaw);
                String mulsrcnum = rawdataToString(mulsrcnumRaw);
                LOG.info("record " + mulrecord + " srcnum " + mulsrcnum);
                if ( mulrecord.equals(CHANGE_TO_EXCLUDE_MODE) && mulsrcnum.equals(zero_number_of_source)){ // join group
                	scheduler.run(join,srcIP,mulIP);
                    LOG.info("Join {} group",mulIP);
                }
                else if ( mulrecord.equals(CHANGE_TO_INCLUDE_MODE) && mulsrcnum.equals(zero_number_of_source)){ // leave group
                	scheduler.run(leave,srcIP,mulIP);
                    LOG.info("Leave {} group",mulIP);
                }
            }
            // multicast send
            else if (Integer.parseInt(dstIPsplit[0]) >= 224){
            //else if ( dstIP.compareTo(multicaststart) > 0 ) {
            	scheduler.run(multicast,srcIP,dstIP);
                LOG.info("multicast DstIP = {}",dstIP);
            }
            else {
            	scheduler.run(unicast,srcIP,dstIP);
            	LOG.info("Unitcast DstIP = {}",dstIP);
            }

            //FlowModify(true,"L31","1","192.168.14.100",0,10);
            /*
            FlowModify(true,"L31","1","192.168.14.100",0,10);
            FlowModify(true,"L31","1",dstIP,0,10);
            FlowModify(true,"L32","1","192.168.14.100"PacketHandler,0,12);
            FlowModify(true,"L33","1","192.168.14.102",0,14);
            FlowModify(true,"L34","1","192.168.14.104",0,16);
            FlowModify(true,"L35","1","0",10,18);
            */
            
            /*
            try {
	            Thread.sleep(20000);
	        } catch (InterruptedException e) {
	            e.printStackTrace(); 
	        }
            */
            
            /*
            FlowModify(false,"L31","1",dstIP,0,10);
            FlowModify(false,"L32","1","192.168.14.100",0,12);
            FlowModify(false,"L33","1","192.168.14.102",0,14);
            FlowModify(false,"L34","1","192.168.14.104",0,16);
            FlowModify(false,"L35","1","192.168.14.106",0,18);
            */
            
            /*
            NodeConnectorId egressNodeConnectorId;
            if (ingressNodeConnectorId.equals(InventoryUtils.getNodeConnectorId(ingressNodeId,14)))
                egressNodeConnectorId = InventoryUtils.getNodeConnectorId(ingressNodeId,16);
            else
                egressNodeConnectorId = InventoryUtils.getNodeConnectorId(ingressNodeId,14);
        	*/
            
            // packet out
            //NodeConnectorRef egressNodeConnectorRef = InventoryUtils.getNodeConnectorRef(egressNodeConnectorId);
            //packetOut(ingressNodeRef ,egressNodeConnectorRef, notification.getPayload());  //send packet!!
            LOG.debug("PacketHandler progress packet done");
        }

    }

    private void packetOut(NodeRef egressNodeRef, NodeConnectorRef egressNodeConnectorRef, byte[] payload) {
            // Construct input for RPC call to packet processing service
            TransmitPacketInput input = new TransmitPacketInputBuilder().setPayload(payload).setNode(egressNodeRef)
                            .setEgress(egressNodeConnectorRef).build();

            packetProcessingService.transmitPacket(input);
    }
    
    //private void FlowModify(NodeId nodeId, String dstIP, NodeConnectorId ingressNodeConnectorId, NodeConnectorId egressNodeConnectorId) {
    public static void FlowModify(boolean isAdd, String flowId ,String dpid, String dstIP, long inport , String outport) {
        /* Programming a flow involves:
         * 1. Creating a Flow object that has a match and a list of instructions,
         * 2. Adding Flow object as an augmentation to the Node object in the inventory. 
         * 3. FlowProgrammer module of OpenFlowPlugin will pick up this data change and eventually program the switch.
        */
    	String nodeName = OPENFLOW_NODE_PREFIX + dpid;
    	NodeId nodeId = new NodeId(nodeName);
    	
        LOG.info("FLowModify in "+nodeId);
        
    	if (!dstIP.equals("0") && dstIP.indexOf("/32") == -1){
            dstIP += "/32";
    	}
    	NodeConnectorId ingressNodeConnectorId = null;
    	if (inport != 0){
    		ingressNodeConnectorId = InventoryUtils.getNodeConnectorId(nodeId,inport);
    	}
    	
        
        //Creating match object
        MatchBuilder matchBuilder = new MatchBuilder();
        if (!dstIP.equals("0"))
        	MatchUtils.createDstL3IPv4Match(matchBuilder, new Ipv4Prefix(dstIP));
        if ( ingressNodeConnectorId != null)
        	MatchUtils.createInPortMatch(matchBuilder, ingressNodeConnectorId);

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        InstructionBuilder ib = new InstructionBuilder();
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        ActionBuilder ab = new ActionBuilder();
        List<Action> actionList = Lists.newArrayList();
        List<Instruction> instructions = Lists.newArrayList();

        // Set output action
        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(65535); //Send full packet and No buffer
        
        LOG.info("outport " + outport);
        int index;
        for (int i = 0 ; ; i++){
        	if (outport.charAt(i) >='0' && outport.charAt(i)<='9'){
        		index = i;
        		break;
        	}
        }
        
        outport = outport.substring(index);
        LOG.info("outport substring " + outport);
        String[] outportSplit = outport.split(";+");
  
        for (int i = 0 ; i < outportSplit.length ; i++){
        	LOG.info(i + outportSplit[i]);
        	
	        NodeConnectorId egressNodeConnectorId = InventoryUtils.getNodeConnectorId(nodeId,Long.parseLong(outportSplit[i]));
	        LOG.info("connector Id " + egressNodeConnectorId);
	        if (!egressNodeConnectorId.equals(0)){
	        	output.setOutputNodeConnector(egressNodeConnectorId);
	        	ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
	        	ab.setOrder(i);	// one action
	        	ab.setKey(new ActionKey(i));	        	
	        	actionList.add(ab.build());
	        }
        }
        

	        	LOG.info("action list " + actionList);
        
        // Create Apply Actions Instruction
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Create Flow
        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());

        //String flowId = "L3_Rule_" + dstIP;
        //flowId = "L3_Rule_1";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true); 
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId((short)0);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(32768);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        flowBuilder.setInstructions(isb.setInstruction(instructions).build());

        InstanceIdentifier<Flow> flowIID = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(flowBuilder.getTableId()))
                .child(Flow.class, flowBuilder.getKey())
                .build();
 
        LOG.info("Create transcation");
        GenericTransactionUtils.writeData(PacketHandler.dataBroker, LogicalDatastoreType.CONFIGURATION, flowIID, flowBuilder.build(), isAdd);
        
    }
    
    //impl packet out RPC (no impl)
    /*
    @Override
    public Future<RpcResult<SendPacketOutput>> sendPacket(SendPacketInput input) {
        SendPacketOutputBuilder packetBuilder = new SendPacketOutputBuilder();

        //LOG.info("Packetout Initiated");
        NodeConnectorRef egress = input.getEgress();
        NodeRef node = input.getNode();
        byte[] payload = input.getRawpacket();
        packetOut(node,egress, payload);  //send packet!!

        packetBuilder.setResult("OK");
        return RpcResultBuilder.success(packetBuilder.build()).buildFuture();
    }    
    */
}
