package io.hotmoka.network.models.updates;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.hotmoka.beans.updates.Update;

/**
 * The model of the state of an object: just the set of its updates.
 */
public class StateModel {
    private final List<UpdateModel> updates;

    /**
     * Builds the model of the given state of an object.
     * 
     * @param state the state
     */
    public StateModel(Stream<Update> state) {
    	this.updates = state.map(UpdateModel::new).collect(Collectors.toList());
    }

    /**
     * Yields the updates having this model.
     * 
     * @return the updates
     */
    public Stream<Update> toBean() {
    	return updates.stream().map(UpdateModel::toBean).collect(Collectors.toSet()).stream();
    }

    /**
     * Yields the models of the updates in the state.
     * 
     * @return the models of the updates
     */
    public Stream<UpdateModel> getUpdates() {
    	return updates.stream();
    }
}