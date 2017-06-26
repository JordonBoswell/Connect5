package com.jordonproj.connect5.game;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Jordon on 14/01/2017.
 */

public class Connect5 {

    static final String tag = "connect5";

    // (x-1)*2. +0 or +1. YES
    private static final int DIAGONAL_UP = 1;
    private static final int HORIZONTAL_RIGHT = 2;
    private static final int DIAGONAL_DOWN = 3;
    private static final int VERTICAL_DOWN = 4;
    private static final int NUM_PLANES = 8; //technically 4, but ah well

    private static final int WINDOW_SIZE = 11;
    private static final int WINDOW_RADIUS = 5; //number of elements from center to outside
    private static final int WINDOW_MIDPOINT = 6; //6 is midway through 11, 5 is the middle index

    public static final int MAX_SCORE = (int) Math.pow(2, 30);

    //AI is o, player is x, blank is _
    private static final int[] AI_WIN = {-1,-1,-1,-1,-1};
    private static final int[] PLAYER_WIN = {1, 1, 1, 1, 1};
    private static final int[] WIN_START = {1, 2, 3, 4, 5};

    private static final int[] _oooo_ = {0,-1,-1,-1,-1,0};
    private static final int[] _xxxx_ = {0, 1, 1, 1, 1, 0};
    private static final int[] OPEN_FOUR_START = {1, 2, 3, 4};

    //threats. block immediately
    //block 4 in a row before 3 in a row. playtesting.
    private static final int[] oo_oo = {-1, -1, 0, -1, -1};
    private static final int[] xx_xx = {1, 1, 0, 1, 1};
    private static final int[] THREAT_ONE_START = {1, 2, 4, 5};

    private static final int[] o_ooo = {-1, 0, -1, -1, -1};
    private static final int[] x_xxx = {1, 0, 1, 1, 1};
    private static final int[] THREAT_TWO_START = {1, 3, 4, 5};

    private static final int[] __ooo__ = {0, 0, -1, -1, -1, 0, 0};
    private static final int[] __xxx__ = {0, 0, 1, 1, 1, 0, 0};
    private static final int[] THREAT_THREE_START = {1, 2, 3};

    private static final int[] xoooo_ = {1, -1, -1, -1, -1, 0};
    private static final int[] oxxxx_ = {-1, 1, 1, 1, 1, 0};
    private static final int[] THREAT_FOUR_START = {1, 2, 3, 4};

    private static final int[] _oo_o_ = {0, -1, -1, 0, -1, 0};
    private static final int[] _xx_x_ = {0, 1, 1, 0, 1, 0};
    private static final int[] THREAT_FIVE_START = {1, 3, 4};

    private static final int[] x_ooo__ = {1, 0, -1, -1, -1, 0, 0};
    private static final int[] o_xxx__ = {-1, 0, 1, 1, 1, 0, 0};
    private static final int[] THREAT_SIX_START = {1, 2, 3};

    private static final int[][] AI_THREATS = {oo_oo, o_ooo, __ooo__, xoooo_, _oo_o_, x_ooo__};
    private static final int[][] PLAYER_THREATS = {xx_xx, x_xxx, __xxx__, oxxxx_, _xx_x_, o_xxx__};
    private static final int[][] THREAT_STARTS = {THREAT_ONE_START, THREAT_TWO_START,
            THREAT_THREE_START, THREAT_FOUR_START, THREAT_FIVE_START, THREAT_SIX_START};

    private static final int[][] AI_FOURS = {oo_oo, o_ooo, xoooo_};
    private static final int[][] PLAYER_FOURS = {xx_xx, x_xxx, oxxxx_};
    private static final int[][] FOURS_STARTS = {THREAT_ONE_START, THREAT_TWO_START, THREAT_FOUR_START};

    private static final int[][] AI_THREES = {__ooo__, _oo_o_, x_ooo__};
    private static final int[][] PLAYER_THREES = {__xxx__, _xx_x_, o_xxx__};
    private static final int[][] THREES_STARTS = {THREAT_THREE_START, THREAT_FIVE_START, THREAT_SIX_START};


    private static final int DEFAULT_X = 8; //default board size
    private static final int DEFAULT_Y = 8;
    private static final int DEFAULT_DEPTH = 4;

    public static final int PLAYER_HUMAN = 1;
    public static final int PLAYER_COMPUTER = -1;

    private static final int INT_MAX = Integer.MAX_VALUE;
    private static final int INT_MIN = -INT_MAX;
    private static final int DEPTH_WHERE_EMPTIES_FASTEST = 14;

    private static final int MINWIN_COUNTERS = 5;

    private static int AI_DEPTH = DEFAULT_DEPTH;

    private GameGrid mGd;

    private PieceLocation mLastPlayerMove;

    /* Default game init */
    public void init() {
        init(DEFAULT_X, DEFAULT_Y);
    }

    public void init(int xsize, int ysize)
    {
        this.mGd = new GameGrid(xsize, ysize);
    }

    public int getAIDepth()
    {
        return AI_DEPTH;
    }


    public GameGrid getGrid() {
        return mGd;
    }


