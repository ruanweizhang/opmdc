/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.opmdc.impl;

//import org.opendaylight.openflowplugin.api.openflow.configuration;
import java.io.*;
//import com.csvreader.CsvReader;
//import com.csvreader.CsvWriter;

import java.lang.*;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;  
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector; 

import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder; 

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.PortNumberUni;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.flow.capable.port.StateBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId; 
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;


import org.opendaylight.yang.gen.v1.urn.opendaylight.direct.statistics.rev160511.GetFlowStatisticsOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.OpendaylightFlowTableStatisticsService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev150304.FlowCapableTransactionService;


import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTosActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwTtlActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpDstActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetTpSrcActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;

public class Monitor {
	private static final Logger LOG = LoggerFactory.getLogger(Monitor.class);
	private DataBroker dataBroker;

	public static int arrayScale = MemoryManager.switchAmount*MemoryManager.portAmount;
	private String switchName = "openflow:";//private MacAddress sw1mac = new MacAddress("08:9e:01:9a:8a:23");
	private double avgTableElapsedTime = 0, avgNodeConnectorElapsedTime = 0, avgfcncsdElapsedTime = 0;
	private int tableCount = 0, nodeConnectorCount = 0, fcncsdCount = 0;
	private int loopCount = 2;
	private double totalBW = 10^(10);

	CyclicBarrier cyclicBarrier;
	private int threadAmount = 4; //MemoryManager.switchAmount*MemoryManager.portAmount;
	private double threadTime = 0, threadAvgTime = 0, startThreadTime, endThreadTime;
	private int dontCount = 0;
	private List<Thread> threadList = new ArrayList<Thread>(threadAmount);
	public static int threadAmountTest[]={99, 71, 63, 55, 47, 39, 35, 31, 23, 19, 15, 11, 7, 4, 3, 2, 1}, threadLoopCount = 3000;
	private int realCount = threadLoopCount - dontCount;

	private static double[] threadTimeArray = new double[threadLoopCount]; // for writing csv file
	private static double[] seqTimeArray = new double[threadLoopCount];
	private static int tTAIndex = 0; // for writing csv file

	private class Summer implements Runnable{
		//int swNum, portNum;
		CyclicBarrier cyclicBarrier;
		int threadId, startPortRange, endPortRange;
		int threadLoopCount;
		
		public Summer(CyclicBarrier cyclicBarrier, int threadNum) { 
			this.cyclicBarrier = cyclicBarrier;
			this.threadId = threadNum;
			this.endPortRange = arrayScale / threadAmount*(int)(this.threadId);
			this.startPortRange = arrayScale / threadAmount*(int)(this.threadId-1)+1;
			if (this.endPortRange > arrayScale || (arrayScale - endPortRange <= (arrayScale % threadAmount))){
				endPortRange = arrayScale;
			}
			//System.out.println("threadId: "+threadId);
			//System.out.println("startPortRange " + startPortRange + ", endPortRange " + endPortRange);
			this.threadLoopCount = Monitor.threadLoopCount;
		}

		@Override
		public void run() {
			int swNum = 1, portNum = 0;
			while (true){ //while(threadLoopCount > 0){// 
				try {
					if (threadId == 1){
						portNum=1;
						readNodeConnectorFromNodeIdandPort(swNum, portNum); 
					}
					else if(threadId == 2){
						portNum=2;
						readNodeConnectorFromNodeIdandPort(swNum, portNum);
					}
					else if(threadId == 3){
						portNum=3;
						readNodeConnectorFromNodeIdandPort(swNum, portNum);
					}
					else if(threadId == 4){
						portNum=4;
						readNodeConnectorFromNodeIdandPort(swNum, portNum);
					}
					//System.out.println("ThreadId is " + threadId);
					/*for(int i = startPortRange; i <= endPortRange; i++) {
						swNum = i / MemoryManager.portAmount + 1;
						portNum = i % MemoryManager.portAmount;
						if (portNum == 0){
							portNum = 16;
							swNum -= 1;
						}
						//System.out.println("swNum " + swNum + ", portNum " + portNum);
						readNodeConnectorFromNodeIdandPort(swNum, portNum); 
					}*/
					//System.out.println("\n\n");
					if (cyclicBarrier.await() == 0) {
						//barrierComplete(cyclicBarrier);
						//System.out.println("Thread all done!");
					}
					//barrier.reset(); //This adds a race condition and throws a BrokenBarrierException
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				} catch (BrokenBarrierException ex) {
					ex.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				threadLoopCount--;
			}
		}
	} //end of Summer class

