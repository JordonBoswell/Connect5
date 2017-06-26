package com.jordonproj.connect5.game;

import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Jordon on 14/01/2017.
 */

public class GameGrid implements Serializable
{
    private static final int HUMAN = 1;
    private static final int BLANK = 0;
    private static final int COMPUTER = -1;

    private int[][] mGameBoard;
    public static int mBoardWidth;
    public static int mBoardHeight;
    public static int mBoardSize;
    public int mPiecesPlayed;

    public GameGrid(int width, int height)
    {
        mBoardWidth=width;
        mBoardHeight=height;
        mGameBoard = new int[mBoardWidth][mBoardHeight];
        mPiecesPlayed = 0;
        mBoardSize = mBoardHeight * mBoardWidth;
    }

    public GameGrid(int width, int height, int depth, int[][] gameBoard)
    {
        mBoardWidth=width;
        mBoardHeight=height;
        mGameBoard = new int[mBoardWidth][mBoardHeight];
        for(int x = 0; x<mBoardWidth; x++)
        {
            for(int y = 0; y<mBoardHeight; y++)
            {
                mGameBoard[x][y] = gameBoard[x][y];
            }
        }
        mPiecesPlayed = depth;
        mBoardSize = mBoardHeight * mBoardWidth;
    }

    public static GameGrid copyGameGrid(GameGrid gd)
    {
        //getGameBoard was original way
        GameGrid mgd = new GameGrid(gd.getXsize(), gd.getYsize(), gd.getDepth(), gd.getGameBoard());
        return mgd;
    }

    /**
     * This method is used for reducing the number of times memory for GameGrids must be allocated.
     * There will be depth+1 game grids. The depth will be used as the index, the 0th position
     * being used to hold the current best node.
     *
     * @param dest
     * @param src
     */
    public static void copyGameGrid(GameGrid dest, GameGrid src)
    {
        dest.setDepth(src.getDepth());
        dest.setGameBoard(src.getGameBoard());
    }

    public int getXsize()
    {
        return mBoardWidth;
    }

    public int getYsize()
    {
        return mBoardHeight;
    }

    public int getDepth() { return mPiecesPlayed;  }

    public void setDepth(int depth) { mPiecesPlayed = depth; }

    public void setGameBoard(int[][] src) { mGameBoard = src; }

    public int[][] getGameBoardRef()
    {
        return mGameBoard;
    }

    /**
     *
     * @param piece The object representing the chosen selection and which player made it.
     * @return 0 for success, as the tile was blank. 1 for the tile being occupied by a human's
     * piece, -1 for it being occupied by the computer's piece.
     */
    public boolean putPiece(PieceLocation piece)
    {
        if(validatePutPiece(piece))
        {
            mGameBoard[piece.getX()][piece.getY()] = piece.getType();
            mPiecesPlayed++;
            return true;
        }
        return false;
    }

    public boolean validatePutPiece(PieceLocation piece)
    {
        int piece_x = piece.getX();
        int piece_y = piece.getY();
        if (piece_x >= mBoardWidth || piece_x < 0 || piece_y >= mBoardHeight || piece_y < 0)
        {
            return false;
        }
        //check piece can be placed in this spot
       // if()
        return mGameBoard[piece_x][piece_y] == BLANK;
    }

    /**
     *
     * @param test_piece The piece that represents the move the player has selected but not
     *                   confirmed. It is so the player may see where they have put their piece.
     *                   This is optional.
     * @return A COPY of the array representing the current state of the game board plus the
     * piece the player has indicated they'd like to play.
     */
    public synchronized int[][] getGameBoard(PieceLocation test_piece)
    {
        int[][] gb = mGameBoard;
        int[][] gb_copy = new int[mBoardWidth][mBoardHeight];
        for(int i = 0; i<mBoardWidth; i++)
        {
            for (int j = 0; j<mBoardHeight; j++)
            {
                gb_copy[i][j] = gb[i][j];
            }
        }
        if (test_piece != null) gb_copy[test_piece.getX()][test_piece.getY()] = test_piece.getType();
        return gb_copy;
    }

    public int[][] getGameBoard()
    {
        int[][] gb_copy = new int[mBoardHeight][mBoardWidth];
        for(int i = 0; i<mBoardWidth; i++)
        {
            for (int j = 0; j<mBoardHeight; j++)
            {
                gb_copy[i][j] = mGameBoard[i][j];
            }
        }
        return gb_copy;
    }

    /**
     *
     * @return Only to be used by the AI
     *
     *
     */
    public synchronized PieceLocation[] getEmptySpaces()
    {
        //create an array the exact size that's needed for all blank pieces.
        //Number of board squares - number of pieces played = number of free tiles
        int size = mBoardSize-mPiecesPlayed;
        PieceLocation[] empties = new PieceLocation[size];
        PieceLocation temp;

        //
        int i = 0;
        for (int x = 0; x<(mBoardWidth); x++)
        {
            for (int y = 0; y<(mBoardHeight); y++)
            {
                if (mGameBoard[x][y] == 0)
                {
                    //this is needed, else array out of bounds. It SHOULDN'T though
                    if(i>=size) break;
              //      Log.i("humnum", x+","+y);
                    empties[i] = new PieceLocation(x, y, -1);
                    i++;

                }
            }
        }
        return empties;
    }

