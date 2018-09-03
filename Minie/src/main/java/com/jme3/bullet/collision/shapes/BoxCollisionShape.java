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
 * A rectangular-solid collision shape based on Bullet's btBoxShape.
 *
 * @author normenhansen
 */
public class BoxCollisionShape extends CollisionShape {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BoxCollisionShape.class.getName());

    /**
     * half-extents of the box on each local axis (not null, no negative
     * component)
     */
    private Vector3f halfExtents;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public BoxCollisionShape() {
    }

    /**
     * Instantiate a box shape with the specified half extents.
     *
     * @param halfExtents the half extents to use (not null, no negative
     * component, alias created) TODO
     */
    public BoxCollisionShape(Vector3f halfExtents) {
        Validate.nonNegative(halfExtents, "half extents");

        this.halfExtents = halfExtents;
        createShape();
    }

    /**
     * Access the half extents.
     *
     * @return the pre-existing instance (not null, no negative component) TODO
     */
    public final Vector3f getHalfExtents() {
        assert MyVector3f.isAllNonNegative(halfExtents) : halfExtents;
        return halfExtents;
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
        capsule.write(halfExtents, "halfExtents", new Vector3f(1f, 1f, 1f));
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
        Vector3f he = (Vector3f) capsule.readSavable("halfExtents",
                new Vector3f(1f, 1f, 1f));
        this.halfExtents = he;
        createShape();
    }

    /**
     * Create the configured shape in Bullet.
     */
    private void createShape() {
        assert MyVector3f.isAllNonNegative(halfExtents) : halfExtents;

        objectId = createShape(halfExtents);
        assert objectId != 0L;
        logger.log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(Vector3f halfExtents);
}
