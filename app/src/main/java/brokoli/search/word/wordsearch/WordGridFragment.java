package brokoli.search.word.wordsearch;


import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;


/**
 * A simple {@link Fragment} subclass.
 */
public class WordGridFragment extends Fragment implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;
    public static final int EAST = 4;
    public static final int SOUTHEAST = 8;
    public static final int SOUTHWEST = 7;
    public static final int NORTHEAST = 6;
    public static final int NORTHWEST = 5;

    View view;
    GridView gridView;
    String[] alphabet = new String[] {
            "A", "B", "C", "D", "E",
            "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O",
            "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"};

    String [] letters = new String[100];
    String word = "";
    ArrayAdapter<String> adapter;
    int direction;
    String [] selectedWords;
    boolean isScrolling = false;
    ImageView drawingImageView;
    int howManyPlaced;
    String [] placedWords;
    String [] foundWords;
    int howManyFound = 0;
    int color = Color.BLUE;
    int [] constraint = new int[9];
    boolean isNewGame;
    String [] foundWordsStartingPositions;
    String [] foundWordsEndingPositions;
    String [] foundWordsDirections;
    ViewTreeObserver viewTreeObserver;
    boolean isLinesCreated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_word_grid, container, false);
        gridView = (GridView) view.findViewById(R.id.gridView);
        drawingImageView = (ImageView) view.findViewById(R.id.rectangle);
        Bundle bundle = this.getArguments();
        selectedWords = bundle.getStringArray("selectedWords");
        isNewGame = bundle.getBoolean("isNewGame");
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if(!isNewGame) {
            loadLastGame();
        }else {
            generateLetters();
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        saveCurrentGame();
        super.onDestroy();
    }

    public void loadLastGame() {
        SharedPreferences prefs = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String restoredLetters = prefs.getString("letters", null);
        String restoredPlacedWords = prefs.getString("placedWords", null);
        String restoredFoundWords = prefs.getString("foundWords", null);
        if (restoredLetters != null && restoredPlacedWords != null && restoredFoundWords != null) {
            letters = decodeStringAsArray(restoredLetters);
            placedWords = decodeStringAsArray(restoredPlacedWords);
            foundWords = decodeStringAsArray(restoredFoundWords);
            howManyPlaced = prefs.getInt("howManyPlaced", 0);
            howManyFound = prefs.getInt("howManyFound", 0);
            if(howManyFound > 0) {
                foundWordsStartingPositions = decodeStringAsArray(prefs.getString("foundWordsStartingPositions", null));
                foundWordsEndingPositions = decodeStringAsArray(prefs.getString("foundWordsEndingPositions", null));
                foundWordsDirections = decodeStringAsArray(prefs.getString("foundWordsDirections", null));
            }else {
                foundWordsStartingPositions = new String[howManyPlaced];
                foundWordsEndingPositions = new String[howManyPlaced];
                foundWordsDirections = new String[howManyPlaced];
            }
            initializeGrid();
            ((MainActivity) getActivity()).updateNavigationDrawer(placedWords, howManyPlaced);
            viewTreeObserver = gridView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(this);
        }
    }

    public void loadAlreadyFoundImagesLines() {
        for(int i = 0; i < howManyFound; i++) {
            int start = Integer.parseInt(foundWordsStartingPositions[i]);
            int end = Integer.parseInt(foundWordsEndingPositions[i]);
            direction = Integer.parseInt(foundWordsDirections[i]);
            Log.e("deneme", start + " " + end + " " + direction);
            generateNewColor();
            drawRectangle(start, end, 0);
            drawRectangle(start, end, 1);
        }
        generateNewColor();
    }

    public void saveCurrentGame() {
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("letters", encodeArrayAsString(letters));
        editor.putString("placedWords", encodeArrayAsString(placedWords));
        editor.putString("foundWords", encodeArrayAsString(foundWords));
        editor.putInt("howManyPlaced", howManyPlaced);
        editor.putInt("howManyFound", howManyFound);
        if(howManyFound > 0) {
            editor.putString("foundWordsStartingPositions", encodeArrayAsString(foundWordsStartingPositions));
            editor.putString("foundWordsEndingPositions", encodeArrayAsString(foundWordsEndingPositions));
            editor.putString("foundWordsDirections", encodeArrayAsString(foundWordsDirections));
        }
        editor.commit();
    }

    public String encodeArrayAsString(String [] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append(arr[i]).append(",");
        }
        return sb.toString();
    }

    public String [] decodeStringAsArray(String str) {
        return str.split(",");
    }

    private void initializeGrid() {
        adapter = new ArrayAdapter<String>(this.getActivity(),
                R.layout.custom_list_item, letters);
        gridView.setAdapter(adapter);
        final GestureDetector gestureDetector = new GestureDetector(this.getActivity(), new CustomGestureListener());
        gridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean result = gestureDetector.onTouchEvent(event);
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    result = false;
                    drawingImageView.setVisibility(View.INVISIBLE);
                }
                if(result) {
                    drawingImageView.setVisibility(View.VISIBLE);
                }
                return result;
            }
        });
    }

    private void generateLetters() {
        for (int i = 0; i < 100; i++) {
            letters[i] = "-";
        }
        initializeGrid();
        Random r = new Random();
        int pos;
        howManyPlaced = 0;
        placedWords = new String[100];
        for (int i = 0; i < 5012; i++ ) {
            pos = r.nextInt(100);
            String tmp = selectedWords[i];
            ArrayList<Integer> availables = findAvailableDirections(pos, tmp, letters);
            int availSize = availables.size();
            int tm;
            if(availSize > 0 && isUniqueString(placedWords, tmp, howManyPlaced)) {
                placedWords[howManyPlaced] = tmp;
                howManyPlaced++;
                tm = r.nextInt(availSize);
                int direction = availables.get(tm);
                constraint[direction]++;
                placeWord(pos, tmp, direction);
            }else {
                continue;
            }
        }
        for (int i = 0; i < 100; i++) {
            if(letters[i].equals("-")) {
                letters[i] = alphabet[r.nextInt(26)];
            }
        }
        foundWords = new String[howManyPlaced];
        foundWordsStartingPositions = new String[howManyPlaced];
        foundWordsEndingPositions = new String[howManyPlaced];
        foundWordsDirections = new String[howManyPlaced];
        initializeGrid();
        ((MainActivity) getActivity()).updateNavigationDrawer(placedWords, howManyPlaced);
    }

    private boolean isUniqueString(String[] placedWords, String word, int howManyPlaced) {
        for(int i = 0; i < howManyPlaced; i++) {
            if(word.equals(placedWords[i])) {
                Log.e("same word is placed : ", word + " " + placedWords[i]);
                return false;
            }
        }
        return true;
    }

    private void placeWord(int position, String word, int direction) {
        int count = 0;
        letters[position] = "" + word.charAt(count);
        int columnNumber = position % 10;
        int rowNumber = position / 10;
        int size = word.length();
        switch (direction) {
            case NORTH:
                int endRow = rowNumber - size + 1;
                while(rowNumber > endRow) {
                    rowNumber--;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case SOUTH:
                int endRow2 = rowNumber + size - 1;
                while (rowNumber < endRow2) {
                    rowNumber++;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case WEST:
                int endColumn = columnNumber - size + 1;
                while(columnNumber > endColumn) {
                    columnNumber--;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case EAST:
                int endColumn2 = columnNumber + size - 1;
                while (columnNumber < endColumn2) {
                    columnNumber++;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case NORTHWEST:
                int endRow3 = rowNumber - size + 1;
                int endColumn3 = columnNumber - size + 1;
                while (columnNumber > endColumn3 && rowNumber > endRow3) {
                    columnNumber--;
                    rowNumber--;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case NORTHEAST:
                int endRow4 = rowNumber - size + 1;
                int endColumn4 = columnNumber + size - 1;
                while (columnNumber < endColumn4 && rowNumber > endRow4) {
                    columnNumber++;
                    rowNumber--;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case SOUTHWEST:
                int endRow5 = rowNumber + size - 1;
                int endColumn5 = columnNumber - size + 1;
                while (columnNumber > endColumn5 && rowNumber < endRow5) {
                    columnNumber--;
                    rowNumber++;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
            case SOUTHEAST:
                int endRow6 = rowNumber + size - 1;
                int endColumn6 = columnNumber + size - 1;
                while (columnNumber < endColumn6 && rowNumber < endRow6) {
                    columnNumber++;
                    rowNumber++;
                    count++;
                    letters[rowNumber * 10 + columnNumber] = "" + word.charAt(count);
                }
                break;
        }
    }

    private ArrayList<Integer> findAvailableDirections(int position, String word, String [] letters) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        int columnNumber = position % 10;
        int rowNumber = position / 10;
        int size = word.length();
        if(rowNumber - size > -1 && constraint[NORTH] < 2) {
            String tmp = findWord(position, (rowNumber - size + 1) * 10 + columnNumber);
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(NORTH);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(NORTH);
                }
            }
        }
        if(rowNumber + size < 11 && constraint[SOUTH] < 2) {
            String tmp = findWord(position, (rowNumber + size - 1) * 10 + columnNumber);
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
               result.add(SOUTH);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(SOUTH);
                }
            }
        }
        if(columnNumber - size > -1 && constraint[WEST] < 2) {
            String tmp = findWord(position, rowNumber * 10 + (columnNumber - size + 1));
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(WEST);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(WEST);
                }
            }
        }
        if(columnNumber + size < 11 && constraint[EAST] < 2) {
            String tmp = findWord(position, rowNumber * 10 + (columnNumber + size - 1));
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(EAST);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(EAST);
                }
            }
        }
        if(rowNumber - size > -1 && columnNumber - size > -1) {
            String tmp = findWord(position, (rowNumber - size + 1) * 10 + (columnNumber - size + 1));
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(NORTHWEST);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(NORTHWEST);
                }
            }
        }
        if(rowNumber - size > -1 && columnNumber + size < 11) {
            String tmp = findWord(position, (rowNumber - size + 1) * 10 + (columnNumber + size - 1));
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(NORTHEAST);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(NORTHEAST);
                }
            }
        }
        if(rowNumber + size < 11 && columnNumber - size > -1) {
            String tmp = findWord(position, (rowNumber + size - 1) * 10 + (columnNumber - size + 1));
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(SOUTHWEST);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(SOUTHWEST);
                }
            }
        }
        if(rowNumber + size < 11 && columnNumber + size < 11) {
            String tmp = findWord(position, (rowNumber + size - 1) * 10 + (columnNumber + size - 1));
            String waitingStr = "";
            for(int i = 0; i < size; i++) {
                waitingStr += "-";
            }
            if(tmp.equals(waitingStr)) {
                result.add(SOUTHEAST);
            }else {
                boolean isSameLetter = true;
                for(int i = 0; i < size; i++) {
                    String currentLetter = "" + tmp.charAt(i);
                    String currentWordLetter = "" + word.charAt(i);
                    if(!currentLetter.equals("-") && !currentLetter.equals(currentWordLetter)) {
                        isSameLetter = false;
                    }
                }
                if (isSameLetter) {
                    result.add(SOUTHEAST);
                }
            }
        }
        return result;
    }


    private void drawRectangle(int position1, int position2, int isPerminent) {
        View itemView1 = gridView.getChildAt(position1);
        View itemView2 = gridView.getChildAt(position2);
        float widthSize = itemView1.getWidth();
        float heightSize = itemView1.getHeight();
        float x1 = itemView1.getX();
        float y1 = itemView1.getY();
        float x2 = itemView2.getX();
        float y2 = itemView2.getY();
        switch (direction) {
            case NORTH:
                x1 = x1 + widthSize/2;
                x2 = x2 + widthSize/2;
                y1 = y1 + heightSize;
                break;
            case SOUTH:
                x1 = x1 + widthSize/2;
                x2 = x2 + widthSize/2;
                y2 = y2 + heightSize;
                break;
            case WEST:
                y1 = y1 + heightSize/2;
                y2 = y2 + heightSize/2;
                x1 = x1 + widthSize;
                break;
            case EAST:
                y1 = y1 + heightSize/2;
                y2 = y2 + heightSize/2;
                x2 = x2 + widthSize;
                break;
            case NORTHWEST:
                x1 = x1 + widthSize;
                y1 = y1 + heightSize;
                break;
            case NORTHEAST:
                y1 = y1 + heightSize;
                x2 = x2 + widthSize;
                break;
            case SOUTHWEST:
                x1 = x1 + widthSize;
                y2 = y2 + heightSize;
                break;
            case SOUTHEAST:
                x2 = x2 + widthSize;
                y2 = y2 + heightSize;
                break;
        }
        int screenWidth = view.getWidth();
        int screenHeight = view.getHeight();
        if(isPerminent > 0) {
            drawingImageView = new ImageView(view.getContext());
            FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.gridFragment);
            frameLayout.addView(drawingImageView);
        }
        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawingImageView.setImageBitmap(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setAlpha(80);
        paint.setStrokeWidth(25f);
        canvas.drawLine(x1, y1, x2, y2, paint);
    }

    public String findWord(int position1, int position2) {
        String result = adapter.getItem(position1);
        int columnNumber1 = position1 % 10;
        int rowNumber1 = position1 / 10;
        int columnNumber2 = position2 % 10;
        int rowNumber2 = position2 / 10;
        if(columnNumber1 == columnNumber2) {
            while(rowNumber1 > rowNumber2) {
                rowNumber1--;
                result += getValue(columnNumber1, rowNumber1);
                direction = NORTH;
            }
            while(rowNumber1 < rowNumber2){
                rowNumber1++;
                result += getValue(columnNumber1, rowNumber1);
                direction = SOUTH;
            }
        }else if(rowNumber1 == rowNumber2) {
            while(columnNumber1 > columnNumber2) {
                columnNumber1--;
                result += getValue(columnNumber1, rowNumber1);
                direction = WEST;
            }
            while(columnNumber1 < columnNumber2){
                columnNumber1++;
                result += getValue(columnNumber1, rowNumber1);
                direction = EAST;
            }

        }else {
            while (columnNumber1 > columnNumber2 && rowNumber1 > rowNumber2) {
                columnNumber1--;
                rowNumber1--;
                result += getValue(columnNumber1, rowNumber1);
                direction = NORTHWEST;
            }
            while (columnNumber1 < columnNumber2 && rowNumber1 > rowNumber2) {
                columnNumber1++;
                rowNumber1--;
                result += getValue(columnNumber1, rowNumber1);
                direction = NORTHEAST;
            }
            while (columnNumber1 > columnNumber2 && rowNumber1 < rowNumber2) {
                columnNumber1--;
                rowNumber1++;
                result += getValue(columnNumber1, rowNumber1);
                direction = SOUTHWEST;
            }
            while (columnNumber1 < columnNumber2 && rowNumber1 < rowNumber2) {
                columnNumber1++;
                rowNumber1++;
                result += getValue(columnNumber1, rowNumber1);
                direction = SOUTHEAST;
            }
        }
        return result;
    }

    public String getValue(int columnNumber, int rowNumber) {
        return adapter.getItem(rowNumber * 10 + columnNumber);
    }


    public int findWantedPosition(int position1, int position2) {
        int columnNumber1 = position1 % 10;
        int rowNumber1 = position1 / 10;
        int columnNumber2 = position2 % 10;
        int rowNumber2 = position2 / 10;
        if(columnNumber1 == columnNumber2 || rowNumber1 == rowNumber2 || Math.abs(rowNumber2 - rowNumber1) == Math.abs(columnNumber2 - columnNumber1)) {
            return position2;
        }
        return -1;
    }

    public void generateNewColor() {
        Random rand = new Random();
        int r = rand.nextInt(255);
        int g = rand.nextInt(255);
        int b = rand.nextInt(255);
        color = Color.rgb(r, g, b);
    }

    @Override
    public void onGlobalLayout() {
        if(!isLinesCreated) {
            isLinesCreated = true;
            loadAlreadyFoundImagesLines();
        }
    }

    public class CustomGestureListener extends GestureDetector.SimpleOnGestureListener{

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            isScrolling = true;
            int x1 = (int) e1.getX();
            int y1 = (int) e1.getY();
            int x2 = (int) e2.getX();
            int y2 = (int) e2.getY();
            int position1 = gridView.pointToPosition(x1, y1);
            int position2 = gridView.pointToPosition(x2, y2);
            int wantedPosition = findWantedPosition(position1, position2);
            if(wantedPosition != -1) {
                word = findWord(position1, wantedPosition);
                drawRectangle(position1, wantedPosition, 0);
                for(int i = 0; i < howManyPlaced; i++) {
                    if(placedWords[i].equals(word)) {
                        Toast.makeText(getActivity().getBaseContext(), "buldun", Toast.LENGTH_SHORT).show();
                        foundWords[howManyFound] = placedWords[i];
                        foundWordsStartingPositions[howManyFound] = "" + position1;
                        foundWordsEndingPositions[howManyFound] = "" + wantedPosition;
                        foundWordsDirections[howManyFound] = "" + direction;
                        howManyFound++;
                        placedWords[i] = "";
                        ((MainActivity) getActivity()).updateNavigationDrawer(placedWords, howManyPlaced);
                        drawRectangle(position1, wantedPosition, 1);
                        generateNewColor();
                    }
                }
            }
            return true;
        }
    }
}
