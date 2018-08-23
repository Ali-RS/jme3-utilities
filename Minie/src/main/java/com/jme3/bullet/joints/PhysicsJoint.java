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
package com.jme3.bullet.joints;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.*;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract physics joint
 *
 * @author normenhansen
 */
public abstract class PhysicsJoint implements Savable {

    protected long objectId = 0;
    protected PhysicsRigidBody nodeA;
    protected PhysicsRigidBody nodeB;
    protected Vector3f pivotA;
    protected Vector3f pivotB;
    protected boolean collisionBetweenLinkedBodies = true;

    /**
     * No-argument constructor for serialization purposes only. Do not invoke
     * directly!
     */
    public PhysicsJoint() {
    }

    /**
     * Create a new PhysicsJoint. To be effective, the joint must be added to a
     * physics space.
     *
     * @param nodeA the 1st body connected by the joint (alias created)
     * @param nodeB the 2nd body connected by the joint (alias created)
     * @param pivotA local offset of the joint connection point in node A (alias
     * created)
     * @param pivotB local offset of the joint connection point in node B (alias
     * created)
     */
    public PhysicsJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.pivotA = pivotA;
        this.pivotB = pivotB;
        nodeA.addJoint(this);
        nodeB.addJoint(this);
    }

    public float getAppliedImpulse() {
        return getAppliedImpulse(objectId);
    }

    private native float getAppliedImpulse(long objectId);

    /**
     * @return unique constraint id
     */
    public long getObjectId() {
        return objectId;
    }

    /**
     * Test whether collisions are allowed between the linked bodies.
     *
     * @return true if collision are allowed, otherwise false
     */
    public boolean isCollisionBetweenLinkedBodys() {
        return collisionBetweenLinkedBodies;
    }

    /**
     * Enable or disable collisions between the linked bodies. The joint must be
     * removed from and added to PhysicsSpace for this to be effective.
     *
     * @param enable true &rarr; allow collisions, false &rarr; prevent them
     */
    public void setCollisionBetweenLinkedBodys(boolean enable) {
        this.collisionBetweenLinkedBodies = enable;
    }

    /**
     * Access the 1st body specified in during construction.
     *
     * @return the pre-existing body
     */
    public PhysicsRigidBody getBodyA() {
        return nodeA;
    }

    /**
     * Access the 2nd body specified in during construction.
     *
     * @return the pre-existing body
     */
    public PhysicsRigidBody getBodyB() {
        return nodeB;
    }

    /**
     * Access the local offset of the joint connection point in node A.
     *
     * @return the pre-existing vector
     */
    public Vector3f getPivotA() {
        return pivotA;
    }

    /**
     * Access the local offset of the joint connection point in node A.
     *
     * @return the pre-existing vector
     */
    public Vector3f getPivotB() {
        return pivotB;
    }

    /**
     * Destroy this joint and remove it from the joint lists of its connected
     * bodies.
     */
    public void destroy() {
        getBodyA().removeJoint(this);
        getBodyB().removeJoint(this);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(nodeA, "nodeA", null);
        capsule.write(nodeB, "nodeB", null);
        capsule.write(pivotA, "pivotA", null);
        capsule.write(pivotB, "pivotB", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        this.nodeA = (PhysicsRigidBody) capsule.readSavable("nodeA", new PhysicsRigidBody());
        this.nodeB = (PhysicsRigidBody) capsule.readSavable("nodeB", new PhysicsRigidBody());
        this.pivotA = (Vector3f) capsule.readSavable("pivotA", new Vector3f());
        this.pivotB = (Vector3f) capsule.readSavable("pivotB", new Vector3f());
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing Joint {0}", Long.toHexString(objectId));
        finalizeNative(objectId);
    }

    private native void finalizeNative(long objectId);
}