	public Monitor(DataBroker dataBroker) {
		LOG.info("Monitor Session Initiated");
		this.dataBroker = dataBroker;
		if (dataBroker == null){
			LOG.warn("Monitor dataBroker is null!!!");
			return;
		}
		
		try {
			Thread.sleep(10000); //1000 milliseconds is one second.
		} catch(InterruptedException ex) { //Add Interrupt to exit
			Thread.currentThread().interrupt();
		}

		new Thread(() -> {
			LOG.info("Thread For Keepmonitor Begin");
			keepmonitor();
		}).start();
			
    } // end of Monitor constructor

    /*public void barrierComplete(CyclicBarrier cb) {
        System.out.println("Collating task");

        if (count == threadAmount) {
            System.out.println("Exit from system");
            return;
        }
        count++;

        startThreadTime = System.nanoTime();
        for (int i = 1; i <= MemoryManager.switchAmount; i++) {
			final int swNum = i;
			for (int j = 1; j <= MemoryManager.portAmount; j++) {
				final int portNum = j;
				Thread threadB = new Thread(new Summer(cyclicBarrier, i, j));
				threadB.start();//threadList.set( (swNum-1)*MemoryManager.portAmount+(portNum-1), threadB); //threadList.add(threadB);
			}
		}
		for (Thread threadB : threadList) {
			threadB.start();
		}
		endThreadTime = System.nanoTime();
		threadTime  = (endThreadTime - startThreadTime);
    }*/


    private void sequentialSwitchTraffic(DataBroker dataBroker, String switchName, int switchAmount, int portAmount){//List<Thread> threadList, String switchName, int switchAmount, int portAmount){
//Sequential readData   	
    	long startTime, endTime;
		/*startTime = System.nanoTime();
		for (int swNum = 1; swNum <= switchAmount; swNum++){
			for (int portNum = 1; portNum <= portAmount; portNum++){
				readNodeConnectorFromNodeIdandPort(swNum, portNum);
			}
		}
		endTime = System.nanoTime();
		sequentialTime = (endTime - startTime);*/
		int swNum = 1; 
		double avgSeqTime = 0, sequentialTime;
		for (int i = 0; i < Monitor.threadLoopCount; i++){

			startTime = System.nanoTime();
			readNodeConnectorFromNodeIdandPort(swNum, 1);
			readNodeConnectorFromNodeIdandPort(swNum, 2);
			readNodeConnectorFromNodeIdandPort(swNum, 3);
			readNodeConnectorFromNodeIdandPort(swNum, 4);
			endTime = System.nanoTime();

			sequentialTime = (endTime - startTime);
			avgSeqTime += sequentialTime;
			seqTimeArray[i] = sequentialTime;
		}

		//LOG.info("Monitor Sequential readData Elapsed Time(nano secs): {}", (endTime - startTime));
		System.out.println("Avg Sequential readData Elapsed Time(nano secs): " + avgSeqTime / (double)(threadLoopCount));
	} // end of sequentialSwitchTraffic (DataBroker dataBroker, String switchName, int switchAmount, int portAmount) function
    
