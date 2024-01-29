package io.devjoy.gitea.k8s;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class UpdateControlState<P extends HasMetadata> {

    private UpdateControl<P> state = UpdateControl.noUpdate();
    private final P resource;

    public UpdateControlState(P resource) {
        this.resource = resource;
    }

    public UpdateControlState(P resource, UpdateControl<P> state) {
        this.resource = resource;
        this.state = state;
    }

    public void patchStatus() {
        if (!state.isPatchStatus()) {
            if (state.isUpdateResource()) {
                state = UpdateControl.updateResourceAndPatchStatus(resource);
            } else {
                state = UpdateControl.patchStatus(resource);
            }
        }
    }

    public void updateStatus() {
        if (!state.isUpdateStatus()) {
            if (state.isUpdateResource()) {
                state = UpdateControl.updateResourceAndStatus(resource);
            } else {
                state = UpdateControl.updateStatus(resource);
            }
        }
    }
    
    public void updateResource() {
        if (!state.isUpdateResource()) {
            if (state.isPatchStatus()) {
                state = UpdateControl.updateResourceAndPatchStatus(resource);
            } else if (state.isUpdateStatus()) {
                state = UpdateControl.updateResourceAndStatus(resource);
            }
            else {
                state = UpdateControl.updateResource(resource);
            }
        }
    }

    public void updateResourceAndStatus() {
        if (!state.isUpdateResource() || !state.isUpdateStatus()) {
            if(state.isPatchStatus()) {
                state = UpdateControl.updateResourceAndPatchStatus(resource);
            } else {
                state = UpdateControl.updateResourceAndStatus(resource);
            }
        } 
        
    }

    public void updateResourceAndPatchStatus() {
        state = UpdateControl.updateResourceAndPatchStatus(resource);
    }

    public UpdateControl<P> getState() {
        return this.state;
    }
}
