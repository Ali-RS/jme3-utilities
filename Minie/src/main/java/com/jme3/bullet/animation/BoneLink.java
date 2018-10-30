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
import com.jme3.export.Savable;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.MySkeleton;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Link an animated bone in a skeleton to a jointed rigid body in a ragdoll.
 *
 * @author Stephen Gold sgold@sonic.net
 *
 * Based on KinematicRagdollControl by Normen Hansen and Rémy Bouquet (Nehon).
 */
public class BoneLink extends PhysicsLink {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(BoneLink.class.getName());
    /**
     * local copy of {@link com.jme3.math.Matrix3f#IDENTITY}
     */
    final private static Matrix3f matrixIdentity = new Matrix3f();
    // *************************************************************************
    // fields

    /**
     * bones managed by this link, in a pre-order, depth-first traversal of the
     * skeleton, starting with the linked bone
     */
    private Bone[] managedBones = null;
    /**
     * submode when kinematic
     */
    private KinematicSubmode submode = KinematicSubmode.Animated;
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
    public BoneLink() {
    }

    /**
     * Instantiate a purely kinematic link between the named skeleton bone and
     * the specified rigid body.
     *
     * @param control the control that will manage this link (not null, alias
     * created)
     * @param bone the linked bone (not null, alias created)
     * @param rigidBody the rigid body to link (not null, alias created)
     * @param localOffset the location of the body's center (in the bone's local
     * coordinates, not null, unaffected)
     */
    BoneLink(DynamicAnimControl control, Bone bone, PhysicsRigidBody rigidBody,
            Vector3f localOffset) {
        super(control, bone, rigidBody, localOffset);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a physics joint to this link and configure its range of motion. Also
     * initialize the link's parent and its array of managed bones.
     *
     * @param parentLink (not null, alias created)
     */
    void addJoint(PhysicsLink parentLink) {
        assert parentLink != null;

        setParent(parentLink);

        Transform parentToWorld = parentLink.physicsTransform(null);
        parentToWorld.setScale(1f);
        Transform worldToParent = parentToWorld.invert();

        Transform childToWorld = physicsTransform(null);
        childToWorld.setScale(1f);

        Transform childToParent = childToWorld.clone();
        childToParent.combineWithParent(worldToParent);

        Spatial transformer = getControl().getTransformer();
        Vector3f pivotMesh = getBone().getModelSpacePosition();
        Vector3f pivotWorld = transformer.localToWorld(pivotMesh, null);

        PhysicsRigidBody parentBody = parentLink.getRigidBody();
        PhysicsRigidBody childBody = getRigidBody();
        Vector3f pivotParent
                = parentToWorld.transformInverseVector(pivotWorld, null);
        Vector3f pivotChild
                = childToWorld.transformInverseVector(pivotWorld, null);
        Matrix3f rotParent = childToParent.getRotation().toRotationMatrix();
        Matrix3f rotChild = matrixIdentity;
        // TODO try Point2PointJoint or HingeJoint
        SixDofJoint joint = new SixDofJoint(parentBody, childBody, pivotParent,
                pivotChild, rotParent, rotChild, true);
        super.setJoint(joint);

        String name = getBoneName();
        RangeOfMotion rangeOfMotion = getControl().getJointLimits(name);
        rangeOfMotion.setupJoint(joint, false, false, false);

        joint.setCollisionBetweenLinkedBodies(false);

        assert managedBones == null;
        managedBones = getControl().listManagedBones(name);

        int numManagedBones = managedBones.length;
        startBoneTransforms = new Transform[numManagedBones];
        for (int i = 0; i < numManagedBones; i++) {
            startBoneTransforms[i] = new Transform();
        }
    }

    /**
     * Begin blending this link to a fully kinematic mode.
     *
     * @param submode enum value (not null)
     * @param blendInterval the duration of the blend interval (in seconds,
     * &ge;0)
     */
    public void blendToKinematicMode(KinematicSubmode submode,
            float blendInterval) {
        Validate.nonNull(submode, "submode");
        Validate.nonNegative(blendInterval, "blend interval");

        super.blendToKinematicMode(blendInterval);

        this.submode = submode;
        /*
         * Save initial bone transforms for blending.
         */
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
     * Immediately put this link into dynamic mode and update its joint.
     *
     * @param uniformAcceleration the uniform acceleration vector (in
     * physics-space coordinates, not null, unaffected)
     * @param lockX true to lock the joint's X-axis
     * @param lockY true to lock the joint's Y-axis
     * @param lockZ true to lock the joint's Z-axis
     */
    public void setDynamic(Vector3f uniformAcceleration, boolean lockX,
            boolean lockY, boolean lockZ) {
        Validate.nonNull(uniformAcceleration, "uniform acceleration");

        super.setDynamic(uniformAcceleration);

        String name = getBoneName();
        RangeOfMotion preset = getControl().getJointLimits(name);
        preset.setupJoint((SixDofJoint) getJoint(), lockX, lockY, lockZ);

        for (Bone managedBone : managedBones) {
            managedBone.setUserControl(true);
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
        prevBoneTransforms = cloner.clone(prevBoneTransforms);
        startBoneTransforms = cloner.clone(startBoneTransforms);
    }

    /**
     * Update this link in Dynamic mode, setting the linked bone's transform
     * based on the transform of the rigid body.
     */
    @Override
    protected void dynamicUpdate() {
        assert !getRigidBody().isKinematic();

        Transform transform = localBoneTransform(null);
        MySkeleton.setLocalTransform(getBone(), transform);

        for (Bone managedBone : managedBones) {
            managedBone.updateModelTransforms();
        }
    }

    /**
     * Immediately freeze this link.
     */
    @Override
    public void freeze() {
        if (isKinematic()) {
            blendToKinematicMode(KinematicSubmode.Frozen, 0f);
        } else {
            setDynamic(new Vector3f(0f, 0f, 0f), true, true, true);
        }
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public BoneLink jmeClone() {
        try {
            BoneLink clone = (BoneLink) super.clone();
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
     * name and the same managed bones.
     *
     * @param oldLink the link to copy from (not null, unaffected)
     */
    void postRebuild(BoneLink oldLink) {
        int numManagedBones = managedBones.length;
        assert oldLink.managedBones.length == numManagedBones;

        super.postRebuild(oldLink);
        submode = oldLink.submode;

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
        prevBoneTransforms = RagUtils.readTransformArray(ic,
                "prevBoneTransforms");
        startBoneTransforms = RagUtils.readTransformArray(ic,
                "startBoneTransforms");
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
         * Convert to the bone's local coordinate system by factoring out the
         * parent bone's transform. TODO utility
         */
        Bone parentBone = getBone().getParent();
        Vector3f pmTranslate = parentBone.getModelSpacePosition();
        Quaternion pmRotInv = parentBone.getModelSpaceRotation().inverse();
        Vector3f pmScale = parentBone.getModelSpaceScale();
        location.subtractLocal(pmTranslate);
        location.divideLocal(pmScale);
        pmRotInv.mult(location, location);
        scale.divideLocal(pmScale);
        pmRotInv.mult(orientation, orientation);
        /*
         * Subtract the body's local offset, rotated and scaled.
         */
        Vector3f parentOffset = localOffset(null);
        parentOffset.multLocal(scale);
        orientation.mult(parentOffset, parentOffset);
        location.subtractLocal(parentOffset);

        return result;
    }
}
