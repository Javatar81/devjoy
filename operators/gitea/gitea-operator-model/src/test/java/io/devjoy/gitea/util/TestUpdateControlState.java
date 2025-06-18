package io.devjoy.gitea.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.devjoy.gitea.k8s.model.Gitea;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

class TestUpdateControlState {
    
    private Gitea resource = new Gitea();

    @Test
    void patchStatusOfNoUpdate() {
        UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
        noUpdate.patchStatus();
        assertTrue(noUpdate.getState().isPatchStatus());
        assertFalse(noUpdate.getState().isPatchResource());
        assertFalse(noUpdate.getState().isPatchResourceAndStatus());
        assertFalse(noUpdate.getState().isNoUpdate());
    }

    @Test
    void patchStatusOfPatchedStatus() {
        UpdateControlState<Gitea> patchStatus = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
        patchStatus.patchStatus();
        assertTrue(patchStatus.getState().isPatchStatus());
        assertFalse(patchStatus.getState().isPatchResource());
        assertFalse(patchStatus.getState().isPatchResourceAndStatus());
        assertFalse(patchStatus.getState().isNoUpdate());
    }

    @Test
    void patchStatusOfPatchedResource() {
        UpdateControlState<Gitea> patchResource = new UpdateControlState<>(resource, UpdateControl.patchResource(resource));
        patchResource.patchStatus();
        assertTrue(patchResource.getState().isPatchStatus());
        assertTrue(patchResource.getState().isPatchResource());
        assertTrue(patchResource.getState().isPatchResourceAndStatus());
        assertFalse(patchResource.getState().isNoUpdate());
    }

    @Test
    void patchStatusOfPatchedResourceAndPatcheStatus() {
        UpdateControlState<Gitea> patchResourceAndStatus = new UpdateControlState<>(resource, UpdateControl.patchResourceAndStatus(resource));
        patchResourceAndStatus.patchStatus();
        assertTrue(patchResourceAndStatus.getState().isPatchStatus());
        assertTrue(patchResourceAndStatus.getState().isPatchResource());
        assertTrue(patchResourceAndStatus.getState().isPatchResourceAndStatus());
        assertFalse(patchResourceAndStatus.getState().isNoUpdate());
    }

    @Test
    void patchResourceOfNoUpdate() {
        UpdateControlState<Gitea> noUpdate = new UpdateControlState<>(resource);
        noUpdate.patchResource();
        assertFalse(noUpdate.getState().isPatchStatus());
        assertTrue(noUpdate.getState().isPatchResource());
        assertFalse(noUpdate.getState().isPatchResourceAndStatus());
        assertFalse(noUpdate.getState().isNoUpdate());
    }

    @Test
    void patchResourceOfPatchedResource() {
        UpdateControlState<Gitea> patchResource = new UpdateControlState<>(resource, UpdateControl.patchResource(resource));
        patchResource.patchResource();
        assertFalse(patchResource.getState().isPatchStatus());
        assertTrue(patchResource.getState().isPatchResource());
        assertFalse(patchResource.getState().isPatchResourceAndStatus());
        assertFalse(patchResource.getState().isNoUpdate());
    }

    @Test
    void patchResourceOfPatchedStatus() {
        UpdateControlState<Gitea> patchResource = new UpdateControlState<>(resource, UpdateControl.patchStatus(resource));
        patchResource.patchResource();
        assertTrue(patchResource.getState().isPatchStatus());
        assertTrue(patchResource.getState().isPatchResource());
        assertTrue(patchResource.getState().isPatchResourceAndStatus());
        assertFalse(patchResource.getState().isNoUpdate());
    }

    @Test
    void patchResourceOfPatchedResourceAndPatcheStatus() {
        UpdateControlState<Gitea> patchResourceAndStatus = new UpdateControlState<>(resource, UpdateControl.patchResourceAndStatus(resource));
        patchResourceAndStatus.patchResource();
        assertTrue(patchResourceAndStatus.getState().isPatchStatus());
        assertTrue(patchResourceAndStatus.getState().isPatchResource());
        assertTrue(patchResourceAndStatus.getState().isPatchResourceAndStatus());
        assertFalse(patchResourceAndStatus.getState().isNoUpdate());
    }

    
}
