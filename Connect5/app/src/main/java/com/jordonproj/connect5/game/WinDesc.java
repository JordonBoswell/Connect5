package com.jordonproj.connect5.game;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import static android.os.UserHandle.readFromParcel;

/**
 * Created by Jordon on 14/01/2017.
 */

/**
 * The WinDesc object holds the information regarding the win.
 *
 * I have organised the methods here to be in the order that they are called by the process using it.
 */
public class WinDesc
{
    static public final int PLAYER_NONE     = 0;
    static public final int PLAYER_HUMAN    = 1;
    static public final int PLAYER_AI       = -1;

    private int mPlayer = PLAYER_NONE;
    private WinCoord mStartCoord;
    private WinCoord mEndCoord;
    private int mPlane;
    private int mXInARow;

    public WinDesc()
    {
        //defaults
        mPlayer = PLAYER_NONE;
        mXInARow = 0;
    }

    public WinDesc(int player, WinCoord startcoord, WinCoord endcoord, int plane, int xInARow)
    {
        mPlayer = player;
        mStartCoord = startcoord;
        mEndCoord = endcoord;
        mPlane = plane;
        mXInARow = xInARow;
    }

    public WinDesc(int player, int xinarow)
    {
        mPlayer = player;
        mXInARow = xinarow;
    }

    public static class WinCoord
    {
        private int mX;
        private int mY;

        public WinCoord(int x, int y) {
            mX = x;
            mY = y;
        }

        public int getX() {
            return mX;
        }

        public int getY()
        {
            return mY;
        }
    }

    public int getXInARow()
    {
        return mXInARow;
    }

    public int getPlane()
    {
        return mPlane;
    }

    public WinCoord getStartWin()
    {
        return mStartCoord;
    }

    public WinCoord getEndWin()
    {
        return mEndCoord;
    }

    public int getPlayer() { return mPlayer; }
}

