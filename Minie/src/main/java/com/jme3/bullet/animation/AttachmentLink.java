/*
 * Copyright (c) 2018 jMonkeyEngine
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
package com.jme3.bullet.animation;

import com.jme3.animation.Bone;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySkeleton;
import jme3utilities.math.MyMath;

/**
 * Link an attachments node to a jointed rigid body in a ragdoll.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class AttachmentLink extends PhysicsLink {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(AttachmentLink.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix3f#IDENTITY}
     */
    final private static Matrix3f matrixIdentity = new Matrix3f();
    /**
     * local copy of {@link com.jme3.math.Quaternion#IDENTITY}
     */
    final private static Quaternion rotateIdentity = new Quaternion();
    // *************************************************************************
    // fields

    /**
     * attached model (not null)
     */
    private Spatial attachedModel;
    /**
     * local transform for the attached model at the end of this link's most
     * recent blend interval, or null for no spatial blending
     */
    private Transform endModelTransform = null;
    /**
     * local transform of the attached model at the start of this link's most
     * recent blend interval
     */
    private Transform startModelTransform = new Transform();
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public AttachmentLink() {
    }

    /**
     * Instantiate a purely kinematic link between the specified model and the
     * specified rigid body.
     *
     * @param control the control that will manage this link (not null, alias
     * created)
     * @param associatedBone the bone associated with the attachment node (not
     * null, alias created)
     * @param manager the bone/torso link that manages the associated bone (not
     * null, alias created)
     * @param attachModel the attached model to link (not null, alias created)
     * @param rigidBody the rigid body to link (not null, alias created)
     * @param localOffset the location of the body's center (in the attached
     * model's local coordinates, not null, unaffected)
     */
    AttachmentLink(DynamicAnimControl control, Bone associatedBone,
            PhysicsLink manager, Spatial attachModel,
            PhysicsRigidBody rigidBody, Vector3f localOffset) {
        super(control, associatedBone, rigidBody, localOffset);
        assert manager != null;
        assert attachModel != null;

        this.attachedModel = attachModel;
        setParent(manager);

        PhysicsRigidBody managerBody = manager.getRigidBody();
        Transform managerToWorld = manager.physicsTransform(null);
        Transform worldToManager = managerToWorld.invert();

        Transform attachToWorld = physicsTransform(null);
        Transform attachToManager = attachToWorld.clone();
        attachToManager.combineWithParent(worldToManager);

        Vector3f pivotMesh = getBone().getModelSpacePosition();
        Spatial transformer = control.getTransformer();
        Vector3f pivotWorld = transformer.localToWorld(pivotMesh, null);
        managerToWorld.setScale(1f);
        Vector3f pivotManager
                = managerToWorld.transformInverseVector(pivotWorld, null);
        attachToWorld.setScale(1f);
        Vector3f pivot = attachToWorld.transformInverseVector(pivotWorld, null);

        Matrix3f rotManager = attachToManager.getRotation().toRotationMatrix();
        Matrix3f rot = matrixIdentity;

        SixDofJoint joint = new SixDofJoint(managerBody, rigidBody,
                pivotManager, pivot, rotManager, rot, true);
        setJoint(joint);

        RangeOfMotion rangeOfMotion = new RangeOfMotion();
        rangeOfMotion.setupJoint(joint, false, false, false);

        joint.setCollisionBetweenLinkedBodies(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin blending this link to a fully kinematic mode.
     *
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endModelTransform the desired local transform for the attached
     * model the blend completes or null for no change to local transform
     * (unaffected)
     */
    void blendToKinematicMode(float blendInterval,
            Transform endModelTransform) {
        assert blendInterval >= 0f : blendInterval;

        super.blendToKinematicMode(blendInterval);
        this.endModelTransform = endModelTransform;
        /*
         * Save initial transform for blending.
         */
        if (endModelTransform != null) {
            Transform current = attachedModel.getLocalTransform();
            startModelTransform.set(current);
        }
    }

    /**
     * Access the attached model (not the attachment node).
     *
     * @return the pre-existing instance (not null)
     */
    public Spatial getAttachedModel() {
        assert attachedModel != null;
        return attachedModel;
    }
    // *************************************************************************
    // PhysicsLink methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned link into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this link (not null)
     * @param original the instance from which this link was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        attachedModel = cloner.clone(attachedModel);
        endModelTransform = cloner.clone(endModelTransform);
        startModelTransform = cloner.clone(startModelTransform);
    }

    /**
     * Update this link in Dynamic mode, setting the local transform of the
     * attached model based on the transform of the linked rigid body.
     */
    @Override
    protected void dynamicUpdate() {
        assert !getRigidBody().isKinematic();

        Transform transform = localModelTransform(null);
        attachedModel.setLocalTransform(transform);
    }

    /**
     * Immediately freeze this link.
     */
    @Override
    void freeze() {
        if (isKinematic()) {
            blendToKinematicMode(0f, null);
        } else {
            setDynamic(new Vector3f(0f, 0f, 0f));
        }
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public AttachmentLink jmeClone() {
        try {
            AttachmentLink clone = (AttachmentLink) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Update this link in blended Kinematic mode.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    protected void kinematicUpdate(float tpf) {
        assert tpf >= 0f : tpf;
        assert getRigidBody().isKinematic();

        Transform transform = new Transform();

        if (endModelTransform != null && isKinematic()) {
            /*
             * For a smooth transition, blend the saved model transform
             * (from the start of the blend interval) into the goal transform.
             */
            Quaternion startQuat = startModelTransform.getRotation();
            Quaternion endQuat = endModelTransform.getRotation();
            if (startQuat.dot(endQuat) < 0f) {
                endQuat.multLocal(-1f);
            }
            MyMath.slerp(kinematicWeight(), startModelTransform,
                    endModelTransform, transform);
            attachedModel.setLocalTransform(transform);
        }
        // The rigid-body transform gets updated by prePhysicsTick().

        super.kinematicUpdate(tpf);
    }

    /**
     * Calculate a physics transform for the rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated transform (in physics coordinates, either
     * storeResult or a new transform, not null)
     */
    @Override
    final public Transform physicsTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        /*
         * Start with the rigid body's transform in the attached model's
         * local coordinates.
         */
        result.setTranslation(localOffset(null));
        result.setRotation(rotateIdentity);
        result.setScale(1f);
        /*
         * Convert to bone local coordinates.
         */
        Transform tmp = attachedModel.getLocalTransform();
        result.combineWithParent(tmp);
        /*
         * Convert to mesh coordinates.
         */
        tmp = MySkeleton.copyMeshTransform(getBone(), null);
        result.combineWithParent(tmp);
        /*
         * Convert to physics/world coordinates.
         */
        getControl().meshTransform(tmp);
        result.combineWithParent(tmp);

        return result;
    }

    /**
     * Copy animation data from the specified link, which must correspond to the
     * same bone.
     *
     * @param oldLink the link to copy from (not null, unaffected)
     */
    void postRebuild(AttachmentLink oldLink) {
        assert oldLink != null;

        super.postRebuild(oldLink);
        endModelTransform
                = (Transform) Misc.deepCopy(oldLink.endModelTransform);
        startModelTransform.set(oldLink.startModelTransform);
    }

    /**
     * De-serialize this link, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        attachedModel = (Spatial) ic.readSavable("attachedModel", null);
        endModelTransform = (Transform) ic.readSavable("endModelTransform",
                new Transform());
        startModelTransform = (Transform) ic.readSavable("startModelTransform",
                new Transform());
    }

    /**
     * Serialize this link, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        oc.write(attachedModel, "attachedModel", null);
        oc.write(endModelTransform, "endModelTransform", null);
        oc.write(startModelTransform, "startModelTransform", null);
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the local transform for the attached model to match the physics
     * transform of the rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated model transform (in local coordinates, either
     * storeResult or a new transform, not null)
     */
    private Transform localModelTransform(Transform storeResult) {
        Transform result
                = (storeResult == null) ? new Transform() : storeResult;
        Vector3f location = result.getTranslation();
        Quaternion orientation = result.getRotation();
        Vector3f scale = result.getScale();
        /*
         * Start with the rigid body's transform in physics/world coordinates.
         */
        getRigidBody().getPhysicsTransform(result);
        /*
         * Convert to mesh coordinates.
         */
        Transform worldToMesh = getControl().meshTransform(null).invert();
        result.combineWithParent(worldToMesh);
        /**
         * Convert to bone local coordinates.
         */
        Transform meshToBone
                = MySkeleton.copyMeshTransform(getBone(), null).invert();
        result.combineWithParent(meshToBone);
        /*
         * Subtract the body's local offset, rotated and scaled.
         */
        Vector3f modelOffset = localOffset(null);
        modelOffset.multLocal(scale);
        orientation.mult(modelOffset, modelOffset);
        location.subtractLocal(modelOffset);

        return result;
    }
}