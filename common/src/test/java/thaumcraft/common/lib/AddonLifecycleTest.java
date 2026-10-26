package thaumcraft.common.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import thaumcraft.api.ThaumcraftApi;

/** The add-on hook runs once, after the loader initialized every mod; late callbacks run at once. */
class AddonLifecycleTest {
    @AfterEach void reset() { AddonLifecycle.resetForTest(); }

    @Test void callbacksWaitForTheLoaderThenRunOnceInOrder() {
        AddonLifecycle.resetForTest();
        List<String> calls = new ArrayList<>();
        ThaumcraftApi.whenReady(() -> calls.add("early"));
        assertFalse(ThaumcraftApi.isReady());
        assertTrue(calls.isEmpty(), "nothing runs before the loader reports every mod initialized");
        AddonLifecycle.fireReady(List.of(new AddonLifecycle.Hook("addon", () -> calls.add("hook"))));
        assertEquals(List.of("hook", "early"), calls);
        assertTrue(ThaumcraftApi.isReady());
        ThaumcraftApi.whenReady(() -> calls.add("late"));
        assertEquals(List.of("hook", "early", "late"), calls, "a late callback runs immediately");
        AddonLifecycle.fireReady(List.of(new AddonLifecycle.Hook("addon", () -> calls.add("again"))));
        assertEquals(3, calls.size(), "the hooks run exactly once");
    }

    @Test void aFailingHookNamesItsMod() {
        AddonLifecycle.resetForTest();
        var failure = assertThrows(IllegalStateException.class, () -> AddonLifecycle.fireReady(List.of(
                new AddonLifecycle.Hook("brokenaddon", () -> { throw new IllegalArgumentException("Unknown item: x"); }))));
        assertTrue(failure.getMessage().contains("brokenaddon"), failure.getMessage());
    }
}
