/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Siemens AG and the thingweb community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.webthing.binding.coap;

import de.webthing.binding.Binding;
import de.webthing.binding.RESTListener;
import de.webthing.binding.ResourceBuilder;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CoapBinding implements Binding {

    private static final Logger log = LoggerFactory.getLogger(CoapBinding.class);
	private CoapServer m_coapServer;

	@Override
	public void initialize() {
		m_coapServer = new CoapServer();
	}

	@Override
	public ResourceBuilder getResourceBuilder() {
		return new ResourceBuilder() {
			@Override
            public void newResource(String url, RESTListener restListener) {
                String[] parts = url.split("/");
                if(parts.length == 0) return;

                Resource current = m_coapServer.getRoot();

                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].isEmpty()) {
                        continue;
                    }

                    Resource child = current.getChild(parts[i]);

                    if (child == null) {
                        child = new CoapResource(parts[i]);
                        current.add(child);
                    }

                    current = child;
                }

                String lastPart = parts[parts.length - 1];
                // TODO this has a side-effect: replacing the resource makes the children unreachable
                // a clean tree-replace would require to store the children and append them to the new resource
                current.add(new WotCoapResource(lastPart, restListener));
            }
		};
	}

	@Override
	public void start() {
		m_coapServer.start();
	}

}
