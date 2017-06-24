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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.Control;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility methods for manipulating skeletonized spatials, skeletons, and bones.
 * All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MySkeleton {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MySkeleton.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_XYZ}
     */
    final private static Vector3f identityScale = new Vector3f(1f, 1f, 1f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MySkeleton() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy the bind transform of the specified bone.
     *
     * @param bone which bone to use (not null, unaffected)
     * @param storeResult (modified if not null)
     * @return transform in local coordinates (either storeResult or a new
     * instance)
     */
    public static Transform copyBindTransform(Bone bone,
            Transform storeResult) {
        if (storeResult == null) {
            storeResult = new Transform();
        }

        Vector3f translation = bone.getBindPosition();
        storeResult.setTranslation(translation);

        Quaternion rotation = bone.getBindRotation();
        storeResult.setRotation(rotation);

        Vector3f scale = bone.getBindScale();
        if (scale == null) {
            scale = identityScale;
        }
        storeResult.setScale(scale);

        return storeResult;
    }

    /**
     * Find a named bone in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial to search (not null)
     * @param boneName name of the bone to access (not null)
     * @return a pre-existing instance (or null if not found)
     */
    public static Bone findBone(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Skeleton skeleton = findSkeleton(spatial);
        Bone result = skeleton.getBone(boneName);

        return result;
    }

    /**
     * Find the index of a named bone in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial that contains the bone (not null,
     * unaffected)
     * @param boneName name of the bone to find (not null)
     * @return bone index (&ge; 0) or -1 if not found
     */
    public static int findBoneIndex(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Skeleton skeleton = findSkeleton(spatial);
        int index = skeleton.getBoneIndex(boneName);

        return index;
    }

    /**
     * Find a skeleton of the specified spatial.
     *
     * @param spatial which spatial to search (not null)
     * @return a pre-existing instance, or null if none found
     */
    public static Skeleton findSkeleton(Spatial spatial) {
        Skeleton skeleton = null;
        AnimControl animControl = spatial.getControl(AnimControl.class);
        if (animControl != null) {
            skeleton = animControl.getSkeleton();
        }

        if (skeleton == null) {
            SkeletonControl skeletonControl;
            skeletonControl = spatial.getControl(SkeletonControl.class);
            if (skeletonControl != null) {
                skeleton = skeletonControl.getSkeleton();
            }
        }

        return skeleton;
    }

    /**
     * Enumerate all named bones in the specified skeleton.
     *
     * @param skeleton which skeleton (not null, unaffected)
     * @param addResult (added to if not null)
     * @return a list of names in arbitrary order, without any duplicates
     * (either addResult or a new list)
     */
    public static List<String> listBones(Skeleton skeleton,
            List<String> addResult) {
        int boneCount = skeleton.getBoneCount();
        if (addResult == null) {
            addResult = new ArrayList<>(boneCount);
        }

        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            if (bone != null) {
                String name = bone.getName();
                if (name != null && !addResult.contains(name)) {
                    addResult.add(name);
                }
            }
        }

        return addResult;
    }

    /**
     * List all bones in a skeletonized spatial.
     *
     * @param spatial skeletonized spatial (not null)
     * @return a new list of names in lexicographic order, without any
     * duplicates (may be empty)
     */
    public static List<String> listBones(Spatial spatial) {
        List<String> result = new ArrayList<>(80);

        int numControls = spatial.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; controlIndex++) {
            Control control = spatial.getControl(controlIndex);
            if (control instanceof AnimControl) {
                AnimControl animControl = (AnimControl) control;
                Skeleton skeleton = animControl.getSkeleton();
                if (skeleton != null) {
                    listBones(skeleton, result);
                }
            } else if (control instanceof SkeletonControl) {
                SkeletonControl skeletonControl = (SkeletonControl) control;
                Skeleton skeleton = skeletonControl.getSkeleton();
                if (skeleton != null) {
                    listBones(skeleton, result);
                }
            }
        }

        Collections.sort(result);

        return result;
    }

    /**
     * Find the largest weight in the specified mesh for the specified bone.
     *
     * @param mesh which mesh (not null, possibly modified)
     * @param boneIndex which bone (&ge;0)
     * @return bone weight, or 0f if no influence found
     */
    public static float maxWeight(Mesh mesh, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getData();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getData();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        float result = 0f;
        byte biByte = (byte) boneIndex;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                float bIndex = boneIndexBuffer.get();
                if (wIndex < maxWeightsPerVert
                        && bIndex == biByte
                        && weight > result) {
                    result = weight;
                }
            }
        }

        return result;
    }

    /**
     * Convert a location in a bone's local space to a location in model space.
     *
     * @param bone (not null)
     * @param x displacement along the bone's X-axis
     * @param y displacement along the bone's Y-axis
     * @param z displacement along the bone's Z-axis
     * @return a new vector
     *
     */
    public static Vector3f modelLocation(Bone bone, float x, float y, float z) {
        Vector3f tail = bone.getModelSpacePosition();
        Vector3f scale = bone.getModelSpaceScale();
        Vector3f result = new Vector3f(x, y, z).multLocal(scale);
        // rotation??
        result.addLocal(tail);

        return result;
    }

    /**
     * Count vertices in the specified mesh influenced by the specified bone.
     *
     * @param mesh which mesh (not null, possibly modified)
     * @param boneIndex which bone (&ge;0)
     * @return count (&ge;0)
     */
    public static int numInfluenced(Mesh mesh, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        int maxWeightsPerVert = mesh.getMaxNumWeights();
        assert maxWeightsPerVert > 0 : maxWeightsPerVert;
        assert maxWeightsPerVert <= 4 : maxWeightsPerVert;

        VertexBuffer biBuf = mesh.getBuffer(VertexBuffer.Type.BoneIndex);
        ByteBuffer boneIndexBuffer = (ByteBuffer) biBuf.getData();
        boneIndexBuffer.rewind();
        int numBoneIndices = boneIndexBuffer.remaining();
        assert numBoneIndices % 4 == 0 : numBoneIndices;
        int numVertices = boneIndexBuffer.remaining() / 4;

        VertexBuffer wBuf = mesh.getBuffer(VertexBuffer.Type.BoneWeight);
        FloatBuffer weightBuffer = (FloatBuffer) wBuf.getData();
        weightBuffer.rewind();
        int numWeights = weightBuffer.remaining();
        assert numWeights == numVertices * 4 : numWeights;

        int result = 0;
        byte biByte = (byte) boneIndex;
        for (int vIndex = 0; vIndex < numVertices; vIndex++) {
            for (int wIndex = 0; wIndex < 4; wIndex++) {
                float weight = weightBuffer.get();
                float bIndex = boneIndexBuffer.get();
                if (wIndex < maxWeightsPerVert
                        && bIndex == biByte
                        && weight > 0f) {
                    result++;
                }
            }
        }

        return result;
    }

    /**
     * Count the number of leaf bones in the specified skeleton.
     *
     * @param skeleton (not null)
     * @return count (&ge;0)
     */
    public static int numLeafBones(Skeleton skeleton) {
        int boneCount = skeleton.getBoneCount();
        int result = 0;
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            List<Bone> children = bone.getChildren();
            if (children.isEmpty()) {
                ++result;
            }
        }

        return result;
    }

    /**
     * Count the number of root bones in the specified skeleton.
     *
     * @param skeleton (not null)
     * @return count (&ge;0)
     */
    public static int numRootBones(Skeleton skeleton) {
        Bone[] roots = skeleton.getRoots();
        int result = roots.length;

        return result;
    }

    /**
     * Alter the name of the specified bone. The caller is responsible for
     * avoiding duplicate names. TODO: rename the attachNode, if any
     *
     * @param bone bone to change (not null)
     * @param newName name to apply
     * @return true if successful, otherwise false
     */
    public static boolean setName(Bone bone, String newName) {
        /*
         * The override sequence is A, X, B, and Y.
         */
        Class<?> boneClass = bone.getClass();
        Field nameField;
        try {
            nameField = boneClass.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            return false;
        }
        nameField.setAccessible(true);
        try {
            nameField.set(bone, newName);
        } catch (IllegalAccessException e) {
            return false;
        }

        return true;
    }

    /**
     * Alter all the user-control flags in the specified skeleton.
     *
     * @param skeleton skeleton to alter (not null)
     * @param newSetting true to enable user control, false to disable
     */
    public static void setUserControl(Skeleton skeleton, boolean newSetting) {
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserControl(newSetting);
        }
    }

    /**
     * Alter all the user-control flags in the specified subtree. Note:
     * recursive!
     *
     * @param subtree subtree to alter (not null)
     * @param newSetting true to enable user control, false to disable
     */
    public static void setUserControl(Spatial subtree, boolean newSetting) {
        Validate.nonNull(subtree, "spatial");

        int numControls = subtree.getNumControls();
        for (int controlIndex = 0; controlIndex < numControls; controlIndex++) {
            Control control = subtree.getControl(controlIndex);
            if (control instanceof AnimControl) {
                AnimControl animControl = (AnimControl) control;
                Skeleton skeleton = animControl.getSkeleton();
                if (skeleton != null) {
                    setUserControl(skeleton, newSetting);
                }
            } else if (control instanceof SkeletonControl) {
                SkeletonControl skeletonControl = (SkeletonControl) control;
                Skeleton skeleton = skeletonControl.getSkeleton();
                if (skeleton != null) {
                    setUserControl(skeleton, newSetting);
                }
            }
        }

        if (subtree instanceof Node) {
            Node node = (Node) subtree;
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                setUserControl(child, newSetting);
            }
        }
    }

    /**
     * Calculate the world location of (the tail of) a named bone.
     *
     * @param spatial skeletonized spatial that contains the bone (not null)
     * @param boneName (not null)
     * @return new vector in world coordinates
     */
    public static Vector3f worldLocation(Spatial spatial, String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone name");

        Bone bone = findBone(spatial, boneName);
        Vector3f localCoordinates = bone.getModelSpacePosition();
        Vector3f result = spatial.localToWorld(localCoordinates, null);

        return result;
    }

    /**
     * Calculate the world orientation of a bone.
     *
     * @param spatial skeletonized spatial that contains the bone (not null)
     * @param bone (not null)
     * @return new instance in world coordinates
     */
    public static Quaternion worldOrientation(Spatial spatial, Bone bone) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(bone, "bone");

        Quaternion boneInModel = bone.getModelSpaceRotation();
        Quaternion modelInWorld = spatial.getWorldRotation();
        Quaternion result = modelInWorld.mult(boneInModel);

        return result;
    }

    /**
     * Calculate the world orientation of a named bone.
     *
     * @param spatial skeletonized spatial that contains the bone (not null)
     * @param boneName (not null)
     * @return new instance
     */
    public static Quaternion worldOrientation(Spatial spatial,
            String boneName) {
        Validate.nonNull(spatial, "spatial");
        Validate.nonNull(boneName, "bone");

        Bone bone = findBone(spatial, boneName);
        Quaternion result = worldOrientation(spatial, bone);

        return result;
    }
}