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

package de.webthing.servient.impl;

import de.webthing.binding.AbstractRESTListener;
import de.webthing.thing.Action;
import de.webthing.thing.Content;
import de.webthing.thing.MediaType;
import de.webthing.util.encoding.ContentHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

/**
 * Created by Johannes on 07.10.2015.
 */
public class ActionListener extends AbstractRESTListener {

	private static final Logger log = LoggerFactory.getLogger(ActionListener.class);
    private final Action action;
    private StateContainer m_state;

    public ActionListener(StateContainer m_state, Action action) {
        this.action = action;
        this.m_state = m_state;
    }

    @Override
	public Content onGet()	{
		return new Content(("Action: " + action.getName()).getBytes(), MediaType.TEXT_PLAIN);
	}


    @Override
	public void onPut(Content data) {
		log.warn("Action was called by PUT, which is a violation of the spec");
		Function<?, ?> handler = m_state.getHandler(action);

		System.out.println("invoking " + action.getName());

		Map map = (Map) ContentHelper.parse(data, Map.class);
		Object o = map.get("value");

		Function<Object, Object> objectHandler = (Function<Object, Object>) handler;
		objectHandler.apply(o);
	}

    @Override
	public Content onPost(Content data) {
		Function<?, ?> handler = m_state.getHandler(action);

		System.out.println("invoking " + action.getName());

		Map map = (Map) ContentHelper.parse(data, Map.class);
		Object o = map.get("value");

		Function<Object, Object> objectHandler = (Function<Object, Object>) handler;
		Object response = objectHandler.apply(o);

		return ContentHelper.wrap(response,MediaType.APPLICATION_JSON);
	}
}
