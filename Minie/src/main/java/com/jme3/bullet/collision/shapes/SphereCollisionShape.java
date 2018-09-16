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
package com.jme3.bullet.collision.shapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A spherical collision shape based on Bullet's btSphereShape. TODO also
 * provide a shape based on btMultiSphereShape
 *
 * @author normenhansen
 */
public class SphereCollisionShape extends CollisionShape {

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(SphereCollisionShape.class.getName());

    /**
     * copy of radius (&ge;0)
     */
    private float radius;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public SphereCollisionShape() {
    }

    /**
     * Instantiate a sphere shape with the specified radius.
     *
     * @param radius the desired radius (&ge;0)
     */
    public SphereCollisionShape(float radius) {
        this.radius = radius;
        createShape();
    }

    /**
     * Read the radius of this shape.
     *
     * @return the radius (&ge;0)
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Alter the collision margin for this shape. This feature is disabled for
     * sphere shapes.
     *
     * @param margin the desired margin distance (in physics-space units)
     */
    @Override
    public void setMargin(float margin) {
    }

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(radius, "radius", 0.5f);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        radius = capsule.readFloat("radius", 0.5f);
        createShape();
    }

    /**
     * Alter the scaling factors of this shape. Non-uniform scaling is disabled
     * for sphere shapes.
     * <p>
     * Note that if the shape is shared (between collision objects and/or
     * compound shapes) changes can have unexpected consequences.
     *
     * @param scale the desired scaling factor for each local axis (not null, no
     * negative component, unaffected, default=1,1,1)
     */
    @Override
    public void setScale(Vector3f scale) {
        Validate.nonNegative(scale, "scale");

        if (MyVector3f.isScaleUniform(scale)) {
            super.setScale(scale);
        } else {
            logger.log(Level.SEVERE,
                    "SphereCollisionShape cannot be scaled non-uniformly.");
        }
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    private void createShape() {
        objectId = createShape(radius);
        logger.log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale); // Set the scale to 1,1,1
    }

    private native long createShape(float radius);
}
