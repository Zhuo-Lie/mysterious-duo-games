package com.local.tapsumo;

import java.util.HashSet;

public final class GameFrameworkTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(GameCatalog.ALL.size()==9,"Nine games registered");
        HashSet<GameCatalog.Id> ids=new HashSet<GameCatalog.Id>();int playable=0,boards=0;
        for(GameCatalog.Game game:GameCatalog.ALL){
            check(ids.add(game.id),"IDs unique");check(GameCatalog.find(game.id)==game,"Stable catalog lookup");
            check(!game.controls.isEmpty()&&!game.goal.isEmpty()&&!game.rule.isEmpty(),"All previews explain their direction");
            if(game.playable)playable++;
            if(game.category==GameCatalog.Category.BOARD)boards++;
        }
        check(playable==9,"All nine implementations available");
        check(boards==2&&GameCatalog.find(GameCatalog.Id.GOMOKU).category==GameCatalog.Category.BOARD,"Gomoku and tic tac toe grouped together");
        GameNavigator nav=new GameNavigator();check(nav.screen()==GameNavigator.Screen.LOBBY,"Cold start goes to lobby");
        for(GameCatalog.Game game:GameCatalog.ALL){
            nav.preview(game.id);check(nav.screen()==GameNavigator.Screen.PREVIEW&&nav.selected()==game,"Preview shows requested game");
            check(nav.start()==game.playable,"Unimplemented games cannot start");
            check(nav.screen()==(game.playable?GameNavigator.Screen.MATCH:GameNavigator.Screen.PREVIEW),"Failed start remains in preview");
            nav.home();check(nav.screen()==GameNavigator.Screen.LOBBY,"Every route returns home");
        }
        boolean immutable=false;try{GameCatalog.ALL.clear();}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"A screen cannot mutate the shared catalog");
        SeriesScore series=new SeriesScore();series.record(0);series.record(1);series.record(0);series.record(2);
        check(series.wins[0]==2&&series.wins[1]==1&&series.draws==1&&series.rounds()==4,"Local series aggregates all completed games");
        series.clear();check(series.rounds()==0,"Series reset");
        System.out.println("Game framework tests passed: 9 entries, preview gating, gomoku category, navigation.");
    }
}
