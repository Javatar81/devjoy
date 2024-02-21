package io.devjoy.gitea.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.devjoy.gitea.k8s.model.Gitea;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

class TestUpdateControlState {
    
    private Gitea resource = new Gitea();

    //patchStatus
    @Test
    void patchStatusOfNoUpdate() {
        UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
        noUpdate.patchStatus();
        assertTrue(noUpdate.getState().isPatchStatus());
        // Patch is specific type of update
        assertTrue(noUpdate.getState().isUpdateStatus());
        assertFalse(noUpdate.getState().isUpdateResource());
    }

    @Test
    void patchStatusOfResourceUpdate() {
        UpdateControlState<Gitea> resourceUpdate = new UpdateControlState<>(resource, UpdateControl.updateResource(resource));
        resourceUpdate.patchStatus();
        assertTrue(resourceUpdate.getState().isPatchStatus());
        assertTrue(resourceUpdate.getState().isUpdateStatus());
        assertTrue(resourceUpdate.getState().isUpdateResource());
    }

    @Test
    void patchStatusOfPatchedStatus() {
        UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
        patchStatus.patchStatus();
        assertTrue(patchStatus.getState().isPatchStatus());
        assertTrue(patchStatus.getState().isUpdateStatus());
        assertFalse(patchStatus.getState().isUpdateResource());
    }

