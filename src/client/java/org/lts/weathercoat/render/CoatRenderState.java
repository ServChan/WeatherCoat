package org.lts.weathercoat.render;

public interface CoatRenderState {
    int weathercoat$wetStage();

    int weathercoat$snowStage();

    int weathercoat$stages();

    void weathercoat$setCoat(int wetStage, int snowStage, int stages);
}
