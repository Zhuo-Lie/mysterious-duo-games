package com.local.tapsumo;

/** Navigation is independent of any individual game's match or physics. */
final class GameNavigator {
    enum Screen { LOBBY, PREVIEW, MATCH }
    private Screen screen=Screen.LOBBY;
    private GameCatalog.Id selected=GameCatalog.Id.SUMO;
    Screen screen(){return screen;}
    GameCatalog.Game selected(){return GameCatalog.find(selected);}
    void preview(GameCatalog.Id id){selected=GameCatalog.find(id).id;screen=Screen.PREVIEW;}
    boolean start(){
        if(!selected().playable)return false;
        screen=Screen.MATCH;return true;
    }
    void home(){screen=Screen.LOBBY;}
}
