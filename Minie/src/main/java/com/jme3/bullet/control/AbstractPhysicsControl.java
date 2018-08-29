/*
 * Copyright (c) 2009-2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySpatial;

/**
 * AbstractPhysicsControl manages the lifecycle of a physics object that is
 * attached to a spatial in the scene graph.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public abstract class AbstractPhysicsControl
        implements PhysicsControl, JmeCloneable {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(AbstractPhysicsControl.class.getName());
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    /**
     * local copy of {@link com.jme3.math.Vector3f#ZERO}
     */
    final private static Vector3f translateIdentity = new Vector3f(0f, 0f, 0f);

    private final Quaternion tmp_inverseWorldRotation = new Quaternion();
    protected Spatial spatial;
    protected boolean enabled = true; // TODO privatize
    protected boolean added = false;
    private PhysicsSpace space = null;
    /**
     * true&rarr;match local transform to physics transform, false&rarr;match
     * world transform to physics transform
     */
    private boolean applyLocal = false;

    /**
     * Called when the control is added to a new spatial, create any
     * spatial-dependent data here.
     *
     * @param spat The new spatial, guaranteed not to be null
     */
    protected abstract void createSpatialData(Spatial spat);

    /**
     * Called when the control is removed from a spatial, remove any
     * spatial-dependent data here.
     *
     * @param spat The old spatial, guaranteed not to be null
     */
    protected abstract void removeSpatialData(Spatial spat);

    /**
     * Called when the physics object is supposed to move to the spatial
     * position.
     *
     * @param vec spatial location coordinates
     */
    protected abstract void setPhysicsLocation(Vector3f vec);

    /**
     * Called when the physics object is supposed to move to the spatial
     * rotation.
     *
     * @param quat spatial orientation
     */
    protected abstract void setPhysicsRotation(Quaternion quat);

    /**
     * Called when the physics object is supposed to add all objects it needs to
     * manage to the physics space.
     *
     * @param space which physics space
     */
    protected abstract void addPhysics(PhysicsSpace space);

    /**
     * Called when the physics object is supposed to remove all objects added to
     * the physics space.
     *
     * @param space which physics space
     */
    protected abstract void removePhysics(PhysicsSpace space);

    /**
     * Test whether physics coordinates should match the local transform of the
     * Spatial.
     *
     * @return true if matching local transform, false if matching world
     * transform
     */
    public boolean isApplyPhysicsLocal() {
        return applyLocal;
    }

    /**
     * Alter whether physics coordinates should match the local transform of the
     * Spatial
     *
     * @param applyPhysicsLocal true&rarr;match local transform,
     * false&rarr;match world transform
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        applyLocal = applyPhysicsLocal;
    }

    /**
     * Access whichever Spatial translation corresponds to the physics location.
     *
     * @return the pre-existing vector (not null)
     */
    protected Vector3f getSpatialTranslation() {
        if (MySpatial.isIgnoringTransforms(spatial)) {
            return translateIdentity;
        } else if (applyLocal) {
            return spatial.getLocalTranslation();
        } else {
            return spatial.getWorldTranslation();
        }
    }

    /**
     * Access whichever Spatial rotation is relevant to the physics rotation.
     *
     * @return the pre-existing quaternion (not null)
     */
    protected Quaternion getSpatialRotation() {
        if (MySpatial.isIgnoringTransforms(spatial)) {
            return rotateIdentity;
        } else if (applyLocal) {
            return spatial.getLocalRotation();
        } else {
            return spatial.getWorldRotation();
        }
    }

    /**
     * Apply a physics transform to the spatial
     *
     * @param worldLocation physics location (not null)
     * @param worldRotation physics orientation (not null)
     */
    protected void applyPhysicsTransform(Vector3f worldLocation, Quaternion worldRotation) {
        if (enabled && spatial != null) {
            Vector3f localLocation = spatial.getLocalTranslation();
            Quaternion localRotationQuat = spatial.getLocalRotation();
            if (!applyLocal && spatial.getParent() != null) {
                localLocation.set(worldLocation).subtractLocal(spatial.getParent().getWorldTranslation());
                localLocation.divideLocal(spatial.getParent().getWorldScale());
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);
                localRotationQuat.set(worldRotation);
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().mult(localRotationQuat, localRotationQuat);

                spatial.setLocalTranslation(localLocation);
                spatial.setLocalRotation(localRotationQuat);
            } else {
                spatial.setLocalTranslation(worldLocation);
                spatial.setLocalRotation(worldRotation);
            }
        }

    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.spatial = cloner.clone(spatial);
        createSpatialData(this.spatial);
    }

    /**
     * Alter which spatial is controlled.
     *
     * @param newSpatial spatial to control (or null)
     */
    @Override
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && this.spatial != spatial) {
            removeSpatialData(this.spatial);
        } else if (this.spatial == spatial) {
            return;
        }
        this.spatial = spatial;
        if (spatial == null) {
            return;
        }
        createSpatialData(this.spatial);
        setPhysicsLocation(getSpatialTranslation());
        setPhysicsRotation(getSpatialRotation());
    }

    /**
     * Enable or disable this control.
     * <p>
     * The physics object is removed from its physics space when the control is
     * disabled. When the control is enabled again, the physics object is moved
     * to the current location of the spatial and then added to the physics
     * space.
     *
     * @param enabled true&rarr;enable the control, false&rarr;disable it
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if (spatial != null) {
                    setPhysicsLocation(getSpatialTranslation());
                    setPhysicsRotation(getSpatialRotation());
                }
                addPhysics(space);
                added = true;
            } else if (!enabled && added) {
                removePhysics(space);
                added = false;
            }
        }
    }

    /**
     * Test whether this control is enabled.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Add this control's physics object to the specified physics space and
     * remove it from the space it's currently in.
     *
     * @param space where to add, or null to simply remove
     */
    @Override
    public void setPhysicsSpace(PhysicsSpace space) {
        if (space == null) {
            if (this.space != null) {
                removePhysics(this.space);
                added = false;
            }
        } else {
            if (this.space == space) {
                return;
            } else if (this.space != null) {
                removePhysics(this.space);
            }
            addPhysics(space);
            added = true;
        }
        this.space = space;
    }

    /**
     * Access the physics space containing this control's physics object.
     *
     * @return the pre-existing space, or null for none
     */
    @Override
    public PhysicsSpace getPhysicsSpace() {
        return space;
    }

    /**
     * Serialize this object, for example when saving to a J3O file.
     *
     * @param e exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(applyLocal, "applyLocalPhysics", false);
        oc.write(spatial, "spatial", null);
    }

    /**
     * De-serialize this control from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
        applyLocal = ic.readBoolean("applyLocalPhysics", false);
    }
}