    public void readNodeConnectorFromNodeIdandPort(int swNumber, int portNumber){
    	NodeId nodeId = new NodeId(switchName+swNumber);
    	//System.out.println(""+switchName+swNumber+":"+portNumber);
		NodeConnectorId ncId = InventoryUtils.getNodeConnectorId(nodeId, portNumber);
		InstanceIdentifier<Node> nodeIId = createNodeIId(nodeId);
		//InstanceIdentifier<NodeConnector> nodeConnectorIId = nodeIId.child(NodeConnector.class, new NodeConnectorKey(nodeConnector.getId()));
		InstanceIdentifier<NodeConnector> nodeConnectorIId = nodeIId.child(NodeConnector.class, new NodeConnectorKey(ncId));
		
		long startNodeConnectorTime, endNodeConnectorTime;
/////test
		/*startNodeConnectorTime = System.nanoTime();
		NodeConnector ncTest = GenericTransactionUtils.readData(this.dataBroker, LogicalDatastoreType.CONFIGURATION, nodeConnectorIId);
		endNodeConnectorTime = System.nanoTime();
		LOG.info("Monitor NodeConnector CONFIGURATION readData Elapsed Time(nano secs): {}", (endNodeConnectorTime - startNodeConnectorTime));
*//////test

		startNodeConnectorTime = System.nanoTime();
		NodeConnector nodeConnector = GenericTransactionUtils.readData(this.dataBroker, LogicalDatastoreType.OPERATIONAL, nodeConnectorIId);
		endNodeConnectorTime = System.nanoTime();
		//LOG.info("Monitor NodeConnector OPERATIONAL readData Elapsed Time(nano secs): {}", (endNodeConnectorTime - startNodeConnectorTime));
		long totalT = endNodeConnectorTime - startNodeConnectorTime;
		
		if (nodeConnector != null) {
			this.avgNodeConnectorElapsedTime += (endNodeConnectorTime - startNodeConnectorTime);
			this.nodeConnectorCount++;
			//System.out.println("NodeId and Port Monitor1Impl NC info.: " + nodeConnector);
			//LOG.info("Monitor NodeID {} NodeConnector SUCCESS!!!", nodeId);
		} //End of if (table != null) statement
		else{
			//LOG.info("Monitor NodeID {} nciid {} NodeConnctor is null!!!", nodeId, ncId);
			return ;
		}
//The newest try
		//NodeConnector nc = nodeConnector;
		FlowCapableNodeConnectorStatisticsData fnc = nodeConnector.getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
        if (fnc != null){
        	//System.out.println("fnc SUCCESS!!!");
	        InstanceIdentifier<FlowCapableNodeConnectorStatisticsData> tIID = InstanceIdentifier
	                .create(Nodes.class)
	                .child(Node.class, new NodeKey(new NodeId(nodeId))) // origin:node.getKey()
	                .child(NodeConnector.class, nodeConnector.getKey())
	                .augmentation(FlowCapableNodeConnectorStatisticsData.class);

	        long startfcncsdTime = System.nanoTime();
	        FlowCapableNodeConnectorStatisticsData fcncsd = GenericTransactionUtils.readData(this.dataBroker, LogicalDatastoreType.OPERATIONAL, tIID);
	        long endfcncsdTime = System.nanoTime();
	        //LOG.info("Monitor fcncsd readData Elapsed Time(nano secs): {}", (endfcncsdTime - startfcncsdTime));
	        totalT += (endfcncsdTime - startfcncsdTime);
	        //LOG.info("Monitor totalT:"+totalT);
	        
	        if (fcncsd!=null){
	        	this.avgfcncsdElapsedTime += (endfcncsdTime - startfcncsdTime);
				this.fcncsdCount++;
	        	//System.out.println("fcncsd SUCCESS!!!");
	        	//System.out.println(fcncsd);
	        	//LOG.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!Monitor getByte().getTransmitted()!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	        	//LOG.info("Monitor Accumulated Transmitted Bytes"+fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getTransmitted());
	        	//System.out.println("Monitor tx:"+fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getTransmitted());
	        	//LOG.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!Monitor getDuration().getSecond().getValue()!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	        	//LOG.info("Monitor "+fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getSecond().getValue());
	        	//System.out.println("Monitor Duration secs:"+fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getSecond().getValue());
	        	//LOG.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!Monitor getDuration().getNanosecond().getValue()!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	        	//LOG.info("Monitor "+fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getNanosecond().getValue());
	        	//System.out.println("Monitor Duration nano:"+fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getNanosecond().getValue());

				int rowIndex = (swNumber-1)*MemoryManager.portAmount + (portNumber-1);
				//LOG.info("swNum:"+swNumber+", portNum:"+portNumber+", rowIndex:"+rowIndex);
				if (MemoryManager.portStatistics_preTimeslot[rowIndex][0]==0){
					MemoryManager.portStatistics_preTimeslot[rowIndex][0] = fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getTransmitted().doubleValue();
					MemoryManager.portStatistics_preTimeslot[rowIndex][1] = fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getReceived().doubleValue();
					MemoryManager.portStatistics_preTimeslot[rowIndex][2] = (double)(fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getSecond().getValue() + 
																fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getNanosecond().getValue()*Math.pow(10, -9));
	        		return ;
				}

				MemoryManager.portStatistics[rowIndex][0] = fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getTransmitted().doubleValue() - MemoryManager.portStatistics_preTimeslot[rowIndex][0];
				MemoryManager.portStatistics[rowIndex][1] = fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getReceived().doubleValue() - MemoryManager.portStatistics_preTimeslot[rowIndex][1];
				MemoryManager.portStatistics[rowIndex][2] = (double)(fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getSecond().getValue() + 
															fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getNanosecond().getValue()*Math.pow(10, -9)) - MemoryManager.portStatistics_preTimeslot[rowIndex][2];
	        	
	        	MemoryManager.portStatistics_preTimeslot[rowIndex][0] = fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getTransmitted().doubleValue();
	        	MemoryManager.portStatistics_preTimeslot[rowIndex][1] = fcncsd.getFlowCapableNodeConnectorStatistics().getBytes().getReceived().doubleValue();
				MemoryManager.portStatistics_preTimeslot[rowIndex][2] = (double)(fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getSecond().getValue() + 
																		fcncsd.getFlowCapableNodeConnectorStatistics().getDuration().getNanosecond().getValue()*Math.pow(10, -9));
				
				MemoryManager.portSpeed[rowIndex][0] = MemoryManager.portStatistics[rowIndex][0]/MemoryManager.portStatistics[rowIndex][2];
				MemoryManager.portSpeed[rowIndex][1] = MemoryManager.portStatistics[rowIndex][1]/MemoryManager.portStatistics[rowIndex][2];
				LOG.info("Monitor port:" + (rowIndex+1) + ", Tx_byte:" + MemoryManager.portStatistics[rowIndex][0] + ", Rx_byte:" + MemoryManager.portStatistics[rowIndex][1]+", Duration:"+MemoryManager.portStatistics[rowIndex][2]);
				//System.out.println("	Monitor port:" + (rowIndex+1) + ", Tx_bps (Mbps):" + MemoryManager.portSpeed[rowIndex][0]*8/Math.pow(10, 6) + ", Rx_bps (Mbps):" + MemoryManager.portSpeed[rowIndex][1]*8/Math.pow(10, 6));
	        	updatePortUtilization(rowIndex, MemoryManager.portStatistics[rowIndex][0], MemoryManager.portStatistics[rowIndex][2]);//tx_bytes, duration
	        }
	        else{
	        	//LOG.info("FlowCapableNodeConnectorStatisticsData fcncsd from readData is NULL!!!");
	        	//System.out.println("FlowCapableNodeConnectorStatisticsData fcncsd from readData is NULL!!!");
	        	return ;
	        }
		}
		else{
			//LOG.info("FlowCapableNodeConnectorStatisticsData fnc from NC Augmentation is NULL!!!QQQQQQQQQQQQQ");
			return ;
		}

        /*LOG.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!QueueStatistics Trial!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
	    if (fcnc != null) {
	    	LOG.info("fcnc SUCCESS!!!");
	    	LOG.info(""+fcnc);
			List<Queue> queues = fcnc.getQueue();
            if (queues != null) {
                for (Queue q : queues) {
                	LOG.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!Queue For Loop!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> tIID2 = InstanceIdentifier
                            .create(Nodes.class)
                            .child(Node.class, new NodeKey(new NodeId(nodeId)))
                            .child(NodeConnector.class,nodeConnector.getKey())
                            .augmentation(FlowCapableNodeConnector.class)
                            .child(Queue.class, q.getKey())
                            .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
                    FlowCapableNodeConnectorQueueStatisticsData fcncQsd = GenericTransactionUtils.readData(this.dataBroker, LogicalDatastoreType.OPERATIONAL, tIID2);
                	if (fcncQsd != null){
                		LOG.info("fcncQsd SUCCESS!");
                		LOG.info(""+fcncQsd);
                		FlowCapableNodeConnectorQueueStatistics gs = fcncQsd.getFlowCapableNodeConnectorQueueStatistics();
                		LOG.info("gs.getTransmittedBytes() is");
                		LOG.info(""+gs.getTransmittedBytes());
                	}
                	else{
                		LOG.info("fcncQsd is NULL!!!QQQQQQQQQQQQQ");
                	}
                }
            }
            else{
            	LOG.info("Queue is NULL!!!QQQQQQQQQQQQQ");
            }
    	}
    	else{
    		LOG.info("fcnc is NULL!!!QQQQQQQQQQQQQ");
		}*/
		return ;
	} // end of readNodeConnectorFromNodeIdandPort(NodeId nodeId, int portNumber) function
    public InstanceIdentifier<FlowCapableNodeConnector> constructFlowCapableNodeConnectorIId (InstanceIdentifier<NodeConnector> ncIId) {
		InstanceIdentifier<FlowCapableNodeConnector> fcncIId = ncIId.augmentation(FlowCapableNodeConnector.class);
		return fcncIId;
	}
	public InstanceIdentifier<Node> createNodeIId(NodeId nodeId) {
		return InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(new NodeId(nodeId))).build();
	}
	private void updatePortUtilization(int rowIndex, double tx_bytes, double duration){
		for(int i = 0; i < arrayScale; i++){
			if (MemoryManager.connectionTable[rowIndex][i] != 0){
				MemoryManager.portUtilization[rowIndex][i] = tx_bytes/totalBW;
			}
		}
	}

