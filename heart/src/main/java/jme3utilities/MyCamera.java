/*
 Copyright (c) 2013-2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities;

import com.jme3.input.InputManager;
import com.jme3.math.FastMath;
import com.jme3.math.Line;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods that operate on jME3 cameras and view ports. All methods
 * should be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyCamera {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyCamera.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyCamera() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the azimuth angle of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return radians east of north
     */
    public static float azimuth(Camera camera) {
        Vector3f direction = camera.getDirection();
        float azimuth = MyVector3f.azimuth(direction);

        return azimuth;
    }

    /**
     * Test whether the bounds of the specified view port contain the specified
     * screen position.
     *
     * @param viewPort (not null, unaffected)
     * @param screenXY (in pixels, not null, unaffected)
     *
     * @return true if contained, otherwise false
     */
    public static boolean contains(ViewPort viewPort, Vector2f screenXY) {
        Camera camera = viewPort.getCamera();
        float xFraction = screenXY.x / camera.getWidth();
        float leftX = camera.getViewPortLeft();
        float rightX = camera.getViewPortRight();

        boolean result = false;
        if (xFraction > leftX && xFraction < rightX) {
            float yFraction = screenXY.y / camera.getHeight();
            float bottomY = camera.getViewPortBottom();
            float topY = camera.getViewPortTop();
            if (yFraction > bottomY && yFraction < topY) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Generate a textual description of a camera.
     *
     * @param camera instance to describe (unaffected)
     * @return description (not null, not empty)
     * @see #describeMore(com.jme3.renderer.Camera)
     */
    public static String describe(Camera camera) {
        String result;
        if (camera == null) {
            result = "null";

        } else {
            String name = camera.getName();
            Vector3f location = camera.getLocation();
            Vector3f direction = camera.getDirection();
            result = String.format("camera %s loc=%s dir=%s",
                    MyString.quote(name), location.toString(),
                    direction.toString());
        }

        return result;
    }

    /**
     * Generate additional textual description of a camera.
     *
     * @param camera camera to describe (not null, unaffected)
     * @return description (not null, not empty)
     * @see #describe(com.jme3.renderer.Camera)
     */
    public static String describeMore(Camera camera) {
        String projection = camera.isParallelProjection() ? "paral" : "persp";
        float fAspect = frustumAspectRatio(camera);
        float vAspect = viewAspectRatio(camera);
        String result = String.format("%s F%.3f:1 V%.3f:1", projection,
                fAspect, vAspect);

        float near = camera.getFrustumNear();
        float far = camera.getFrustumFar();
        float left = camera.getViewPortLeft();
        float right = camera.getViewPortRight();
        float bottom = camera.getViewPortBottom();
        float top = camera.getViewPortTop();
        int dWidth = camera.getWidth();
        int dHeight = camera.getHeight();
        result += String.format(
                " fz[%.2f %.2f] vx[%.2f %.2f] vy[%.2f %.2f] %dx%d",
                near, far, left, right, bottom, top, dWidth, dHeight);

        return result;
    }

    /**
     * Calculate the aspect ratio of the display.
     *
     * @param camera camera to use (not null, unaffected)
     * @return width divided by height (&gt;0)
     */
    public static float displayAspectRatio(Camera camera) {
        /*
         * Note: camera.getHeight() returns the height of the display,
         * not the height of the camera!
         */
        float height = camera.getHeight();
        assert height > 0f : height;

        float width = camera.getWidth();
        assert width > 0f : width;

        float ratio = width / height;

        assert ratio > 0f : ratio;
        return ratio;
    }

    /**
     * Calculate the vertical field-of-view angle of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return radians from bottom of frustum to top of frustum (&ge;0)
     */
    public static float fovY(Camera camera) {
        if (camera.isParallelProjection()) {
            return 0f;
        }

        float near = camera.getFrustumNear();
        assert near > 0f : near;

        float top = camera.getFrustumTop();
        assert top > 0f : top;

        float fovY = 2f * FastMath.atan(top / near);

        assert fovY > 0f : fovY;
        return fovY;
    }

    /**
     * Calculate the aspect ratio of the specified camera's frustum.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return width divided by height (&gt;0)
     */
    public static float frustumAspectRatio(Camera camera) {
        /*
         * Note: camera.getHeight() returns the height of the display,
         * not the height of the camera!  The display and the camera frustum
         * often have the same aspect ratio, but not always.
         */
        float height = camera.getFrustumTop();
        assert height > 0f : height;

        float width = camera.getFrustumRight();
        assert width > 0f : width;

        float ratio = width / height;

        assert ratio > 0f : ratio;
        return ratio;
    }

    /**
     * Enumerate all view ports that contain the specified screen position.
     *
     * @param screenXY (in pixels, not null, unaffected)
     * @param renderManager (not null)
     *
     * @return a new list of pre-existing view ports
     */
    public static List<ViewPort> listViewPorts(RenderManager renderManager,
            Vector2f screenXY) {
        Validate.nonNull(screenXY, "screen xy");

        List<ViewPort> result = new ArrayList<>(4);

        List<ViewPort> preViews = renderManager.getPreViews();
        for (ViewPort preView : preViews) {
            if (contains(preView, screenXY)) {
                result.add(preView);
            }
        }

        List<ViewPort> mainViews = renderManager.getMainViews();
        for (ViewPort mainView : mainViews) {
            if (contains(mainView, screenXY)) {
                result.add(mainView);
            }
        }

        List<ViewPort> postViews = renderManager.getPostViews();
        for (ViewPort postView : postViews) {
            if (contains(postView, screenXY)) {
                result.add(postView);
            }
        }

        return result;
    }

    /**
     * Rotate a camera without changing its location, setting its "up" direction
     * automatically.
     *
     * @param camera camera to rotate (not null)
     * @param direction (length&gt;0, unaffected)
     */
    public static void look(Camera camera, Vector3f direction) {
        Validate.nonZero(direction, "new direction");

        if (direction.x == 0f && direction.z == 0f) {
            /*
             * When looking straight up or down, use +X as the up direction.
             */
            camera.lookAtDirection(direction, xAxis);
        } else {
            camera.lookAtDirection(direction, yAxis);
        }
    }

    /**
     * Convert the mouse-pointer location into a line.
     *
     * @param camera (not null, unaffected)
     * @param inputManager (not null)
     *
     * @return a new line in world coordinates
     */
    public static Line mouseLine(Camera camera, InputManager inputManager) {
        Vector2f screenXY = inputManager.getCursorPosition();
        /*
         * Convert screen coordinates to world coordinates.
         */
        Vector3f vertex = camera.getWorldCoordinates(screenXY, 0f);
        Vector3f far = camera.getWorldCoordinates(screenXY, 1f);

        Vector3f direction = far.subtract(vertex);
        Line line = new Line(vertex, direction);

        return line;
    }

    /**
     * Convert the mouse-pointer location into a ray.
     *
     * @param camera (not null, unaffected)
     * @param inputManager (not null)
     *
     * @return a new ray in world coordinates
     */
    public static Ray mouseRay(Camera camera, InputManager inputManager) {
        Vector2f screenXY = inputManager.getCursorPosition();
        /*
         * Convert screen coordinates to world coordinates.
         */
        Vector3f vertex = camera.getWorldCoordinates(screenXY, 0f);
        Vector3f far = camera.getWorldCoordinates(screenXY, 1f);

        Vector3f direction = far.subtract(vertex);
        MyVector3f.normalizeLocal(direction);
        Ray ray = new Ray(vertex, direction);

        return ray;
    }

    /**
     * Alter a camera's near and far planes without affecting its aspect ratio
     * or field-of-view.
     *
     * @param camera camera to alter (not null)
     * @param newNear distance to the near clipping plane (&lt;newFar, &gt;0)
     * @param newFar distance to the far clipping plane (&gt;newNear)
     */
    public static void setNearFar(Camera camera, float newNear, float newFar) {
        Validate.positive(newNear, "near");
        if (!(newFar > newNear)) {
            logger.log(Level.SEVERE, "far={0} near={1}",
                    new Object[]{newFar, newNear});
            throw new IllegalArgumentException(
                    "far should be greater than near");
        }

        if (camera.isParallelProjection()) {
            camera.setFrustumFar(newFar);
            camera.setFrustumNear(newNear);
        } else {
            float fAspect = frustumAspectRatio(camera);
            float yDegrees = yDegrees(camera);
            camera.setFrustumPerspective(yDegrees, fAspect, newNear, newFar);
        }
    }

    /**
     * Alter a camera's field-of-view tangents.
     *
     * @param camera camera to alter (not null)
     * @param newTangent tangent of the vertical field-of-view half-angle
     * (&gt;0)
     */
    public static void setYTangent(Camera camera, float newTangent) {
        Validate.nonNull(camera, "camera");
        Validate.positive(newTangent, "tangent");

        float yTangent = yTangent(camera);
        float factor = newTangent / yTangent;
        zoom(camera, factor);
    }

    /**
     * Calculate the aspect ratio of the specified camera's viewport.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return width divided by height (&gt;0)
     */
    public static float viewAspectRatio(Camera camera) {
        /*
         * Note: camera.getHeight() returns the height of the display,
         * not the height of the viewport!  The display and the viewport
         * often have the same aspect ratio, but not always.
         */
        float bottom = camera.getViewPortBottom();
        assert bottom >= 0f : bottom;
        assert bottom <= 1f : bottom;

        float top = camera.getViewPortTop();
        assert top >= 0f : top;
        assert top <= 1f : top;

        float yFraction = top - bottom;
        assert yFraction > 0f : yFraction;

        float height = camera.getHeight() * yFraction;
        assert height > 0f : height;

        float left = camera.getViewPortLeft();
        assert left >= 0f : left;
        assert left <= 1f : left;

        float right = camera.getViewPortRight();
        assert right >= 0f : right;
        assert right <= 1f : right;

        float xFraction = right - left;
        assert xFraction > 0f : xFraction;

        float width = camera.getWidth() * xFraction;
        assert width > 0f : width;

        float ratio = width / height;

        assert ratio > 0f : ratio;
        return ratio;
    }

    /**
     * Calculate the vertical field-of-view angle of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return vertical angle in degrees (&gt;0)
     */
    public static float yDegrees(Camera camera) {
        if (camera.isParallelProjection()) {
            return 0f;
        }

        float yTangent = yTangent(camera);
        float yRadians = 2f * FastMath.atan(yTangent);
        float yDegrees = MyMath.toDegrees(yRadians);

        return yDegrees;
    }

    /**
     * Calculate the vertical field-of-view tangent of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return tangent of the vertical field-of-view half-angle (&gt;0)
     */
    public static float yTangent(Camera camera) {
        float near = camera.getFrustumNear();
        assert near > 0f : near;

        float top = camera.getFrustumTop();
        assert top > 0f : top;

        float yTangent = top / near;

        assert yTangent > 0f : yTangent;
        return yTangent;
    }

    /**
     * Increase a camera's field-of-view tangents by the specified factor.
     *
     * @param camera camera to alter (not null)
     * @param factor amount to reduce both field-of-view tangents (&gt;0)
     */
    public static void zoom(Camera camera, float factor) {
        Validate.positive(factor, "factor");

        float bottom = camera.getFrustumBottom();
        camera.setFrustumBottom(bottom * factor);

        float left = camera.getFrustumLeft();
        camera.setFrustumLeft(left * factor);

        float right = camera.getFrustumRight();
        camera.setFrustumRight(right * factor);

        float top = camera.getFrustumTop();
        camera.setFrustumTop(top * factor);
    }
}
