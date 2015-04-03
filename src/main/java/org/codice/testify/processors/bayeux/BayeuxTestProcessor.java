/*
 * Copyright 2015 Codice Foundation
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.codice.testify.processors.bayeux;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codice.testify.bayeux.actions.support.BayeuxClientFactory;
import org.codice.testify.objects.AllObjects;
import org.codice.testify.objects.TestifyLogger;
import org.codice.testify.objects.Request;
import org.codice.testify.objects.Response;
import org.codice.testify.processors.TestProcessor;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import sun.org.mozilla.javascript.internal.json.JsonParser;

import java.io.IOException;
import java.util.HashMap;

/**
 * The BayeuxTestProcessor class is a Testify Test Processor service for interacting with the bayeux protocol
 * @author Michael O'Connor
 * @see org.codice.testify.processors.TestProcessor
 */
public class BayeuxTestProcessor implements BundleActivator, TestProcessor{

    @Override
    public Response executeTest(Request request) {
        TestifyLogger.debug("Running " + this.getClass().getSimpleName(), this.getClass().getSimpleName());

        // Create Bayeux Client Factory
        final BayeuxClientFactory bayeuxClientFactory = new BayeuxClientFactory();

        // Isolate the request body
        String requestBody = request.getTestBlock().substring(request.getTestBlock().indexOf("<body>") + 6, request.getTestBlock().indexOf("</body>")).trim();

        // Isolate the request channel
        String requestChannel = request.getTestBlock().substring(request.getTestBlock().indexOf("<channel>") + 9, request.getTestBlock().indexOf("</channel>")).trim();

        // Check that request body and channel are not null
        if (requestBody.length() > 0 && requestChannel.length() > 0)
        {
            TestifyLogger.debug("Publishing to channel: " + requestChannel + " with message: " + requestBody, this.getClass().getSimpleName());


            // Convert request to HashMap
            com.fasterxml.jackson.core.JsonParser jsonParser = null;
            HashMap<String, Object> jsonRequest
            try {
                jsonParser = new JsonFactory().createJsonParser(requestBody);
            } catch (IOException e) { TestifyLogger.error("Error creating json parser " + e, this.getClass().getSimpleName()); }
            try {
                jsonRequest = new ObjectMapper().readValue(jsonParser, HashMap.class);
            } catch (IOException e) { TestifyLogger.error("Error creating json request " + e, this.getClass().getSimpleName()); }

            // Create Bayeux Client
            BayeuxClient client = null;
            try {
                client = bayeuxClientFactory.spawnClient(request.getEndpoint());
            } catch (Exception e) { TestifyLogger.error("Error Creating Bayeux Client ", this.getClass().getSimpleName()); }

            // Publish to channel
            client.getChannel(requestChannel).publish(jsonRequest, new ClientSessionChannel.MessageListener() {
                @Override
                public void onMessage(ClientSessionChannel csc, Message message) {
                    if (!message.isSuccessful()) {
                        String error = (String) message.get("error");
                        if (error != null) {
                            TestifyLogger.error("Published was unsuccessful: " + message, this.getClass().getSimpleName());
                        }
                    } else {
                        TestifyLogger.debug("Received publish response: " + message.getJSON(), this.getClass().getSimpleName());
                    }
                }
            });
            // TODO: determine best method to acquire the response from the client
            //  might be better to have this processor piggyback on the session created by the StartBayeuxClient Action
        } else { TestifyLogger.error("Request Body and Request Channel must be greater than zero characters", this.getClass().getSimpleName()); }
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }
}