    private void displayElapsedTime(){
    	System.out.println("" + threadTime);
    	//System.out.println("    Thread readData Elapsed Time(nano secs): " + threadTime);
    	if (this.tableCount != 0){
			LOG.info("Monitor Table AVG Elapsed Time(nano secs): {}", this.avgTableElapsedTime/(double)this.tableCount);
		}
		if (this.nodeConnectorCount != 0){
			LOG.info("Monitor NodeConnector AVG Elapsed Time(nano secs): {}", this.avgNodeConnectorElapsedTime/(double)this.nodeConnectorCount);
		}
		if (this.fcncsdCount != 0){
			LOG.info("Monitor FlowCapableNodeConnectorStatisticsData AVG Elapsed Time(nano secs): {}", this.avgfcncsdElapsedTime/(double)this.fcncsdCount);
		}
    } 
	
    private void keepmonitor(){
		for(int i = 0; i < arrayScale; i++){
			MemoryManager.portStatistics_preTimeslot[i][0] = 0;
			MemoryManager.portStatistics_preTimeslot[i][1] = 0;
			MemoryManager.portStatistics_preTimeslot[i][2] = 0;
		}
		//test seq
		/*sequentialSwitchTraffic(dataBroker, switchName, MemoryManager.switchAmount, MemoryManager.portAmount);
		try {
			writeFile(seqTimeArray, "/Users/imac/Desktop/Sesame_TM/Seq0810_"+System.nanoTime()+".csv");
		} catch(IOException ioe){
			LOG.warn("IOException:"+ioe);
		}*/
		//end of test seq

		Runnable merger = new Runnable() {
			public void run() {
				endThreadTime = System.nanoTime();
				//System.out.println("LALALA I'm merger!");
				threadTime  = (endThreadTime - startThreadTime);
				if (dontCount!=0){
					//System.out.println("" + threadTime + " Don't count Loop!" + dontCount);
					dontCount--;
				}
				else{
					threadAvgTime += threadTime; 
					//System.out.println("merger"+threadTime);
					//displayElapsedTime();

					//threadTimeArray[tTAIndex] = threadTime; tTAIndex++; //for testing csv
				}

				try {
					Thread.sleep(MemoryManager.monitorPeriod); //1000 milliseconds is one second.
				} catch(InterruptedException ex) { //Add Interrupt to exit
					Thread.currentThread().interrupt();
				}
				startThreadTime = System.nanoTime();
			}
		};
		/*
         * public CyclicBarrier(int parties,Runnable barrierAction)
         * Creates a new CyclicBarrier that will trip when the given number
         * of parties (threads) are waiting upon it, and which will execute 
         * the merger task when the barrier is tripped, performed 
         * by the last thread entering the barrier.
         */
		cyclicBarrier = new CyclicBarrier(threadAmount, merger);

		for (int i = 0; i < threadAmount; i++){
			Thread threadB = new Thread(new Summer(cyclicBarrier, i+1));
			threadList.add(threadB);
		}
		System.out.println("threaList size:"+threadList.size());

		startThreadTime = System.nanoTime();
		for (Thread threadB : threadList) {
			threadB.start();
		}
		for (Thread threadB : threadList) {
			try {
				threadB.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("all loop time is"+(double)((threadLoopCount-1)*threadAmount));
		System.out.println("Avg Thread readData Elapsed Time(nano secs): " + threadAvgTime/(double)(realCount));//    *threadAmount));
		//displayThreadTimeArray();
		/*try {
			writeFile(threadTimeArray, "/Users/imac/Desktop/Sesame_TM/Thread0810_"+System.nanoTime()+".csv");
		} catch(IOException ioe){
			LOG.warn("IOException:"+ioe);
		}*/
		LOG.info("Avg Thread readData Elapsed Time(nano secs): " + threadAvgTime/(double)(realCount));
//test
		/*for(int ta : threadAmountTest){
			threadAmount = ta;
			cyclicBarrier = new CyclicBarrier(threadAmount, merger);
			threadList = new ArrayList<Thread>(threadAmount);
			for (int i = 0; i < threadAmount; i++){
				Thread threadB = new Thread(new Summer(cyclicBarrier, i+1));
				threadList.add(threadB);
			}
			System.out.println("threaList size:"+threadList.size());

			startThreadTime = System.nanoTime();
			for (Thread threadB : threadList) {
				threadB.start();
			}
			for (Thread threadB : threadList) {
				try {
					threadB.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}*/
//end of test	
    } // end of keepmonitor() function
    
	public static void writeFile(double array[], String fileName) throws IOException {//for writing csv
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		//CSVWriter bw = new CSVWriter(new FileWriter(fileName));
		if (bw!=null){
			LOG.info("Succes to open file!");
		}
		else{
			LOG.info("Failed to open file!");
			return ;
		}
		bw.write("Trial,4port_MonitoringTime\n");
		try{
			/*for(double d:data){
				bw.write(d);
				bw.newLine();        
			}*/
			for(int i = 0; i < Monitor.threadLoopCount; i++){
				bw.write(String.valueOf(i+1));
				bw.write(',');
				bw.write(String.valueOf(array[i]));
				bw.write('\n');
			}
			bw.flush();
		} catch(IOException ioe){
			LOG.warn("IOException:"+ioe);
		} finally{
			bw.close();      
		}    
	}//end of writing csv function
    
    private void displayThreadTimeArray(){
    	System.out.println("displayThreadTimeArray");
    	for(int i = 0; i < tTAIndex; i++){
    		System.out.println(""+threadTimeArray[i]+"\n");
    	}
    	/*for(double d:threadTimeArray){
    		System.out.println(d);
    	}*/
    }

    private void readTablefromSwitch(String switchName){

		NodeId nodeId = new NodeId(switchName);// + input.getSwitchId().toString());
		TableKey tableKey = new TableKey((short)0); // SDN SW using table[0] by default
		InstanceIdentifier<Table> tableIID = InstanceIdentifier.builder(Nodes.class)
				.child(Node.class, new NodeKey(nodeId))
				.augmentation(FlowCapableNode.class)
				.child(Table.class, tableKey).build();
		
		long startTableTime = System.nanoTime();
		Table table = GenericTransactionUtils.readData(this.dataBroker, LogicalDatastoreType.OPERATIONAL, tableIID);
		//table = GenericTransactionUtils.readData(this.dataBroker, LogicalDatastoreType.CONFIGURATION, tableIID);
		long endTableTime = System.nanoTime();
		// LOG.info("Monitor1Impl readData Elapsed Time(nano secs): {}", (endTime - startTime));

		if (table != null) {
			this.avgTableElapsedTime += (endTableTime - startTableTime);
			this.tableCount++;
			System.out.println("Monitor1Impl Table OPERATIONAL SUCCESS!!!");
			System.out.println("Monitor1Impl table info.: " + table);
			LOG.info("Monitor1Impl NodeID {} Table SUCCESS!!!", nodeId);
			//readFlowfromTable(table, tableKey, nodeId);
		} //End of if (table != null) statement
		else{
			System.out.println("Monitor1Impl Table OPERATIONAL is NULL!!!");
			//LOG.info("Monitor1Impl NodeID {} Table is null!!!", nodeId);
		}
	}

	private void readFlowfromTable(Table table, TableKey tableKey, NodeId nodeId){
		if (table.getFlow() != null) {
			int flowCount = 0;
			List<Flow> flows = table.getFlow();
			//GetAllFlowRulesFromASwitchOutputBuilder flowRulesOutputBuilder = new GetAllFlowRulesFromASwitchOutputBuilder();
			//List<FlowRules> flowRuleList = Lists.newArrayList();
			
			for (Iterator<Flow> iterator = flows.iterator(); iterator.hasNext();) {
				flowCount++;
				FlowKey flowKey = iterator.next().getKey();
		        InstanceIdentifier<Flow> flowIID = InstanceIdentifier.create(Nodes.class)
		        		.child(Node.class, new NodeKey(nodeId))
		        		.augmentation(FlowCapableNode.class)
		        		.child(Table.class, tableKey)
		        		.child(Flow.class, flowKey);
		        Flow flow = GenericTransactionUtils.readData(dataBroker, LogicalDatastoreType.OPERATIONAL, flowIID);
		        
		        if (flow==null){
		        	System.out.println("Monitor1Impl Flow OPERATIONAL is NULL!!!");
					flow = GenericTransactionUtils.readData(dataBroker, LogicalDatastoreType.CONFIGURATION, flowIID);
				}
				else{
					System.out.println("Monitor1Impl Table OPERATIONAL Not null!!!");
				}


		        if (flow != null){
		        	//FlowRulesBuilder flowBuilder = new FlowRulesBuilder();
		        	////---------Extract Flow properties information
		        	if (flow.getId() != null){
		        		//LOG.info("Monitor1Impl flowID is {}!!!", flow.getId().getValue());
		        	}
		        	else {
		        		return;
		        	}
		        	//flow.getPriority();
		        	//flow.getIdleTimeout();
		        	//flow.getHardTimeout();
		        	if (flow.getMatch().getInPort() != null){
		        		//flowBuilder.setInPortId((long)Integer.parseInt(flow.getMatch().getInPort().getValue().split(":")[2]));
		        		LOG.info("Monitor1Impl Inport is {}!!!", flow.getMatch().getInPort().getValue());
		        	}
		        	///---------------Extract IP and MAC address information
		        	if (flow.getMatch().getEthernetMatch().getEthernetSource() != null){
		        		flow.getMatch().getEthernetMatch().getEthernetSource().getAddress().getValue();
		        	}
		        	if (flow.getMatch().getEthernetMatch().getEthernetDestination() != null){
		        		flow.getMatch().getEthernetMatch().getEthernetDestination().getAddress().getValue();
		        	}
		        	if (flow.getMatch().getLayer3Match() != null){
		        		Ipv4Match ipv4Match = (Ipv4Match) flow.getMatch().getLayer3Match();
		        		if (ipv4Match.getIpv4Source() != null) {
		        			ipv4Match.getIpv4Source().getValue();
		        		}
		        		if (ipv4Match.getIpv4Destination() != null) {
		        			ipv4Match.getIpv4Destination().getValue();
		        		}
		        	}
		        	//----------Extract Port Information
		        	if (flow.getMatch().getLayer4Match() != null) {
		        		TcpMatch tcpMatch = (TcpMatch) flow.getMatch().getLayer4Match();
		        		if (tcpMatch.getTcpDestinationPort() != null) {
		        			tcpMatch.getTcpDestinationPort().getValue();
		        		}
		        		if (tcpMatch.getTcpSourcePort() != null) {
		        			tcpMatch.getTcpSourcePort().getValue();
		        		}
		        	}
		        	///---------------Extract Network Protocol information
		        	if (flow.getMatch().getIpMatch() != null && flow.getMatch().getIpMatch().getIpProto() != null){
		        		if (flow.getMatch().getIpMatch().getIpProto().getIntValue() == 1){
		        			LOG.info("Monitor1Impl TrafficType is ICMP!!!");
		        		}
		        		else if (flow.getMatch().getIpMatch().getIpProto().getIntValue() == 6){
		        			LOG.info("Monitor1Impl TrafficType is TCP!!!");
		        			if (flow.getMatch().getEthernetMatch().getEthernetType() != null) {
				        		if (flow.getMatch().getEthernetMatch().getEthernetType().getType().getValue() == 0x0800){
				        			if (flow.getMatch().getLayer4Match() != null) {
				        				TcpMatch tcpMatch = (TcpMatch) flow.getMatch().getLayer4Match();
				        				if (tcpMatch.getTcpDestinationPort() != null) {
				        					if (tcpMatch.getTcpDestinationPort().getValue() == 80) {
				        						LOG.info("Monitor1Impl TrafficType is HTTP!!!");
				        					}
				        					else if (tcpMatch.getTcpDestinationPort().getValue() == 443) {
				        						LOG.info("Monitor1Impl TrafficType is HTTPS!!!");
				        					}
				        				}
				        			}
				        		}
				        	}
		        		}
		        		else if (flow.getMatch().getIpMatch().getIpProto().getIntValue() == 17){
		        			LOG.info("Monitor1Impl TrafficType is UDP!!!");
		        			if (flow.getMatch().getEthernetMatch().getEthernetType() != null) {
				        		if (flow.getMatch().getEthernetMatch().getEthernetType().getType().getValue() == 0x0800){
				        			if (flow.getMatch().getLayer4Match() != null) {
				        				TcpMatch tcpMatch = (TcpMatch) flow.getMatch().getLayer4Match();
				        				if (tcpMatch.getTcpDestinationPort() != null) {
				        					if (tcpMatch.getTcpDestinationPort().getValue() == 53) {
				        						LOG.info("Monitor1Impl TrafficType is DNS!!!");
				        					}
				        					else if (tcpMatch.getTcpDestinationPort().getValue() == 67) {
				        						LOG.info("Monitor1Impl TrafficType is DHCP!!!");
				        					}
				        				}
				        			}
				        		}
				        	}
		        		}
		        		
		        	} //End of statement if (flow.getMatch().getIpMatch() != null && flow.getMatch().getIpMatch().getIpProto() != null)
		        	else{
						//LOG.info("Monitor1Impl No Network Protocol Info.!!!");
					}
		        	//---------------Extract ARP Traffic Info ---------------
		        	if (flow.getMatch().getEthernetMatch() != null && flow.getMatch().getEthernetMatch().getEthernetType() != null) {
		        		if (flow.getMatch().getEthernetMatch().getEthernetType().getType().getValue() == 0x0806){
		        			LOG.info("Monitor1Impl TrafficType is ARP!!!");
		        		}
		        	}
		        	else{
		        		//LOG.info("Monitor1Impl No ARP Traffic Info.!!!");
		        	}
		        	//----------------------------Extract Action Information ---------------------
		        	Instructions instructions = flow.getInstructions();
		        	for (Instruction instruction : instructions.getInstruction()){

		        		//System.out.println("Monitor1Impl getInstruction: " + instruction);

		        		ApplyActionsCase applyActionCase = (ApplyActionsCase) instruction.getInstruction();
		        		ApplyActions applyAction = applyActionCase.getApplyActions();
		        		for (Action action : applyAction.getAction()) {

		        			//System.out.println("Monitor1Impl getAction: " + action);

		        			if (action.getAction() instanceof OutputActionCase) {
		        				OutputActionCase outputCase = (OutputActionCase) action.getAction();
		        				String outputPort = outputCase.getOutputAction().getOutputNodeConnector().getValue().split(":")[2];
		        				LOG.info("Monitor1Impl Outport is {}!!!", outputPort);	
		        				//flowBuilder.setActionOutputPort(outputPort);
		        			
		        			} else if (action.getAction() instanceof DropActionCase){
		        				//flowBuilder.setActionOutputPort("0");
		        			
		        			} else if (action.getAction() instanceof SetNwSrcActionCase) {
		        				SetNwSrcActionCase setNwSrcCase = (SetNwSrcActionCase) action.getAction();
		        				Ipv4 ipv4Address = (Ipv4) setNwSrcCase.getSetNwSrcAction().getAddress();
		        				LOG.info("Monitor1Impl IPv4 Src address is {}", ipv4Address.getIpv4Address().getValue());
		        				//flowBuilder.setActionSetSourceIpv4Address(ipv4Address.getIpv4Address().getValue());
		        				
		        			} else if (action.getAction() instanceof SetNwDstActionCase) {
		        				SetNwDstActionCase setNwDstCase = (SetNwDstActionCase) action.getAction();
		        				Ipv4 ipv4Address = (Ipv4) setNwDstCase.getSetNwDstAction().getAddress();
		        				LOG.info("Monitor1Impl IPv4 Dst address is {}", ipv4Address.getIpv4Address().getValue());
		        				//flowBuilder.setActionSetDstIpv4Address(ipv4Address.getIpv4Address().getValue());
		        				
		        			} else if (action.getAction() instanceof SetTpSrcActionCase) {
		        				SetTpSrcActionCase setTpCase = (SetTpSrcActionCase) action.getAction();
		        				//flowBuilder.setActionSetTcpSrcPort(setTpCase.getSetTpSrcAction().getPort().getValue());
		        				
		        			} else if (action.getAction() instanceof SetTpDstActionCase) {
		        				SetTpDstActionCase setTpCase = (SetTpDstActionCase) action.getAction();
		        				//flowBuilder.setActionSetTcpDstPort(setTpCase.getSetTpDstAction().getPort().getValue());
		        				
		        			} else if (action.getAction() instanceof SetNwTosActionCase) {
		        				SetNwTosActionCase setNwTosCase = (SetNwTosActionCase) action.getAction();
		        				//flowBuilder.setActionSetIpv4Tos(setNwTosCase.getSetNwTosAction().getTos());
		        				
		        			} else if (action.getAction() instanceof SetNwTtlActionCase) {
		        				SetNwTtlActionCase setNwTtlCase = (SetNwTtlActionCase) action.getAction();
		        				//flowBuilder.setActionSetIpv4Ttl(setNwTtlCase.getSetNwTtlAction().getNwTtl());
		        			}
		        		}
		        	}
		        	//flowRuleList.add(flowBuilder.build());
		        }/////////////////////////////////////////////////////--------------------End of IF Flow != NULL
			} //...................End of For loop Iterating through Flows
			// ------------ Create output object of the RPC
			//flowRulesOutputBuilder.setFlowRules(flowRuleList);
			//return RpcResultBuilder.success(flowRulesOutputBuilder.build()).buildFuture();
		} // End of IF No Flows IN Table 0
		else{
			LOG.info("Monitor1Impl NodeID {} Table[0] has NO FLOW!!!", nodeId);
		} // End of the if (table.getFlow() != null) statement
	} // End of private void getFlowfromTable(Table table)
}
