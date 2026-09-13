package com.modernnh.smoke;

import java.lang.reflect.Field;

import com.modernnh.waila.chromatic.ChromaticAnimation;
import com.slprime.chromatictooltips.TooltipHandler;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.api.TooltipRequest;
import com.slprime.chromatictooltips.api.TooltipTarget;

import mcp.mobius.waila.overlay.OverlayRenderer;
import mcp.mobius.waila.overlay.Tooltip;

final class ChromaticSmoke {

    static void verifyTheme() throws Exception {
        Field field = OverlayRenderer.class.getDeclaredField("previousContext");
        field.setAccessible(true);
        TooltipContext context = (TooltipContext) field.get(null);
        WailaSmoke.require(
            context.getRenderer()
                == TooltipHandler.getRendererFor(new TooltipRequest("waila", TooltipTarget.EMPTY, null, null)),
            "stale resource pack theme");
    }

    static void verifyInventoryIsolation(Tooltip tooltip) {
        TooltipRequest request = new TooltipRequest("item", TooltipTarget.EMPTY, null, null);
        TooltipContext context = new TooltipContext(request, TooltipHandler.getRendererFor(request));
        ChromaticAnimation.beginOverlay(tooltip);
        try {
            WailaSmoke.require(ChromaticAnimation.begin(context, 20, 20) == null, "inventory tooltip animated");
        } finally {
            ChromaticAnimation.endOverlay();
        }
    }
}