    public static Object[] best_move(GameGrid gd, PieceLocation[] pla)
    {
        Object[] ret = new Object[2];
        int[][] board;
        GameGrid current_gd_ai;
        GameGrid current_gd_player;

        int pla_size = pla.length;
        int[][] ai_sequence_array;
        int[][] player_sequence_array;
        int score = 0;
        int best_score = 0;
        PieceLocation best_piece = pla[0];
        PieceLocation ai_piece = pla[0];//just to satisfy the compiler
        PieceLocation human_piece;
        WinDesc xinarow;

        int num_ai_four_threats = 0;
        int num_player_four_threats = 0;
        int num_ai_three_threats = 0;
        int num_player_three_threats = 0;
        int num_ai_threats = 0;
        int num_player_threats = 0;

        for (int pla_index = 0; pla_index<pla_size; pla_index++)
        {


            ai_piece = pla[pla_index];
            human_piece = new PieceLocation(ai_piece.getX(), ai_piece.getY(), PLAYER_HUMAN);
            current_gd_ai = GameGrid.copyGameGrid(gd);
            current_gd_player = GameGrid.copyGameGrid(gd);
            current_gd_ai.putPiece(ai_piece);
            current_gd_player.putPiece(human_piece);

            ai_sequence_array = createSequenceArray(current_gd_ai, ai_piece);
            player_sequence_array = createSequenceArray(current_gd_player, human_piece);

            num_ai_four_threats = checkFourThreats(ai_piece, ai_sequence_array);
            num_player_four_threats = checkFourThreats(human_piece, player_sequence_array);

            num_ai_three_threats = checkThreeThreats(ai_piece, ai_sequence_array);
            num_player_three_threats = checkThreeThreats(human_piece, player_sequence_array);

            num_ai_threats = num_ai_four_threats + num_ai_three_threats;
            num_player_threats = num_player_four_threats + num_player_three_threats;

            if (checkIfWin(ai_piece, ai_sequence_array))
            {
                score += MAX_SCORE;
                Log.d(tag, "ai win found");
            }
            if (checkIfWin(human_piece, player_sequence_array))
            {
                score += MAX_SCORE/2;
                Log.d(tag, "player win blocked");
            }
            if (checkIfCreatesOpenFour(ai_piece, ai_sequence_array))
            {
                score += MAX_SCORE/4;
                Log.d(tag, "open four created");
            }
            if (checkIfCreatesOpenFour(human_piece, player_sequence_array))
            {
                score += MAX_SCORE/8;
                Log.d(tag, "open four creation blocked");
            }

            if (num_ai_threats >= 2) score += MAX_SCORE/16;
            if (num_player_threats >= 2) score += MAX_SCORE/32;
            if (num_ai_four_threats > 0) score += MAX_SCORE/64;
            if (num_player_four_threats > 0) score += MAX_SCORE/128;
            if (num_ai_three_threats > 0) score += MAX_SCORE/256;
            if (num_player_three_threats > 0) score += MAX_SCORE/512;

            //how many in a row
            xinarow = win_analysis(current_gd_ai, ai_piece);
            score += xinarow.getXInARow() * 4;
            xinarow = win_analysis(current_gd_player, human_piece);
            score += xinarow.getXInARow();

            if(score > best_score)
            {
                best_score = score;
                best_piece = ai_piece;
            }
            score = 0;
        }
        ret[0] = best_piece;
        ret[1] = best_score;
        return ret;
    }

