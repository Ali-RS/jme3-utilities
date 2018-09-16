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
package com.jme3.bullet.objects;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collision object for intangibles, based on Bullet's
 * btPairCachingGhostObject.
 * <p>
 * <i>From Bullet manual:</i><br>
 * GhostObject can keep track of all objects that are overlapping. By default,
 * this overlap is based on the AABB. This is useful for creating a character
 * controller, collision sensors/triggers, explosions etc.
 *
 * @author normenhansen
 */
public class PhysicsGhostObject extends PhysicsCollisionObject {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(PhysicsGhostObject.class.getName());

    final private List<PhysicsCollisionObject> overlappingObjects
            = new LinkedList<>();

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsGhostObject() {
    }

    /**
     * Instantiate an object with the specified collision shape.
     *
     * @param shape the desired shape (not null, alias created)
     */
    public PhysicsGhostObject(CollisionShape shape) {
        collisionShape = shape;
        buildObject();
    }

    /**
     * Create the configured object in Bullet.
     */
    private void buildObject() {
        if (objectId == 0L) {
            objectId = createGhostObject();
            logger.log(Level.FINE, "Created Ghost Object {0}",
                    Long.toHexString(objectId));
            setGhostFlags(objectId);
            initUserPointer();
        }
        attachCollisionShape(objectId, collisionShape.getObjectId());
    }

    private native long createGhostObject();

    private native void setGhostFlags(long objectId);

