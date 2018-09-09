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

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.infos.VehicleTuning;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collision object for simplified vehicle simulation based on Bullet's
 * btRaycastVehicle.
 * <p>
 * <i>From Bullet manual:</i><br>
 * For most vehicle simulations, it is recommended to use the simplified Bullet
 * vehicle model as provided in btRaycastVehicle. Instead of simulation each
 * wheel and chassis as separate rigid bodies, connected by constraints, it uses
 * a simplified model. This simplified model has many benefits, and is widely
 * used in commercial driving games.<br>
 * The entire vehicle is represented as a single rigid body, the chassis. The
 * collision detection of the wheels is approximated by ray casts, and the tire
 * friction is a basic anisotropic friction model.
 *
 * @author normenhansen
 */
public class PhysicsVehicle extends PhysicsRigidBody {

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PhysicsVehicle.class.getName());

    /**
     * Unique identifier of the btRaycastVehicle. The constructor sets this to a
     * non-zero value.
     */
    private long vehicleId = 0L;
    /**
     * Unique identifier of the ray caster.
     */
    private long rayCasterId = 0L;
    /**
     * tuning parameters
     */
    protected VehicleTuning tuning = new VehicleTuning();
    /**
     * list of wheels
     */
    protected ArrayList<VehicleWheel> wheels = new ArrayList<>();
    /**
     * physics space where this vehicle is added, or null if none
     */
    private PhysicsSpace physicsSpace;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public PhysicsVehicle() {
    }

    /**
     * Instantiate a vehicle with the specified collision shape and mass=1.
     *
     * @param shape the desired shape (not null, alias created)
     */
    public PhysicsVehicle(CollisionShape shape) {
        super(shape);
    }

    /**
     * Instantiate a vehicle with the specified collision shape and mass.
     *
     * @param shape the desired shape (not null, alias created)
     * @param mass (&gt;0)
     */
    public PhysicsVehicle(CollisionShape shape, float mass) {
        super(shape, mass);
    }

    /**
     * used internally
     */
    public void updateWheels() {
        if (vehicleId != 0) {
            for (int i = 0; i < wheels.size(); i++) {
                updateWheelTransform(vehicleId, i, true);
                wheels.get(i).updatePhysicsState();
            }
        }
    }

    private native void updateWheelTransform(long vehicleId, int wheel, boolean interpolated);

    /**
     * used internally
     */
    public void applyWheelTransforms() {
        if (wheels != null) {
            for (int i = 0; i < wheels.size(); i++) {
                wheels.get(i).applyWheelTransform();
            }
        }
    }

    @Override
    protected void postRebuild() {
        super.postRebuild();
        motionState.setVehicle(this);
        createVehicle(physicsSpace);
    }

    /**
     * Used internally, creates the actual vehicle constraint when vehicle is
     * added to physics space.
     *
     * @param space which physics space
     */
    public void createVehicle(PhysicsSpace space) {
        physicsSpace = space;
        if (space == null) {
            return;
        }
        if (space.getSpaceId() == 0L) {
            throw new IllegalStateException("Physics space is not initialized!");
        }
        if (rayCasterId != 0L) {
            logger.log(Level.FINE, "Clearing RayCaster {0}",
                    Long.toHexString(rayCasterId));
            logger.log(Level.FINE, "Clearing Vehicle {0}",
                    Long.toHexString(vehicleId));
            finalizeNative(rayCasterId, vehicleId);
        }
        rayCasterId = createVehicleRaycaster(objectId, space.getSpaceId());
        logger.log(Level.FINE, "Created RayCaster {0}",
                Long.toHexString(rayCasterId));
        vehicleId = createRaycastVehicle(objectId, rayCasterId);
        logger.log(Level.FINE, "Created Vehicle {0}",
                Long.toHexString(vehicleId));
        setCoordinateSystem(vehicleId, 0, 1, 2);
        for (VehicleWheel wheel : wheels) {
            wheel.setVehicleId(vehicleId, addWheel(vehicleId,
                    wheel.getLocation(), wheel.getDirection(), wheel.getAxle(),
                    wheel.getRestLength(), wheel.getRadius(), tuning,
                    wheel.isFrontWheel()));
        }
    }

    private native long createVehicleRaycaster(long objectId,
            long physicsSpaceId);

    private native long createRaycastVehicle(long objectId, long rayCasterId);

    private native void setCoordinateSystem(long objectId, int a, int b, int c);

    private native int addWheel(long objectId, Vector3f location,
            Vector3f direction, Vector3f axle, float restLength, float radius,
            VehicleTuning tuning, boolean frontWheel);

    /**
     * Add a wheel to this vehicle
     *
     * @param connectionPoint The starting point of the ray, where the
     * suspension connects to the chassis (chassis space)
     * @param direction the direction of the wheel (should be -Y / 0,-1,0 for a
     * normal car)
     * @param axle The axis of the wheel, pointing right in vehicle direction
     * (should be -X / -1,0,0 for a normal car)
     * @param suspensionRestLength The current length of the suspension (metres)
     * @param wheelRadius the wheel radius
     * @param isFrontWheel sets if this wheel is a front wheel (steering)
     * @return the PhysicsVehicleWheel object to get/set infos on the wheel
     */
    public VehicleWheel addWheel(Vector3f connectionPoint, Vector3f direction,
            Vector3f axle, float suspensionRestLength, float wheelRadius,
            boolean isFrontWheel) {
        return addWheel(null, connectionPoint, direction, axle,
                suspensionRestLength, wheelRadius, isFrontWheel);
    }

    /**
     * Add a wheel to this vehicle
     *
     * @param spat the wheel Geometry
     * @param connectionPoint The starting point of the ray, where the
     * suspension connects to the chassis (chassis space)
     * @param direction the direction of the wheel (should be -Y / 0,-1,0 for a
     * normal car)
     * @param axle The axis of the wheel, pointing right in vehicle direction
     * (should be -X / -1,0,0 for a normal car)
     * @param suspensionRestLength The current length of the suspension (metres)
     * @param wheelRadius the wheel radius
     * @param isFrontWheel sets if this wheel is a front wheel (steering)
     * @return the PhysicsVehicleWheel object to get/set infos on the wheel
     */
    public VehicleWheel addWheel(Spatial spat, Vector3f connectionPoint,
            Vector3f direction, Vector3f axle, float suspensionRestLength,
            float wheelRadius, boolean isFrontWheel) {
        VehicleWheel wheel = null;
        if (spat == null) {
            wheel = new VehicleWheel(connectionPoint, direction, axle,
                    suspensionRestLength, wheelRadius, isFrontWheel);
        } else {
            wheel = new VehicleWheel(spat, connectionPoint, direction, axle,
                    suspensionRestLength, wheelRadius, isFrontWheel);
        }
        wheel.setFrictionSlip(tuning.frictionSlip);
        wheel.setMaxSuspensionTravelCm(tuning.maxSuspensionTravelCm);
        wheel.setSuspensionStiffness(tuning.suspensionStiffness);
        wheel.setWheelsDampingCompression(tuning.suspensionCompression);
        wheel.setWheelsDampingRelaxation(tuning.suspensionDamping);
        wheel.setMaxSuspensionForce(tuning.maxSuspensionForce);
        wheels.add(wheel);
        if (vehicleId != 0) {
            wheel.setVehicleId(vehicleId, addWheel(vehicleId,
                    wheel.getLocation(), wheel.getDirection(), wheel.getAxle(),
                    wheel.getRestLength(), wheel.getRadius(), tuning,
                    wheel.isFrontWheel()));
        }
        return wheel;
    }

    /**
     * This rebuilds the vehicle as there is no way in bullet to remove a wheel.
     *
     * @param wheel the index of the wheel to remove
     */
    public void removeWheel(int wheel) {
        wheels.remove(wheel);
        rebuildRigidBody();
//        updateDebugShape();
    }

    /**
     * Access the indexed wheel of this vehicle.
     *
     * @param wheel the index of the wheel to access (&ge;0, &lt;count)
     * @return the pre-existing instance
     */
    public VehicleWheel getWheel(int wheel) {
        return wheels.get(wheel);
    }

    /**
     * Read the number of wheels on this vehicle.
     *
     * @return count (&ge;0)
     */
    public int getNumWheels() {
        return wheels.size();
    }

    /**
     * Read the friction-slip tuning parameter of this vehicle.
     *
     * @return the value
     */
    public float getFrictionSlip() {
        return tuning.frictionSlip;
    }

    /**
     * Use before adding wheels. This sets the value applied when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The coefficient of friction between the tyre and the ground. Should be
     * about 0.8 for realistic cars, but can increased for better handling. Set
     * large (10000.0) for kart racers
     *
     * @param frictionSlip the frictionSlip to set
     */
    public void setFrictionSlip(float frictionSlip) {
        tuning.frictionSlip = frictionSlip;
    }

    /**
     * The coefficient of friction between the tyre and the ground. Should be
     * about 0.8 for realistic cars, but can increased for better handling. Set
     * large (10000.0) for kart racers
     *
     * @param wheel the index of the wheel to modify
     * @param frictionSlip the desired coefficient of friction
     */
    public void setFrictionSlip(int wheel, float frictionSlip) {
        wheels.get(wheel).setFrictionSlip(frictionSlip);
    }

    /**
     * Reduces the rolling torque applied from the wheels that cause the vehicle
     * to roll over. This is a bit of a hack, but it's quite effective. 0.0 = no
     * roll, 1.0 = physical behaviour. If m_frictionSlip is too high, you'll
     * need to reduce this to stop the vehicle rolling over. You should also try
     * lowering the vehicle's centre of mass
     *
     * @param wheel the index of the wheel to modify
     * @param rollInfluence the value to use
     */
    public void setRollInfluence(int wheel, float rollInfluence) {
        wheels.get(wheel).setRollInfluence(rollInfluence);
    }

    /**
     * @return the maxSuspensionTravelCm
     */
    public float getMaxSuspensionTravelCm() {
        return tuning.maxSuspensionTravelCm;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The maximum distance the suspension can be compressed (centimetres)
     *
     * @param maxSuspensionTravelCm the maxSuspensionTravelCm to set
     */
    public void setMaxSuspensionTravelCm(float maxSuspensionTravelCm) {
        tuning.maxSuspensionTravelCm = maxSuspensionTravelCm;
    }

    /**
     * The maximum distance the suspension can be compressed (centimetres)
     *
     * @param wheel the index of the wheel to modify
     * @param maxSuspensionTravelCm the distance to use (in centimetres)
     */
    public void setMaxSuspensionTravelCm(int wheel,
            float maxSuspensionTravelCm) {
        wheels.get(wheel).setMaxSuspensionTravelCm(maxSuspensionTravelCm);
    }

    /**
     * Read the maximum suspension force.
     *
     * @return the force limit
     */
    public float getMaxSuspensionForce() {
        return tuning.maxSuspensionForce;
    }

    /**
     * Alter the maximum suspension force. If the suspension cannot handle the
     * weight of the vehicle, increase this.
     *
     * @param maxSuspensionForce the desired force limit (default=6000)
     */
    public void setMaxSuspensionForce(float maxSuspensionForce) {
        tuning.maxSuspensionForce = maxSuspensionForce;
    }

    /**
     * This value caps the maximum suspension force, raise this above the
     * default 6000 if your suspension cannot handle the weight of your vehicle.
     *
     * @param wheel the index of the wheel to modify
     * @param maxSuspensionForce the desired limit
     */
    public void setMaxSuspensionForce(int wheel, float maxSuspensionForce) {
        wheels.get(wheel).setMaxSuspensionForce(maxSuspensionForce);
    }

    /**
     * @return the suspensionCompression
     */
    public float getSuspensionCompression() {
        return tuning.suspensionCompression;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The damping coefficient for when the suspension is compressed. Set to k *
     * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to
     * critical damping.<br>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     *
     * @param suspensionCompression the suspensionCompression to set
     */
    public void setSuspensionCompression(float suspensionCompression) {
        tuning.suspensionCompression = suspensionCompression;
    }

    /**
     * The damping coefficient for when the suspension is compressed. Set to k *
     * 2.0 * FastMath.sqrt(m_suspensionStiffness) so k is proportional to
     * critical damping.<br>
     * k = 0.0 undamped and bouncy, k = 1.0 critical damping<br>
     * 0.1 to 0.3 are good values
     *
     * @param wheel the index of the wheel to modify
     * @param suspensionCompression the desired damping coefficient
     */
    public void setSuspensionCompression(int wheel,
            float suspensionCompression) {
        wheels.get(wheel).setWheelsDampingCompression(suspensionCompression);
    }

    /**
     * @return the suspensionDamping
     */
    public float getSuspensionDamping() {
        return tuning.suspensionDamping;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The damping coefficient for when the suspension is expanding. See the
     * comments for setSuspensionCompression for how to set k.
     *
     * @param suspensionDamping the suspensionDamping to set
     */
    public void setSuspensionDamping(float suspensionDamping) {
        tuning.suspensionDamping = suspensionDamping;
    }

    /**
     * The damping coefficient for when the suspension is expanding. See the
     * comments for setSuspensionCompression for how to set k.
     *
     * @param wheel the index of the wheel to modify
     * @param suspensionDamping the desired damping coefficient
     */
    public void setSuspensionDamping(int wheel, float suspensionDamping) {
        wheels.get(wheel).setWheelsDampingRelaxation(suspensionDamping);
    }

    /**
     * @return the suspensionStiffness
     */
    public float getSuspensionStiffness() {
        return tuning.suspensionStiffness;
    }

    /**
     * Use before adding wheels, this is the default used when adding wheels.
     * After adding the wheel, use direct wheel access.<br>
     * The stiffness constant for the suspension. 10.0 - Offroad buggy, 50.0 -
     * Sports car, 200.0 - F1 Car
     *
     * @param suspensionStiffness the desired stiffness coefficient
     */
    public void setSuspensionStiffness(float suspensionStiffness) {
        tuning.suspensionStiffness = suspensionStiffness;
    }

    /**
     * The stiffness constant for the suspension. 10.0 - Offroad buggy, 50.0 -
     * Sports car, 200.0 - F1 Car
     *
     * @param wheel the index of the wheel to modify
     * @param suspensionStiffness the desired stiffness coefficient
     */
    public void setSuspensionStiffness(int wheel, float suspensionStiffness) {
        wheels.get(wheel).setSuspensionStiffness(suspensionStiffness);
    }

    /**
     * Reset the suspension
     */
    public void resetSuspension() {
        resetSuspension(vehicleId);
    }

    private native void resetSuspension(long vehicleId);

    /**
     * Apply the given engine force to all wheels, works continuously
     *
     * @param force the force
     */
    public void accelerate(float force) {
        for (int i = 0; i < wheels.size(); i++) {
            accelerate(i, force);
        }
    }

    /**
     * Apply the given engine force. Works continuously.
     *
     * @param wheel the wheel to apply the force on
     * @param force the force
     */
    public void accelerate(int wheel, float force) {
        applyEngineForce(vehicleId, wheel, force);
    }

    private native void applyEngineForce(long vehicleId, int wheel, float force);

    /**
     * Alter the steering angle of all front wheels.
     *
     * @param value the desired steering angle (in radians, 0=straight)
     */
    public void steer(float value) {
        for (int i = 0; i < wheels.size(); i++) {
            if (getWheel(i).isFrontWheel()) {
                steer(i, value);
            }
        }
    }

    /**
     * Alter the steering angle of the indexed wheel.
     *
     * @param wheel the index of the wheel to steer (&ge;0)
     * @param value the desired steering angle (in radians, 0=straight)
     */
    public void steer(int wheel, float value) {
        steer(vehicleId, wheel, value);
    }

    private native void steer(long vehicleId, int wheel, float value);

    /**
     * Apply the given brake force to all wheels, works continuously
     *
     * @param force the force
     */
    public void brake(float force) {
        for (int i = 0; i < wheels.size(); i++) {
            brake(i, force);
        }
    }

    /**
     * Apply the given brake force, works continuously
     *
     * @param wheel the wheel to apply the force on
     * @param force the force
     */
    public void brake(int wheel, float force) {
        brake(vehicleId, wheel, force);
    }

    private native void brake(long vehicleId, int wheel, float force);

    /**
     * Get the current speed of the vehicle in km/h
     *
     * @return speed (in kilometers per hour)
     */
    public float getCurrentVehicleSpeedKmHour() {
        return getCurrentVehicleSpeedKmHour(vehicleId);
    }

    private native float getCurrentVehicleSpeedKmHour(long vehicleId);

    /**
     * Get the current forward vector of the vehicle in world coordinates
     *
     * @param vector storage for the result (modified if not null)
     * @return the vector (either the provided storage or a new vector)
     */
    public Vector3f getForwardVector(Vector3f vector) {
        if (vector == null) {
            vector = new Vector3f();
        }
        getForwardVector(vehicleId, vector);
        return vector;
    }

    private native void getForwardVector(long objectId, Vector3f vector);

    /**
     * used internally
     *
     * @return the Bullet id
     */
    public long getVehicleId() {
        return vehicleId;
    }

    /**
     * De-serialize this vehicle, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        tuning = new VehicleTuning();
        tuning.frictionSlip = capsule.readFloat(
                "frictionSlip", 10.5f);
        tuning.maxSuspensionTravelCm = capsule.readFloat(
                "maxSuspensionTravelCm", 500f);
        tuning.maxSuspensionForce = capsule.readFloat(
                "maxSuspensionForce", 6000f);
        tuning.suspensionCompression = capsule.readFloat(
                "suspensionCompression", 0.83f);
        tuning.suspensionDamping = capsule.readFloat(
                "suspensionDamping", 0.88f);
        tuning.suspensionStiffness = capsule.readFloat(
                "suspensionStiffness", 5.88f);
        wheels = capsule.readSavableArrayList(
                "wheelsList", new ArrayList<VehicleWheel>());
        motionState.setVehicle(this);
        super.read(im);
    }

    /**
     * Serialize this vehicle, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(tuning.frictionSlip, "frictionSlip", 10.5f);
        capsule.write(tuning.maxSuspensionTravelCm,
                "maxSuspensionTravelCm", 500f);
        capsule.write(tuning.maxSuspensionForce, "maxSuspensionForce", 6000f);
        capsule.write(tuning.suspensionCompression,
                "suspensionCompression", 0.83f);
        capsule.write(tuning.suspensionDamping, "suspensionDamping", 0.88f);
        capsule.write(tuning.suspensionStiffness, "suspensionStiffness", 5.88f);
        capsule.writeSavableArrayList(wheels,
                "wheelsList", new ArrayList<VehicleWheel>());
        super.write(ex);
    }

    /**
     * Finalize this vehicle just before it is destroyed. Should be invoked only
     * by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        logger.log(Level.FINE, "Finalizing RayCaster {0}",
                Long.toHexString(rayCasterId));
        logger.log(Level.FINE, "Finalizing Vehicle {0}",
                Long.toHexString(vehicleId));
        finalizeNative(rayCasterId, vehicleId);
    }

    private native void finalizeNative(long rayCaster, long vehicle);
}