    @Test
    void patchStatusOfUpdatedStatus() {
        UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.updateStatus(resource));
        patchStatus.patchStatus();
        assertTrue(patchStatus.getState().isPatchStatus());
        assertTrue(patchStatus.getState().isUpdateStatus());
        assertFalse(patchStatus.getState().isUpdateResource());
    }

    //updateStatus
    @Test
    void updateStatusOfNoUpdate() {
        UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
        noUpdate.updateStatus();
        assertFalse(noUpdate.getState().isPatchStatus());
        // Patch is specific type of update
        assertTrue(noUpdate.getState().isUpdateStatus());
        assertFalse(noUpdate.getState().isUpdateResource());
    }

    @Test
    void updateStatusOfResourceUpdate() {
        UpdateControlState<Gitea> resourceUpdate = new UpdateControlState<>(resource, UpdateControl.updateResource(resource));
        resourceUpdate.updateStatus();
        assertFalse(resourceUpdate.getState().isPatchStatus());
        assertTrue(resourceUpdate.getState().isUpdateStatus());
        assertTrue(resourceUpdate.getState().isUpdateResource());
    }

    @Test
    void updateStatusOfPatchedStatus() {
        UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
        patchStatus.updateStatus();
        assertTrue(patchStatus.getState().isPatchStatus());
        assertTrue(patchStatus.getState().isUpdateStatus());
        assertFalse(patchStatus.getState().isUpdateResource());
    }

    @Test
    void updateStatusOfUpdatedStatus() {
        UpdateControlState<Gitea> updateStatus = new UpdateControlState<>(resource, UpdateControl.updateStatus(resource));
        updateStatus.updateStatus();
        assertFalse(updateStatus.getState().isPatchStatus());
        assertTrue(updateStatus.getState().isUpdateStatus());
        assertFalse(updateStatus.getState().isUpdateResource());
    }

    //updateResource
    @Test
    void updateResourceOfNoUpdate() {
        UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
        noUpdate.updateResource();
        assertFalse(noUpdate.getState().isPatchStatus());
        // Patch is specific type of update
        assertFalse(noUpdate.getState().isUpdateStatus());
        assertTrue(noUpdate.getState().isUpdateResource());
    }

    @Test
    void updateResourceOfResourceUpdate() {
        UpdateControlState<Gitea> resourceUpdate = new UpdateControlState<>(resource, UpdateControl.updateResource(resource));
        resourceUpdate.updateResource();
        assertFalse(resourceUpdate.getState().isPatchStatus());
        assertFalse(resourceUpdate.getState().isUpdateStatus());
        assertTrue(resourceUpdate.getState().isUpdateResource());
    }

    @Test
    void updateResourceOfPatchedStatus() {
        UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
        patchStatus.updateResource();
        assertTrue(patchStatus.getState().isPatchStatus());
        assertTrue(patchStatus.getState().isUpdateStatus());
        assertTrue(patchStatus.getState().isUpdateResource());
    }

    @Test
    void updateResourceOfUpdatedStatus() {
        UpdateControlState<Gitea> updateStatus = new UpdateControlState<>(resource, UpdateControl.updateStatus(resource));
        updateStatus.updateResource();
        assertFalse(updateStatus.getState().isPatchStatus());
        assertTrue(updateStatus.getState().isUpdateStatus());
        assertTrue(updateStatus.getState().isUpdateResource());
    }

    //updateResourceAndPatchStatus
    @Test
    void updateResourceAndPatchStatusOfNoUpdate() {
        UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
        noUpdate.updateResourceAndPatchStatus();
        assertTrue(noUpdate.getState().isPatchStatus());
        // Patch is specific type of update
        assertTrue(noUpdate.getState().isUpdateStatus());
        assertTrue(noUpdate.getState().isUpdateResource());
    }

    @Test
    void updateResourceAndPatchStatusOfResourceUpdate() {
        UpdateControlState<Gitea> resourceUpdate = new UpdateControlState<>(resource, UpdateControl.updateResource(resource));
        resourceUpdate.updateResourceAndPatchStatus();
        assertTrue(resourceUpdate.getState().isPatchStatus());
        assertTrue(resourceUpdate.getState().isUpdateStatus());
        assertTrue(resourceUpdate.getState().isUpdateResource());
    }

    @Test
    void updateResourceAndPatchStatusOfPatchedStatus() {
        UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
        patchStatus.updateResourceAndPatchStatus();
        assertTrue(patchStatus.getState().isPatchStatus());
        assertTrue(patchStatus.getState().isUpdateStatus());
        assertTrue(patchStatus.getState().isUpdateResource());
    }

    @Test
    void updateResourceAndPatchStatusOfUpdatedStatus() {
        UpdateControlState<Gitea> updateStatus = new UpdateControlState<>(resource, UpdateControl.updateStatus(resource));
        updateStatus.updateResourceAndPatchStatus();
        assertTrue(updateStatus.getState().isPatchStatus());
        assertTrue(updateStatus.getState().isUpdateStatus());
        assertTrue(updateStatus.getState().isUpdateResource());
    }

     //updateResourceAndStatus
     @Test
     void updateResourceAndStatusOfNoUpdate() {
         UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
         noUpdate.updateResourceAndStatus();
         assertFalse(noUpdate.getState().isPatchStatus());
         // Patch is specific type of update
         assertTrue(noUpdate.getState().isUpdateStatus());
         assertTrue(noUpdate.getState().isUpdateResource());
     }
 
     @Test
     void updateResourceAndStatusOfResourceUpdate() {
         UpdateControlState<Gitea> resourceUpdate = new UpdateControlState<>(resource, UpdateControl.updateResource(resource));
         resourceUpdate.updateResourceAndStatus();
         assertFalse(resourceUpdate.getState().isPatchStatus());
         assertTrue(resourceUpdate.getState().isUpdateStatus());
         assertTrue(resourceUpdate.getState().isUpdateResource());
     }
 
     @Test
     void updateResourceAndStatusOfPatchedStatus() {
         UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
         patchStatus.updateResourceAndStatus();
         assertTrue(patchStatus.getState().isPatchStatus());
         assertTrue(patchStatus.getState().isUpdateStatus());
         assertTrue(patchStatus.getState().isUpdateResource());
     }
 
     @Test
     void updateResourceAndStatusOfUpdatedStatus() {
         UpdateControlState<Gitea> updateStatus = new UpdateControlState<>(resource, UpdateControl.updateStatus(resource));
         updateStatus.updateResourceAndStatus();
         assertFalse(updateStatus.getState().isPatchStatus());
         assertTrue(updateStatus.getState().isUpdateStatus());
         assertTrue(updateStatus.getState().isUpdateResource());
     }

    
}
