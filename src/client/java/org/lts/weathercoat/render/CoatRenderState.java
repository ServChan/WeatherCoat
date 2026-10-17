package org.lts.weathercoat.render;

public interface CoatRenderState {
    int weathercoat$wetStage();

    int weathercoat$snowStage();

    int weathercoat$frostStage();

    int weathercoat$stages();

    void weathercoat$setCoat(int wetStage, int snowStage, int frostStage, int stages);
}
