/*
 Copyright (c) 2019, Stephen Gold
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
package jme3utilities.nifty.displaysettings;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.PopupMenuBuilder;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.nifty.dialog.DisplaySizeDialog;
import jme3utilities.ui.DisplaySettings;
import jme3utilities.ui.DisplaySizeLimits;
import jme3utilities.ui.InputMode;

/**
 * Screen controller for the display settings editor.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DsScreen
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DsScreen.class.getName());
    /**
     * action prefix: argument is a decimal number of bits
     */
    final public static String apSelectColorDepth = "select colorDepth ";
    /**
     * action prefix: argument is a number of samples per pixel, formatted using
     * {@link #describeMsaaFactor(int)}
     */
    final public static String apSelectMsaaFactor = "select msaaFactor ";
    /**
     * action prefix: arguments are the decimal width in pixels, an "x", and the
     * decimal height in pixels
     */
    final public static String apSetDimensions = "set dimensions ";
    /**
     * action prefix: argument is a decimal refresh rate in Hertz
     */
    final public static String apSetRefreshRate = "set refreshRate ";
    /**
     * short name for this screen
     */
    final public static String name = "ds";
    // *************************************************************************
    // fields

    /**
     * display settings: set by constructor
     */
    final private DisplaySettings displaySettings;
    /**
     * input mode for this screen controller: set by constructor
     */
    final private DsInputMode inputMode;
    /**
     * input mode to return to
     */
    private InputMode returnMode = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled screen controller.
     *
     * @param ds the application's display settings (not null, alias created)
     */
    public DsScreen(DisplaySettings ds) {
        super(name, "Interface/Nifty/screens/ds.xml", false);

        displaySettings = ds;
        inputMode = new DsInputMode(this);
        setListener(inputMode);
        influence(inputMode);
        inputMode.influence(this);

        assert !isEnabled();
        assert !isInitialized();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Activate this screen.
     */
    public void activate() {
        assert !isEnabled();
        assert isInitialized();
        assert returnMode == null;

        returnMode = InputMode.getActiveMode();
        assert returnMode != inputMode;
        assert returnMode.isEnabled();
        returnMode.setEnabled(false);
        setEnabled(true);
    }

    /**
     * Describe an MSAA sampling factor.
     *
     * @param factor samples per pixel (&ge;0, &le;16)
     * @return a textual description (not null, not empty)
     */
    public static String describeMsaaFactor(int factor) {
        String description;
        if (factor <= 1) {
            description = "disabled";
        } else {
            description = String.format("%dx", factor);
        }

        return description;
    }

    /**
     * Callback handler that Nifty invokes after a check box changes.
     *
     * @param checkBoxId Nifty element id of the check box (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*CheckBox")
    public void onCheckBoxChanged(final String checkBoxId,
            final CheckBoxStateChangedEvent event) {
        Validate.nonNull(checkBoxId, "check box id");
        Validate.nonNull(event, "event");
        assert checkBoxId.endsWith("CheckBox");

        if (!isIgnoreGuiChanges() && hasStarted()) {
            String checkBoxName = MyString.removeSuffix(checkBoxId, "CheckBox");
            boolean isChecked = event.isChecked();
            switch (checkBoxName) {
                case "fullscreen":
                    displaySettings.setFullscreen(isChecked);
                    break;

                case "gammaCorrection":
                    displaySettings.setGammaCorrection(isChecked);
                    break;

                case "vSync":
                    displaySettings.setVSync(isChecked);
                    break;

                default:
                    throw new IllegalArgumentException(checkBoxId);
            }
        }
    }

    /**
     * Display a menu to select the color depth (bits per pixel) for the display
     * using the "select colorDepth " action prefix.
     */
    public void selectColorDepth() {
        PopupMenuBuilder builder = new PopupMenuBuilder();
        int depth = displaySettings.colorDepth();

        if (displaySettings.isFullscreen()) {
            int height = displaySettings.height();
            int width = displaySettings.width();
            GraphicsEnvironment environment
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeDepth = mode.getBitDepth();
                int modeHeight = mode.getHeight();
                int modeWidth = mode.getWidth();
                if (modeDepth >= 16 && modeDepth != depth
                        && modeHeight == height && modeWidth == width) {
                    String modeItem = Integer.toString(modeDepth);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }

        } else {
            if (depth != 24) {
                builder.add("24");
            }
            if (depth != 32) {
                builder.add("32");
            }
        }

        showPopupMenu(apSelectColorDepth, builder);
    }

    /**
     * Display a menu to select the (full-screen) display dimensions using the
     * "set dimensions " action prefix.
     */
    public void selectDimensions() {
        PopupMenuBuilder builder = new PopupMenuBuilder();

        int height = displaySettings.height();
        int width = displaySettings.width();

        GraphicsEnvironment environment
                = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice device = environment.getDefaultScreenDevice();
        DisplayMode[] modes = device.getDisplayModes();
        for (DisplayMode mode : modes) {
            int modeHeight = mode.getHeight();
            int modeWidth = mode.getWidth();
            DisplaySizeLimits limits = displaySettings.sizeLimits;
            if (limits.isValidDisplaySize(modeWidth, modeHeight)
                    && (modeHeight != height || modeWidth != width)) {
                String modeItem = DisplaySizeDialog.describeDisplaySize(
                        modeWidth, modeHeight);
                if (!builder.hasItem(modeItem)) {
                    builder.add(modeItem);
                }
            }
        }

        showPopupMenu(apSetDimensions, builder);
    }

    /**
     * Display a menu to configure MSAA using the "select msaaFactor " action
     * prefix.
     */
    public void selectMsaaFactor() {
        PopupMenuBuilder builder = new PopupMenuBuilder();

        int selectedFactor = displaySettings.msaaFactor();
        for (int factor : new int[]{1, 2, 4, 6, 8, 16}) {
            if (factor != selectedFactor) {
                String description = describeMsaaFactor(factor);
                builder.add(description);
            }
        }

        showPopupMenu(apSelectMsaaFactor, builder);
    }

    /**
     * Display a menu to select the refresh rate for the display using the "set
     * refreshRate " action prefix.
     */
    public void selectRefreshRate() {
        if (displaySettings.isFullscreen()) {
            PopupMenuBuilder builder = new PopupMenuBuilder();
            int refreshRate = displaySettings.refreshRate();
            int height = displaySettings.height();
            int width = displaySettings.width();
            GraphicsEnvironment env
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = env.getDefaultScreenDevice();
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int modeHeight = mode.getHeight();
                int modeRate = mode.getRefreshRate();
                int modeWidth = mode.getWidth();
                if (modeRate != refreshRate
                        && modeHeight == height && modeWidth == width) {
                    String modeItem = Integer.toString(modeRate);
                    if (!builder.hasItem(modeItem)) {
                        builder.add(modeItem);
                    }
                }
            }
            showPopupMenu(apSetRefreshRate, builder);
        }
    }

    /**
     * Display a "set dimensions" dialog to enter the dimensions.
     */
    public void setDimensions() {
        int height = displaySettings.height();
        int width = displaySettings.width();
        String defaultText
                = DisplaySizeDialog.describeDisplaySize(width, height);

        DisplaySizeLimits dsl = displaySettings.sizeLimits;
        DialogController controller = new DisplaySizeDialog("Set", dsl);

        closeAllPopups();
        showTextEntryDialog("Enter display dimensions in pixels:",
                defaultText, apSetDimensions, controller);
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            logger.log(Level.INFO, "Got action {0}",
                    MyString.quote(actionString));
        }
        /*
         * Parse the action string and attempt to handle the action.
         */
        boolean handled = false;
        if (ongoing) {
            InputMode defaultMode = actionApplication.getDefaultInputMode();
            switch (actionString) {
                case "apply displaySettings":
                    displaySettings.applyToDisplay();
                    return;

                case SimpleApplication.INPUT_MAPPING_HIDE_STATS:
                case SimpleApplication.INPUT_MAPPING_MEMORY:
                    defaultMode.onAction(actionString, ongoing, tpf);
                    return;

                case "return":
                    deactivate();
                    return;

                case "save displaySettings":
                    displaySettings.save();
                    return;
            }

            String[] words = actionString.split(" ");
            String firstWord = words[0];
            switch (firstWord) {
                case "select":
                    handled = selectAction(actionString);
                    break;

                case "set":
                    handled = setAction(actionString);
            }
        }

        if (!handled) {
            /*
             * Forward the unhandled action to the application.
             */
            actionApplication.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // GuiScreenController methods

    /**
     * Initialize this screen.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        assert !isInitialized();
        assert !isEnabled();

        inputMode.initialize(stateManager, application);
        super.initialize(stateManager, application);
    }

    /**
     * Update this screen prior to rendering. (Invoked once per frame.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        assert isEnabled();
        super.update(tpf);

        Screen screen = getScreen();
        if (!screen.isBound()) {
            /*
             * Avoid Nifty exceptions and warnings regarding unbound controls.
             */
            return;
        }

        boolean fullscreen = displaySettings.isFullscreen();
        setChecked("fullscreen", fullscreen);

        boolean gamma = displaySettings.isGammaCorrection();
        setChecked("gammaCorrection", gamma);

        boolean vSync = displaySettings.isVSync();
        setChecked("vSync", vSync);

        updateButtonTexts();

        String feedbackText = "";
        if (!displaySettings.areValid()) {
            feedbackText = displaySettings.feedbackValid();
        } else if (!displaySettings.canApply()) {
            feedbackText = displaySettings.feedbackApplicable();
        } else if (!displaySettings.areApplied()) {
            feedbackText = "There are unapplied changes.";
        } else if (!displaySettings.areSaved()) {
            feedbackText = "There are unsaved changes.";
        }
        Element feedbackElement = getScreen().findElementById("feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedbackText);
    }
    // *************************************************************************
    // private methods

    /**
     * Deactivate this screen and return to the previous input mode.
     */
    private void deactivate() {
        assert isEnabled();
        assert isInitialized();
        assert returnMode != null;
        assert returnMode != inputMode;
        assert !returnMode.isEnabled();

        closeAllPopups();
        setEnabled(false);
        returnMode.setEnabled(true);
        returnMode = null;
    }

    /**
     * Process an ongoing action that starts with the word "select".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean selectAction(String actionString) {
        String arg;
        boolean handled = true;

        if (actionString.equals("select colorDepth")) {
            selectColorDepth();

        } else if (actionString.startsWith(apSelectColorDepth)) {
            arg = MyString.remainder(actionString, apSelectColorDepth);
            int bitsPerPixel = Integer.parseInt(arg);
            displaySettings.setColorDepth(bitsPerPixel);

        } else if (actionString.equals("select msaaFactor")) {
            selectMsaaFactor();

        } else if (actionString.startsWith(apSelectMsaaFactor)) {
            arg = MyString.remainder(actionString, apSelectMsaaFactor);
            int factor = 16;
            for (int f : new int[]{1, 2, 4, 6, 8}) {
                String aaDescription = describeMsaaFactor(f);
                if (arg.equals(aaDescription)) {
                    factor = f;
                    break;
                }
            }
            displaySettings.setMsaaFactor(factor);

        } else {
            handled = false;
        }

        return handled;
    }

    /**
     * Process an ongoing action that starts with the word "set".
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean setAction(String actionString) {
        String arg;
        boolean handled = true;

        if (actionString.equals("set dimensions")) {
            if (displaySettings.isFullscreen()) {
                selectDimensions();
            } else {
                setDimensions();
            }

        } else if (actionString.startsWith(apSetDimensions)) {
            arg = MyString.remainder(actionString, apSetDimensions);
            String lcArg = arg.toLowerCase(Locale.ROOT);
            if ("min".equals(lcArg)) {
                displaySettings.setMinDimensions();
            } else if ("max".equals(lcArg)) {
                displaySettings.setMaxDimensions();
            } else {
                int[] wh = DisplaySizeDialog.parseDisplaySize(lcArg);
                if (wh == null) {
                    handled = false;
                } else {
                    displaySettings.setDimensions(wh[0], wh[1]);
                }
            }

        } else if (actionString.equals("set refreshRate")) {
            selectRefreshRate();

        } else if (actionString.startsWith(apSetRefreshRate)) {
            arg = MyString.remainder(actionString, apSetRefreshRate);
            int hertz = Integer.parseInt(arg);
            displaySettings.setRefreshRate(hertz);

        } else {
            handled = false;
        }

        return handled;
    }

    /**
     * Update the texts of the Nifty buttons.
     */
    private void updateButtonTexts() {
        int width = displaySettings.width();
        int height = displaySettings.height();
        String dimensionsButton
                = DisplaySizeDialog.describeDisplaySize(width, height);
        setButtonText("displayDimensions", dimensionsButton);

        int msaaFactor = displaySettings.msaaFactor();
        String msaaButton = describeMsaaFactor(msaaFactor);
        setButtonText("displayMsaa", msaaButton);

        String refreshRateButton = "";
        boolean fullscreen = displaySettings.isFullscreen();
        if (fullscreen) {
            int refreshRate = displaySettings.refreshRate();
            if (refreshRate <= 0) {
                refreshRateButton = "unknown";
            } else {
                refreshRateButton = String.format("%d Hz", refreshRate);
            }
        }
        setButtonText("refreshRate", refreshRateButton);

        int colorDepth = displaySettings.colorDepth();
        String colorDepthButton = String.format("%d bpp", colorDepth);
        setButtonText("colorDepth", colorDepthButton);

        String applyButton = "";
        if (displaySettings.canApply() && !displaySettings.areApplied()) {
            applyButton = "Apply";
        }
        setButtonText("applyDisplaySettings", applyButton);

        String saveButton = "";
        if (displaySettings.areValid() && !displaySettings.areSaved()) {
            saveButton = "Save";
        }
        setButtonText("saveDisplaySettings", saveButton);

        InputMode defaultMode = actionApplication.getDefaultInputMode();
        String returnLabel = String.format("Return to %s mode",
                MyString.quote(defaultMode.getShortName()));
        setButtonText("return", returnLabel);
    }
}
