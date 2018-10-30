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
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Link the torso of an animated model to a rigid body in a ragdoll.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class TorsoLink extends PhysicsLink {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(TorsoLink.class.getName());
    // *************************************************************************
    // fields

    /**
     * bones managed by this link, in a pre-order, depth-first traversal of the
     * skeleton
     */
    private Bone[] managedBones = null;
    /**
     * submode when kinematic
     */
    private KinematicSubmode submode = KinematicSubmode.Animated;
    /**
     * local transform for the controlled spatial at the end of this link's most
     * recent blend interval, or null for no spatial blending
     */
    private Transform endModelTransform = null;
    /**
     * transform from mesh coordinates to model coordinates
     */
    private Transform meshToModel = null;
    /**
     * local transform of the controlled spatial at the start of this link's
     * most recent blend interval
     */
    private Transform startModelTransform = new Transform();
    /**
     * local transform of each managed bone from the previous update
     */
    private Transform[] prevBoneTransforms = null;
    /**
     * local transform of each managed bone at the start of the most recent
     * blend interval
     */
    private Transform[] startBoneTransforms = null;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public TorsoLink() {
    }

    /**
     * Instantiate a purely kinematic link between the torso of the specified
     * control and the specified rigid body.
     *
     * @param control the control that will manage this link (not null, alias
     * created)
     * @param mainRootBone the root bone with the most animation weight (not
     * null, alias created)
     * @param rigidBody the rigid body to link (not null, alias created)
     * @param meshToModel the transform from mesh coordinates to model
     * coordinates (not null, unaffected)
     * @param localOffset the location of the body's center (in the bone's local
     * coordinates, not null, unaffected)
     */
    TorsoLink(DynamicAnimControl control, Bone mainRootBone,
            PhysicsRigidBody rigidBody, Transform meshToModel,
            Vector3f localOffset) {
        super(control, mainRootBone, rigidBody, localOffset);
        this.meshToModel = meshToModel.clone();
        managedBones = control.listManagedBones(DynamicAnimControl.torsoName);

        int numManagedBones = managedBones.length;
        startBoneTransforms = new Transform[numManagedBones];
        for (int i = 0; i < numManagedBones; i++) {
            startBoneTransforms[i] = new Transform();
        }
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Begin blending this link to a fully kinematic mode.
     *
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     * @param endModelTransform the desired local transform for the controlled
     * spatial when the blend completes or null for no change to local transform
     * (unaffected)
     */
    public void blendToKinematicMode(KinematicSubmode submode,
            float blendInterval, Transform endModelTransform) {
        Validate.nonNull(submode, "submode");
        Validate.nonNegative(blendInterval, "blend interval");

        super.blendToKinematicMode(blendInterval);

        this.submode = submode;
        this.endModelTransform = endModelTransform;
        /*
         * Save initial transforms for blending.
         */
        if (endModelTransform != null) {
            Transform current = getControl().getSpatial().getLocalTransform();
            startModelTransform.set(current);
        }
        int numManagedBones = managedBones.length;
        for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
            Transform transform;
            if (prevBoneTransforms == null) { // this link not updated yet
                Bone managedBone = managedBones[mbIndex];
                transform = MySkeleton.copyLocalTransform(managedBone, null);
            } else {
                transform = prevBoneTransforms[mbIndex];
            }
            startBoneTransforms[mbIndex].set(transform);
        }
        /*
         * Take or release control of the managed bones.
         */
        boolean wantUserControl;
        if (submode == KinematicSubmode.Animated) {
            wantUserControl = false;
        } else {
            wantUserControl = true;
        }
        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(wantUserControl);
        }
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

        managedBones = cloner.clone(managedBones);
        endModelTransform = cloner.clone(endModelTransform);
        meshToModel = cloner.clone(meshToModel);
        prevBoneTransforms = cloner.clone(prevBoneTransforms);
        startBoneTransforms = cloner.clone(startBoneTransforms);
        startModelTransform = cloner.clone(startModelTransform);
    }

    /**
     * Update this link in Dynamic mode, setting the local transform of the
     * model's root spatial based on the transform of the linked rigid body.
     */
    @Override
    protected void dynamicUpdate() {
        /*
         * Calculate the inverse world transform of the model's parent node.
         */
        Transform worldToParent;
        Node parent = getControl().getSpatial().getParent();
        if (parent == null) {
            worldToParent = new Transform();
        } else {
            Transform parentToWorld = MySpatial.worldTransform(parent, null);
            worldToParent = parentToWorld.invert();
        }

        Transform transform = meshToModel.clone();
        Transform shapeToWorld = getRigidBody().getPhysicsTransform(null);
        transform.combineWithParent(shapeToWorld);
        transform.combineWithParent(worldToParent);
        getControl().getSpatial().setLocalTransform(transform);

        localBoneTransform(transform);
        Bone[] rootBones = getControl().getSkeleton().getRoots();
        for (Bone rootBone : rootBones) {
            MySkeleton.setLocalTransform(rootBone, transform);
        }

        for (Bone managedBone : managedBones) {
            managedBone.updateModelTransforms();
        }
    }

    /**
     * Immediately freeze this link.
     */
    @Override
    public void freeze() {
        blendToKinematicMode(KinematicSubmode.Frozen, 0f, null);
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public TorsoLink jmeClone() {
        try {
            TorsoLink clone = (TorsoLink) super.clone();
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
            getControl().getSpatial().setLocalTransform(transform);
        }

        for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
            Bone managedBone = managedBones[mbIndex];
            switch (submode) {
                case Amputated:
                    MySkeleton.copyBindTransform(managedBone, transform);
                    transform.getScale().set(0.001f, 0.001f, 0.001f);
                    break;
                case Animated:
                    MySkeleton.copyLocalTransform(managedBone, transform);
                    break;
                case Bound:
                    MySkeleton.copyBindTransform(managedBone, transform);
                    break;
                case Frozen:
                    transform.set(prevBoneTransforms[mbIndex]);
                    break;
                default:
                    throw new IllegalStateException(submode.toString());
            }

            if (isKinematic()) {
                /*
                 * For a smooth transition, blend the saved bone transform
                 * (from the start of the blend interval)
                 * into the goal transform.
                 */
                Transform start = startBoneTransforms[mbIndex];
                Quaternion startQuat = start.getRotation();
                Quaternion endQuat = transform.getRotation();
                if (startQuat.dot(endQuat) < 0f) {
                    endQuat.multLocal(-1f);
                }
                MyMath.slerp(kinematicWeight(), startBoneTransforms[mbIndex],
                        transform, transform);
                // TODO smarter sign flipping for bones
            }
            /*
             * Update the managed bone.
             */
            MySkeleton.setLocalTransform(managedBone, transform);
            managedBone.updateModelTransforms();
            // The rigid-body transform gets updated by prePhysicsTick().
        }

        super.kinematicUpdate(tpf);
    }

    /**
     * Copy animation data from the specified link, which must have the same
     * main bone.
     *
     * @param oldLink the link to copy from (not null, unaffected)
     */
    void postRebuild(TorsoLink oldLink) {
        int numManagedBones = managedBones.length;
        assert oldLink.managedBones.length == numManagedBones;

        super.postRebuild(oldLink);
        submode = oldLink.submode;
        endModelTransform
                = (Transform) Misc.deepCopy(oldLink.endModelTransform);
        startModelTransform.set(oldLink.startModelTransform);

        if (prevBoneTransforms == null) {
            prevBoneTransforms = new Transform[numManagedBones];
            for (int i = 0; i < numManagedBones; i++) {
                prevBoneTransforms[i] = new Transform();
            }
        }
        for (int i = 0; i < numManagedBones; i++) {
            prevBoneTransforms[i].set(oldLink.prevBoneTransforms[i]);
            startBoneTransforms[i].set(oldLink.startBoneTransforms[i]);
        }
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

        Savable[] tmp = ic.readSavableArray("managedBones", null);
        if (tmp == null) {
            managedBones = null;
        } else {
            managedBones = new Bone[tmp.length];
            for (int i = 0; i < tmp.length; i++) {
                managedBones[i] = (Bone) tmp[i];
            }
        }

        submode = ic.readEnum("submode", KinematicSubmode.class,
                KinematicSubmode.Animated);
        endModelTransform = (Transform) ic.readSavable("endModelTransform",
                new Transform());
        meshToModel
                = (Transform) ic.readSavable("meshToModel", new Transform());
        startModelTransform = (Transform) ic.readSavable("startModelTransform",
                new Transform());
        prevBoneTransforms = RagUtils.readTransformArray(ic,
                "prevBoneTransforms");
        startBoneTransforms = RagUtils.readTransformArray(ic,
                "startBoneTransforms");
    }

    /**
     * Immediately put this link into dynamic mode.
     *
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     */
    @Override
    public void setDynamic(Vector3f uniformAcceleration) {
        Validate.nonNull(uniformAcceleration, "uniform acceleration");

        super.setDynamic(uniformAcceleration);

        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(true);
        }
    }

    /**
     * Internal callback, invoked once per frame during the logical-state
     * update, provided the control is added to a scene.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    void update(float tpf) {
        assert tpf >= 0f : tpf;

        if (prevBoneTransforms == null) {
            /*
             * On the first update, allocate and initialize
             * the array of previous bone transforms, if it wasn't
             * allocated in blendToKinematicMode().
             */
            int numManagedBones = managedBones.length;
            prevBoneTransforms = new Transform[numManagedBones];
            for (int mbIndex = 0; mbIndex < numManagedBones; mbIndex++) {
                Bone managedBone = managedBones[mbIndex];
                Transform boneTransform
                        = MySkeleton.copyLocalTransform(managedBone, null);
                prevBoneTransforms[mbIndex] = boneTransform;
            }
        }

        super.update(tpf);
        /*
         * Save copies of the latest bone transforms.
         */
        for (int mbIndex = 0; mbIndex < managedBones.length; mbIndex++) {
            Transform lastTransform = prevBoneTransforms[mbIndex];
            Bone managedBone = managedBones[mbIndex];
            MySkeleton.copyLocalTransform(managedBone, lastTransform);
        }
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

        oc.write(managedBones, "managedBones", null);
        oc.write(submode, "submode", KinematicSubmode.Animated);
        oc.write(endModelTransform, "endModelTransforms", new Transform());
        oc.write(meshToModel, "meshToModel", new Transform());
        oc.write(startModelTransform, "startModelTransforms", new Transform());
        oc.write(prevBoneTransforms, "prevBoneTransforms", new Transform[0]);
        oc.write(startBoneTransforms, "startBoneTransforms", new Transform[0]);
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the local bone transform to match the physics transform of the
     * rigid body.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the calculated bone transform (in local coordinates, either
     * storeResult or a new transform, not null)
     */
    private Transform localBoneTransform(Transform storeResult) {
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
        /*
         * Subtract the body's local offset, rotated and scaled.
         */
        Vector3f meshOffset = localOffset(null);
        meshOffset.multLocal(scale);
        orientation.mult(meshOffset, meshOffset);
        location.subtractLocal(meshOffset);

        return result;
    }
}
