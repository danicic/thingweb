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

import de.webthing.binding.RESTListener;
import de.webthing.binding.ResourceBuilder;
import de.webthing.servient.Defines;
import de.webthing.servient.InteractionListener;
import de.webthing.servient.ThingServer;
import de.webthing.thing.Action;
import de.webthing.thing.Content;
import de.webthing.thing.Property;
import de.webthing.thing.Thing;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;


/**
 * {@link ThingServer} implementation capable of offering a Thing via multiple
 * bindings simultaneously.
 */
public class MultiBindingThingServer implements ThingServer {
	
	/** The logger. */
	private final static Logger log = Logger.getLogger(MultiBindingThingServer.class.getCanonicalName());
	
	public MultiBindingThingServer(Thing thingModel, 
			ResourceBuilder ... bindings) {
		
		if (null == thingModel) {
			throw new IllegalArgumentException("thingModel must not be null");
		}
		
		for (ResourceBuilder b : bindings) {
			m_bindings.add(b);
		}
		
		m_thingModel = thingModel;
		m_state = new StateContainer(m_thingModel);

		createBindings();

	}
	

	@Override
	public void setProperty(Property property, Content value) {
		if (null == property) {
			throw new IllegalArgumentException("property must not be null");
		}
		if (!m_thingModel.isOwnerOf(property)) {
			throw new IllegalArgumentException(
					"property does not belong to served thing");
		}
		
		synchronized (m_stateSync) {
			m_state.setProperty(property, value);
		}
	}


	@Override
	public void setProperty(String propertyName, Content value) {
		Property prop = m_thingModel.getProperty(propertyName);
		
		if (null == prop) {
			throw new IllegalArgumentException("no such property: " + 
					propertyName);
		}
		
		setProperty(prop, value);
	}
	
	
	@Override
	public Content getProperty(Property property) {
		if (null == property) {
			throw new IllegalArgumentException("property must not be null");
		}
		if (!m_thingModel.isOwnerOf(property)) {
			throw new IllegalArgumentException(
					"property does not belong to served thing");
		}
		
		synchronized (m_stateSync) {
			return m_state.getProperty(property);
		}
	}


	@Override
	public Content getProperty(String propertyName) {
		Property prop = m_thingModel.getProperty(propertyName);
		
		if (null == prop) {
			throw new IllegalArgumentException("no such property: " + 
					propertyName);
		}
		
		return getProperty(prop);
	}

	@Override
	public void onUpdate(String propertyName, Consumer<Content> callback) {
		this.addInteractionListener(new InteractionListener() {
			@Override
			public void onReadProperty(String propertyName, ThingServer thingServer) {

			}

			@Override
			public void onWriteProperty(String changedPropertyName, Content newValue, ThingServer thingServer) {
				if(changedPropertyName.equals(propertyName)) {
					callback.accept(newValue);
				}
			}
		});
	}

	@Override
	public void onInvoke(String actionName, Function<Object, Object> callback) {
		Action action = m_thingModel.getAction(actionName);
		m_state.addHandler(action, callback);
	}

	@Override
	public void addInteractionListener(InteractionListener listener) {
		m_listeners.add(listener);
	}
	
	
	private void createBindings() {
		for (ResourceBuilder binding : m_bindings) {
			createBinding(binding);
		}
	}

		
	private void createBinding(ResourceBuilder resources) {

		// root
		resources.newResource(Defines.BASE_URL, new HypermediaIndex(
						new HyperMediaLink("things", Defines.BASE_THING_URL)
			)
		);

		// things
		resources.newResource(Defines.BASE_THING_URL, new HypermediaIndex(
						new HyperMediaLink("thing", Defines.BASE_THING_URL + m_thingModel.getName())
				)
		);

		// thing root
		resources.newResource(Defines.BASE_THING_URL + m_thingModel.getName(), new HypermediaIndex(
						new HyperMediaLink("actions",Defines.BASE_THING_URL + m_thingModel.getName() +
								Defines.REL_ACTION_URL),
						new HyperMediaLink("properties",Defines.BASE_THING_URL + m_thingModel.getName() +
								Defines.REL_PROPERTY_URL)
				)
		);



		Collection<Property> properties = m_thingModel.getProperties();
		Collection<Action> actions = m_thingModel.getActions();

		List<HyperMediaLink> actionLinks = new LinkedList<>();
		List<HyperMediaLink> propertyLinks = new LinkedList<>();

		Map<String,RESTListener> interactionListeners = new HashMap<>();

		// collect properties
		for (Property property : properties) {
			String url = Defines.BASE_THING_URL + m_thingModel.getName() +
					Defines.REL_PROPERTY_URL + property.getName();
			interactionListeners.put(url, new PropertyListener(this, property));
			propertyLinks.add(new HyperMediaLink("property", url));
		}

		// collect actions
		for (Action action : actions) {
			//TODO optimize by preconstructing strings and using format
			String url = Defines.BASE_THING_URL + m_thingModel.getName() +
					Defines.REL_ACTION_URL + action.getName();
			interactionListeners.put(url, new ActionListener(m_state, action));
			actionLinks.add(new HyperMediaLink("action", url));
		}

		// actions
		resources.newResource(Defines.BASE_THING_URL + m_thingModel.getName() +
						Defines.REL_ACTION_URL,
				new HypermediaIndex(actionLinks));

		// properties
		resources.newResource(Defines.BASE_THING_URL + m_thingModel.getName() +
						Defines.REL_PROPERTY_URL,
				new HypermediaIndex(propertyLinks));

		// leaves last (side-effect of coap-binding)
		interactionListeners.entrySet().stream().forEachOrdered(
				entry -> resources.newResource(entry.getKey(), entry.getValue())
		);
	}
	
	
	Content readProperty(Property property) {
		for (InteractionListener listener : m_listeners) {
			listener.onReadProperty(property.getName(), this);
		}
		
		return getProperty(property);
	}
	
	
	void writeProperty(Property property, Content value) {
		setProperty(property, value);
		
		for (InteractionListener listener : m_listeners) {
			listener.onWriteProperty(property.getName(), value, this);
		}
	}

	/**
	 * Sync object for {@link #m_stateSync}.
	 */
	private final Object m_stateSync = new Object();
	
	
	private final StateContainer m_state;
	

	private final Collection<InteractionListener> m_listeners = 
			new CopyOnWriteArrayList<>();

	private final Collection<ResourceBuilder> m_bindings = new ArrayList<>(); 

	private final Thing m_thingModel;

}
