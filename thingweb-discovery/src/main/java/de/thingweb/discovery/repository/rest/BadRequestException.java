package de.thingweb.discovery.repository.rest;

import java.io.IOException;

public class BadRequestException extends RESTException {
	
	@Override
	public int getStatus() {
		return 400;
	}
	
}
