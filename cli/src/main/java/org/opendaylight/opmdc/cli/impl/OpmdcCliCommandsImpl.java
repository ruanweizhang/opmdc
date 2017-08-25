/*
 * Copyright © 2016 opmdc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.opmdc.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.opmdc.cli.api.OpmdcCliCommands;

public class OpmdcCliCommandsImpl implements OpmdcCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(OpmdcCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public OpmdcCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("OpmdcCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}