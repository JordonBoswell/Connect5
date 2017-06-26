package com.jordonproj.connect5.game;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by Jeremy on 25/04/2017.
 */

public class GameState implements Serializable
{
   private static final long serialVersionUID = 16543447L;

    private int boardWidth;
    private int boardHeight;
    private boolean valid;
    private int gamePhase;
    private GameGrid gd;

    public GameState() { }
    public void setValidity(boolean validity) { valid = validity; }
    public boolean getValidity() { return valid; }
    public void setGamePhase(int phase)
    {
        gamePhase = phase;
    }
    public int getGamePhase()
    {
        return gamePhase;
    }

    public void setGameGrid(GameGrid gamegrid)
    {
        gd = gamegrid;
        boardWidth = gd.getXsize();
        boardHeight = gd.getYsize();
    }
    public GameGrid getGameGrid()
    {
        gd.setWidth(boardWidth);
        gd.setHeight(boardHeight);
        gd.setBoardSize(boardWidth * boardHeight);
        return gd;
    }
}
