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
package jme3utilities.nifty;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.MenuItemActivatedEvent;
import de.lessvoid.nifty.elements.Element;
import java.util.logging.Logger;
import org.bushe.swing.event.EventTopicSubscriber;

/**
 * Event subscriber for a simple Nifty popup menu (or submenu) whose items are
 * Strings.
 * <p>
 * When a menu item is activated, invoke GuiScreenController.perform() with the
 * item appended to the popup menu's action prefix, then close the popup.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PopupMenu
        implements EventTopicSubscriber<MenuItemActivatedEvent<String>> {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            PopupMenu.class.getName());
    // *************************************************************************
    // fields

    /**
     * the parent popup menu which opened the menu, or null if not a submenu:
     * set by constructor
     */
    final private PopupMenu parent;
    /**
     * prefix for the menu's action strings: set by constructor
     */
    final private String actionPrefix;
    /**
     * items in the menu: set by constructor
     */
    final private String[] itemArray;
    /**
     * Nifty id of the popup: set by constructor
     */
    final private String popupId;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a record of a popup menu or submenu.
     *
     * @param popupId Nifty id of the popup element (not null)
     * @param actionPrefix prefix for action strings (not null)
     * @param itemArray items in the popup menu (not null, unaffected)
     * @param parent the menu which opened this submenu (or null if not a
     * submenu)
     */
    PopupMenu(String popupId, String actionPrefix, String[] itemArray,
            PopupMenu parent) {
        assert popupId != null;
        assert actionPrefix != null;
        assert itemArray != null;

        this.popupId = popupId;
        this.actionPrefix = actionPrefix;
        this.itemArray = itemArray.clone();
        this.parent = parent;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Close this menu.
     */
    void close() {
        Nifty nifty = GuiScreenController.getNifty();
        nifty.closePopup(popupId);
    }

    /**
     * Generate the action string for an indexed item in this menu. TODO rename
     *
     * @param index item to generate for (&ge;0, 0 &rarr; 1st)
     * @return action string, or null for an invalid index
     */
    String getActionString(int index) {
        if (index < 0 || index >= itemArray.length) {
            return null;
        }
        /*
         * Generate the action string for the item by appending the item's
         * name to this menu's action prefix.
         */
        String itemName = itemArray[index];
        String actionString = actionPrefix + itemName;

        return actionString;
    }

    /**
     * Access the parent: the popup menu which opened this submenu.
     *
     * @return the pre-existing instance, or null if not a submenu
     */
    PopupMenu getParent() {
        return parent;
    }

    /**
     * Enable or disable this menu.
     *
     * @param newState true to enable, false to disable
     */
    void setEnabled(boolean newState) {
        Element element = getElement();
        if (newState) {
            element.enable();
        } else {
            element.disable();
        }
    }
    // *************************************************************************
    // EventTopicSubscriber methods

    /**
     * Callback from Nifty when an item in this menu is activated.
     *
     * @param controlId Nifty id of the menu's control (ignored)
     * @param event details, such as which item got activated (not null)
     */
    @Override
    public void onEvent(String controlId,
            MenuItemActivatedEvent<String> event) {
        /*
         * Generate the action string for the item by appending the item's
         * name to this menu's action prefix.
         */
        String itemName = event.getItem();
        String actionString = actionPrefix + itemName;
        /*
         * Perform the action described by the action string.
         */
        GuiScreenController.perform(actionString);
        /*
         * If this menu is still active, close it and all its ancestors.
         */
        GuiScreenController.closePopupMenu(this);
    }
    // *************************************************************************
    // private methods

    /**
     * Access the Nifty element for this popup menu.
     *
     * @return the pre-existing instance (not null)
     */
    private Element getElement() {
        Nifty nifty = GuiScreenController.getNifty();
        Element element = nifty.findPopupByName(popupId);

        assert element != null;
        return element;
    }
}
