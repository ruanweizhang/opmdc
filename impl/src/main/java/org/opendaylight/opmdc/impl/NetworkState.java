/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.opmdc.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;



public class NetworkState{
	private static final Logger LOG = LoggerFactory.getLogger(NetworkState.class);

	public NetworkState() {
        LOG.info("NetworkState Session Initiated");
    }

    public int CanEstablishNewConn(int srcOADS, int dstOADS){
    	return 0;
    }
    public Boolean Estab_intra_pod_Conn(int srcOADS, int dstOADS, int color, int port){
    	return true;
    }
    public Boolean Estab_inter_pod_Conn_Not_passing_tier2(int srcOADS, int dstOADS, int srcColor, int dstColor, int port){
    	return true;
    }
    public Boolean Estab_inter_pod_Conn(int srcOADS, int srcWXCIndex, int dstOADS, int dstWXCIndex, int srcColor, int dstColor, int port, int EorW){
    	return true;
    }
    public void initFixed(){

    }
    public void DeAllocate(int connID){


    }
    public int convertToPort(long result){
    	int port = 0;
		long temp = 1;
		//if (result == 0) cout << "error: result is zero" << endl;
		while ((result & (temp << port)) == 0) {
			port++;
		}
		return port % MemoryManager.BAND;
    }

}