    /**
     * Apply the specified CollisionShape to this object. Note that the object
     * should not be in any physics space while changing shape; the object gets
     * rebuilt on the physics side.
     *
     * @param collisionShape the shape to apply (not null, alias created)
     */
    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        super.setCollisionShape(collisionShape);
        if (objectId == 0) {
            buildObject();
        } else {
            attachCollisionShape(objectId, collisionShape.getObjectId());
        }
    }

    /**
     * Directly alter the location of this object's center.
     *
     * @param location the desired location (not null, unaffected)
     */
    public void setPhysicsLocation(Vector3f location) {
        setPhysicsLocation(objectId, location);
    }

    private native void setPhysicsLocation(long objectId, Vector3f location);

    /**
     * Directly alter this object's orientation.
     *
     * @param rotation the desired orientation (rotation matrix, not null,
     * unaffected)
     */
    public void setPhysicsRotation(Matrix3f rotation) {
        setPhysicsRotation(objectId, rotation);
    }

    private native void setPhysicsRotation(long objectId, Matrix3f rotation);

    /**
     * Directly alter this object's orientation.
     *
     * @param rotation the desired orientation (quaternion, not null,
     * unaffected)
     */
    public void setPhysicsRotation(Quaternion rotation) {
        setPhysicsRotation(objectId, rotation);
    }

    private native void setPhysicsRotation(long objectId, Quaternion rotation);

    /**
     * Copy the location of this object's center.
     *
     * @param trans storage for the result (modified if not null)
     * @return the physics location (either the provided storage or a new
     * vector, not null)
     */
    public Vector3f getPhysicsLocation(Vector3f trans) {
        if (trans == null) {
            trans = new Vector3f();
        }
        getPhysicsLocation(objectId, trans);
        return trans;
    }

    private native void getPhysicsLocation(long objectId, Vector3f vector);

    /**
     * Copy this object's orientation to a quaternion.
     *
     * @param rot storage for the result (modified if not null)
     * @return the physics orientation (either the provided storage or a new
     * quaternion, not null)
     */
    public Quaternion getPhysicsRotation(Quaternion rot) {
        if (rot == null) {
            rot = new Quaternion();
        }
        getPhysicsRotation(objectId, rot);
        return rot;
    }

    private native void getPhysicsRotation(long objectId, Quaternion rot);

    /**
     * Copy this object's orientation to a matrix.
     *
     * @param rot storage for the result (modified if not null)
     * @return the orientation (either the provided storage or a new matrix, not
     * null)
     */
    public Matrix3f getPhysicsRotationMatrix(Matrix3f rot) {
        if (rot == null) {
            rot = new Matrix3f();
        }
        getPhysicsRotationMatrix(objectId, rot);
        return rot;
    }

    private native void getPhysicsRotationMatrix(long objectId, Matrix3f rot);

    /**
     * Copy the location of this object's center.
     *
     * @return a new location vector (not null)
     */
    public Vector3f getPhysicsLocation() {
        Vector3f vec = new Vector3f();
        getPhysicsLocation(objectId, vec);

        return vec;
    }

    /**
     * Copy this object's orientation to a quaternion.
     *
     * @return a new quaternion (not null)
     */
    public Quaternion getPhysicsRotation() {
        Quaternion quat = new Quaternion();
        getPhysicsRotation(objectId, quat);

        return quat;
    }

    /**
     * Copy this object's orientation to a matrix.
     *
     * @return a new matrix (not null)
     */
    public Matrix3f getPhysicsRotationMatrix() {
        Matrix3f mtx = new Matrix3f();
        getPhysicsRotationMatrix(objectId, mtx);

        return mtx;
    }

    /**
     * Destroy this object and remove it from memory. (Has no effect.) TODO
     * remove
     */
    public void destroy() {
    }

    /**
     * Access a list of overlapping objects.
     * <p>
     * Another object overlaps with this one if and if only their
     * CollisionShapes overlap.
     *
     * @return an internal list which may get reused (not null)
     */
    public List<PhysicsCollisionObject> getOverlappingObjects() {
        overlappingObjects.clear();
        getOverlappingObjects(objectId);
//        for (com.bulletphysics.collision.dispatch.CollisionObject collObj : gObject.getOverlappingPairs()) {
//            overlappingObjects.add((PhysicsCollisionObject) collObj.getUserPointer());
//        }
        return overlappingObjects;
    }

    native private void getOverlappingObjects(long objectId);

    /**
     * This method is invoked from native code.
     *
     * @param co the collision object to add
     */
    private void addOverlappingObject_native(PhysicsCollisionObject co) {
        overlappingObjects.add(co);
    }

    /**
     * Count how many CollisionObjects this object overlaps.
     *
     * @return count (&ge;0)
     */
    public int getOverlappingCount() {
        return getOverlappingCount(objectId);
    }

    private native int getOverlappingCount(long objectId);

    /**
     * Access an overlapping collision object by its position in the list.
     *
     * @param index which list position (&ge;0, &lt;count)
     * @return the pre-existing object
     */
    public PhysicsCollisionObject getOverlapping(int index) {
        return overlappingObjects.get(index);
    }

    /**
     * Alter the continuous collision detection (CCD) swept sphere radius for
     * this object.
     *
     * @param radius (&ge;0)
     */
    public void setCcdSweptSphereRadius(float radius) {
        setCcdSweptSphereRadius(objectId, radius);
    }

    private native void setCcdSweptSphereRadius(long objectId, float radius);

    /**
     * Alter the amount of motion required to trigger continuous collision
     * detection (CCD).
     * <p>
     * This addresses the issue of fast objects passing through other objects
     * with no collision detected.
     *
     * @param threshold the desired threshold value (squared velocity, &gt;0) or
     * zero to disable CCD (default=0)
     */
    public void setCcdMotionThreshold(float threshold) {
        setCcdMotionThreshold(objectId, threshold);
    }

    private native void setCcdMotionThreshold(long objectId, float threshold);

    /**
     * Read the radius of the sphere used for continuous collision detection
     * (CCD).
     *
     * @return radius (&ge;0)
     */
    public float getCcdSweptSphereRadius() {
        return getCcdSweptSphereRadius(objectId);
    }

    private native float getCcdSweptSphereRadius(long objectId);

    /**
     * Read the continuous collision detection (CCD) motion threshold for this
     * object.
     *
     * @return threshold value (squared velocity, &ge;0)
     */
    public float getCcdMotionThreshold() {
        return getCcdMotionThreshold(objectId);
    }

    private native float getCcdMotionThreshold(long objectId);

    /**
     * Read the CCD square motion threshold for this object.
     *
     * @return threshold value (&ge;0)
     */
    public float getCcdSquareMotionThreshold() {
        return getCcdSquareMotionThreshold(objectId);
    }

    private native float getCcdSquareMotionThreshold(long objectId);

    /**
     * Serialize this object, for example when saving to a J3O file.
     *
     * @param e exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(getPhysicsLocation(new Vector3f()),
                "physicsLocation", new Vector3f());
        capsule.write(getPhysicsRotationMatrix(new Matrix3f()),
                "physicsRotation", new Matrix3f());
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0f);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0f);
    }

    /**
     * De-serialize this object from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        buildObject();
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation",
                new Vector3f()));
        setPhysicsRotation(((Matrix3f) capsule.readSavable("physicsRotation",
                new Matrix3f())));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0f));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0f));
    }
}
