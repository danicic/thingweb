/*
 *
 *  * The MIT License (MIT)
 *  *
 *  * Copyright (c) 2016 Siemens AG and the thingweb community
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in
 *  * all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  * THE SOFTWARE.
 *
 */

package de.thingweb.client.impl;

import de.thingweb.client.Callback;
import de.thingweb.client.UnsupportedException;
import de.thingweb.desc.pojo.ActionDescription;
import de.thingweb.desc.pojo.EventDescription;
import de.thingweb.desc.pojo.Metadata;
import de.thingweb.desc.pojo.PropertyDescription;
import de.thingweb.desc.pojo.Protocol;
import de.thingweb.thing.Content;
import de.thingweb.thing.MediaType;
import org.eclipse.californium.core.CoapObserveRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpClientImpl extends AbstractClientImpl {

	private static final Logger log = LoggerFactory.getLogger(HttpClientImpl.class);

	private static final int NTHREDS = 5;
	private static final ExecutorService executorService = Executors.newFixedThreadPool(NTHREDS);

	Map<String, CoapObserveRelation> observes = new HashMap<>();
	
	public HttpClientImpl(Protocol prot, Metadata metadata, List<PropertyDescription> properties, List<ActionDescription> actions,
			List<EventDescription> events) {
		super(prot.getUri(), metadata, properties, actions, events);
	}

	public void put(String propertyName, Content propertyValue, Callback callback) throws UnsupportedException {
		put(propertyName, propertyValue, callback, null);
	}
	
	public void put(String propertyName, Content propertyValue, Callback callback, String securityAsToken) throws UnsupportedException {
		try {
			CallbackPutActionTask cgt = new CallbackPutActionTask(propertyName, propertyValue, callback, false, securityAsToken);
			executorService.submit(cgt);
		} catch (Exception e) {
			log.warn(e.getMessage());
			callback.onPutError(propertyName);
		}
	}

	public void get(String propertyName, Callback callback) throws UnsupportedException {
		get(propertyName, callback, null);
	}
	
	public void get(String propertyName, Callback callback, String securityAsToken) throws UnsupportedException {
		try {
			CallbackGetTask cgt = new CallbackGetTask(propertyName, callback, securityAsToken);
			executorService.submit(cgt);
		} catch (Exception e) {
			log.warn(e.getMessage());
			callback.onGetError(propertyName);
		}
	}

	public void observe(String propertyName, Callback callback) throws UnsupportedException {
		observe(propertyName, callback, null);
	}
	
	public void observe(String propertyName, Callback callback, String securityAsToken) throws UnsupportedException {
		callback.onObserveError(propertyName);
		// throw new UnsupportedException("Not implemented yet");
	}

	public void observeRelease(String propertyName) throws UnsupportedException {
		throw new UnsupportedException("Not implemented yet");
	}

	public void action(String actionName, Content actionValue, Callback callback) throws UnsupportedException {
		action(actionName, actionValue, callback, null);
	}
	
	public void action(String actionName, Content actionValue, Callback callback, String securityAsToken) throws UnsupportedException {
		try {
			CallbackPutActionTask cgt = new CallbackPutActionTask(actionName, actionValue, callback, true, securityAsToken);
			executorService.submit(cgt);
		} catch (Exception e) {
			log.warn(e.getMessage());
			callback.onActionError(actionName);
		}
	}

	class CallbackGetTask implements Runnable {
		private final String propertyName;
		private final Callback callback;
		private final String securityAsToken;

		CallbackGetTask(String propertyName, Callback callback) {
			this(propertyName, callback, null);
		}
		
		CallbackGetTask(String propertyName, Callback callback, String securityAsToken) {
			this.propertyName = propertyName;
			this.callback = callback;
			this.securityAsToken = securityAsToken;
		}

		public void run() {
			try {
				URL url = new URL(uri + URI_PART_PROPERTIES + propertyName + (useValueStringInGetAndPutUrl ? "" : "/value"));
				HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
				httpCon.setRequestMethod("GET");
				if(securityAsToken != null) {
					httpCon.setRequestProperty("Authorization", "Bearer " + securityAsToken);
				}

				InputStream is = httpCon.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int b;
				while ((b = is.read()) != -1) {
					baos.write(b);
				}

				String contentType = httpCon.getHeaderField("content-type");
				MediaType mediaType = MediaType.getMediaType(contentType);
				
				httpCon.disconnect();
				
				Content c = new Content(baos.toByteArray(), mediaType);

				callback.onGet(propertyName, c);
			} catch (Exception e) {
				log.warn(e.getMessage());
				callback.onGetError(propertyName);
			}
		}
	}

	class CallbackPutActionTask implements Runnable {
		private final String name;
		private final Callback callback;
		private final Content propertyValue;
		private final boolean isAction;
		private final String securityAsToken;

		CallbackPutActionTask(String name, Content propertyValue, Callback callback, boolean isAction) {
			this(name, propertyValue, callback, isAction, null);
		}
		
		CallbackPutActionTask(String name, Content propertyValue, Callback callback, boolean isAction, String securityAsToken) {
			this.name = name;
			this.propertyValue = propertyValue;
			this.callback = callback;
			this.isAction = isAction;
			this.securityAsToken = securityAsToken;
		}
		
		// String securityAsToken

		public void run() {
			try {
				String uriPart = isAction ? URI_PART_PROPERTIES : URI_PART_ACTIONS;
				URL url;
				if(!isAction) {
					url = new URL(uri + uriPart + name + (useValueStringInGetAndPutUrl ? "" : "/value"));
				} else {
					url = new URL(uri + uriPart + name);
				}
				HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
				httpCon.setDoOutput(true);
				httpCon.setRequestProperty("content-type", propertyValue.getMediaType().mediaType);
				httpCon.setRequestMethod(isAction ? "POST" : "PUT");
				if(securityAsToken != null) {
					httpCon.setRequestProperty("Authorization", "Bearer " + securityAsToken);
				}

				OutputStream out = httpCon.getOutputStream();
				out.write(propertyValue.getContent());
				out.close();
				
				InputStream is = httpCon.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int b;
				while ((b = is.read()) != -1) {
					baos.write(b);
				}

				String contentType = httpCon.getHeaderField("content-type");
				MediaType mediaType = MediaType.getMediaType(contentType);
				
				httpCon.disconnect();
				
				Content c = new Content(baos.toByteArray(), mediaType);

				if(!isAction) {
					callback.onPut(name,  c);
				} else {
					callback.onAction(name, c);
				}
			} catch (Exception e) {
				log.warn(e.getMessage());
				callback.onPutError(name);
			}
		}
	}

}