    /**
     * It returns an array of piece locations, representing moves on the board the current player
     * can make.
     * COMPATIBILITY: It used to return an array of suitable empty spaces, which were simply
     * locations. As I'm now using this to return suitable MOVES the pieces must also have a type
     * so they can be played.
     * @return An array of suitable moves t make.
     */
    public PieceLocation[] worthwhileEmptySpaces(int player)
    {
        //returns empty spaces within 'range' of played pieces
        int range = 2;
        int size = mBoardSize-mPiecesPlayed;
        PieceLocation[] pla = new PieceLocation[size];
        //
        int col = 0;
        int row = 0;
        int normalised_x;
        int normalised_y;
        int i = 0;
        int location;
        for (int y = 0; y<(mBoardHeight); y++)
        {
            for (int x = 0; x<(mBoardWidth); x++)
            {
                if (mGameBoard[x][y] == 0)
                {
                    //this is needed, else array out of bounds. It SHOULDN'T though
                   // if(i>=size) break;
                    //      Log.i("humnum", x+","+y);
                    for(col = -range; col<range+1; col++)
                    {
                        for(row = -range; row<range+1; row++)
                        {

                            //normalise coordinates
                            normalised_x = normalise(x+col, mBoardWidth);
                            normalised_y = normalise(y+row, mBoardHeight);
                            if(mGameBoard[normalised_x][normalised_y] != 0)
                            {
                                PieceLocation pl = new PieceLocation(x, y, player);
                                pla[i] = pl;
                                i++;
                                col = row = range+2;
                            }
                        }
                    }
                }
            }
        }

        int num_PieceLocations = i;
        PieceLocation[] ret = new PieceLocation[num_PieceLocations];
        for(i = 0; i<num_PieceLocations; i++) ret[i] = pla[i];
        return ret;
    }

    public PieceLocation[] bestEmptySpaces(int player, PieceLocation last_played)
    {
        //returns empty spaces within 'range' of played pieces
        int range = 1;
        int size = mBoardSize-mPiecesPlayed;
        PieceLocation[] pla = new PieceLocation[size];
        //
        int col = 0;
        int row = 0;
        int normalised_x;
        int normalised_y;
        int i = 0;
        int x;
        int y;
        int last_x = last_played.getX();
        int last_y = last_played.getY();
        PieceLocation temp;
        int front_index = 0;
        int location;
        for (y = 0; y<(mBoardHeight); y++)
        {
            for (x = 0; x<(mBoardWidth); x++)
            {
                if (mGameBoard[x][y] == 0)
                {
                    for(col = -range; col<range+1; col++)
                    {
                        for(row = -range; row<range+1; row++)
                        {
                            //normalise coordinates
                            normalised_x = normalise(x+col, mBoardWidth);
                            normalised_y = normalise(y+row, mBoardHeight);
                            if(mGameBoard[normalised_x][normalised_y] != 0)
                            {
                                PieceLocation pl = new PieceLocation(x, y, player);
                                pla[i] = pl;
                                i++;
                                col = row = range+2;
                            }
                            if ( (normalised_x == last_x) && (normalised_y == last_y) )
                            {
                                temp = PieceLocation.copyPiece(pla[front_index]);
                                pla[front_index] = pla[i-1];
                                pla[i-1] = temp;
                                front_index++;
                            }
                        }
                    }
                }
            }
        }




        int num_PieceLocations = i;
        PieceLocation[] ret = new PieceLocation[num_PieceLocations];
        for(i = 0; i<num_PieceLocations; i++) ret[i] = pla[i];
        return ret;
    }

    public static int normalise(int num, int boundary)
    {
        if(num >= boundary) return num-boundary;
        else if(num < 0) return num+boundary;
        else return num;
    }

    public String getKey()
    {
        //create a unique long from the game grid
        long key;
        String ret;
        int[] too_complicated = new int[mBoardSize];
        int i=0;

        for(int y=0; y<mBoardHeight; y++)
        {
            for(int x=0; x<mBoardWidth; x++)
            {
                too_complicated[i] = mGameBoard[x][y];
                i++;
            }
        }
        ret = Integer.toString(Arrays.hashCode(too_complicated));
        return ret;
    }

    public static void printGameGrid(int[][] grid)
    {
        int width = grid[0].length;
        int height = grid.length;

        for (int y = 0; y<height; y++)
        {
            String row = new String();
            for (int x = 0; x<width; x++)
            {
                if(grid[x][y] == 1)
                {
                    row += 'x';
                }
                else if(grid[x][y] == 0)
                {
                    row+= '-';
                }
                else if(grid[x][y] == -1)
                {
                    row += 'o';
                }
            }
            System.out.println(row);
        }
    }

    public PieceLocation[] getPieces()
    {
        PieceLocation[] ret = new PieceLocation[mPiecesPlayed];
        PieceLocation temp;
        int i = 0;
        for (int y = 0; y<mBoardHeight; y++)
        {
            for(int x = 0; x<mBoardWidth; x++)
            {
                if(mGameBoard[x][y] != 0)
                {
                    temp = new PieceLocation(x, y, mGameBoard[x][y]);
                    ret[i] = temp;
                    Log.i("GameGrid", "Piece "+i+" found");
                    i++;
                }
            }
        }
        return ret;
    }

    public void setHeight(int height)
    {
        mBoardHeight = height;
    }

    public void setWidth(int width)
    {
        mBoardWidth = width;
    }

    public void setBoardSize(int size)
    {
        mBoardSize = size;
    }
}