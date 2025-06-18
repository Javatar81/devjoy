package io.devjoy.gitea.util;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class UpdateControlState<P extends HasMetadata> {

    private UpdateControl<P> state = UpdateControl.noUpdate();
    private Optional<Duration> scheduleDelay = Optional.empty();
    private final P resource;

    public UpdateControlState(P resource) {
        this.resource = resource;
    }

    public UpdateControlState(P resource, UpdateControl<P> state) {
        this.resource = resource;
        this.state = state;
    }

    public UpdateControlState<P> patchStatus() {
        if (!state.isPatchStatus()) {
            if (state.isPatchResource()) {
                state = UpdateControl.patchResourceAndStatus(resource);
            } else {
                state = UpdateControl.patchStatus(resource);
            }
        }
        return this;
    }

    public UpdateControlState<P> patchResource() {
        if (!state.isPatchResource()) {
            if (state.isPatchStatus()) {
                state = UpdateControl.patchResourceAndStatus(resource);
            } else {
                state = UpdateControl.patchResource(resource);
            }
        }
        return this;
    }

    public UpdateControlState<P> patchResourceAndStatus() {
        if(!state.isPatchResourceAndStatus()) {
            state = UpdateControl.patchResourceAndStatus(resource);
        }
        return this;
    }

    public UpdateControl<P> getState() {
    	scheduleDelay.ifPresent(d -> this.state.rescheduleAfter(d));
        return this.state;
    }

	public UpdateControlState<P> rescheduleAfter(long delay, TimeUnit unit) {
		scheduleDelay = Optional.of(Duration.ofMillis(unit.toMillis(delay)));
		return this;
	}
	
	public UpdateControlState<P> rescheduleAfter(long delayMillis) {
		scheduleDelay = Optional.of(Duration.ofMillis(delayMillis));
		return this;
	}
	
	public UpdateControlState<P> rescheduleAfter(Duration scheduleDelay) {
		this.scheduleDelay = Optional.of(scheduleDelay);
		return this;
	}
    
}
