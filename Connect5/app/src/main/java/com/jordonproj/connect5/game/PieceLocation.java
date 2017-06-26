package com.jordonproj.connect5.game;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;

/**
 * Created by Jordon on 15/01/2017.
 */

public class PieceLocation implements Serializable
{
    static final int TYPE_EMPTY = 0;
    static final int TYPE_PLAYER = 1;
    static final int TYPE_AI = -1;

    private int mXloc;
    private int mYloc;
    private int mType;

    public PieceLocation(int x, int y, int type)
    {
        mXloc = x;
        mYloc = y;
        mType = type;
    }

    public PieceLocation(int x, int y)
    {
        mXloc = x;
        mYloc = y;
        mType = TYPE_EMPTY;
    }

    public static PieceLocation copyPiece(PieceLocation pl)
    {
        PieceLocation ret = new PieceLocation(pl.getX(), pl.getY(), pl.getType());
        return ret;
    }

    public int getX()
    {
        return mXloc;
    }

    public int getY()
    {
        return mYloc;
    }

    public int getType() { return mType; }

    public void setX(int x) { mXloc = x; }

    public void setY(int y) { mYloc = y; }

}
