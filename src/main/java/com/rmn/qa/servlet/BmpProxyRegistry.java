/**
 * Copyright Qualcomm Inc. 2016
 */
package com.rmn.qa.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lightbody.bmp.BrowserMobProxy;

/**
 * @author denisa
 *
 */
public class BmpProxyRegistry {

    private static final Logger log = LoggerFactory.getLogger(BmpServlet.class);
    private static final BmpProxyRegistry instance = new BmpProxyRegistry();
    private static final long MAX_PROXY_TIME = 1000 * 60 * 60 * 24 * 2;// 2 days
    protected static final long SLEEP_TIME = 1000 * 60 * 60 * 8;// 8 hours
    private Map<String, ProxyHolder> proxyMap = new HashMap<String, ProxyHolder>();

    /**
     * @return the instance
     */
    public static BmpProxyRegistry getInstance() {
        return instance;
    }

    private BmpProxyRegistry() {
        // Start a proxy cleanup thread
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (true) {
                        removeUnusedProxies();
                        Thread.sleep(SLEEP_TIME);
                    }
                } catch (InterruptedException e) {
                    log.warn("Error in reaping unused proxies: " + e, e);
                }

            }
        });
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    /**
     * 
     * @param uuid
     * @param proxy
     */
    public void addProxy(String uuid, BrowserMobProxy proxy) {
        if (proxyMap.containsKey(uuid)) {
            throw new RuntimeException("Proxy for uuid " + uuid + " already started.");
        }
        proxyMap.put(uuid, new ProxyHolder(proxy));
    }

    /**
     * @return the proxyMap
     */
    public Map<String, ProxyHolder> getProxyMap() {
        return new HashMap<String, ProxyHolder>(proxyMap);
    }

    /**
     * 
     * @param uuid
     */
    public void stopProxy(String uuid) {
        ProxyHolder holder = proxyMap.remove(uuid);
        if (holder != null) {
            log.warn("stopping proxy for uuid " + uuid + " is not in registry.");
            holder.proxy.stop();
        } else {
            log.warn("proxy for uuid " + uuid + " is not in registry.");
        }
    }

    private void removeUnusedProxies() {
        Map<String, ProxyHolder> copy = new HashMap<String, ProxyHolder>(proxyMap);
        for (Entry<String, ProxyHolder> entry : copy.entrySet()) {
            if (System.currentTimeMillis() - entry.getValue().created > MAX_PROXY_TIME) {
                stopProxy(entry.getKey());
            }
        }
    }

    public static class ProxyHolder {
        private BrowserMobProxy proxy;
        private final long created = System.currentTimeMillis();

        /**
         * @param proxy
         */
        public ProxyHolder(BrowserMobProxy proxy) {
            super();
            this.proxy = proxy;
        }

        /**
         * @return the proxy
         */
        public BrowserMobProxy getProxy() {
            return proxy;
        }

        /**
         * @return the created
         */
        public long getCreated() {
            return created;
        }

    }

}
