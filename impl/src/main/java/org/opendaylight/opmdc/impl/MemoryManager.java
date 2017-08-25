/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.opmdc.impl;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.GetResponseOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.GetResponseOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.OpmdcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.SetParameterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.SetParameterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.opmdc.rev170705.SetParameterOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class MemoryManager implements OpmdcService{
	private static final Logger LOG = LoggerFactory.getLogger(MemoryManager.class);
	
    public static int switchAmount = 25, portAmount = 16, monitorPeriod = 700;//monitorPeriod is milli seconds
    public static int[][] connectionTable = new int[switchAmount*portAmount][switchAmount*portAmount];
    public static int[][] minAll2AllTable = new int[switchAmount*portAmount][switchAmount*portAmount];
    public static double[][] portUtilization = new double[switchAmount*portAmount][switchAmount*portAmount];
    public static double[][] portStatistics_preTimeslot = new double[switchAmount*portAmount][3];
    public static double[][] portStatistics = new double[switchAmount*portAmount][3]; // Tx_bytes, Rx_bytes, Duration
    public static double[][] portSpeed = new double[switchAmount*portAmount][2]; // Tx_bw, Rx_bw (bps)


    //network constant
    public static final int MAXPortNum = 16;
    public static final int PORT = 8;
    public static final int subnodeset = 2;
    public static final int BAND = 8;
    public static final int BLUE = 1;
    public static final int RED = 0;
    public static final int k = 5;
    public static final int EW = 2;
    public static final long ini_wavelength = 0xFFFFFFFFFFL;

    
    //RESTful API setting
    public static int table = 1;
    public static int alarm = 0;
    public static final int TableType = 1;

    //equivalent to c++ struct
    public static class connection{
        public int connID;
        public int srcOADS;
        public int dstOADS;
        public int srcColor;
        public int dstColor;
        public int port;
        public Queue<Integer> connQueue;

        public connection(int connID, int srcOADS, int dstOADS, int srcColor, int dstColor, int port){
            this.connID = connID;
            this.srcOADS = srcOADS;
            this.dstOADS = dstOADS;
            this.srcColor = srcColor;
            this.dstColor = dstColor;
            this.port = port;
            this.connQueue = new LinkedList<Integer>(); 
        }
    }

    //to do list
    //request nodeID and set up

    //network state
    public static connection[] connections = new connection[k*k*MAXPortNum];
    public static ArrayList<HashSet<Integer>> ConnIDSortedBySrcDst = new ArrayList<HashSet<Integer>>(k*k*MAXPortNum);
    public static ArrayList<HashSet<Integer>> FixedConnIDSortedBySrcDst = new ArrayList<HashSet<Integer>>(k*k*MAXPortNum);
    public static long[][] ToHorizonCombiner = new long[k][EW];
    public static long[][] ToTier1Combiner = new long[k][subnodeset];
    public static long[][] DestinationSubnodeWave = new long[k*k][subnodeset];
    public static long[][] SourceSubnodeWave = new long[k*k][subnodeset];
    public static Boolean[][][] DestinationSubnodePort = new Boolean[k*k][subnodeset][PORT];
    public static Boolean[][][] SourceSubnodePort = new Boolean[k*k][subnodeset][PORT];
    public static Boolean[] fixed_alloc =new Boolean[k*k*MAXPortNum];

    //store output Port list in each ToR switch, classified by multiIP group
    public static HashMap<String, HashMap<Integer, String>> MulticastOutputPortLists = new HashMap<String, HashMap<Integer, String>>();
    
    //store IP that joins a multiIP group, classified by OADS
    public static HashMap<String, HashMap<Integer, HashSet<String>>> MulIPGroup = new HashMap<String, HashMap<Integer, HashSet<String>>>();
    
    public static void init(){
    	
    	LOG.info("enter init function");

    	MulticastOutputPortLists.clear();
    	MulIPGroup.clear();
    	
    	for(int i=0; i<k*k*MAXPortNum; i++){
    		HashSet<Integer> set = new HashSet<Integer>();
    		ConnIDSortedBySrcDst.add(set);
    		FixedConnIDSortedBySrcDst.add(set);
    	}

    	for(int i=0; i<k*k*MAXPortNum; i++){
    		connections[i] = null;
    		fixed_alloc[i] = false;
    		
    	}
    	for(int i=0; i<k; i++){
    		for(int j=0;j<EW; j++){
    			ToHorizonCombiner[i][j] = ini_wavelength;
    			ToTier1Combiner[i][j] = ini_wavelength;
    		}
    	}
    	for(int i=0; i<k*k; i++){
    		for(int j=0; j<subnodeset; j++){
    			DestinationSubnodeWave[i][j] = ini_wavelength;
    			SourceSubnodeWave[i][j] = ini_wavelength;
    			for(int l = 0; l<PORT; l++){
    				DestinationSubnodePort[i][j][l] = true;
    				SourceSubnodePort[i][j][l] = true;
    			}
    		}
    	}
    	LOG.info("exit init function");
    	
    }
    
    //impl setparameter RPC
    @Override
    public Future<RpcResult<SetParameterOutput>> setParameter(SetParameterInput input) {
        SetParameterOutputBuilder parameterBuilder = new SetParameterOutputBuilder();
        LOG.info("received input:"+input);

        //LOG.info("Packetout Initiated");
        //NodeConnectorRef egress = input.getEgress();
        //NodeRef node = input.getNode();
        //byte[] payload = input.getRawpacket();

        LOG.info("getAlarm:"+input.getParameters());
        String[] parameters = input.getParameters().split(";+");
        for(String parameter: parameters){
        	String[] NameValue = parameter.split(":");
        	if(NameValue.length != 2) continue;
        	switch (NameValue[0]){
        	case "T":{
        		if(NameValue[1].charAt(0)>='0' || NameValue[1].charAt(0)<='9'){
        			table = Integer.parseInt(NameValue[1].substring(0, 1));
        			Scheduler.OnParameterSet(TableType);
        		}
        		break;
        	}
        	case "A":{
        		if(NameValue[1].charAt(0)>='0' || NameValue[1].charAt(0)<='9'){
        			alarm = Integer.parseInt(NameValue[1].substring(0, 1));
        			//Scheduler.OnParameterSet(TableType);
        		}
        		break;
        	}
        	case "M":{
        		if(NameValue[1].charAt(0)>='0' || NameValue[1].charAt(0)<='9'){
        			monitorPeriod = Integer.parseInt(NameValue[1].substring(0, 1)); // three digits representing monitoring period in microseconds
        		}
        		break;
        	}
        	default: break;
        	}
        }

        parameterBuilder.setResult("OK");
        return RpcResultBuilder.success(parameterBuilder.build()).buildFuture();
    }    

    //impl get-responseRPC
    @Override
    public Future<RpcResult<GetResponseOutput>> getResponse() {
    	GetResponseOutputBuilder responseBuilder = new GetResponseOutputBuilder();

        //LOG.info("Packetout Initiated");
        //NodeConnectorRef egress = input.getEgress();
        //NodeRef node = input.getNode();
        //byte[] payload = input.getRawpacket();

    	responseBuilder.setResponse("you get response");
        return RpcResultBuilder.success(responseBuilder.build()).buildFuture();
    }    
}
