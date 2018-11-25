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
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A 6 degree-of-freedom joint based on Bullet's btGeneric6DofSpringConstraint.
 * <p>
 * <i>From the Bullet manual:</i><br>
 * This generic constraint can emulate a variety of standard constraints, by
 * configuring each of the 6 degrees of freedom (dof). The first 3 dof axis are
 * linear axis, which represent translation of rigidbodies, and the latter 3 dof
 * axis represent the angular motion. Each axis can be either locked, free or
 * limited. On construction of a new btGeneric6DofSpring2Constraint, all axis
 * are locked. Afterwards the axis can be reconfigured. Note that several
 * combinations that include free and/or limited angular degrees of freedom are
 * undefined.
 * <p>
 * For each axis:<ul>
 * <li>Lowerlimit = Upperlimit &rarr; axis is locked</li>
 * <li>Lowerlimit &gt; Upperlimit &rarr; axis is free</li>
 * <li>Lowerlimit &lt; Upperlimit &rarr; axis it limited in that range</li>
 * </ul>
 * <p>
 * TODO try btGeneric6DofSpring2Constraint
 *
 * @author normenhansen
 */
public class SixDofSpringJoint extends SixDofJoint {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger3
            = Logger.getLogger(SixDofSpringJoint.class.getName());
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public SixDofSpringJoint() {
    }

    /**
     * Instantiate a double-ended SixDofSpringJoint. To be effective, the joint
     * must be added to the physics space of the 2 bodies. Also, the bodies must
     * be dynamic and distinct.
     *
     * @param nodeA the 1st body to constrain (not null, alias created)
     * @param nodeB the 2nd body to constrain (not null, alias created)
     * @param pivotA the pivot location in A's scaled local coordinates (not
     * null, unaffected)
     * @param pivotB the pivot location in B's scaled local coordinates (not
     * null, unaffected)
     * @param rotA the local orientation of the connection to node A (not null,
     * unaffected)
     * @param rotB the local orientation of the connection to node B (not null,
     * unaffected)
     * @param useLinearReferenceFrameA true&rarr;use node A, false&rarr;use node
     * B
     */
    public SixDofSpringJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB,
            Vector3f pivotA, Vector3f pivotB, Matrix3f rotA, Matrix3f rotB,
            boolean useLinearReferenceFrameA) {
        super(nodeA, nodeB, pivotA, pivotB, rotA, rotB,
                useLinearReferenceFrameA);
    }
    // *************************************************************************
    // new methods exposed TODO re-order methods

    /**
     * Enable or disable the spring for the indexed degree of freedom.
     *
     * @param dofIndex which degree of freedom (&ge;0, &lt;6)
     * @param onOff true &rarr; enable, false &rarr; disable
     */
    public void enableSpring(int dofIndex, boolean onOff) {
        Validate.inRange(dofIndex, "DOF index", 0, 5);
        enableSpring(objectId, dofIndex, onOff);
    }

    /**
     * Alter the spring stiffness for the indexed degree of freedom.
     *
     * @param dofIndex which degree of freedom (&ge;0, &lt;6)
     * @param stiffness the desired stiffness
     */
    public void setStiffness(int dofIndex, float stiffness) {
        Validate.inRange(dofIndex, "DOF index", 0, 5);
        setStiffness(objectId, dofIndex, stiffness);
    }

    /**
     * Alter the damping for the indexed degree of freedom.
     *
     * @param dofIndex which degree of freedom (&ge;0, &lt;6)
     * @param damping the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=1)
     */
    public void setDamping(int dofIndex, float damping) {
        Validate.inRange(dofIndex, "DOF index", 0, 5);
        setDamping(objectId, dofIndex, damping);
    }

    /**
     * Alter the equilibrium points for all degrees of freedom, based on the
     * joint's current location/orientation.
     */
    public void setEquilibriumPoint() {
        setEquilibriumPoint(objectId);
    }

    /**
     * Alter the equilibrium point of the indexed degree of freedom, based on
     * the joint's current location/orientation.
     *
     * @param dofIndex which degree of freedom (&ge;0, &lt;6)
     */
    public void setEquilibriumPoint(int dofIndex) {
        Validate.inRange(dofIndex, "DOF index", 0, 5);
        setEquilibriumPoint(objectId, dofIndex);
    }
    // *************************************************************************
    // SixDofJoint methods

    @Override
    native protected long createJoint(long objectIdA, long objectIdB,
            Vector3f pivotInA, Matrix3f rotInA, Vector3f pivotInB,
            Matrix3f rotInB, boolean useLinearReferenceFrameA);
    // *************************************************************************
    // private methods

    native private void enableSpring(long objectId, int index, boolean onOff);

    native private void setDamping(long objectId, int index, float damping);

    native private void setEquilibriumPoint(long objectId);

    native private void setEquilibriumPoint(long objectId, int index);

    native private void setStiffness(long objectId, int index, float stiffness);
}
