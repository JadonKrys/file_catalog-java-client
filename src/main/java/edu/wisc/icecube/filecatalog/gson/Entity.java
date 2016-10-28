package edu.wisc.icecube.filecatalog.gson;

import com.google.gson.Gson;

public class Entity {
	private Links _links;
	
	public Entity(final Links _links) {
		this._links = _links;
	}
	
	public Links getLinks() {
		return _links;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
