package thaumcraft.fabric.client;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

/** Loader registration only; all planes, grass and overlay geometry now live in common. */
public final class FabricTaintFibresModel {
    private FabricTaintFibresModel() {}
    public static void init() {
        ModelLoadingPlugin.register(plugin->plugin.modifyModelAfterBake().register((model,context)->{
            if(model==null||context.topLevelId()==null||context.topLevelId().variant().equals("inventory"))return model;
            var id=context.topLevelId().id();
            return id.getNamespace().equals("thaumcraft")&&id.getPath().equals("taint_fibres")
                    ? new FabricPositionBlockModels(model):model;
        }));
    }
}
