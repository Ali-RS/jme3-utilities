/*
 Copyright (c) 2018, Stephen Gold
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
package jme3utilities.minie.test.tunings;

import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.RangeOfMotion;
import java.util.logging.Logger;

/**
 * A DynamicAnimControl configured specifically for the Sinbad model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SinbadControl extends DynamicAnimControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger4
            = Logger.getLogger(SinbadControl.class.getName());
    // *************************************************************************
    // constructors

    public SinbadControl() {
        super();
        super.setMass(torsoName, 5f);

        super.link("Waist", 5f,
                new RangeOfMotion(1f, -0.4f, 0.8f, -0.8f, 0.4f, -0.4f));
        super.link("Chest", 5f,
                new RangeOfMotion(0.4f, 0f, 0.4f));
        super.link("Neck", 2f,
                new RangeOfMotion(1f, -0.5f, 1f, -1f, 1f, -1f));

        super.link("Clavicle.R", 2f,
                new RangeOfMotion(0.3f, -0.6f, 0f, 0f, 0.4f, -0.4f));
        super.link("Humerus.R", 3f,
                new RangeOfMotion(1.6f, -0.8f, 1f, -1f, 1.6f, -1f));
        super.link("Ulna.R", 2f,
                new RangeOfMotion(0f, 0f, 1f, -1f, 0f, -2f));
        super.link("Hand.R", 1f,
                new RangeOfMotion(0.8f, 0f, 0.2f));

        super.link("Clavicle.L", 2f,
                new RangeOfMotion(0.6f, -0.3f, 0f, 0f, 0.4f, -0.4f));
        super.link("Humerus.L", 3f,
                new RangeOfMotion(0.8f, -1.6f, 1f, -1f, 1f, -1.6f));
        super.link("Ulna.L", 2f,
                new RangeOfMotion(0f, 0f, 1f, -1f, 2f, 0f));
        super.link("Hand.L", 1f,
                new RangeOfMotion(0.8f, 0f, 0.2f));

        super.link("Thigh.R", 3f,
                new RangeOfMotion(0.4f, -1f, 0.4f, -0.4f, 0.5f, -0.5f));
        super.link("Calf.R", 2f,
                new RangeOfMotion(2f, 0f, 0f, 0f, 0f, 0f));
        super.link("Foot.R", 1f,
                new RangeOfMotion(0.3f, 0.5f, 0f));

        super.link("Thigh.L", 3f,
                new RangeOfMotion(1f, -0.4f, 0.4f, -0.4f, 0.5f, -0.5f));
        super.link("Calf.L", 2f,
                new RangeOfMotion(2f, 0f, 0f, 0f, 0f, 0f));
        super.link("Foot.L", 1f,
                new RangeOfMotion(0.3f, 0.5f, 0f));
    }
}