package com.jordonproj.connect5;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.jordonproj.connect5.game.Connect5;
import com.jordonproj.connect5.game.GameGrid;
import com.jordonproj.connect5.game.GameState;
import com.jordonproj.connect5.game.PieceLocation;
import com.jordonproj.connect5.game.WinDesc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    static final String TAG = "C5 main act";
    static final String GAME_STATUS_FILENAME = "gstat.ser"; //.bin

    // game phases
    static final int GAME_IDLE                   = 0;
    static final int GAME_WAITING_HUMAN_MOVE     = 1;
    static final int GAME_ANALYSING_HUMAN_MOVE   = 2;
    static final int GAME_CALCULATING_AI_MOVE    = 3;
    static final int GAME_ANALYSING_AI_MOVE      = 4;
    static final int GAME_GO_AGAIN               = 5;
    static final int GAME_HUMAN_WIN              = 6;
    static final int GAME_AI_WIN                 = 7;

    static final int ORDER_PLAYER_FIRST = 1;
    static final int ORDER_AI_FIRST = 2;

    // messages
    //static final int MSG_NONE       = 0;
    static final int MSG_WINDESC    = 1;
    static final int MSG_AIRESULT   = 2;
    static final int MSG_DISPUPDATE = 3;
    static final int MSG_START_AI   = 4;

    private Connect5 mConnect5;
    private PieceLocation mCurrentHumanMove = new PieceLocation(4,4); //so AI can go first
    //private PieceLocation tempAIPiece = null;
    //private int tempAiScore = -Integer.MAX_VALUE;
    //private int tempAiPiecesCount = 0;
    //private int numAiThreads = 1;

  //  private ImageButton mPlayButton;
    private Button mPlayButton;
    private Button mAbortButton;
    private Button mPlayerOneButton;
    private ImageView mBoardImageView;

    //default pieces
    private Bitmap mHumanPiece = null;
    private Bitmap mAIPiece = null;
    private Bitmap mWinPiece = null;
    private Bitmap mBoardCurrentBMap = null;
    private int mPieceWidth = 0;
    private int mWinPieceWidth = 0;

    private Handler mHandler;

    private int mGamePhase;
    private int whoFirst;
    private boolean mHumanContinueFlag; //whether the human has placed a piece and not confirmed it.

    //debugging
    private static long TIME_START;
    private static long TIME_END;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mConnect5 = new Connect5();

        //mPlayButton = (ImageButton) findViewById(R.id.play_button);
        mPlayButton = (Button) findViewById(R.id.play_button);
        mPlayButton.setOnClickListener(this);
        mAbortButton = (Button) findViewById(R.id.abort_button);
        mAbortButton.setOnClickListener(this);
        mPlayerOneButton = (Button) findViewById(R.id.player_one_button);
        mPlayerOneButton.setOnClickListener(this);

        mBoardImageView = (ImageView) findViewById(R.id.BoardimageView);

        mBoardImageView.setOnTouchListener(this);

        mGamePhase = GAME_IDLE;
        whoFirst = ORDER_PLAYER_FIRST;
        initHandler();

        initBitmaps();
        //saveGameState();
        loadGameState();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        updatePlayButtonMode(); // belt and braces
    }

    @Override
    protected void onResume()
    {
        super.onResume();
       // loadGameState();
        Log.i("tag", "onResume");
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        saveGameState();
        Log.i("tag", "onPause");
    }

    @Override
    protected void onStop()
    {
        Log.i("tag", "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    protected void saveGameState()
    {
        try
        {
            boolean validity = true;
            FileOutputStream fos = openFileOutput(GAME_STATUS_FILENAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            GameState gs = mConnect5.getGameState();
            if(mGamePhase == GAME_AI_WIN || mGamePhase == GAME_HUMAN_WIN || mGamePhase == GAME_IDLE)
            {
                validity = false;
                oos.writeByte(-1);
                return;
            }
            gs.setGamePhase(mGamePhase);
            gs.setValidity(validity);
            oos.writeObject(gs);
            oos.close();
            fos.close();
            Log.i(TAG, "game saved");
        }
        catch (IOException e)
        {
            Log.e(TAG, "Save game status object failed");
            e.printStackTrace();
        }
    }

    protected void loadGameState()
    {
        PieceLocation[] pieces;
        File file = new File(getFilesDir(), GAME_STATUS_FILENAME);
        if (file.exists() == false) return;
            try
            {
                FileInputStream fis = openFileInput(GAME_STATUS_FILENAME);
                ObjectInputStream ois = new ObjectInputStream(fis);
                if(ois.available() == -1)  //first byte is EOF
                {
                    ois.close();
                    fis.close();
                    return;
                }
                GameState gs = (GameState) ois.readObject();
                fis.close();
                ois.close();
                if (gs.getValidity() == false)  return;
                mConnect5.setGameState(gs);
                pieces = gs.getGameGrid().getPieces();
                for (int i = 0; i < pieces.length; i++)
                {
                    drawMoveConfirm(pieces[i]);
                }
                mGamePhase=gs.getGamePhase();
                mHandler.sendEmptyMessage(MSG_DISPUPDATE);
                Log.i(TAG, "file loaded");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
    }

    private void initBitmaps()
    {
        //to improve startup time, hand this to a thread maybe?
        mHumanPiece = BitmapFactory.decodeResource(this.getResources(), R.drawable.piece_1);
        mAIPiece = BitmapFactory.decodeResource(this.getResources(), R.drawable.piece_2);
        mWinPiece = BitmapFactory.decodeResource(this.getResources(), R.drawable.piece_win);
        mBoardCurrentBMap = BitmapFactory.decodeResource(this.getResources(), R.drawable.square_game_board);
        mPieceWidth = mHumanPiece.getWidth();
        mWinPieceWidth = mWinPiece.getWidth();
    }

    private void initHandler()
    {
        mHandler = new Handler( Looper.getMainLooper())
        {
            public void handleMessage(Message msg)
            {
                WinDesc wd;
                PieceLocation pl;
                switch(msg.what)
                {
                    case MSG_WINDESC:
                        wd = (WinDesc) msg.obj;
                        //if win, Say so.
                        if(wd.getXInARow()>=5)
                        {
                            Log.i(TAG, wd.getXInARow()+" many in a row in plane "+wd.getPlane());
                            displayWin(wd);
                            if(wd.getPlayer() == Connect5.PLAYER_HUMAN)
                            {
                                //TODO What happens when one has won?
                                mGamePhase = GAME_HUMAN_WIN;
                            }
                            else if(wd.getPlayer() == Connect5.PLAYER_COMPUTER)
                            {
                                mGamePhase = GAME_AI_WIN;
                                displayWin(wd);
                            }
                        }
                        else if(wd.getPlayer() == WinDesc.PLAYER_HUMAN)
                        {
                            mGamePhase = GAME_CALCULATING_AI_MOVE;
                            mHandler.sendEmptyMessage(MSG_START_AI);
                        }
                        else if(wd.getPlayer() == WinDesc.PLAYER_AI)
                        {
                            mGamePhase = GAME_WAITING_HUMAN_MOVE;
                        }

                        break;
                    case MSG_AIRESULT:
                        pl = (PieceLocation) ((Object[]) msg.obj)[0];
                        confirmAiTurn(pl);
                        mGamePhase = GAME_ANALYSING_AI_MOVE;
                        checkWin(pl);
                        //updateAiTurn((Object[]) msg.obj);
                        //confirmAiTurn(pl);
                        break;
                    case MSG_DISPUPDATE:
                        mPlayButton.refreshDrawableState();
                        mBoardImageView.refreshDrawableState();
                        // Update display
                        break;
                    case MSG_START_AI:
                        mGamePhase = GAME_CALCULATING_AI_MOVE;
                        updatePlayButtonMode();
                        TIME_START = System.nanoTime();
                        //handleAiTurnThreads();
                        //handleAiTurn();
                        new_ai();
                        break;
                }
            }
        };
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId()) {
            case (R.id.play_button):
                if ((mGamePhase == GAME_IDLE  || mGamePhase == GAME_GO_AGAIN) && !mHumanContinueFlag) // i'm the play button
                {
                    mConnect5.init(); // initialise game
                    Log.i(TAG, "Game init - button press");
                    mHumanContinueFlag = false; // belt and braces

                    if(whoFirst == ORDER_PLAYER_FIRST) {
                        mGamePhase = GAME_WAITING_HUMAN_MOVE;
                        updatePlayButtonMode();
                    }
                    else if(whoFirst == ORDER_AI_FIRST)
                    {
                        mGamePhase = GAME_CALCULATING_AI_MOVE;
                        mHandler.sendEmptyMessage(MSG_START_AI);
                        updatePlayButtonMode();
                    }
                    return;
                }
                if (mGamePhase == GAME_WAITING_HUMAN_MOVE && !mHumanContinueFlag)
                {
                   // mConnect5.getGrid().putPiece(mCurrentHumanMove); // update grid with move
                    //drawMoveConfirm(mCurrentHumanMove);
                    Log.i(TAG, "Game waiting for human move");
                 //   updatePlayButtonMode();
                }
                if (mGamePhase == GAME_WAITING_HUMAN_MOVE && mHumanContinueFlag) // i'm now continue
                {
                    confirmPlayerTurn();
                    mHandler.sendEmptyMessage(MSG_DISPUPDATE);  // trigger display refresh if needed
                  //  mHandler.sendEmptyMessageDelayed(MSG_START_AI, 100);   // start AI
                    Log.i(TAG, "Move confirmed");
                }
                break;
            case (R.id.abort_button):
                /* reset game */
                Log.i(TAG, "Game abort - button press");
                mGamePhase = GAME_IDLE;
                mHumanContinueFlag = false;
                mConnect5 = new Connect5();
                mConnect5.init();
                resetBoardView();
                updatePlayButtonMode();

                break;
            default:
                break;
            case(R.id.player_one_button):
                if(mGamePhase != GAME_IDLE && mGamePhase != GAME_GO_AGAIN) break;
                else if(whoFirst == ORDER_PLAYER_FIRST)
                {
                    mPlayerOneButton.setText(R.string.ai_first);
                    whoFirst = ORDER_AI_FIRST;
                    break;
                }
                else if(whoFirst == ORDER_AI_FIRST)
                {
                    mPlayerOneButton.setText(R.string.player_first);
                    whoFirst = ORDER_PLAYER_FIRST;
                    break;
                }

        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        float event_x = event.getX();
        float event_y = event.getY();
       // Log.d(TAG, "event_x = "+event_x+" event_y = "+event_y);
        //if event happens beyond view, ABORT. prevents sliding.
        if (event_x <= v.getLeft() || event_x >= v.getRight() ||
                event_y <= v.getTop() || event_y >= v.getBottom())
        {
            //abort
            Log.d(TAG, "Aborting onTouch");
            return true;
        }

        if(v.getId() == R.id.BoardimageView && mGamePhase == GAME_WAITING_HUMAN_MOVE )
        {
            handlePlayerTurn(v, event);
            //need to wait for the continue button.
            mHumanContinueFlag = true;
            updatePlayButtonMode();
            return true;
            //now do AI
        }

        return false;
    }

    /* set play button mode to play or continue depending on global flag */
    private void updatePlayButtonMode()
    {

        if(mGamePhase == GAME_IDLE || mGamePhase == GAME_HUMAN_WIN || mGamePhase == GAME_AI_WIN)
        {
            mPlayButton.setText(R.string.new_game);
           // mPlayButton.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.button_new_game));
        }
        else if(mHumanContinueFlag)
        {
            mPlayButton.setText(R.string.confirm_button);
            //mPlayButton.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.button_confirm));
        }
        else if(mGamePhase == GAME_WAITING_HUMAN_MOVE && mHumanContinueFlag)
        {
            mPlayButton.setText(R.string.play_button);
        }
        else if(mGamePhase == GAME_CALCULATING_AI_MOVE)
        {
            mPlayButton.setText(R.string.ai_move);
            //mPlayButton.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.button_computer));
        }
        else
        {
            mPlayButton.setText(R.string.play_button);
            //mPlayButton.setImageBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.button_place));
        }
        mPlayButton.refreshDrawableState();
    }


    /**
     * This method checks if a player has won by putting this piece down. If it has, it draws
     * @param piece
     */
    private void checkWin(PieceLocation piece)
    {
        WinAnalThread winanal = new WinAnalThread(mConnect5.getGrid(), mHandler, piece);
        Thread check_win = new Thread(winanal);
        check_win.start();
    }

    private void confirmPlayerTurn()
    {
        mConnect5.getGrid().putPiece(mCurrentHumanMove);
        PieceLocation pl = mCurrentHumanMove;
        checkWin(mCurrentHumanMove);
       // WinAnalThread winanal = new WinAnalThread(mConnect5.getGrid(), mHandler, pl);
      //  Thread thread = new Thread(winanal);
       // thread.start();
        mGamePhase = GAME_ANALYSING_HUMAN_MOVE;
        drawMoveConfirm(mCurrentHumanMove);
    }

    public void handlePlayerTurn(View v, MotionEvent event)
    {
        GameGrid gd = mConnect5.getGrid();
        int cellX;
        int cellY;

        //float cell_width = v.getWidth() /gd.getXsize();
        //float cell_height = v.getHeight() /gd.getYsize();
        float event_x = event.getX();
        float event_y = event.getY();
        /**
         * To find cellX:
         * take how many pixels across the view the touch event is
         * divide it by the width of the view, giving you the portion of the view it's in.
         * Multiply that by how many cells wide the board is.
         * Round that down to the nearest integer and you have the cell index of the touch.
         */
        cellX = (int) (( event_x * gd.getXsize() ) / v.getWidth() );
        cellY = (int) (( event_y * gd.getYsize() ) / v.getHeight());

        //Log.d(TAG, "x tile = "+cellX+" y tile = "+cellY);
        mCurrentHumanMove = new PieceLocation(cellX, cellY, 1); //1 means human

        if( !gd.validatePutPiece(mCurrentHumanMove)) return;
        drawTempMove(mCurrentHumanMove);
        // get current game grid fitted with temporary human move
        //   int[][] gb = mConnect5.getGrid().getGameBoard(mCurrentHumanMove);
        // display human move including calc display coords for graphical buttons
    }

    /* Runs win/grid analysis on new thread
        I was going to use asyncThread but it's not recommended
        for processes that take more than a few seconds */
    private class WinAnalThread implements Runnable
    {
        private GameGrid mGd;
        private PieceLocation mPl;
        private Handler mHandler;

        WinAnalThread(GameGrid gd, Handler handler, PieceLocation piece)
        {
            mGd = gd;
            mPl = piece; //formerly piece location array
            mHandler = handler;
        }

        public void run()
        {
            //Played piece location will be only item in mPla.
            WinDesc wd = Connect5.win_analysis(mGd, mPl);

            //report wd back to UI thread for processing
            Message msg = Message.obtain(mHandler, MSG_WINDESC, wd);
            mHandler.sendMessage(msg);
        }
    }

    /*********************************** AI STUFF ************************************************/

    public void new_ai()
    {
        AiCalcThread_new aicalcthread = new AiCalcThread_new(mConnect5.getGrid(), mHandler);
        Thread thread = new Thread(aicalcthread);
        thread.start();
    }

    private class AiCalcThread_new implements Runnable
    {
        private GameGrid mGd;
        private Handler mHandler;
        private PieceLocation[] mMoves;

        //if running in one thread
        AiCalcThread_new(GameGrid gd, Handler handler)
        {
            mGd = gd;
            mHandler = handler;
            mMoves = mGd.getEmptySpaces();
        }

        //if running across multiple threads
        AiCalcThread_new(GameGrid gd, Handler handler, PieceLocation[] pla)
        {
            mGd = gd;
            mHandler = handler;
            mMoves = pla;
        }
        public void run()
        {
            Object obj;
            //obj = Connect5.best_location(mGd, mMoves);
            obj = Connect5.best_move(mGd, mMoves);
            Message msg = Message.obtain(mHandler, MSG_AIRESULT, obj);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * This places the piece the AI has chosen to play in its space, draws it, and allows the
     * human to take their turn.
     * @param ai_piece
     */
    public void confirmAiTurn(PieceLocation ai_piece)
    {
        mConnect5.getGrid().putPiece(ai_piece);
        drawMoveConfirm(ai_piece);
        mHumanContinueFlag = false;
        mGamePhase = GAME_ANALYSING_AI_MOVE;
        updatePlayButtonMode();
    }




    /******************************* BOARD VIEW STUFF ********************************************/

    public void setBoardOnLoad(Bitmap bitmap)
    {
        Drawable d = new BitmapDrawable(bitmap);
        mBoardCurrentBMap = bitmap;
        mBoardImageView.setImageDrawable(d);
        mBoardImageView.refreshDrawableState();
    }

    public void resetBoardView()
    {
        Bitmap bmap = BitmapFactory.decodeResource(this.getResources(), R.drawable.square_game_board);
        Drawable d = new BitmapDrawable(getResources(), bmap);
        mBoardCurrentBMap = bmap;
        mBoardImageView.setImageDrawable(d);
        mBoardImageView.refreshDrawableState();
    }

    /**
     *  This method is for drawing pieces that aren't yet deliberate and confirmed moves.
     *
     * @param piece The piece being placed, holding the board co-ordinates it's being placed and
     *              which player it belongs to.
     *
     *              This copies the bitmap of the current board, overlays the counter, then creates
     *              a drawable which boardImageView uses.
     *              It does not alter the bitmap displaying the pieces of past turns that are
     *              confirmed.
     */

    //TODO this is a tag
    public void drawTempMove(PieceLocation piece)
    {
        int[] drawCoords = new int[2];
        Bitmap final_map = null;

        if (piece.getType()!=1) return;  //this should be fine so long as only one human player

        final_map = mBoardCurrentBMap.copy(mBoardCurrentBMap.getConfig(), true);
        Canvas canvas = new Canvas(final_map);
        drawCoords = drawLocFromCell(piece, final_map.getWidth(), final_map.getHeight(), mPieceWidth);

        canvas.drawBitmap(mHumanPiece, drawCoords[0], drawCoords[1], null);

        Drawable d = new BitmapDrawable(getResources(), final_map);
        mBoardImageView.setImageDrawable(d);
        mBoardImageView.refreshDrawableState();
    }


    /**
     * This method is for drawing a move that has been made and confirmed.
     *
     * @param piece The piece being placed, holding the board co-ordinates it's being placed and
     *              which player it belongs to.
     *
     *              This draws onto the bitmap that holds the board and all previous moves.
     *              It alters the bitmap that the ImageView holds and does not get undone.
     */
    public void drawMoveConfirm(PieceLocation piece)
    {
        int[] drawCoords = new int[2];
        Bitmap final_map = null;

        final_map = mBoardCurrentBMap.copy(mBoardCurrentBMap.getConfig(), true);
        Canvas canvas = new Canvas(final_map);
        drawCoords = drawLocFromCell(piece, final_map.getWidth(), final_map.getHeight(), mPieceWidth);

        if(piece.getType()==1) canvas.drawBitmap(mHumanPiece, drawCoords[0], drawCoords[1], null);
        else canvas.drawBitmap(mAIPiece, drawCoords[0], drawCoords[1], null);
        mBoardCurrentBMap = final_map;
        Drawable d = new BitmapDrawable(getResources(), final_map);
        mBoardImageView.setImageDrawable(d);
        mBoardImageView.refreshDrawableState();
        updatePlayButtonMode();

    }

    public int[] drawLocFromCell(PieceLocation piece, int width, int height, int pieceWidth)
    {
        int[] coords = new int[2];

        coords[0] = width/mConnect5.getGrid().getXsize()*piece.getX();
        coords[0] += ( (width/mConnect5.getGrid().getXsize()) - pieceWidth) /2;
        coords[1] = height/mConnect5.getGrid().getYsize()*piece.getY();
        coords[1] += ( (height/mConnect5.getGrid().getYsize()) - pieceWidth) /2;
        return coords;
    }

    //needs to overwrite background bitmap, else each new piece will undo the previous draw
    public void displayWin(WinDesc win)
    {
        //get all piece coords to display
        int player = win.getPlayer();
        int xInARow = win.getXInARow();
        PieceLocation[] win_pieces = new PieceLocation[xInARow];
        int plane = win.getPlane();
        int current_x;
        int current_y;
        int direction_x = 0;
        int direction_y = 0;

        if (plane == 1)
        {
            direction_y = 1;
            direction_x = 1;
        }
        else if (plane == 2)
        {
            direction_x = 1;
            direction_y = 0;
        }
        else if (plane == 3)
        {
            direction_y = -1;
            direction_x = 1;
        }
        else if (plane == 4)
        {
            direction_x = 0;
            direction_y = 1;
        }

        //fill array with winning move coords

        win_pieces[0] = new PieceLocation(win.getStartWin().getX(), win.getStartWin().getY(), 0);
        current_x = win_pieces[0].getX();
        current_y = win_pieces[0].getY();
        for (int i = 1; i<xInARow-1; i++) // for all tiles other than the first
        {
            current_x = normaliseCoordX(current_x + direction_x);
            current_y = normaliseCoordY(current_y + direction_y);
            win_pieces[i] = new PieceLocation(current_x, current_y);

        }
        win_pieces[xInARow-1] = new PieceLocation(win.getEndWin().getX(), win.getEndWin().getY(), 0);
        Log.i(TAG, "start is at "+win_pieces[0].getX()+","+win_pieces[0].getY()+" end is at "+win_pieces[xInARow-1].getX()+","+win_pieces[xInARow-1].getY());
        //paint each piece
        paint_win(win_pieces);

    }

    public void paint_win(PieceLocation[] pieces)
    {
        Bitmap final_map = null;
        int length = pieces.length;
        int[][] drawCoords = new int[length][2];

        //get board from background
        final_map = mBoardCurrentBMap.copy(mBoardCurrentBMap.getConfig(), true);

        //create canvas with board as background
        Canvas canvas = new Canvas(final_map);

        for (int i=0; i<length; i++)
        {
            Log.d(TAG, "piece "+i+" being drawn at "+pieces[i].getX()+","+pieces[i].getY());
            drawCoords[i] = drawLocFromCell(pieces[i], final_map.getWidth(), final_map.getHeight(),
                    mWinPieceWidth);
            canvas.drawBitmap(mWinPiece, drawCoords[i][0], drawCoords[i][1], null);
        }

        mBoardCurrentBMap = final_map;
        Drawable d = new BitmapDrawable(getResources(), final_map);
        mBoardImageView.setImageDrawable(d);
        mBoardImageView.refreshDrawableState();
    }

    public int normaliseCoordX(int x)
    {
        int width = mConnect5.getGrid().getXsize();
        if (x >= width) x -= width;
        else if (x < 0) x += width;
        return x;
    }

    public int normaliseCoordY(int y)
    {
        int height = mConnect5.getGrid().getYsize();
        if (y >= height) y -= height;
        else if (y < 0) y += height;
        return y;
    }
}



//TODO put the old unused methods down here
/********************************* OLD STUFF ************************************************

    public void handleAiTurnThreads()
    {
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        Log.i(TAG, "Number of cores is "+NUMBER_OF_CORES);
        numAiThreads = NUMBER_OF_CORES; //how many threads CAN I run?
        int AI_depth = mConnect5.getAIDepth();

        /**
         ConcurrentHashMap[] hashMaps = new ConcurrentHashMap[AI_depth];
         for (int i=0; i<AI_depth; i++)
         {
         ConcurrentHashMap hashmap = new ConcurrentHashMap<String, Integer>(1000000);
         initHashmap(mConnect5.getGrid(), hashmap);
         hashMaps[i] = hashmap;
         }
         //
        ConcurrentHashMap hashMap = new ConcurrentHashMap<String, Integer>(1000000);
        hashMap.put(mConnect5.getGrid().getKey(), -Integer.MAX_VALUE);

        PieceLocation[] moves = mConnect5.getGrid().bestEmptySpaces(-1, mCurrentHumanMove);
        int num_moves = moves.length;
        int portion_size = num_moves/numAiThreads;
        int portion_end = portion_size-1;//the index of the last location of the current child array
        int portion_start = 0;

        Log.i(TAG, "Ai threads created:"+numAiThreads);
        for(int i=0; i<numAiThreads; i++)
        {
            if(i==numAiThreads-1) portion_end = num_moves;
            PieceLocation[] moves_for_thread = Arrays.copyOfRange(moves, portion_start, portion_end);
            AiCalcThread AiRunnable = new AiCalcThread(mConnect5.getGrid(), mHandler, hashMap, moves_for_thread);
            Thread child_thread = new Thread(AiRunnable);
            child_thread.start();
            portion_start = portion_end+1;
            portion_end += portion_size;
            if(i==numAiThreads-1) portion_end = num_moves-1; //just to be sure that nothing is left out
        }
    }

 /**
 * This is called when it's the AI's turn to move. It creates and runs only a single thread.
 //
    public void handleAiTurn()
    {

    /**
     int AI_depth = mConnect5.getAIDepth();
     ConcurrentHashMap[] hashMaps = new ConcurrentHashMap[AI_depth];
     for (int i=0; i<AI_depth; i++)
     {
     ConcurrentHashMap hashmap = new ConcurrentHashMap<String, Integer>(10000);
     initHashmap(mConnect5.getGrid(), hashmap);
     hashMaps[i] = hashmap;
     }
     //
     ConcurrentHashMap hashMap = new ConcurrentHashMap<String, Integer>(100000);
     initHashmap(mConnect5.getGrid(), hashMap);
     AiCalcThread aiCalcThread = new AiCalcThread(mConnect5.getGrid(), mHandler, hashMap);
     Thread t = new Thread(aiCalcThread);
     t.start();
     }

 /**
 * Alpha-Beta needs to know the score of the best move so far. If Alpha (the score of the best
 * move at the start of the tree) is local to each thread and not shared, the benefits of
 * Alpha-Beta will be greatly reduced. In order to allow all threads to see it, the value must
 * be stored somewhere where all threads can access it. The hashmap is already shared and is an
 * easy place to keep it. The key used to access this value must be unique and not something
 * that will be accidentally overwritten. The board state is used as the key. Only future board
 * states are used, so using the state of the board at the start of the AI's turn means it
 * won't be accessed by any nodes deeper in the tree. Only the root node can access it.
 * This method initialises Alpha to the minimum value as required by Alpha-Beta.
 * @param gd        The game grid used as the key to store Alpha
 * @param hashmap   The ConcurrentHashMap used to store Alpha and act as transposition table.
 //
private void initHashmap(GameGrid gd, ConcurrentHashMap hashmap)
{
    String key = gd.getKey();
    int key_value = -Integer.MAX_VALUE; //best move of AI is initialised to worst
    hashmap.put(key, key_value);
}

private class AiCalcThread implements Runnable
{
private GameGrid mGd;
private Handler mHandler;
private ConcurrentHashMap mHashMap;
private PieceLocation[] mMoves;

AiCalcThread(GameGrid gd, Handler handler, ConcurrentHashMap concurrentHashMap, PieceLocation[] moves)
{
mGd = gd;
mHandler = handler;
mHashMap = concurrentHashMap;
mMoves = moves;
}

AiCalcThread(GameGrid gd, Handler handler, ConcurrentHashMap concurrentHashMap)
{
mGd = gd;
mHandler = handler;
mHashMap = concurrentHashMap;
}

public void run()
{
//Log.i(TAG, "Thread started");
if(mMoves == null) mMoves = mGd.worthwhileEmptySpaces(-1); //-1 is AI's number
Object[] move;
PieceLocation pl;
int score;

//this gets the best move and its score out of all moves present in mMoves
move = Connect5.MiniMaxRootNode(mGd, mHashMap, mMoves);
// Long end_time = System.nanoTime();
// Log.i(TAG, "That thread took "+(end_time - start_time)/1000000000+" seconds to complete!");
//Log.i(TAG, "Threads's best move is at "+pl.getX()+"x, "+pl.getY()+"y");


/**
boolean tile_chosen = false;
do
{
int xloc = (int) (Math.random() * 8);
int yloc = (int) (Math.random() * 8);
piece_location = new PieceLocation(xloc, yloc, -1);
tile_chosen = mGd.validatePutPiece(piece_location);
} while (tile_chosen);

 //
// Build list of AI moves here

// maybe with a win iterator as a static method in the connect5 class
// or just fleshed out here

// creating a pla array
// PieceLocation[] pla = null;

            /* win analysis then churns the array of piece locations
             returning the best win descriptor
                    //
//  WinDesc wd = Connect5.win_analysis(mGd, pla);

//report wd back to UI thread for processing

Message msg = Message.obtain(mHandler, MSG_AIRESULT, move);
            mHandler.sendMessage(msg);
                    }
                    }

public void updateAiTurn(Object[] move)
{
PieceLocation current_move;
int current_score;
current_move = (PieceLocation) move[0];
current_score = (Integer) move[1];
Log.i(TAG, "Thread returned best move at "+current_move.getX()+","+current_move.getY()+" with score of "+current_score);
if (current_score > tempAiScore)
{
tempAiScore = current_score;
tempAIPiece = (PieceLocation) move[0];
}
tempAiPiecesCount++;
if (tempAiPiecesCount == numAiThreads)
{
confirmAiTurn(tempAIPiece);
tempAiPiecesCount = 0;
tempAiScore = Integer.MIN_VALUE;
TIME_END = System.nanoTime();
Log.i(TAG, "Ai took "+((TIME_END-TIME_START)/1000000000)+" seconds to complete");
}
}

public void updateAiTurn_new(Object[] obj)
{
Log.i(TAG, "Score of move is "+obj[1]);
confirmAiTurn( (PieceLocation) obj[0]);
//  mGamePhase = GAME_ANALYSING_AI_MOVE;
if((int)obj[1] >= Connect5.MAX_SCORE)
{
mGamePhase = GAME_AI_WIN;
}
else
{
mGamePhase = GAME_WAITING_HUMAN_MOVE;
}

}

 */