    private static int[][] createSequenceArray(GameGrid gd, PieceLocation piece_location)
    {
        int boardWidth = gd.getXsize();
        int boardHeight = gd.getYsize();
        int[][] board = gd.getGameBoard(piece_location);
        int[][] sequence_array = new int[8][11];
        int piece_x = piece_location.getX();
        int piece_y = piece_location.getY();
        board[piece_x][piece_y] = piece_location.getType();

        int current_x;
        int current_y;

        //  gd.printGameGrid(board);
        int i;

        //fill up the arrays with the pieces to be analysed.
        //diagonally up
        for (i = 0; i<WINDOW_SIZE; i++)
        {
            //make sure coords are on board.
            current_x = GameGrid.normalise(piece_x - WINDOW_RADIUS + i, boardWidth);
            current_y = GameGrid.normalise(piece_y - WINDOW_RADIUS + i, boardHeight);
            sequence_array[(DIAGONAL_UP-1) *2]   [i] = board[current_x][current_y];
            sequence_array[((DIAGONAL_UP-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
        }

        //horizontal
        current_y = piece_y;
        for (i = 0; i<WINDOW_SIZE; i++)
        {
            current_x = gd.normalise(piece_x - WINDOW_RADIUS + i, boardWidth);
            sequence_array[(HORIZONTAL_RIGHT-1) *2][i] = board[current_x][current_y];
            sequence_array[((HORIZONTAL_RIGHT-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
        }

        //diagonally down
        for (i = 0; i<WINDOW_SIZE; i++)
        {
            current_x = gd.normalise(piece_x - WINDOW_RADIUS + i, boardWidth);
            current_y = gd.normalise(piece_y + WINDOW_RADIUS - i, boardHeight);
            sequence_array[(DIAGONAL_DOWN-1) *2][i] = board[current_x][current_y];
            sequence_array[((DIAGONAL_DOWN-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
        }

        //vertical
        current_x = piece_x;
        for(i = 0; i<WINDOW_SIZE; i++)
        {
            current_y = gd.normalise(piece_y + WINDOW_RADIUS - i, boardHeight);
            sequence_array[(VERTICAL_DOWN-1) *2][i] = board[current_x][current_y];
            sequence_array[((VERTICAL_DOWN-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
        }

        return sequence_array;
    }

    private static boolean checkIfWin(PieceLocation piece_location, int[][] sequence_arrays)
    {
        if(piece_location.getType() == PLAYER_COMPUTER)
        {
            for(int i = 0; i<NUM_PLANES; i+=2)
            {
                if (sequenceMatch(AI_WIN, sequence_arrays[i], WIN_START))
                {
                    Log.i(tag, "AI win recognised");
                    return true;
                }
            }
        }
        else
        {
            for(int i = 0; i<NUM_PLANES; i+=2)
            {
                if (sequenceMatch(PLAYER_WIN, sequence_arrays[i], WIN_START))
                {
                    Log.i(tag, "AI win recognised");
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkIfCreatesOpenFour(PieceLocation piece_location, int[][] sequence_arrays)
    {
        if(piece_location.getType() == PLAYER_COMPUTER)
        {
            for(int i = 0; i<NUM_PLANES; i++)
            {
                if(sequenceMatch(_oooo_, sequence_arrays[i], OPEN_FOUR_START))
                    return true;
            }
        }
        else
        {
            for(int i = 0; i<NUM_PLANES; i++)
            {
                if(sequenceMatch(_xxxx_, sequence_arrays[i], OPEN_FOUR_START ))
                    return true;
            }
        }
        return false;
    }

    private static int checkFourThreats(PieceLocation piece_location, int[][] sequence_array)
    {
        int num_threats = 0;
        boolean threat_in_plane = false;
        int[][] four_threats;

        if(piece_location.getType() == PLAYER_COMPUTER)
        {
            four_threats = AI_FOURS;
        }
        else
        {
            four_threats = PLAYER_FOURS;
        }
        //check if a plane has a threat
        for (int i = 0; i<NUM_PLANES/2; i++)
        {
            for (int j = 0; j<2; j++) //for each direction in plane
            {
                if(sequenceMatch(four_threats[0], sequence_array[(i*2) +j], FOURS_STARTS[0])
                    || sequenceMatch(four_threats[1], sequence_array[(i*2) +j], FOURS_STARTS[1])
                    || sequenceMatch(four_threats[2], sequence_array[(i*2) +j], FOURS_STARTS[2]))
                {
                    threat_in_plane = true;
                }
            }
            if (threat_in_plane) num_threats++;
            threat_in_plane = false;
        }
        return num_threats;
    }

    private static int checkThreeThreats(PieceLocation piece_location, int[][] sequence_array)
    {
        int num_threats = 0;
        boolean threat_in_plane = false;
        int[][] three_threats;
        if(piece_location.getType() == PLAYER_COMPUTER)
        {
            three_threats = AI_THREES;
        }
        else
        {
            three_threats = PLAYER_THREES;
        }
        for(int i = 0; i<NUM_PLANES/2; i++)
        {
            for(int j = 0; j<2; j++)
            {
                if(sequenceMatch(three_threats[0], sequence_array[(i*2)+j], THREES_STARTS[0])
                        || sequenceMatch(three_threats[1], sequence_array[(i*2)+j], THREES_STARTS[1])
                        || sequenceMatch(three_threats[2], sequence_array[(i*2)+j], THREES_STARTS[2]))
                {
                    threat_in_plane = true;
                }
            }
            if (threat_in_plane) num_threats++;
            threat_in_plane = false;
        }
        return num_threats;
    }

    public static Object[] best_location(GameGrid gd, PieceLocation[] pla)
    {
        Object[] ret = new Object[2];
        int win = 0;
        int open_four = 0;
        int new_threat_counter = 0;
        int num_in_open_direction = 0;
        int opponent_win = 0;
        int opponent_new_threat = 0;
        int opponent_denied_new_threat = 0;

        int[][] sequence_array = new int[8][WINDOW_SIZE];


        int pla_size = pla.length;
        int pla_index = 0;
        int best_piece_index = 0;
        int best_score = 0;
        int current_score = 0;
        int piece_x;
        int piece_y;
        int current_x;
        int current_y;

        int boardWidth = gd.getXsize();
        int boardHeight = gd.getYsize();
        int[][] board;

        //for all given pieces
        for(pla_index = 0; pla_index<pla_size; pla_index++)
        {
            board = gd.getGameBoard(pla[pla_index]);
            piece_x = pla[pla_index].getX();
            piece_y = pla[pla_index].getY();
            board[piece_x][piece_y] = -1;

          //  gd.printGameGrid(board);
            int i;
            int start = 0; //where in the 11 array the start of the sequence is being matched

            //fill up the arrays with the pieces to be analysed.
            //diagonally up
            for (i = 0; i<WINDOW_SIZE; i++)
            {
                //make sure coords are on board.
                current_x = gd.normalise(piece_x - WINDOW_RADIUS + i, boardWidth);
                current_y = gd.normalise(piece_y - WINDOW_RADIUS + i, boardHeight);
                sequence_array[(DIAGONAL_UP-1) *2]   [i] = board[current_x][current_y];
                sequence_array[((DIAGONAL_UP-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
            }

            //horizontal
            current_y = piece_y;
            for (i = 0; i<WINDOW_SIZE; i++)
            {
                current_x = gd.normalise(piece_x - WINDOW_RADIUS + i, boardWidth);
                sequence_array[(HORIZONTAL_RIGHT-1) *2][i] = board[current_x][current_y];
                sequence_array[((HORIZONTAL_RIGHT-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
            }

            //diagonally down
            for (i = 0; i<WINDOW_SIZE; i++)
            {
                current_x = gd.normalise(piece_x - WINDOW_RADIUS + i, boardWidth);
                current_y = gd.normalise(piece_y + WINDOW_RADIUS - i, boardHeight);
                sequence_array[(DIAGONAL_DOWN-1) *2][i] = board[current_x][current_y];
                sequence_array[((DIAGONAL_DOWN-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
            }

            //vertical
            current_x = piece_x;
            for(i = 0; i<WINDOW_SIZE; i++)
            {
                current_y = gd.normalise(piece_y + WINDOW_RADIUS - i, boardHeight);
                sequence_array[(VERTICAL_DOWN-1) *2][i] = board[current_x][current_y];
                sequence_array[((VERTICAL_DOWN-1) *2) +1][WINDOW_SIZE - 1 - i] = board[current_x][current_y];
            }

            //arrays filled. time to analyse them.

            ////////////////////////CHECK FOR WIN///////////////////////////////////////////////
            int sequence_start;
            int sequence_index;

            for(i = 0; i<NUM_PLANES; i+=2)
            {
                if(sequenceMatch(AI_WIN, sequence_array[i], WIN_START) == true)
                {
                    Log.i(tag, "Win recognised");
                    ret[0] = pla[pla_index];
                    ret[1] = MAX_SCORE;
                    return ret;
                }
            }

            ////////////////////////BLOCK OPPONENT WIN///////////////////////////////////////////
            //block creation of 5 or open 4
            // recognise that if the enemy goes here, they have 5 in a row or open 4 OR DOUBLE THREAT
            //TODO include support for sensing double threats
            for(i = 0; i<NUM_PLANES; i++)
            {
                int[] block_five = new int[]{1, 1, 1, 1, 1};
                int[] block_five_start = new int[]{1, 2, 3, 4};
                int[] block_open_four = new int[]{0, 1, 1, 1, 1, 0};
                int[] block_open_four_start = block_five_start;

                int[] sequence_if_opponent_played = Arrays.copyOf(sequence_array[i], sequence_array[i].length);
                sequence_if_opponent_played[WINDOW_MIDPOINT-1] = 1; //5

                if( (sequenceMatch(PLAYER_WIN, sequence_if_opponent_played, WIN_START) == true) ||
                        (sequenceMatch(block_open_four, sequence_if_opponent_played, block_open_four_start) == true))
                {
                    current_score=MAX_SCORE/2;
                    Log.i(tag, "This is a defensive move");
                }
            }

            /////////////////////////CHECK FOR OPEN FOUR AND NUM THREATS/////////////////////////
            //An open four is a guaranteed win, so isn't exactly a threat anymore.
            int num_threats = 0;
            boolean threat_in_plane;
            boolean open_four_present = false;

            for(i = 0; i<NUM_PLANES/2; i++)
            {
                //for each of 4 planes, check if threat created
                threat_in_plane = false;
                for(int j = 0; j<2; j++)
                {
                    //////////////CHECK OPEN FOUR
                    int[] open_four_start = new int[]{1, 2, 3, 4};
                    if(sequenceMatch(_oooo_, sequence_array[(2*i)+j], open_four_start))
                    {
                        threat_in_plane = true;
                        open_four_present = true;
                        Log.i(tag, "Open four found");
                    }
                    /////////////CHECK THREATS
                    for(int t = 0; t<5; t++)
                    {
                        if(sequenceMatch(AI_THREATS[t], sequence_array[(2*i)+j], THREAT_STARTS[t]))
                        {
                            threat_in_plane = true;
                            Log.i(tag, "Ability to create threat found");
                        }
                    }
                }
                ///////COUNT NUMBER OF THREATS
                if( threat_in_plane)
                {
                    num_threats++;
                }
            }
            if(open_four_present)
            {
                current_score += MAX_SCORE/4;
            }
            if(num_threats == 3) //very unlikely, and just as much of a win as there being 2
            {
                current_score += MAX_SCORE/8;
            }
            else if(num_threats == 2) //creates a double threat, and a won game
            {
                current_score += MAX_SCORE/16;
            }
            else if(num_threats == 1)
            {
                current_score += MAX_SCORE/32;
            }

            ///////////////STOP OPPONENT CREATING THREAT//////////////////////////////////////

            num_threats = 0;
            threat_in_plane = false;
            int[] sequence_if_opponent_played;
            for(i = 0; i<NUM_PLANES/2; i++)
            {
                for(int j = 0; j<2; j++)
                {
                    sequence_if_opponent_played = Arrays.copyOf(sequence_array[(i*2)+j], sequence_array[(i*2)+j].length);
                    sequence_if_opponent_played[WINDOW_MIDPOINT-1] = 1; //5

                    for(int t = 0; t<5; t++)
                    {
                        if(sequenceMatch(PLAYER_THREATS[t], sequence_array[(2*i)+j], THREAT_STARTS[t]))
                        {
                            threat_in_plane = true;
                            Log.i(tag, "Block creation of player's threat");
                        }
                    }
                }
                if(threat_in_plane) num_threats++;
                threat_in_plane = false;
            }
            if(num_threats == 3) current_score += MAX_SCORE/64;
            else if(num_threats >= 2) current_score += MAX_SCORE/128;
            else if(num_threats >= 1) current_score += MAX_SCORE/256;

/**
            //choose highest x in a row
            WinDesc temp;
            PieceLocation tempa = pla[pla_index];
            temp = win_analysis(gd, tempa);
            current_score += temp.getXInARow() * 2;
            tempa = new PieceLocation(tempa.getX(), tempa.getY(), PLAYER_HUMAN);
            temp = win_analysis(gd, tempa);
            current_score+= temp.getXInARow();
*/
            ////////////////////////DEAL WITH SCORE//////////////////////////////////////////////
            if(current_score>best_score)
            {
                best_score = current_score;
                best_piece_index = pla_index;
            }
            current_score = 0;
        }
        ret[0] = pla[best_piece_index]; //best PieceLocation
        ret[1] = best_score;
        return ret;
    }

//    private int checkWin(int player, int[][] sequence_array,)

    //private static boolean DetectThreat

    //the key sequence is what is being searched for in the candidate sequence.
    //the starts indexes are the locations where the sequence is valid if it starts there.
    private static boolean sequenceMatch(int[] key, int[] candidate, int[] candidate_starts)
    {
        int starts_index = 0;//maybe come up with a better name
        int match = 0;
        int sequence_index = 0;

        for (starts_index = 0; starts_index < candidate_starts.length; starts_index++)
        {
            sequence_index = candidate_starts[starts_index];
            for (int x = 0; x < key.length; x++)
            {
                if (candidate[sequence_index] == key[x])
                {
                    match++;
                    if (match == key.length)
                    {
                    //    Log.i(tag, "Match recognised");
                        return true;
                    }
                    sequence_index++;
                }
                else
                {
                    match = 0;
                    break;
                }
            }
        }
        return false;
    }

    public static void printArray(int[] print)
    {
        String print_this = new String();
        int size = print.length;

        for(int i = 0; i<size; i++)
        {
            print_this += print[i];
        }

        Log.d(tag, print_this);
    }
    /**
     * @param gd
     * @param pla The array of moves t
     * @return The move that is part of the most in a row. For now only look at one
     * <p>
     * This is a big function because it needs to run as fast as possible. Method calls slow it
     * down.
     */
    public static synchronized WinDesc win_analysis(GameGrid gd, PieceLocation pla) {
        WinDesc wd;
        WinDesc temp;
        int startx, starty, endx, endy;
        int direction = 0;
        int player;

        int boardWidth = gd.getXsize();
        int boardHeight = gd.getYsize();
        int[][] board = gd.getGameBoard();

        int start_x;
        int start_y;
        int end_x;
        int end_y;

        int proposed_x;
        int proposed_y;
        boolean endOfLine;
        //  int direction_y = 0; //calm the compiler warnings
        //  int direction_x = 0;

        int xInARow = 1;
        int highest_xInARow = 1;
        int highest_direction = 0;
        int flip;
        // int num_subjects = pla.length;

        //  for (int i = 0; i<num_subjects; i++)

        //This block checks the left and right sides of the piece only
        //this will run faster once I unravel it
        int i = 0;
        //  while (i<length+1)
        //  {
        int piece_x = pla.getX();
        int piece_y = pla.getY();
        player = pla.getType();

        //check diagonally up in coords

        start_x = end_x = piece_x;
        start_y = end_y = piece_y;
        for (flip = -1; flip < 2; flip += 2) {
            proposed_x = piece_x + flip;
            proposed_y = piece_y + flip;
            endOfLine = false;

            while (!endOfLine) {
                //make sure coordinates are actually on the board
                if (proposed_x >= boardWidth) proposed_x -= boardWidth;
                if (proposed_y >= boardHeight) proposed_y -= boardHeight;
                if (proposed_x < 0) proposed_x += boardWidth;
                if (proposed_y < 0) proposed_y += boardHeight;

                if (board[proposed_x][proposed_y] == player) {
                    xInARow++;
                    if (flip == -1) {
                        start_x = proposed_x;
                        start_y = proposed_y;
                    } else {
                        end_x = proposed_x;
                        end_y = proposed_y;
                    }
                    proposed_x += flip;
                    proposed_y += flip;
                } else {
                    endOfLine = true;
                }
            }
        }
        if (xInARow >= 5) {
            wd = new WinDesc(player, new WinDesc.WinCoord(start_x, start_y),
                    new WinDesc.WinCoord(end_x, end_y), DIAGONAL_UP, xInARow);
            return wd;
        }
        if (xInARow > highest_xInARow)
        {
            highest_xInARow = xInARow;
            highest_direction = DIAGONAL_UP;
        }
        xInARow = 1;

        start_x = end_x = piece_x;
        start_y = end_y = piece_y;
        //check horizontal, HORIZONTAL RIGHT
        for (flip = -1; flip < 2; flip += 2) {
            proposed_x = piece_x + flip;
            proposed_y = piece_y;
            endOfLine = false;

            while (!endOfLine) {
                //make sure coordinates are actually on the board
                if (proposed_x >= boardWidth) proposed_x -= boardWidth;
                if (proposed_x < 0) proposed_x += boardWidth;

                if (board[proposed_x][proposed_y] == player) {
                    xInARow++;
                    if (flip == -1) {
                        start_x = proposed_x;
                        start_y = proposed_y;
                    } else {
                        end_x = proposed_x;
                        end_y = proposed_y;
                    }
                    proposed_x += flip;
                } else endOfLine = true;
            }
        }
        if (xInARow >= 5) {
            wd = new WinDesc(player, new WinDesc.WinCoord(start_x, start_y),
                    new WinDesc.WinCoord(end_x, end_y), HORIZONTAL_RIGHT, xInARow);
            return wd;
        }
        if (xInARow > highest_xInARow)
        {
            highest_xInARow = xInARow;
            highest_direction = HORIZONTAL_RIGHT;
        }
        xInARow = 1;

        start_x = end_x = piece_x;
        start_y = end_y = piece_y;
        //check diagonally down, DIAGONALLY UP VISUALLY
        for (flip = -1; flip < 2; flip += 2) {
            proposed_x = piece_x + flip;
            proposed_y = piece_y - flip;
            endOfLine = false;

            while (!endOfLine) {
                //make sure coordinates are actually on the board
                if (proposed_x >= boardWidth) proposed_x -= boardWidth;
                if (proposed_y >= boardHeight) proposed_y -= boardHeight;
                if (proposed_x < 0) proposed_x += boardWidth;
                if (proposed_y < 0) proposed_y += boardHeight;

                if (board[proposed_x][proposed_y] == player) {
                    xInARow++;
                    if (flip == -1) {
                        start_x = proposed_x;
                        start_y = proposed_y;
                    } else {
                        end_x = proposed_x;
                        end_y = proposed_y;
                    }
                    proposed_x += flip;
                    proposed_y -= flip;
                } else endOfLine = true;
            }
        }
        if (xInARow >= 5) {
            wd = new WinDesc(player, new WinDesc.WinCoord(start_x, start_y),
                    new WinDesc.WinCoord(end_x, end_y), DIAGONAL_DOWN, xInARow);
            return wd;
        }
        if (xInARow > highest_xInARow)
        {
            highest_xInARow = xInARow;
            highest_direction = DIAGONAL_DOWN;
        }
        xInARow = 1;

        start_x = end_x = piece_x;
        start_y = end_y = piece_y;
        //check vertical, VERTICALLY DOWN
        for (flip = -1; flip < 2; flip += 2) {
            proposed_x = piece_x;
            proposed_y = piece_y + flip;
            endOfLine = false;

            while (!endOfLine) {
                //make sure coordinates are actually on the board
                if (proposed_y >= boardHeight) proposed_y -= boardHeight;
                if (proposed_y < 0) proposed_y += boardHeight;

                if (board[proposed_x][proposed_y] == player) {
                    xInARow++;
                    if (flip == -1) {
                        start_x = proposed_x;
                        start_y = proposed_y;
                    } else {
                        end_x = proposed_x;
                        end_y = proposed_y;
                    }
                    proposed_y += flip;
                } else endOfLine = true;
            }
        }
        if (xInARow >= 5) {
            wd = new WinDesc(player, new WinDesc.WinCoord(start_x, start_y),
                    new WinDesc.WinCoord(end_x, end_y), VERTICAL_DOWN, xInARow);
            return wd;
        }
        if (xInARow > highest_xInARow)
        {
            highest_xInARow = xInARow;
            highest_direction = VERTICAL_DOWN;
        }
        // if(wd == null ||  )
        //   i++;
        // }

        //churning is finished, send off result
        wd = new WinDesc(player, new WinDesc.WinCoord(start_x, start_y),
                new WinDesc.WinCoord(end_x, end_y), highest_direction, highest_xInARow);
        return wd;
        //will be used by AiCalc which will use this heavily
        //is used in winanal thread
    }


    //I need to store best where /all/ threads can access it. The current board will never be used
    // for key creation by this algorithm in the future. I could use its key to store the current
    //best value. Hashmap doesn't allow clashes.
   public static Object[] MiniMaxRootNode(GameGrid gd, ConcurrentHashMap hashMap, PieceLocation[] children)
   {
       String key = gd.getKey();
       int shared_alpha; //local copy of threadpool-wide alpha
       Object[] ret = new Object[2];
       PieceLocation best_child = children[0];//default
       int alpha = INT_MIN; //
       int current;
       int num_children;
       num_children = children.length;

       for(int i= 0; i<num_children; i++)
       {
           GameGrid grid_try = GameGrid.copyGameGrid(gd);
           current = -FreshAlphaBeta(grid_try, children[i], INT_MIN, INT_MAX, AI_DEPTH-1, PLAYER_HUMAN, hashMap);
           if(current > alpha)
           {
               alpha = current;
               best_child = children[i];
           }
           shared_alpha = (int) hashMap.get(key);
           if(shared_alpha > alpha) alpha = shared_alpha;
           else if(shared_alpha < alpha) hashMap.put(key, alpha);
       }
       ret[0] = best_child;
       ret[1] = alpha;
      // Log.i(tag, "Portion of nodes that transposition tables returned: "+(double) debugNodesCutoff/debugTotalNodes);
      // debugNodesCutoff=0;
      // debugTotalNodes=0;
       return ret;
   }

    public static int FreshAlphaBeta(GameGrid gd, PieceLocation pl, int alpha, int beta, int moves_left, int player, ConcurrentHashMap hashmap)
    {
        int current; //value of child currently being analysed
        PieceLocation[] children; //the pieces the current player is considering playing
        int num_children;
        PieceLocation piece_try;
        GameGrid grid_try;
        String key;
        int best;

        current = win_analysis(gd, pl).getXInARow();
        if (current >= 5) return INT_MIN;
        if (moves_left == 0) return - (int) Math.pow(10, (double) current);
        gd.putPiece(pl);
        best = current; //remove if unhelpful

        //check if node already explored. If it has, return the value held in hashmap.
        key = gd.getKey();
        if(hashmap.containsKey(key))
        {
            //Log.i(tag, "Node ALREADY VISITED");
            return (int) hashmap.get((key));
        }

        children = gd.bestEmptySpaces(player, pl);
        num_children = children.length;
        for(int i=0; i<num_children; i++)
        {
            grid_try = GameGrid.copyGameGrid(gd);
            current = -FreshAlphaBeta(grid_try, children[i], -beta, -alpha, moves_left-1, player*-1, hashmap); //-alphabeta
            if(current>alpha) alpha = current;
            if(alpha >= beta) return alpha+best; //KEEP THIS AS IS OR YOU BREAK IT
        }
        //if key not in table, put key & value in table
        if(hashmap.containsKey(key) == false)
        {
            //Log.i(tag, "Node stored");
            hashmap.put(key, alpha);
        }
       // if (best>alpha) return - (int) Math.pow(10, (double) best);
        return alpha+best;

    }

    public GameState getGameState()
    {
        GameState gs = new GameState();
        gs.setGameGrid(mGd);
        return gs;
    }

    public void setGameState(GameState gs)
    {
        //update game status items
        mGd = gs.getGameGrid();
    }
}


/*****************************************************************************************
 * OLD AI
 */
    /**
    public static synchronized int FreshMiniMax(GameGrid gd, PieceLocation pl, int moves_left, int player)
    {
        int max = INT_MIN;
        int current;
        int[][] children;
        int num_children;
        PieceLocation piece_try;
        GameGrid grid_try;

        current = win_analysis(gd, pl).getXInARow();
        if (current >= 5) return -10000000;
        if (moves_left == 0) return -(int) Math.pow(10, (double) current/2);
        gd.putPiece(pl);
        children = gd.worthwhileEmptySpaces();
        num_children = children.length;

        for(int i = 0; i<num_children; i++)
        {
            piece_try = new PieceLocation(children[i][0], children[i][1], player);
            grid_try = GameGrid.copyGameGrid(gd);
            current = -FreshMiniMax(grid_try, piece_try, moves_left-1, player*-1);
            if(current>=max) max = current;
        }
        return max;
    }
}

    public synchronized static PieceLocation MiniMaxRootNodeOld(GameGrid gd) {
        //do only minimax for now, no AlphaBeta.
        //get children. looking at 1 child and no deeper is looking 1 move ahead, to a depth of 1.
        //keep track of best child, child that returns highest is best child.
        int best_x = 1; //this is just to keep the compiler happy, these two will always be initialised
        int best_y = 1;
        int moves_left = DEFAULT_DEPTH; //depth, but an easier way of understanding it
        int[][] children;
        int num_children;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int temp_max;

        children = gd.worthwhileEmptySpaces();
        num_children = children.length;

        for (int i = 0; i < num_children; i++) //for all children
        {
            Log.d(tag, "Considering child " + children[i][0] + "," + children[i][1]+" index is "+i);
            //node scores are purely based on winning moves. wins have a score of int.max/min
            PieceLocation pl = new PieceLocation(children[i][0], children[i][1], PLAYER_COMPUTER);
            GameGrid child_grid = GameGrid.copyGameGrid(gd);
            //if you can instantly win, do it.
            //  if(win_analysis(child_grid, pl).getXInARow() >=5 ) { return pl; }
           // child_grid.putPiece(pl);//the child grid needs to have child move to be different
            //the Ai minimiser wants the lowest scored node.
           // temp_min = MiniMax(child_grid, moves_left, PLAYER_HUMAN);
         //   temp_min = -AlphaBeta(child_grid, pl, moves_left, -beta, -alpha, PLAYER_HUMAN);
            temp_max = -MiniMax(child_grid, pl, moves_left, PLAYER_HUMAN);
            if (temp_max > max)
            {
                max = temp_max;
                best_x = children[i][0];
                best_y = children[i][1];
                Log.i(tag, "Best child is at "+best_x+","+best_y+" with score of "+min);
            }
        }
        Log.i(tag, "min = "+min);
        return new PieceLocation(best_x, best_y, -1);
    }

    public static synchronized int AlphaBeta(GameGrid gd, PieceLocation pl, int moves_left, int alpha, int beta, int player)
    {
        int leaf = win_analysis(gd, pl).getXInARow();
        if (leaf >=5) return INT_MIN;
        if( moves_left == 0) return -leaf;

        gd.putPiece(pl);
        int child_score;
        int[][] children = gd.worthwhileEmptySpaces();
        int num_children = children.length;
        for(int i=0; i<num_children; i++)
        {
            PieceLocation pl_next = new PieceLocation(children[i][0], children[i][1], player);
            GameGrid child_grid = GameGrid.copyGameGrid(gd);
            child_score = -AlphaBeta(child_grid, pl_next, moves_left-1, -beta, -alpha, player*-1);
            if (child_score>alpha) alpha = child_score;
            if (alpha>beta) return alpha;
        }
        return alpha;
    }

    public static synchronized int NewMiniMax(GameGrid parent, PieceLocation piece, int player)
    {
        //get child with highest value
        int best = INT_MIN;
        int current = best;

        //if leaf node or max depth reached, return heuristic value
        current = win_analysis(parent, piece).getXInARow();
        if(current>=5) return INT_MIN; //If this node is a win for the parent, make it unignorable
      //  else if(moves_left == 0) return -current;
       // int[][] children =

        //find which child has highest value
      //  current = -NewMiniMax();
        if (current > best) best = current;

        return best;
    }

    public static synchronized int MiniMax(GameGrid gd, PieceLocation pl, int moves_left, int player)
    {
        int best = INT_MIN;
        int current;
        int[][] children;
        int num_children;

        current = win_analysis(gd, pl).getPlane();
        if(current >= 5) return -100;
        else if (moves_left == 0) return current*10;//-current*10;

        gd.putPiece(pl);
        //previous player has seen if this is leaf node. it's all current player now
        children = gd.worthwhileEmptySpaces();
        num_children = children.length;

        for(int i=0; i<num_children; i++)
        {
            PieceLocation child_piece = new PieceLocation(children[i][0], children[i][1], player);
            GameGrid child_grid = GameGrid.copyGameGrid(gd);
            current = -MiniMax(child_grid, child_piece, moves_left-1, player*-1);
            if (current>best) best=current;
        }
        return best;
    }

}


    /**
    public static synchronized int MiniMax(GameGrid gd, int moves_left, int player)
    {
        //  Log.i(tag, "Minimax called");
        int score = 0;
        //get children
        int[][] children = gd.getEmptySpaces();
        int num_children = children.length;// / 2;

        if (player == PLAYER_COMPUTER) //minimiser
        {
            int min = INT_MAX;
            int temp_min;
            for (int i = 0; i < num_children; i++)
            {
                PieceLocation pl = new PieceLocation(children[i][0], children[i][1], player);
                GameGrid child_grid = GameGrid.copyGameGrid(gd);
                //if can win, is leaf node.
                score = win_analysis(child_grid, pl).getXInARow();
                if (score >= 5) return -5;
                else if (moves_left == 1) return -score;
                child_grid.putPiece(pl);
                temp_min = MiniMax(child_grid, moves_left - 1, PLAYER_HUMAN);
                if (temp_min < min) min = temp_min;
            }
            return min;
        }
        else //player is human
        {
            int max = INT_MIN;
            int temp_max;
            for (int i = 0; i < num_children; i++)
            {
                PieceLocation pl = new PieceLocation(children[i][0], children[i][1], player);
                GameGrid child_grid = GameGrid.copyGameGrid(gd);
                //if can win, is leaf node.
                score = win_analysis(child_grid, pl).getXInARow();
                if (score >= 5) return 5;
                else if (moves_left == 1) return score;
                child_grid.putPiece(pl);
                temp_max = MiniMax(child_grid, moves_left - 1, PLAYER_COMPUTER);
                if (temp_max > max) max = temp_max;
            }
            return max;
        }
    }
}





    /**
     * This function is to be called by the AI thread at the root node of the AI's turn. It sets up
     * the algorithm and returns the piece location.
     * Comments assume current player is minimiser.
     *
     * @param gd    The board as is visible to the player.
     *
     * @return      The best place to play, as the AI can see it.

    /**
    public synchronized static PieceLocation doAI(GameGrid gd)
    {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int best_x = -1;
        int best_y = -1;
        int score;
        //to reduce memory allocation operations
       // GameGrid[] nodes = new GameGrid[DEFAULT_DEPTH +2];
      //  nodes[0] = gd;
        int[] moves = gd.getEmptySpaces();

        //the root node of gga is at the end. This lets depth_left be the index.
        GameGrid[] gga = new GameGrid[DEFAULT_DEPTH+2];
        for(int i=0; i<DEFAULT_DEPTH+1; i++)
        {
            gga[i] = GameGrid.copyGameGrid(gd); //initialises all to root node grid. They get changed
        }
        int length = moves.length/2;
        int i=0;
        while (i<length)
        {
            PieceLocation pl = new PieceLocation(moves[i], moves[i+1], PLAYER_COMPUTER); //this piece is the one being played
            if(win_analysis(gga[DEFAULT_DEPTH], pl).getXInARow() >= 5) return pl; //if instant win, take it
            gga[DEFAULT_DEPTH-1].putPiece(pl);
            //if child is an instant win, take it

       //     nodes[1] = next_grid;
            score = MiniMaxAB(gga, alpha, beta, PLAYER_HUMAN, DEFAULT_DEPTH-1);
            if(score < beta)
            {
                beta = score;
                best_x = pl.getX();
                best_y = pl.getY();
            }
            i+=2;
        }
        PieceLocation pla = new PieceLocation(best_x, best_y, -1);
        return pla;
    }

    private synchronized static int MiniMaxAB(GameGrid[] gga, int alpha, int beta, int player, int depth_left)
    {
        int score;
        int current_alpha = alpha;
        int current_beta = beta;
        //make list of available moves
        int[] moves = gga[depth_left].getEmptySpaces();
        int length = moves.length/2;
        int i = 0;
        if (player == PLAYER_COMPUTER) //AI is minimiser.
        {
            score = Integer.MAX_VALUE;
            while (i < length)
            {
                //copy game that parent node gave you (depth_left) and place it as your child node.
                //get the xInARow score from what happens when you put a piece in it
                //if the child is a leaf node, at depth 0, do not call minimax again.
                PieceLocation pl = new PieceLocation(moves[i], moves[i+1], player);
                GameGrid.copyGameGrid(gga[depth_left-1], gga[depth_left]);
             //   score = -win_analysis(gga[depth_left-1], pl).getXInARow();
                gga[depth_left-1].putPiece(pl);
                //check if node exists in zobrist table. If yes, return value.
               // score = -win_analysis(gga[depth_left], pl).getXInARow();
                if (depth_left>1)
                {
                    score -= MiniMaxAB(gga, current_alpha, current_beta, PLAYER_HUMAN, depth_left - 1);
                    if (score < beta) return score; //if current player can score better than parent
                    //can make it score, parent will never choose that child
                }
                if (score < current_beta) current_beta = score;
                i+=2;
               // Log.d(tag, "One thing checked");
            }
            return current_beta;
        }
        else //player is human
        {
            score = Integer.MIN_VALUE;
            while(i < length)
            {
                PieceLocation pl = new PieceLocation(moves[i], moves[i+1], player);
                GameGrid.copyGameGrid(gga[depth_left-1], gga[depth_left]);
            //    score = win_analysis(gga[depth_left-1], pl).getXInARow();
                gga[depth_left-1].putPiece(pl);
                if (depth_left>1)
                {
                    score += MiniMaxAB(gga, current_alpha, current_beta, PLAYER_COMPUTER, depth_left - 1);
                    if (score > beta) return score;
                }
                if (score > current_alpha) current_alpha = score;
                i+=2;
            }
            return current_alpha;
        }
    }

     }
     **/


