/*
 * Copyright (C) 2014 RetailMeNot, Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package com.rmn.qa.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.grid.internal.Registry;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.io.ByteStreams;
import com.rmn.qa.AutomationConstants;
import com.rmn.qa.servlet.BmpProxyRegistry.ProxyHolder;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.proxy.CaptureType;
import net.lightbody.bmp.proxy.auth.AuthType;

/**
 * Servlet to start instance of BrowserMobProxy for authentication
 */
public class BmpServlet extends RegistryBasedServlet {

    private static final long serialVersionUID = 8484071790930378855L;
    private static final Logger log = LoggerFactory.getLogger(BmpServlet.class);

    private static String coreVersion;
    private static String coreRevision;

    /**
     * Constructs a servlet with default functionality
     */
    public BmpServlet() {
        this(null);
    }

    /**
     * Constructs a servlet with the specified registry
     * 
     * @param registry
     */
    public BmpServlet(Registry registry) {
        super(registry);
        getVersion();
    }

    /**
     * Stops and cleans up the proxy.
     * 
     * Content should contain the uuid in a json content
     * 
     * expects a parameter passed in query string of "uuid"
     * 
     * @return Responds with a 202 Accepted if the proxy is removed or does not exist.
     * @return Responds with a 400 Bad Request if the uuid is not specified or there is no reason to create the proxy
     *         (see above).
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uuid = request.getParameter("uuid");
        if (StringUtils.isBlank(uuid)) {
            log.error("uuid not  present");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "uuid must be specified in json");
            return;
        }
        log.info("Stopping proxy with uuid " + uuid);
        BmpProxyRegistry.getInstance().stopProxy(uuid);
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

    /**
     * Starts a new BrowserMobProxy. Note that either recordHar must be set to true or some credentials are provided or
     * a proxy will not be created.
     *
     * Content should be a json object in the following form.
     * 
     * <pre>
        {
          "uuid": "my-uuid",//required
          "recordHar" : "true",
          "credentials": [{
            "domain" : "",
            "username" : "",
            "password" : "",
          }]
        }
     * </pre>
     * 
     * @return Responds with a 201 Created and the url ins the Location header if proxy is created.
     * 
     * @return Responds with a 400 Bad Request if the uuid is not specified or there is no reason to create the proxy
     *         (see above).
     * 
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // if (request.getContentType().equals("application/json"))
        try {
            JsonNode input = getNodeFromRequest(request);
            String uuid = getJsonString(input, "uuid");
            if (StringUtils.isBlank(uuid)) {
                log.error("uuid not  present");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "uuid must be specified in json");
                return;
            }

            JsonNode harRecording = input.get("recordHar");
            boolean recorrdingHar = harRecording != null && harRecording.asBoolean(false);
            BrowserMobProxy proxy = null;
            if (recorrdingHar) {
                proxy = new BrowserMobProxyServer();
                Set<CaptureType> set = new HashSet<CaptureType>(CaptureType.getRequestCaptureTypes());
                set.addAll(CaptureType.getResponseCaptureTypes());
                set.removeAll(CaptureType.getBinaryContentCaptureTypes());
                proxy.setHarCaptureTypes(set);
            }
            JsonNode creds = input.get("credentials");
            if (creds != null) {
                if (proxy == null) {
                    proxy = new BrowserMobProxyServer();
                }
                if (creds.isArray()) {
                    ArrayNode array = (ArrayNode) creds;
                    Iterator<JsonNode> elements = array.elements();
                    while (elements.hasNext()) {
                        JsonNode cred = elements.next();
                        addCredentials(proxy, cred);
                    }
                } else {
                    addCredentials(proxy, creds);
                }
            }
            if (proxy == null) {
                log.error("Nothing for proxy to do");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "Har recording or credentials not specified. There is no reason to start a proxy.");
                return;
            } else {
                String localhostname;
                // Try and get the IP address from the system property
                String runTimeHostName = System.getProperty(AutomationConstants.IP_ADDRESS);
                try {
                    if (runTimeHostName == null) {
                        log.warn("Host name could not be determined from system property.");
                    }
                    localhostname =
                            (runTimeHostName != null) ? runTimeHostName : InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    log.error("Error parsing out host name", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Host name could not be determined: " + e);
                    return;
                }

                // build the response
                BmpProxyRegistry.getInstance().addProxy(uuid, proxy);
                proxy.start();
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setHeader("Location", localhostname + ":" + proxy.getPort());
            }
        } catch (Exception e) {
            log.error("Error starting proxy: " + e, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error starting proxy: " + e);
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(200);

        StringBuilder builder = new StringBuilder();

        builder.append("<html>");
        builder.append("<head>");

        builder.append("<title>BrowserMobProxy instances</title>");

        builder.append("</head>");

        builder.append("<body>");
        builder.append("<H1>Grid Hub ");
        builder.append(coreVersion).append(coreRevision);
        builder.append("</H1>");
        Map<String, ProxyHolder> proxyMap = BmpProxyRegistry.getInstance().getProxyMap();
        DateFormat df = DateFormat.getDateTimeInstance();
        if (proxyMap.isEmpty()) {
            builder.append("No BrowserMobProxy instances currently running.");
        } else {
            for (Entry<String, ProxyHolder> entry : proxyMap.entrySet()) {
                StringBuilder localBuilder = new StringBuilder();
                ProxyHolder holder = entry.getValue();
                String uuid = entry.getKey();
                localBuilder.append("<fieldset>");
                localBuilder.append("<legend>BrowserMobProxy for test id ").append(uuid).append("</legend>");
                localBuilder.append("listening on ").append(holder.getProxy().getPort()).append("<br/>");
                localBuilder.append("started at ").append(df.format(new Date(holder.getCreated()))).append("<br/>");
                localBuilder.append("</fieldset>");
                builder.append(localBuilder.toString());
            }
        }
        try (InputStream in = new ByteArrayInputStream(builder.toString().getBytes("UTF-8"));) {
            ByteStreams.copy(in, response.getOutputStream());
        } finally {
            response.flushBuffer();
        }
    }

    private JsonNode getNodeFromRequest(HttpServletRequest request) throws IOException {
        String jsonString = null;
        ServletInputStream in = null;
        try {
            in = request.getInputStream();
            jsonString = IOUtils.toString(in, "utf-8");
        } finally {
            IOUtils.closeQuietly(in);
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode input = mapper.readTree(jsonString);
        return input;
    }

    private void addCredentials(BrowserMobProxy proxy, JsonNode creds) {
        String domain = getJsonString(creds, "domain");
        String user = getJsonString(creds, "username");
        String pass = getJsonString(creds, "password");
        proxy.autoAuthorization(domain, user, pass, AuthType.BASIC);

    }

    private String getJsonString(JsonNode node, String key) {
        String ret = null;
        JsonNode child = node.get(key);
        if (child != null) {
            ret = child.asText();
        }
        return ret;
    }

    private void getVersion() {
        final Properties p = new Properties();

        InputStream stream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("VERSION.txt");
        if (stream == null) {
            log.error("Couldn't determine version number");
            return;
        }
        try {
            p.load(stream);
        } catch (IOException e) {
            log.error("Cannot load version from VERSION.txt" + e.getMessage());
        }
        coreVersion = p.getProperty("selenium.core.version");
        coreRevision = p.getProperty("selenium.core.revision");
        if (coreVersion == null) {
            log.error("Cannot load selenium.core.version from VERSION.txt");
        }
    }

}
