/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazon.opentelemetry.load.generator.emitter;

import com.amazon.opentelemetry.load.generator.model.Parameter;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class StatsdMetricEmitter extends MetricEmitter{

    InetAddress ipAddress;
    int portAddress;
    private DatagramSocket socket;

    public StatsdMetricEmitter(Parameter param) {
        super();
        this.param = param;
    }

    @Override
    public void emitDataLoad() throws Exception {
        this.setupProvider();
        this.start(() -> nextDataPoint());
    }

    @Override
    public void setupProvider() throws Exception {
        ipAddress = InetAddress.getByName(param.getEndpoint().split(":",2)[0]);
        portAddress = Integer.parseInt(param.getEndpoint().split(":",2)[1]);
        socket = new DatagramSocket();
    }

    @Override
    public void nextDataPoint() {
        try {
            sendStatsd();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendStatsd() throws IOException {
        byte buf[];
        String payload = "statsdTestMetric1:1|g|#mykey:myvalue,mykey2:myvalue2";
        buf = payload.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, ipAddress, portAddress);
        socket.send(packet);
    }
}
