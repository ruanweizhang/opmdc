/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.opmdc.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private DataBroker dataBroker;
    private NetworkState networkState;

    private static final int MAXPortNum = 16;
    private static final int PORT = 8;
    private static final int subnodeset = 2;
    private static final int BAND = 8;
    private static final int BLUE = 1;
    private static final int RED = 0;
    private static final int k = 5;
    private static final int EW = 2;
    private static final long ini_wavelength = 0xFFFFFFFFFFL;
    private static int CannotEstabNewConn = -1;
    
    private HashMap<String, HashMap<Integer, HashSet<String>>> MulIPGroup = new HashMap<String, HashMap<Integer, HashSet<String>>>();
    

    public Scheduler() {
        LOG.info("Scheduler Session Initiated");
        networkState = new NetworkState();
        MemoryManager.init();
        //RWA();
    }

    public static void OnParameterSet(int type){
    	LOG.info("enter OnParameterSet with type:"+type);
    	LOG.info("table value:"+MemoryManager.table);
    	if(type == MemoryManager.TableType){
    		if(MemoryManager.table == 1){
    			//test1
    			LOG.info("establish connection 100 to 200");
    			run(PacketHandler.unicast, "10.1.1.100","10.1.1.200" );
    		}
    		if(MemoryManager.table == 2){
    			LOG.info("establish connection 200 to 100");
    			run(PacketHandler.unicast, "10.1.1.200","10.1.1.100" );
    			PacketHandler.FlowModify(true,"whatever","1","10.1.1.201", 0,String.valueOf(310));
    		}
    	}
    	
    }
    public static void run(long type, String srcIP, String dstIP){
    	
        //unicast packet in
        //topology mapping
        //srcIP = "10.1.1.1k";//example 1
        String[] srcIPSplit = srcIP.split("\\.");
        int srcPod = Integer.parseInt(srcIPSplit[1]);
        int srcOADSIndex = Integer.parseInt(srcIPSplit[2]);
        int srcOADS = srcPod * k + srcOADSIndex;
        int inputPort = Integer.parseInt(srcIPSplit[3]);
        //LOG.info("SrcOADS:"+srcOADS);
        //LOG.info("SrcInputPort:"+inputPort);

        //dstIP = "10.2.1.10";//example 2
        String[] dstIPSplit = dstIP.split("\\.");
        int dstPod = Integer.parseInt(dstIPSplit[1]);
        int dstOADSIndex = Integer.parseInt(dstIPSplit[2]);
        int dstOADS = dstPod * k + dstOADSIndex;
        int outputPort = Integer.parseInt(dstIPSplit[3]);
        //LOG.info("DstOADS:"+dstOADS);
        //LOG.info("DstOutputPort:"+outputPort);
        
        if(type == PacketHandler.unicast) {
        	LOG.info("receive unicast with IP "+srcIP+" "+dstIP);
            //if(outputPort>4) return;
            
            //if(srcOADS<0 || srcOADS>24 || dstOADS<0 || dstOADS>24) return;
           
            if(srcOADS == dstOADS) {
            	LOG.debug("src IP is the same dst IP");
            	PacketHandler.FlowModify(true,dstIP,"1",dstIP,0, String.valueOf(outputPort));
            } else if (CanEstablishNewConn(srcOADS, dstOADS) != CannotEstabNewConn){
            	 PacketHandler.FlowModify(true,dstIP,"1",dstIP,0, String.valueOf(outputPort));
            }
        }
    	//join multiIP
    	if (type==PacketHandler.join) { //dstIP is the MulIP
    		String MultIP = dstIP;
    		int srcToR = srcOADS;
    		
    		LOG.info("receive a join request, IP:"+ MultIP + " " +srcToR);
    		LOG.info("split 1;2;;3;4;;; :"+"1;2;;3;4;;;".split(";").toString());
    		LOG.info("split 1;2;;3;4;;; :"+"1;2;;3;4;;;".split(";+").toString());
    		
    		if(MemoryManager.MulIPGroup.get(MultIP) != null){
    			
    			if(MemoryManager.MulIPGroup.get(MultIP).get(srcToR) != null) {
    				//add flow entry that routes flows to servers in this ToR switch
    				HashSet<String> set = MemoryManager.MulIPGroup.get(MultIP).get(srcToR);
    				set.add(srcIP);
    				MemoryManager.MulIPGroup.get(MultIP).put(srcToR, set);
    				LOG.info(""+MemoryManager.MulticastOutputPortLists.get(MultIP));
        			String outputPortList = MemoryManager.MulticastOutputPortLists.get(MultIP).get(srcToR);
        			if(outputPortList.contains(srcIP.split("\\.")[3])) return;
        			outputPortList += srcIP.split("\\.")[3]+ ";";
        			MemoryManager.MulticastOutputPortLists.get(MultIP).put(srcToR, outputPortList);
        			PacketHandler.FlowModify(true, MultIP, String.valueOf(srcToR), MultIP, 0, outputPortList);//each srcIP in set will generate a flow entry in which srcIP's port is output port
        			
    			} else {//no such ToR in MulIPGroup
    				HashSet<String> set = new HashSet<String>();
        			set.add(srcIP);
    				MemoryManager.MulIPGroup.get(MultIP).put(srcToR, set);//if exist it means ToR switch already has flow entry heads to other OADSs
        			
    				//in srcToR
    					//add ports that routes flows to servers in this ToR switch
    				String outputPortList = srcIP.split("\\.")[3]+ ";";
    					// to do list
    					//add ports that routes flows to other OADSs
    				/*
        			HashMap<Integer, Set<String>> ToRs = MulIPGroup.get(MultIP);
        			for(int ToR : ToRs.keySet()) {
        				if(ToR != srcToR) {
        					outputPortList += String.valueOf(related port of ToR in srcToR)+";";
        				}
        			}
        			*/
    				
    				MemoryManager.MulticastOutputPortLists.get(MultIP).put(srcToR, outputPortList);
    				//modify flow entry in srcToR
    				PacketHandler.FlowModify(true, MultIP, String.valueOf(srcToR), MultIP, 0, outputPortList);//each srcIP in set will generate a flow entry in which srcIP's port is output port
        			
    				
    				//in other ToRs
    				/*
        			HashMap<Integer, Set<String>> ToRs = MulIPGroup.get(MultIP);
        			for(int ToR : ToRs.keySet()) {
        				if(ToR != srcToR) {
        					String outputPortList = MemoryManager.MulticastOutputPortLists.get(MultIP).get(ToR);
        					outputPortList += String.valueOf(related port of srcToR in ToR)+";";
        					MemoryManager.MulticastOutputPortLists.get(MultIP).put(ToR, outputPortList);
        					PacketHandler.FlowModify(true, MultIP, String.valueOf(ToR), MultIP, 0, outputPortList);
        				}
        			}
        			*/
    			}
    			
    		} else { // no such MultIP in record
    			//LOG.info("create new MultIP");
    			HashMap<Integer, HashSet<String>> ToRtoIP = new HashMap<Integer, HashSet<String>>();
    			HashSet<String> set = new HashSet<String>();
    			set.add(srcIP);//first IP in this MultIP
    			LOG.debug("set:"+set);
    			ToRtoIP.put(srcToR, set);
    			LOG.debug("ToRtoIP:"+ToRtoIP);
    			MemoryManager.MulIPGroup.put(MultIP, ToRtoIP);
    			LOG.debug("MulIPGroup"+MemoryManager.MulIPGroup);
    			
    			//add ports that routes flows to servers in this ToR switch
    			String outputPortList = srcIP.split("\\.")[3]+ ";";
    			
    			HashMap<Integer, String> map = new HashMap<Integer, String>();
    			MemoryManager.MulticastOutputPortLists.put(MultIP, map);
    			LOG.debug("MultioutputPort"+MemoryManager.MulticastOutputPortLists);
    			MemoryManager.MulticastOutputPortLists.get(MultIP).put(srcToR, outputPortList);
    			LOG.debug("MultioutputPort"+MemoryManager.MulticastOutputPortLists);
    			PacketHandler.FlowModify(true, MultIP, String.valueOf(srcToR), MultIP, 0, outputPortList);
    		}
    		return;
    	}
    	
    	//leave multiIP
    	if (type==PacketHandler.leave) {
    		String MultIP = dstIP;
    		int srcToR = srcOADS;
    		
    		if(MemoryManager.MulIPGroup.get(MultIP) != null){
    			if(MemoryManager.MulIPGroup.get(MultIP).get(srcToR) != null) {
    				LOG.info("before move a port:"+MemoryManager.MulIPGroup.get(MultIP).get(srcToR));
    				MemoryManager.MulIPGroup.get(MultIP).get(srcToR).remove(srcIP);//remove srcIP
    				LOG.info("after move a port:"+MemoryManager.MulIPGroup.get(MultIP).get(srcToR));
    				if(MemoryManager.MulIPGroup.get(MultIP).get(srcToR).size() == 0) { // just delete the last IP in srcToR
    					
    					//modify srcToR
    					MemoryManager.MulIPGroup.get(MultIP).remove(srcToR);
    					String outputPortList = MemoryManager.MulticastOutputPortLists.get(MultIP).get(srcToR);//retrieve output port in flow entry
    					MemoryManager.MulticastOutputPortLists.get(MultIP).remove(srcToR);
    					LOG.info("remove an entry: "+ MemoryManager.MulticastOutputPortLists.get(MultIP));
    					PacketHandler.FlowModify(false, MultIP, String.valueOf(srcToR), MultIP, 0, outputPortList);
    					
    					//modfy other ToRs
        				/*
            			HashMap<Integer, Set<String>> ToRs = MulIPGroup.get(MultIP);
            			for(int ToR : ToRs.keySet()) {
            				if(ToR != srcToR) {
            					String outputPortList = MemoryManager.MulticastOutputPortLists.get(MultIP).get(ToR);
            					outputPortList.replace(String.valueOf(related port of srcToR in ToR),""); //remove this port
            					outputPortList =outputPortList.replace(";;", ";");
            					MemoryManager.MulticastOutputPortLists.get(MultIP).put(ToR, outputPortList);
            					PacketHandler.FlowModify(true, MultIP, String.valueOf(ToR), MultIP, 0, outputPortList);
            				}
            			}
            			*/
    					
    				} else {//there are still IPs in srcToR
    					String outputPortList = MemoryManager.MulticastOutputPortLists.get(MultIP).get(srcToR);
    					outputPortList = outputPortList.replace(srcIP.split("\\.")[3], "");//remove this port
    					outputPortList =outputPortList.replace(";;", ";");
    					MemoryManager.MulticastOutputPortLists.get(MultIP).put(srcToR, outputPortList);//update MulticastOutputPortLists
    					LOG.info("remove a port: "+ MemoryManager.MulticastOutputPortLists.get(MultIP));
    					PacketHandler.FlowModify(true, MultIP, String.valueOf(srcToR), MultIP, 0, outputPortList);//modify flow entry
    				}
    				
    			} else {
    				LOG.info("null ToR, this ToR never appear!!");
    			}
    			
    		} else {
    			LOG.info("error: leave a null group!!");
    		}
    		return;
    	}
    	
    	//send multicast
    	if (type==PacketHandler.multicast) {
    		LOG.info("receive a send request, IP:"+ srcIP + " " +dstIP);
    		//nothing to do
    	}
        /*
        if CanEstablishNewConn(srcOADS, dstOADS){
            //following should be written in function Estab_intra_pod_Conn, Estab_inter_pod_Conn_Not_passing_tier2
            FlowModify1();
            FlowModify2();
            FlowModify3();
        }
        */
    }
    

    public static int CanEstablishNewConn(int srcOADS, int dstOADS){
    	LOG.info("enter CanEstab function");
        int srcPod = srcOADS / k;
        int srcOADSIndex = srcOADS % k;
        int dstPod = dstOADS / k;
        int dstOADSIndex = dstOADS % k;
        long WaveBand, result;
        int srcWXCIndex, dstWXCIndex;
        int EorW;
        int srcColor, dstColor;

        if (srcPod == dstPod){

        //try blue 
            srcColor = BLUE; 
            dstColor = BLUE;
            WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
            result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor];
            if (result > 0) {
                int port = convertToPort(result);
                assert(Estab_intra_pod_Conn(srcOADS, dstOADS, srcColor, port));
                int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
                return CanEstabConnID;
            }

        //try red
            srcColor = RED; 
            dstColor = RED;
            WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
            result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor];
            if (result > 0) {
                int port = convertToPort(result);
                assert(Estab_intra_pod_Conn(srcOADS, dstOADS, srcColor, port));
                int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
                return CanEstabConnID;
            }

        }
        else
        {
            if (srcPod != dstPod){
            //From Blue to Blue
                srcColor = BLUE; 
                dstColor = BLUE;
                WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
                srcWXCIndex = (srcPod + 1) % k;
                dstWXCIndex = (dstPod + 1) % k;
                EorW = (((srcWXCIndex - dstWXCIndex + k) % k)>((dstWXCIndex - srcWXCIndex + k) % k)) ? 0 : 1;// 0 to East, 1 to West
                result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor];
                if (result > 0) {
	                int port = convertToPort(result);
	                assert(Estab_inter_pod_Conn(srcOADS, srcWXCIndex, dstOADS, dstWXCIndex, srcColor, dstColor, port, EorW));
	                int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
	                return CanEstabConnID;
	            }

	            //From Red to Red
	            srcColor = RED; 
	            dstColor = RED;
	            WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
	            srcWXCIndex = srcPod;
	            dstWXCIndex = dstPod;
	            EorW = (((srcWXCIndex - dstWXCIndex + k) % k)>((dstWXCIndex - srcWXCIndex + k) % k)) ? 0 : 1;// 0 to East, 1 to West
	            result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor];
	            if (result > 0){
	                int port = convertToPort(result);
	                assert(Estab_inter_pod_Conn(srcOADS, srcWXCIndex, dstOADS, dstWXCIndex, srcColor, dstColor, port, EorW));
	                int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
	                return CanEstabConnID;
	            }
	
	            //From Blue to Red
	            srcColor = BLUE; 
	            dstColor = RED;
	            if (dstPod == (srcPod + 1) % k){
	                //not passing tier-2
	                WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
	                srcWXCIndex = (srcPod + 1) % k;
	                dstWXCIndex = dstPod;
	                result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor];
	                if (result > 0){
	                    int port = convertToPort(result);
	                    assert(Estab_inter_pod_Conn_Not_passing_tier2(srcOADS, dstOADS, srcColor, dstColor, port));
	                    int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
	                    return CanEstabConnID;
	                }
	
	            }
	            else{
	                WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
	                srcWXCIndex = (srcPod + 1) % k;
	                dstWXCIndex = dstPod;
	                EorW = (((srcWXCIndex - dstWXCIndex + k) % k)>((dstWXCIndex - srcWXCIndex + k) % k)) ? 0 : 1;// 0 to East, 1 to West
	                result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor];
	                if (result > 0){
	                    int port = convertToPort(result);
	                    assert(Estab_inter_pod_Conn(srcOADS, srcWXCIndex, dstOADS, dstWXCIndex, srcColor, dstColor, port, EorW));
	                    int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
	                    return CanEstabConnID;
	                }
	            }

	            //From Red to Blue
	            srcColor = RED; 
	            dstColor = BLUE;
	            if (srcPod == (dstPod + 1) % k){
	                WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
	                srcWXCIndex = srcPod;
	                dstWXCIndex = (dstPod + 1) % k;
	                result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor];
	                if (result > 0){
	                    int port = convertToPort(result);
	                    assert(Estab_inter_pod_Conn_Not_passing_tier2(srcOADS, dstOADS, srcColor, dstColor, port));
	                    int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
	                    return CanEstabConnID;
	                }
	
	            }
	            else{
	                WaveBand = (long)(0xFF) << (((srcOADSIndex + 1 - srcColor) % k)*BAND);
	                srcWXCIndex = srcPod;
	                dstWXCIndex = (dstPod + 1) % k;
	                EorW = (((srcWXCIndex - dstWXCIndex + k) % k)>((dstWXCIndex - srcWXCIndex + k) % k)) ? 0 : 1;// 0 to East, 1 to West
	                result = WaveBand & MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor];
	                if (result > 0){
	                    int port = convertToPort(result);
	                    assert(Estab_inter_pod_Conn(srcOADS, srcWXCIndex, dstOADS, dstWXCIndex, srcColor, dstColor, port, EorW));
	                    int CanEstabConnID = srcOADS*MAXPortNum + port + (1 - srcColor)*BAND;
	                    return CanEstabConnID;
	                }
	            }
            }
        }
        return CannotEstabNewConn;
    }
    
    public static Boolean Estab_intra_pod_Conn(int srcOADS, int dstOADS, int color, int port){
        int current_wavelength;
        int connID;
        assert(color == BLUE || color == RED);
        if (color == BLUE) current_wavelength = (srcOADS % k)*BAND + port; else current_wavelength = (((srcOADS % k) + 1) % k) * BAND + port;
        long WaveToOADSPortBit = 1;
        WaveToOADSPortBit = WaveToOADSPortBit << current_wavelength;

        long OADSPortBit = 1;
        OADSPortBit = (OADSPortBit << port) | (OADSPortBit << (port + BAND) | (OADSPortBit << (port + 2 * BAND)) | (OADSPortBit << (port + 3 * BAND)) | (OADSPortBit << (port + 4 * BAND)));


        if (MemoryManager.SourceSubnodePort[srcOADS][color][port] && MemoryManager.DestinationSubnodePort[dstOADS][color][port]){//port is equal to port index
            //from blue to blue

            MemoryManager.SourceSubnodeWave[srcOADS][color] = MemoryManager.SourceSubnodeWave[srcOADS][color] & (~OADSPortBit);
            MemoryManager.DestinationSubnodeWave[dstOADS][color] = MemoryManager.DestinationSubnodeWave[dstOADS][color] & (~OADSPortBit);

            MemoryManager.SourceSubnodePort[srcOADS][color][port] = false;
            MemoryManager.DestinationSubnodePort[dstOADS][color][port] = false;

            //establish connection
            connID = srcOADS * MAXPortNum + (1 - color)*BAND + port;
            assert(MemoryManager.connections[connID] == null);
            MemoryManager.connections[connID]=  new MemoryManager.connection(connID, srcOADS, dstOADS, color, color, port);
            MemoryManager.ConnIDSortedBySrcDst.get(srcOADS*k*k + dstOADS).add(connID);

            //cout << "from  inside"<< " src:" << srcOADS << "  dst:" << dstOADS << " " << 1 << " port:" << port << endl;
            //output to debug file

            return true;
        }
        return false;
    }
    public static Boolean Estab_inter_pod_Conn_Not_passing_tier2(int srcOADS, int dstOADS, int srcColor, int dstColor, int port){
        int srcPod = srcOADS / k, dstPod = dstOADS / k;
        int mutualWXCIndex;
        int srcOADSIndex = srcOADS - srcPod*k;
        int current_wavelength;
        assert(srcColor == BLUE || srcColor == RED);
        assert(dstColor == BLUE || dstColor == RED);

        if (srcColor == BLUE) mutualWXCIndex = (srcPod + 1) % k; else mutualWXCIndex = srcPod;
        if (MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] && MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port]){
            if (srcColor == BLUE) current_wavelength = srcOADSIndex*BAND + port; else current_wavelength = ((srcOADSIndex + 1) % k) * BAND + port;
            long WaveToOADSPortBit = 1;
            WaveToOADSPortBit = WaveToOADSPortBit << current_wavelength;


            long OADSPortBit = 1;
            OADSPortBit = (OADSPortBit << port) | (OADSPortBit << (port + BAND) | (OADSPortBit << (port + 2 * BAND)) | (OADSPortBit << (port + 3 * BAND)) | (OADSPortBit << (port + 4 * BAND)));

            if ((WaveToOADSPortBit &  MemoryManager.ToTier1Combiner[mutualWXCIndex][dstColor]) != 0){
                MemoryManager.ToTier1Combiner[mutualWXCIndex][dstColor] = MemoryManager.ToTier1Combiner[mutualWXCIndex][dstColor] & (~WaveToOADSPortBit);

                MemoryManager.SourceSubnodeWave[srcOADS][srcColor] = MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & (~OADSPortBit);
                MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] = MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & (~OADSPortBit);

                MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] = false;
                MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port] = false;



                //establish connection
                int connID = srcOADS*MAXPortNum + (1 - srcColor)*BAND + port;
                assert(MemoryManager.connections[connID] == null);
                MemoryManager.connections[connID] = new MemoryManager.connection(connID, srcOADS, dstOADS, srcColor, dstColor, port);
                MemoryManager.ConnIDSortedBySrcDst.get(srcOADS*k*k + dstOADS).add(connID);

                return true;
            }
        }
        return false;
    }
    public static Boolean Estab_inter_pod_Conn(int srcOADS, int srcWXCIndex, int dstOADS, int dstWXCIndex, int srcColor, int dstColor, int port, int EorW){
        int srcOADSIndex = srcOADS % k;
        int current_wavelength;
        assert(srcColor == BLUE || srcColor == RED);
        assert(dstColor == BLUE || dstColor == RED);

        if (MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] && MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port]){//transmit from BLUE module
            if (srcColor == BLUE) current_wavelength = srcOADSIndex*BAND + port; else current_wavelength = ((srcOADSIndex + 1) % k) * BAND + port;

            long WaveToOADSPortBit = 1;
            WaveToOADSPortBit = WaveToOADSPortBit << current_wavelength;

            long OADSPortBit = 1;
            OADSPortBit = (OADSPortBit << port) | (OADSPortBit << (port + BAND) | (OADSPortBit << (port + 2 * BAND)) | (OADSPortBit << (port + 3 * BAND)) | (OADSPortBit << (port + 4 * BAND)));

            //cout << (WaveToOADSPortBit & ToHorizonCombiner[srcWXCIndex][EorW] & ToTier1Combiner[dstWXCIndex][BLUE]) << endl;
            if ((WaveToOADSPortBit & MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] & MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor]) != 0){
                MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] = MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] & (~WaveToOADSPortBit);
                MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor] = MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor] & (~WaveToOADSPortBit);

                MemoryManager.SourceSubnodeWave[srcOADS][srcColor] = MemoryManager.SourceSubnodeWave[srcOADS][srcColor] & (~OADSPortBit);
                MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] = MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] & (~OADSPortBit);

                MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] = false;
                MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port] = false;

                //establish connection
                int connID = srcOADS*MAXPortNum + (1 - srcColor)*BAND + port;

                //cout << "connID:" << connID << endl;
                assert(MemoryManager.connections[connID] == null);
                MemoryManager.connections[connID] = new MemoryManager.connection(connID, srcOADS, dstOADS, srcColor, dstColor, port);
                MemoryManager.ConnIDSortedBySrcDst.get(srcOADS*k*k + dstOADS).add(connID);

                return true;
            }
        }
        return false;
    }
    public static void initFixed(){

    }
    public static void DeAllocate(int connID){
            assert(MemoryManager.connections[connID].connQueue.isEmpty());//no flow in the queue

            int srcOADS = MemoryManager.connections[connID].srcOADS;
            int dstOADS = MemoryManager.connections[connID].dstOADS;
            int srcColor = MemoryManager.connections[connID].srcColor;
            int dstColor = MemoryManager.connections[connID].dstColor;
            int port = MemoryManager.connections[connID].port;

            int srcPod = srcOADS / k;
            int srcOADSIndex = srcOADS % k;
            int dstPod = dstOADS / k;
            int dstOADSIndex = dstOADS % k;;

            int current_wavelength;
            long WaveToOADSPortBit = (0x1), OADSPortBit = (0x1);


            if (srcPod == dstPod){
                assert(srcColor == dstColor);

                if (srcColor == BLUE) current_wavelength = (srcOADS % k) * BAND + port;
                else current_wavelength = (((srcOADS % k) + 1) % k) * BAND + port;
                WaveToOADSPortBit = WaveToOADSPortBit << current_wavelength;
                OADSPortBit = (OADSPortBit << port) | (OADSPortBit << (port + BAND) | (OADSPortBit << (port + 2 * BAND)) | (OADSPortBit << (port + 3 * BAND)) | (OADSPortBit << (port + 4 * BAND)));

                MemoryManager.SourceSubnodeWave[srcOADS][srcColor] = MemoryManager.SourceSubnodeWave[srcOADS][srcColor] | OADSPortBit;
                MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] = MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] | OADSPortBit;

                MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] = true;
                MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port] = true;


            }
            else {
                //tier-2 connection
                assert(srcPod != dstPod);

                //not passing tier-2 horizontal links
                if ((((srcPod + 1) % k) == dstPod && (srcColor == BLUE) && (dstColor == RED)) || (((dstPod + 1) % k == srcPod) && (srcColor == RED) && (dstColor == BLUE))){
                    int  mutualWXCIndex;
                    if (srcColor == BLUE) mutualWXCIndex = (srcPod + 1) % k; else mutualWXCIndex = srcPod;
                    if (srcColor == BLUE) current_wavelength = srcOADSIndex*BAND + port; else current_wavelength = ((srcOADSIndex + 1) % k) * BAND + port;
                    WaveToOADSPortBit = WaveToOADSPortBit << current_wavelength;
                    OADSPortBit = (OADSPortBit << port) | (OADSPortBit << (port + BAND) | (OADSPortBit << (port + 2 * BAND)) | (OADSPortBit << (port + 3 * BAND)) | (OADSPortBit << (port + 4 * BAND)));

                    MemoryManager.ToTier1Combiner[mutualWXCIndex][dstColor] = MemoryManager.ToTier1Combiner[mutualWXCIndex][dstColor] | WaveToOADSPortBit;
                    MemoryManager.SourceSubnodeWave[srcOADS][srcColor] = MemoryManager.SourceSubnodeWave[srcOADS][srcColor] | OADSPortBit;
                    MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] = MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] | OADSPortBit;
                    MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] = true;
                    MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port] = true;


                }
                else{
                    int srcWXCIndex, dstWXCIndex;
                    if (srcColor == BLUE) srcWXCIndex = (srcPod + 1) % k; else srcWXCIndex = srcPod;
                    if (dstColor == BLUE) dstWXCIndex = (dstPod + 1) % k; else dstWXCIndex = dstPod;
                    int EorW = (((srcWXCIndex - dstWXCIndex + k) % k)>((dstWXCIndex - srcWXCIndex + k) % k)) ? 0 : 1;// 0 to East, 1 to West
                    if (srcColor == BLUE) current_wavelength = srcOADSIndex*BAND + port; else current_wavelength = ((srcOADSIndex + 1) % k) * BAND + port;
                    WaveToOADSPortBit = WaveToOADSPortBit << current_wavelength;
                    OADSPortBit = (OADSPortBit << port) | (OADSPortBit << (port + BAND) | (OADSPortBit << (port + 2 * BAND)) | (OADSPortBit << (port + 3 * BAND)) | (OADSPortBit << (port + 4 * BAND)));

                    MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] = MemoryManager.ToHorizonCombiner[srcWXCIndex][EorW] | WaveToOADSPortBit;
                    MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor] = MemoryManager.ToTier1Combiner[dstWXCIndex][dstColor] | WaveToOADSPortBit;
                    MemoryManager.SourceSubnodeWave[srcOADS][srcColor] = MemoryManager.SourceSubnodeWave[srcOADS][srcColor] | OADSPortBit;
                    MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] = MemoryManager.DestinationSubnodeWave[dstOADS][dstColor] | OADSPortBit;
                    MemoryManager.SourceSubnodePort[srcOADS][srcColor][port] = true;
                    MemoryManager.DestinationSubnodePort[dstOADS][dstColor][port] = true;


                }
            }
            MemoryManager.connections[connID] = null;
            MemoryManager.ConnIDSortedBySrcDst.get(srcOADS*k*k + dstOADS).remove(connID);
            Boolean ThereIsConn = false;
            for (int i = srcOADS * MAXPortNum; i<srcOADS*MAXPortNum + MAXPortNum; i++){
                if (MemoryManager.connections[i] != null && MemoryManager.connections[i].dstOADS == dstOADS) ThereIsConn = true;
            }
        }

    public static int convertToPort(long result){
        int port = 0;
        long temp = 1;
        //if (result == 0) cout << "error: result is zero" << endl;
        while ((result & (temp << port)) == 0) {
            port++;
        }
        return port % BAND;
    }


}
