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
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ThreadLocalRandom;

@Log4j2
public class StatsdMetricEmitter extends MetricEmitter {

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
        log.info("Setting up Statsd metric emitter to generate load...");
        ipAddress = InetAddress.getByName(param.getEndpoint().split(":", 2)[0]);
        portAddress = Integer.parseInt(param.getEndpoint().split(":", 2)[1]);
        socket = new DatagramSocket();
        log.info("Finished Setting up Statsd metric emitter.");
    }

    @Override
    public void nextDataPoint() {
        try {
            sendStatsd();
        } catch (IOException e) {
            log.error("Error occurred while sending the packet!");
            e.printStackTrace();
        }
    }

    public void sendStatsd() throws IOException {
        byte buf[];
        StringBuilder payload;
        payload = new StringBuilder();
        String attributes = "#mykey:myvalue,datapoint_id:";
        log.debug("Updating metrics...");
        for (int i = 0; i < this.param.getRate(); i++) {
            payload.append("statsdTestMetric").append(i).append(":")
                    .append(ThreadLocalRandom.current().nextInt(-100, 100))
                    .append("|g|").append(attributes).append(i).append("\n");
        }
        buf = payload.toString().getBytes();
        log.debug("Payload size: %d", buf.length);
        if (buf.length >= 64000) {
            throw new RuntimeException("Reduce the number of metrics/data-points. UDP packets have size limitation of 64k");
        }
        DatagramPacket packet = new DatagramPacket(buf, buf.length, ipAddress, portAddress);
        socket.send(packet);
    }
}
