package de.webthing.thing;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import de.webthing.desc.pojo.ActionDescription;
import de.webthing.desc.pojo.InteractionDescription;
import de.webthing.desc.pojo.PropertyDescription;
import de.webthing.desc.pojo.ThingDescription;
import de.webthing.servient.ThingClient;
import de.webthing.servient.ThingServer;


/**
 * The Thing class provides the "model" of a thing, the components and
 * capabilities of a thing.<p>
 * 
 * For implementing things, a {@link ThingServer} implementation should be
 * used, for interacting with things a {@link ThingClient} implementation.<p>
 * 
 * This class is thread safe. However it is not guaranteed that modifications 
 * performed on this class are reflected by {@link ThingClient} or 
 * {@link ThingServer} implementation. Please consult the corresponding
 * documentation whether it is the case or not.
 */
public final class Thing {
	/*
	 * Implementation Note:
	 * This implementation is based on the assumption that change is rare, i.e.,
	 * interactions with the model heavily outweigh modifications of the model.
	 * If this is no longer the case, the type of concurrent collection used
	 * should be changed or other means of synchronization should be used.
	 * 
	 * This class is final to ensure the synchronization guarantees are not
	 * broken by subclasses. 
	 */
	
	
	/**
	 * Creates a new thing model.<p>
	 * 
	 * The thing's name is used to create unique URLs for different things, 
	 * i.e., URLs formed like <code>/things/&lt;name&gt;/...</code>. Names must 
	 * therefore be unique per ThingServer.
	 * 
	 * @param name the name, must not be null
	 */
	public Thing(String name) {
		if (null == name) {
			throw new IllegalArgumentException("name must not be null");
		}
		
		m_name = name;
	}
	
	public Thing(ThingDescription desc) {
	    this(desc.getMetadata().getName());
	    m_td = desc;
	    // TODO check support for HTTP and/or CoAP
	    for (InteractionDescription i : desc.getInteractions()) {
		if (i instanceof PropertyDescription) {
		    Property.Builder b = Property.getBuilder(i.getName());
		    b.setReadable(true);
		    b.setWriteable(((PropertyDescription) i).isWritable());
		    addProperty(b.build());
		} else if (i instanceof ActionDescription) {
		    Action.Builder b = Action.getBuilder(i.getName());
		    // TODO case with several inputs?
		    b.addParam("parm", ((ActionDescription) i).getInputType());
		    addAction(b.build());
		}
	    }
	}

	public String getName() {
		return m_name;
	}
	
	public ThingDescription getThingDescription() {
	    return m_td;
	}
	
	
	public Collection<Property> getProperties() {
		return m_propertiesView;
	}
	
	
	public Collection<Action> getActions() {
		return m_actionsView;
	}
	
	
	/**
	 * Returns a property by name.
	 * 
	 * @param propertyName the name of the property, must not be null
	 * @return the property, or null if no property with the given name exists
	 */
	public Property getProperty(String propertyName) {
		if (null == propertyName) {
			throw new IllegalArgumentException("propertyName must not be null");
		}
		
		for (Property property : m_properties) {
			if (property.getName().equals(propertyName)) {
				return property;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns a property by name and raises an exception if the property does
	 * not exist.<p>
	 * 
	 * Behaves similarly to {@link #getProperty(String)} but throws an 
	 * exception instead of returning null if the property does not exist
	 * 
	 * @param propertyName the name of the property, must not be null
	 * @return the property
	 */
	public Property getRequiredProperty(String propertyName) {
		Property p = getProperty(propertyName);
		
		if (null == p) {
			throw new IllegalArgumentException("no such property " + 
					propertyName);
		}

		return p;
	}
	
	
	public boolean isOwnerOf(Property property) {
		if (null == property) {
			throw new IllegalArgumentException("property must not be null");
		}
		
		for (Property p : m_properties) {
			if (p == property) {
				return true;
			}
		}
		
		return false;
	}
	
	
	public void addModelListener(ModelListener listener) {
		if (null == listener) {
			throw new IllegalArgumentException("listener must not be null");
		}
		
		 m_listeners.add(listener);
	}
	
	
	public void addProperty(Property property) {
		if (null == property) {
			throw new IllegalArgumentException("property must not be null");
		}
		
		if (getProperty(property.getName()) != null) {
			throw new IllegalArgumentException("duplicate property: " + 
					property.getName());
		}
		
		m_properties.add(property);
		
		notifyListeners();
	}

	public void addAction(Action action) {
		if (null == action) {
			throw new IllegalArgumentException("action must not be null");
		}

		if (getProperty(action.getName()) != null) {
			throw new IllegalArgumentException("duplicate action: " +
					action.getName());
		}

		m_actions.add(action);

		notifyListeners();
	}
	
	private void notifyListeners() {
		for (ModelListener listener : m_listeners) {
			listener.onChange(this);
		}
	}
	
	
	private final String m_name;
	
	
	private ThingDescription m_td;
	
	
	private final Collection<Property> m_properties = 
			new CopyOnWriteArrayList<>();
	
	
	private final Collection<Property> m_propertiesView = 
			Collections.unmodifiableCollection(m_properties);
	
	
	private final Collection<Action> m_actions = 
			new CopyOnWriteArrayList<>();
	
	
	private final Collection<Action> m_actionsView = 
			Collections.unmodifiableCollection(m_actions);
	
	
	private final Collection<ModelListener> m_listeners = 
			new CopyOnWriteArrayList<>();

	public Action getAction(String actionName) {
		if (null == actionName) {
			throw new IllegalArgumentException("actionName must not be null");
		}

		for (Action action : m_actions) {
			if(action.getName().equals(actionName))
				return action;
		}

		return null;
	}
}
