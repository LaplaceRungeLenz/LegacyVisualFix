package makamys.satchels.gui;

import com.legacyvisualfix.inventory.InventoryMotion;
import com.legacyvisualfix.inventory.InventoryMotionAccess;

/**
 * Optional-mod identity fixture only, not a Satchels renderer. The real GUI inherits this access from the GuiContainer
 * mixin; keeping the fixture independent lets unit tests exercise the allow-list without launching Minecraft.
 */
public class GuiSatchelsInventory implements InventoryMotionAccess {

    private final InventoryMotion motion = new InventoryMotion();

    @Override
    public InventoryMotion legacyvisualfix$inventoryMotion() {
        return motion;
    }
}
