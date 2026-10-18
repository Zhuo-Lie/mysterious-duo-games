package com.local.tapsumo;

/** Local alternating play. Gomoku uses free rules (overlines also win). */
final class BoardMatch {
    final int size, target;
    final int[][] cells;
    final int[] hx,hy;
    int turn,moves,winner=-1;
    boolean draw;
    BoardMatch(int size,int target,int starter){
        this.size=size;this.target=target;cells=new int[size][size];hx=new int[size*size];hy=new int[size*size];reset(starter);
    }
    void reset(int starter){for(int[] row:cells)java.util.Arrays.fill(row,-1);turn=starter;moves=0;winner=-1;draw=false;}
    boolean play(int x,int y){
        if(x<0||y<0||x>=size||y>=size||winner>=0||draw||cells[y][x]>=0)return false;
        int player=turn;cells[y][x]=player;hx[moves]=x;hy[moves]=y;moves++;
        int[][] dirs={{1,0},{0,1},{1,1},{1,-1}};
        for(int[] d:dirs){int count=1;
            for(int sign=-1;sign<=1;sign+=2){int xx=x+d[0]*sign,yy=y+d[1]*sign;
                while(xx>=0&&yy>=0&&xx<size&&yy<size&&cells[yy][xx]==player){count++;xx+=d[0]*sign;yy+=d[1]*sign;}}
            if(count>=target){winner=player;break;}
        }
        if(winner<0&&moves==size*size)draw=true;
        if(winner<0&&!draw)turn=1-turn;return true;
    }
    boolean undo(){
        if(moves==0||winner>=0||draw)return false;
        moves--;int x=hx[moves],y=hy[moves];turn=cells[y][x];cells[y][x]=-1;return true;
    }
}
