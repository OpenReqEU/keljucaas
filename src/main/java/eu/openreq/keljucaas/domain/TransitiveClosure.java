package eu.openreq.keljucaas.domain;

import java.util.List;
import java.util.Map;

import fi.helsinki.ese.murmeli.ElementModel;

public class TransitiveClosure {

	private ElementModel model;
	private Map<Integer, List<String>> layers;
	private int numberOfOutpointingRelations;
	
	public ElementModel getModel() {
		return model;
	}
	public void setModel(ElementModel model) {
		this.model = model;
	}
	public Map<Integer, List<String>> getLayers() {
		return layers;
	}
	public void setLayers(Map<Integer, List<String>> layers) {
		this.layers = layers;
	}
	public int getNumberOfOutpointingRelations() {
		return numberOfOutpointingRelations;
	}
	public void setNumberOfOutpointingRelations(int numberOfOutpointingRelations) {
		this.numberOfOutpointingRelations = numberOfOutpointingRelations;
	}
}
