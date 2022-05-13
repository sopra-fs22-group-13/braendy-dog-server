package ch.uzh.ifi.hase.soprafs22.game.gameInstance.board;

import ch.uzh.ifi.hase.soprafs22.game.constants.*;
import ch.uzh.ifi.hase.soprafs22.game.exceptions.InvalidMoveException;
import ch.uzh.ifi.hase.soprafs22.game.exceptions.MoveBlockedByMarbleException;
import ch.uzh.ifi.hase.soprafs22.game.exceptions.NoMarbleException;
import ch.uzh.ifi.hase.soprafs22.game.gameInstance.cards.Card;
import ch.uzh.ifi.hase.soprafs22.game.gameInstance.data.BoardData;
import ch.uzh.ifi.hase.soprafs22.game.gameInstance.data.BoardPosition;
import ch.uzh.ifi.hase.soprafs22.game.gameInstance.data.Move;
import ch.uzh.ifi.hase.soprafs22.rest.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.criteria.CriteriaBuilder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class Board implements IBoard {
    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private ArrayList<MARBLE> _mainCircle = new ArrayList<>();

    private ArrayList<MARBLE> _redGoal = new ArrayList<>();
    private ArrayList<MARBLE> _greenGoal = new ArrayList<>();
    private ArrayList<MARBLE> _blueGoal = new ArrayList<>();
    private ArrayList<MARBLE> _yellowGoal = new ArrayList<>();

    private int _redBase = 4;
    private int _greenBase = 4;
    private int _blueBase = 4;
    private int _yellowBase = 4;

    private boolean REDBLOCKED = false;
    private boolean GREENBLOCKED = false;
    private boolean BLUEBLOCKED = false;
    private boolean YELLOWBLOCKED = false;

    private final int REDINTERSECT = 0;
    private final int BLUEINTERSECT = 48;
    private final int GREENINTERSECT = 32;
    private final int YELLOWINTERSECT = 16;

    public Board() {
        // set all marbles to the start pos.
        _redBase = 4;
        _greenBase = 4;
        _blueBase = 4;
        _yellowBase = 4;

        // create the board list
        while (_mainCircle.size() < 64)
            _mainCircle.add(MARBLE.NONE);

        // create the goal lists
        while (_redGoal.size() < 4)
            _redGoal.add(MARBLE.NONE);
        while (_greenGoal.size() < 4)
            _greenGoal.add(MARBLE.NONE);
        while (_blueGoal.size() < 4)
            _blueGoal.add(MARBLE.NONE);
        while (_yellowGoal.size() < 4)
            _yellowGoal.add(MARBLE.NONE);
    }

    private boolean setMarbleAtPosition(int position, MARBLE marble) {
        _mainCircle.set(position, marble);
        return true;
    }

    private boolean setMarbleAtPosition(int position, MARBLE marble, COLOR goalColor) {
        switch (goalColor) {
            case RED:
                if (marble != MARBLE.RED && marble != MARBLE.NONE)
                    return false;
                _redGoal.set(position, marble);
                break;
            case BLUE:
                if (marble != MARBLE.BLUE && marble != MARBLE.NONE)
                    return false;
                _blueGoal.set(position, marble);
                break;
            case YELLOW:
                if (marble != MARBLE.YELLOW && marble != MARBLE.NONE)
                    return false;
                _yellowGoal.set(position, marble);
                break;
            case GREEN:
                if (marble != MARBLE.GREEN && marble != MARBLE.NONE)
                    return false;
                _greenGoal.set(position, marble);
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean isEmptyAt(int position) {
        MARBLE m = _mainCircle.get(position);
        return m == MARBLE.NONE;
    }

    /**
     * Gets the inbetweeners in the main circle in the FORWARDS direction!
     * (exclusive, inclusive)
     * IMPORTANT: NEVER USE THIS METHOD WHEN TRYING TO MOVE BACKWARDS
     */
    private ArrayList<Integer> getInbetweeners(int pos1, int pos2) throws IndexOutOfBoundsException {
        ArrayList<Integer> importantInbetweeners = new ArrayList<>();
        if (pos1 < 0 || pos1 >= 64 || pos2 < 0 || pos2 >= 64)
            throw new IndexOutOfBoundsException("the positions have to be in range 0-63 (inclusive)");

        if (pos1 < pos2) {
            // no loop
            for (int i = pos1 + 1; i <= pos2; i++) {
                if (_mainCircle.get(i) != MARBLE.NONE) {
                    importantInbetweeners.add(i);
                }
            }
        } else {
            // get to half way
            for (int i = pos1 + 1; i < 64; i++) {
                if (_mainCircle.get(i) != MARBLE.NONE) {
                    importantInbetweeners.add(i);
                }
            }
            // rest
            for (int i = 0; i <= pos2; i++) {
                if (_mainCircle.get(i) != MARBLE.NONE) {
                    importantInbetweeners.add(i);
                }
            }
        }

        return importantInbetweeners;
    }

    /**
     *
     * Gets the inbetweeners in the main circle in the BACKWARDS direction!
     * (exclusive, inclusive)
     * IMPORTANT: NEVER USE THIS METHOD WHEN TRYING TO MOVE FORWARDS
     */
    private ArrayList<Integer> getInbetweenersBackwards(int pos1, int pos2) throws IndexOutOfBoundsException
    {
        int newPos1 = pos2;
        int newPos2 = pos1;

        //manage exclusive/inclusive correctly
        newPos1 = newPos1 - 1;
        newPos2 = newPos2 - 1;

        //fix looping
        if(newPos1 < 0)
        {
            newPos1 = 64 + newPos1;
        }
        if(newPos2 < 0)
        {
            newPos2 = 64 + newPos2;
        }

        return getInbetweeners(newPos1, newPos2);
    }

    /**
     * Resets a marble on the main circle back to the base
     */
    private void resetMarble(int pos) throws NoMarbleException, IndexOutOfBoundsException {
        if (pos < 0 || pos >= 64)
            throw new IndexOutOfBoundsException("the position has to be in range 0-63 (inclusive)");

        if (_mainCircle.get(pos) == MARBLE.NONE)
            throw new NoMarbleException();

        MARBLE m = _mainCircle.get(pos);

        setMarbleAtPosition(pos, MARBLE.NONE);
        switch (m) {
            case RED:
                _redBase++;
                break;
            case BLUE:
                _blueBase++;
                break;
            case GREEN:
                _greenBase++;
                break;
            case YELLOW:
                _yellowBase++;
                break;
        }
    }

    /**
     * resets any inbetweeners (exclusive, inclusive)
     */
    private void resetInbetweeners(int pos1, int pos2) throws NoMarbleException {
        ArrayList<Integer> relevantInbetweeners = getInbetweeners(pos1, pos2);

        for (Integer inb : relevantInbetweeners) {
            resetMarble(inb);
        }
    }

    /**
     * Get the TurnColor from a marble at a position x on the main circle
     *
     * @throws NoMarbleException if there is no marble at x
     */
    private COLOR getColorFromPosition(int pos) throws NoMarbleException {
        if (pos < 0 || pos >= 64)
            throw new IndexOutOfBoundsException("the position has to be in range 0-63 (inclusive)");
        MARBLE m = _mainCircle.get(pos);

        switch (m) {
            case GREEN:
                return COLOR.GREEN;
            case BLUE:
                return COLOR.BLUE;
            case RED:
                return COLOR.RED;
            case YELLOW:
                return COLOR.YELLOW;
            default:
                throw new NoMarbleException();
        }
    }

    /**
     * Switches the marbles at pos1 to pos2, no checking is done
     * You can also change a NONE with something.
     */
    private void changePositions(int pos1, int pos2) throws IndexOutOfBoundsException {
        MARBLE marble1;
        MARBLE marble2;

        if (pos1 < 0 || pos1 >= 64 || pos2 < 0 || pos2 >= 64)
            throw new IndexOutOfBoundsException("the positions have to be in range 0-63 (inclusive)");

        marble1 = _mainCircle.get(pos1);
        marble2 = _mainCircle.get(pos2);

        setMarbleAtPosition(pos1, marble2);
        setMarbleAtPosition(pos2, marble1);
    }

    /**
     * Moves the marble from position 1 to position 2 in the main circle
     *
     * @param removeInbetweeners IMPORTANT: NEVER SET THIS TO TRUE WHEN TRYING TO
     *                           MOVE BACKWARDS
     * @throws NoMarbleException            no marble was at pos1
     * @throws MoveBlockedByMarbleException there was already a marble at pos2
     */
    private void movePositions(int pos1, int pos2, boolean removeInbetweeners)
            throws InvalidMoveException, IndexOutOfBoundsException {
        if (pos1 < 0 || pos1 >= 64 || pos2 < 0 || pos2 >= 64)
            throw new IndexOutOfBoundsException("the positions have to be in range 0-63 (inclusive)");

        MARBLE m1 = _mainCircle.get(pos1);
        MARBLE m2 = _mainCircle.get(pos2);

        if (m1 == MARBLE.NONE)
            throw new NoMarbleException();
        if (m2 != MARBLE.NONE) {
            // reset the problem marble
            resetMarble(pos2);
        }

        // all good, make the move
        if (removeInbetweeners) {
            resetInbetweeners(pos1, pos2);
        }

        setMarbleAtPosition(pos2, m1);
        setMarbleAtPosition(pos1, MARBLE.NONE);
    }

    /**
     * Moves the marble from position 1 to position 2 where position 2 is in the
     * respective goal.
     *
     * @param goalColor          the goal color to consider
     * @param startInGoal        defines if position 1 is already in the goal. (ex:
     *                           move 1 forward in the goal)
     * @param removeInbetweeners IMPORTANT: NEVER SET THIS TO TRUE WHEN TRYING TO
     *                           MOVE BACKWARDS
     * @throws NoMarbleException            no marble was at pos1
     * @throws MoveBlockedByMarbleException there was already a marble at pos2
     */
    private void movePositions(int pos1, int pos2, COLOR goalColor, boolean startInGoal, boolean removeInbetweeners)
            throws InvalidMoveException, IndexOutOfBoundsException {
        MARBLE m1;
        MARBLE m2;

        int colorintersect = 0;

        ArrayList<MARBLE> coloredGoalList = _redGoal;
        // get the respective color
        switch (goalColor) {
            case RED:
                coloredGoalList = _redGoal;
                colorintersect = REDINTERSECT;
                break;
            case BLUE:
                coloredGoalList = _blueGoal;
                colorintersect = BLUEINTERSECT;
                break;
            case YELLOW:
                coloredGoalList = _yellowGoal;
                colorintersect = YELLOWINTERSECT;
                break;
            case GREEN:
                coloredGoalList = _greenGoal;
                colorintersect = GREENINTERSECT;
        }

        if (startInGoal) {
            if (pos1 < 0 || pos2 >= 4)
                throw new IndexOutOfBoundsException(
                        "if startInGoal is true, the pos1 can be in the range 0-3 (inclusive)");
            m1 = coloredGoalList.get(pos1);
        } else {
            if (pos1 < 0 || pos2 >= 64)
                throw new IndexOutOfBoundsException(
                        "if startInGoal is false, the pos1 can be in the range 0-63 (inclusive)");
            m1 = _mainCircle.get(pos1);
        }

        if (pos2 < 0 || pos2 >= 4)
            throw new IndexOutOfBoundsException("the pos2 can be in the range 0-3 (inclusive)");
        m2 = coloredGoalList.get(pos2);

        // we now have the start and end position. we move from pos1 to pos2

        if (m1 == MARBLE.NONE)
            throw new NoMarbleException();
        if (m2 != MARBLE.NONE) {
            // reset the problem marble
            resetMarble(pos2);
        }

        // all good, make the move

        if (removeInbetweeners && !startInGoal) {
            resetInbetweeners(pos1, colorintersect);
        }

        setMarbleAtPosition(pos2, m1, goalColor);

        if (startInGoal) {
            setMarbleAtPosition(pos1, MARBLE.NONE, goalColor);
        } else {
            setMarbleAtPosition(pos1, MARBLE.NONE);
        }
    }

    /**
     * gets the next position when moving forward a certain distance on the main
     * field
     */
    private int getDistanceInBetween(int startPosition, int endPosition) throws IndexOutOfBoundsException {
        return getDistanceInBetween(startPosition, endPosition, true);
    }

    /**
     * gets the next position when moving a certain distance on the main field,
     * either forward or backwards
     */
    private int getDistanceInBetween(int startPosition, int endPosition, boolean forward)
            throws IndexOutOfBoundsException {
        if (startPosition < 0 || startPosition >= 64 || endPosition < 0 || endPosition >= 64)
            throw new IndexOutOfBoundsException(
                    "Index is wrong: the main ring has indices between 0 and 63 while goals reach from 0 to 3");
        if (forward) {
            if (startPosition <= endPosition) // we do not loop over
            {
                return endPosition - startPosition;
            } else // we loop over the edge
            {
                int rest = 63 - startPosition;
                return endPosition + rest + 1;
            }
        } else // we move backwards
        {
            if (endPosition <= startPosition) // we do not loop over
            {
                return startPosition - endPosition;
            } else // we loop over the edge
            {
                int rest = 63 - endPosition;
                return startPosition + rest + 1;
            }
        }
    }

    /**
     * gets the next position when moving a certain distance to a goal, forward
     * only!
     *
     * @param startPosition the start position (0-63 or 0-3, depending on
     *                      startInGoal)
     * @param endPosition   the end position (0-3)
     * @param goalColor     the color of the goal we are considering for moving into
     * @param startInGoal   if the marble already starts in its respective goal (eg:
     *                      moving 1 forward in the goal)
     */
    private int getDistanceInBetween(int startPosition, int endPosition, COLOR goalColor, boolean startInGoal)
            throws IndexOutOfBoundsException {
        if (!startInGoal) // normal move into a goal from outside
        {
            int intersect = 0;
            // get the respective intersect
            switch (goalColor) {
                case RED:
                    intersect = REDINTERSECT;
                    break;
                case BLUE:
                    intersect = BLUEINTERSECT;
                    break;
                case YELLOW:
                    intersect = YELLOWINTERSECT;
                    break;
                case GREEN:
                    intersect = GREENINTERSECT;
            }

            // distance from the startposition to the respective INTERSECT
            int distanceToIntersect = getDistanceInBetween(startPosition, intersect);

            if (endPosition < 0 || endPosition >= 4)
                throw new IndexOutOfBoundsException("endposition must be between 0 and 3 (inclusive)");

            // distance between the 2 positions
            return distanceToIntersect + 1 + endPosition;

        } else // from goal to goal, more simple
        {
            if (endPosition < startPosition)
                throw new IndexOutOfBoundsException("cannot move backwards in the goal");
            if (endPosition < 0 || endPosition >= 4)
                throw new IndexOutOfBoundsException("endposition must be between 0 and 3 (inclusive)");
            if (startPosition < 0 || startPosition >= 4)
                throw new IndexOutOfBoundsException("startposition must be between 0 and 3 (inclusive)");

            return endPosition - startPosition;
        }
    }

    public BoardData getFormattedBoardState() {
        ArrayList<String> board = new ArrayList<>();
        for (MARBLE m : _mainCircle) {
            board.add(m.toString());
        }

        ArrayList<String> redGoal = new ArrayList<>();
        for (MARBLE m : _redGoal) {
            redGoal.add(m.toString());
        }
        ArrayList<String> greenGoal = new ArrayList<>();
        for (MARBLE m : _greenGoal) {
            greenGoal.add(m.toString());
        }
        ArrayList<String> blueGoal = new ArrayList<>();
        for (MARBLE m : _blueGoal) {
            blueGoal.add(m.toString());
        }
        ArrayList<String> yellowGoal = new ArrayList<>();
        for (MARBLE m : _yellowGoal) {
            yellowGoal.add(m.toString());
        }

        BoardData boardData = new BoardData(board, redGoal, greenGoal, blueGoal, yellowGoal, _redBase, _greenBase,
                _blueBase, _yellowBase);
        return boardData;
    }

    /**
     * Makes one or more moves specified by the move object.
     * IMPORTANT: This does NOT check if the move is allowed depending on the rules
     * (eg: move cost 12, card value: 8)
     *
     * @param move the move object that holds the move
     * @throws InvalidMoveException if the move cannot be completed because of the
     *                              board state
     */
    public void makeMove(Move move) throws InvalidMoveException {
        // expects the move to be valid
        if (move == null || !move.isWellFormed()) {
            throw new InvalidMoveException("BAD_STRUCTURE", "Bad move structure");
        }

        // For each move, make it
        for (int i = 0; i < move.get_fromPos().size(); i++) {
            int fromPos = move.get_fromPos().get(i).getIndex();
            int toPos = move.get_toPos().get(i).getIndex();
            boolean startsInGoal = move.get_fromPos().get(i).isInGoal();
            boolean endsInGoal = move.get_toPos().get(i).isInGoal();
            // unblock if it was on an intersection
            if(!startsInGoal){
                switch(fromPos){
                    case REDINTERSECT:
                        REDBLOCKED = false;
                        break;
                    case GREENINTERSECT:
                        GREENBLOCKED = false;
                        break;
                    case BLUEINTERSECT:
                        BLUEBLOCKED = false;
                        break;
                    case YELLOWINTERSECT:
                        YELLOWBLOCKED = false;
                        break;
                }
            }
            // do the move
            if (endsInGoal) {
                movePositions(fromPos, toPos, move.get_color(), startsInGoal,
                        move.get_card() != null ? move.get_card().isSeven() : false);
            } else {
                switch(toPos){
                    case REDINTERSECT:
                        REDBLOCKED = false;
                        break;
                    case GREENINTERSECT:
                        GREENBLOCKED = false;
                        break;
                    case BLUEINTERSECT:
                        BLUEBLOCKED = false;
                        break;
                    case YELLOWINTERSECT:
                        YELLOWBLOCKED = false;
                        break;
                }
                movePositions(fromPos, toPos, move.get_card() != null ? move.get_card().isSeven() : false);
            }

        }

        log.info(String.format("CARD: %s: RED: %b, YELLOW %b, GREEN %b, BLUE %b", move.get_card() != null ? move.get_card().getFormatted() : "NOT SPECIFIED", REDBLOCKED, YELLOWBLOCKED, GREENBLOCKED, BLUEBLOCKED));
    }

    /**
     * Makes a starting move (from the base to the starting intersect position)
     * IMPORTANT: This does NOT check if the move is allowed depending on the rules
     * (eg: card is not Ace/Joker/King)
     *
     * @param color the color we want to move out
     * @throws InvalidMoveException if the move cannot be completed because of the
     *                              board state
     */
    public void makeStartingMove(COLOR color) throws InvalidMoveException {
        switch (color) {
            case RED:
                if (_redBase > 0) {
                    if (!isEmptyAt(REDINTERSECT))
                        resetMarble(REDINTERSECT);

                    _redBase = _redBase - 1;
                    setMarbleAtPosition(REDINTERSECT, MARBLE.RED);
                    REDBLOCKED = true;
                } else {
                    throw new InvalidMoveException("NOTHING_LEFT", "there are no marbles left to start with");
                }
                break;
            case YELLOW:
                if (_yellowBase > 0) {
                    if (!isEmptyAt(YELLOWINTERSECT))
                        resetMarble(YELLOWINTERSECT);

                    _yellowBase = _yellowBase - 1;
                    setMarbleAtPosition(YELLOWINTERSECT, MARBLE.YELLOW);
                    YELLOWBLOCKED = true;
                } else {
                    throw new InvalidMoveException("NOTHING_LEFT", "there are no marbles left to start with");
                }
                break;
            case GREEN:
                if (_greenBase > 0) {
                    if (!isEmptyAt(GREENINTERSECT))
                        resetMarble(GREENINTERSECT);

                    _greenBase = _greenBase - 1;
                    setMarbleAtPosition(GREENINTERSECT, MARBLE.GREEN);
                    GREENBLOCKED = true;
                } else {
                    throw new InvalidMoveException("NOTHING_LEFT", "there are no marbles left to start with");
                }
                break;
            case BLUE:
                if (_blueBase > 0) {
                    if (!isEmptyAt(BLUEINTERSECT))
                        resetMarble(BLUEINTERSECT);

                    _blueBase = _blueBase - 1;
                    setMarbleAtPosition(BLUEINTERSECT, MARBLE.BLUE);
                    BLUEBLOCKED = true;
                } else {
                    throw new InvalidMoveException("NOTHING_LEFT", "there are no marbles left to start with");
                }
                break;
        }

        log.info(String.format("CARD: START: RED: %b, YELLOW %b, GREEN %b, BLUE %b", REDBLOCKED, YELLOWBLOCKED, GREENBLOCKED, BLUEBLOCKED));
    }

    public boolean checkWinningCondition(COLOR color) {
        switch (color) {
            case RED:
                for (MARBLE m : _redGoal) {
                    if (m == MARBLE.NONE) {
                        return false;
                    }
                }
                break;
            case BLUE:
                for (MARBLE m : _blueGoal) {
                    if (m == MARBLE.NONE) {
                        return false;
                    }
                }
                break;
            case GREEN:
                for (MARBLE m : _greenGoal) {
                    if (m == MARBLE.NONE) {
                        return false;
                    }
                }
                break;
            case YELLOW:
                for (MARBLE m : _yellowGoal) {
                    if (m == MARBLE.NONE) {
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    /**
     * Makes a switch specified by the two positions on the board
     * IMPORTANT: This does NOT check if the move is allowed depending on the rules
     * (eg: card is not a joker/jack)
     *
     * @throws InvalidMoveException if the move cannot be completed because of the
     *                              board state or if the card does not allow the
     *                              move
     */
    public void makeSwitch(int start, int end) throws InvalidMoveException {
        switch(start){
            case REDINTERSECT:
                REDBLOCKED = false;
                break;
            case YELLOWINTERSECT:
                YELLOWBLOCKED = false;
                break;
            case GREENINTERSECT:
                GREENBLOCKED = false;
                break;
            case BLUEINTERSECT:
                BLUEBLOCKED = false;
                break;
            default:
                break;
        }
        switch(end){
            case REDINTERSECT:
                REDBLOCKED = false;
                break;
            case YELLOWINTERSECT:
                YELLOWBLOCKED = false;
                break;
            case GREENINTERSECT:
                GREENBLOCKED = false;
                break;
            case BLUEINTERSECT:
                BLUEBLOCKED = false;
                break;
            default:
                break;
        }
        try {
            changePositions(start, end);
        } catch (IndexOutOfBoundsException e) {
            throw new InvalidMoveException("OUT_OF_BOUNDS", "one of the positions was out of bounds (0-63)");
        }

        log.info(String.format("CARD: SWITCH: RED: %b, YELLOW %b, GREEN %b, BLUE %b", REDBLOCKED, YELLOWBLOCKED, GREENBLOCKED, BLUEBLOCKED));
    }

    /**
     * Returns a list of move distances valid for a card, ignores JOKER and SEVEN
     * USE WITH CAUTION
     */
    private List<Integer> getMoveValuesBasedOnCard(Card card)
    {
        ArrayList<Integer> allMoveValues = new ArrayList<>();

        switch (card.getValue()) {
            case TWO:
                allMoveValues.add(2);
                break;
            case THREE:
                allMoveValues.add(3);
                break;
            case FIVE:
                allMoveValues.add(5);
                break;
            case SIX:
                allMoveValues.add(6);
                break;
            case EIGHT:
                allMoveValues.add(8);
                break;
            case NINE:
                allMoveValues.add(9);
                break;
            case TEN:
                allMoveValues.add(10);
                break;
            case QUEEN:
                allMoveValues.add(12);
                break;
            case KING:
                allMoveValues.add(13);
                break;
            case FOUR:
                allMoveValues.add(4);
                allMoveValues.add(-4);
                break;
            case ACE:
                allMoveValues.add(1);
                allMoveValues.add(11);
                break;
            default:
                break; //the 7 and Joker are differently tested
        }

        return allMoveValues;
    }

    //Functions that calculate possible moves

    /**
     * Returns a list of all possible generic positions reachable from a given position and a card (+ color)
     * @param startPos the position we start in
     * @param earlyReturn if true, the method will return immediately after the first is found. The return List will ALWAYS have a length of 0 or 1.
     */
    private List<BoardPosition> reachableGenericMoves(BoardPosition startPos, Card card, COLOR color, boolean earlyReturn)
    {
        List<Integer> moveValues = getMoveValuesBasedOnCard(card);

        if(moveValues.size() == 0) return new ArrayList<BoardPosition>(); //not a generic card
        if(startPos.isInGoal()) return new ArrayList<>(); //not a generic move. this is a goal to goal move
        if(startPos.getIndex() == -1) return new ArrayList<>(); //not on field

        // make a new move with the card
        Move m = new Move();
        m.set_card(card);
        m.set_color(color);
        List<BoardPosition> resultPositions = new ArrayList<>();

        // try normal moves
        for (int j = 0; j < moveValues.size(); j++) {

            // try a normal move
            m.set_fromPos(new ArrayList<>(Arrays.asList(new BoardPosition(startPos.getIndex(), false))));
            m.set_toPos(new ArrayList<>(
                    Arrays.asList(new BoardPosition(getIndexAfterDistance(startPos.getIndex(), moveValues.get(j)), false))));

            // always early return, saves this O(scary) algorithm to terminate a lot earlier
            // most of the time
            try {
                if (isValidMove(m))
                {
                    BoardPosition bp = new BoardPosition(m.get_toPos().get(0).getIndex(), false);
                    resultPositions.add(bp);

                    if(earlyReturn) return resultPositions; //early return to save time if possible
                }
            }
            catch (InvalidMoveException e) {
                // do nothing here
                // ik this is terrible, but our move is always well-formed in this case
            }
        }

        return resultPositions;
    }

    /**
     * Returns a list of all possible generic goal positions reachable from a given position and a card (+ color)
     * @param startPos the position we start in
     * @param earlyReturn if true, the method will return immediately after the first is found. The return List will ALWAYS have a length of 0 or 1.
     */
    private List<BoardPosition> reachableGenericGoalMoves(BoardPosition startPos, Card card, COLOR color, boolean earlyReturn)
    {
        List<Integer> moveValues = getMoveValuesBasedOnCard(card);

        if(moveValues.size() == 0) return new ArrayList<BoardPosition>(); //not a generic card
        if(startPos.isInGoal()) return new ArrayList<>(); //not a generic move. this is a goal to goal move
        if(startPos.getIndex() == -1) return new ArrayList<>(); //not on field

        Move m = new Move();
        m.set_card(card);
        m.set_color(color);
        List<BoardPosition> resultPositions = new ArrayList<>();

        for (int j = 0; j < moveValues.size(); j++) {

            // try to construct the valid indices, this should always succeed
            m.set_fromPos(new ArrayList<>(Arrays.asList(new BoardPosition(startPos.getIndex(), false))));
            try {
                //this will return {-1} as a goal position, which will be validated as wrong.
                int goalPos =  getIndexInGoalAfterDistance(startPos.getIndex(), moveValues.get(j), color);
                if(goalPos == -1)
                {
                    continue;
                }
                m.set_toPos(new ArrayList<>(Arrays.asList(new BoardPosition(goalPos, true))));
            }
            catch (IndexOutOfBoundsException e) {
                //a 4 cannot move backwards in a goal (distance has to be positive), so we catch this in this exception
                continue; //will be empty, so fail 100%
            }

            //now try the move
            try {
                if (isValidMove(m))
                {
                    BoardPosition bp = new BoardPosition(m.get_toPos().get(0).getIndex(), true);
                    resultPositions.add(bp);

                    if(earlyReturn) return resultPositions; //early return to save time if possible
                }
            } catch (InvalidMoveException e) {
                // do nothing here
                // ik this is terrible, but our move is always well-formed in this case
            }
        }

        return resultPositions;
    }

    /**
     * Returns a list of all possible goal positions reachable from a given position in a goal and a card (+ color)
     * @param startPos the position we start in
     * @param earlyReturn if true, the method will return immediately after the first is found. The return List will ALWAYS have a length of 0 or 1.
     */
    private List<BoardPosition> reachableGoalToGoalMoves(BoardPosition startPos, Card card, COLOR color, boolean earlyReturn)
    {
        List<Integer> moveValues = getMoveValuesBasedOnCard(card);

        if(moveValues.size() == 0) return new ArrayList<BoardPosition>(); //not a generic card
        if(!startPos.isInGoal()) return new ArrayList<>(); //not a goal to goal move.
        if(startPos.getIndex() == -1) return new ArrayList<>(); //not on field, this should never trigger

        Move m = new Move();
        m.set_card(card);
        m.set_color(color);
        List<BoardPosition> resultPositions = new ArrayList<>();

        for (int j = 0; j < moveValues.size(); j++) {

            if(moveValues.get(j) < 1 || moveValues.get(j) > 3) //will fail %100 percent since its too large
            {
                continue;
            }

            // try a goal move from a goal
            int toPos = startPos.getIndex() + moveValues.get(j);

            if(toPos < 0 || toPos > 3)
            {
                //index out of bounds anyway
                continue;
            }

            m.set_fromPos(new ArrayList<>(Arrays.asList(new BoardPosition(startPos.getIndex(), true))));
            m.set_toPos(new ArrayList<>(Arrays.asList(new BoardPosition(toPos, true))));

            // try the move
            // always early return, saves this O(scary) algorithm to terminate a lot earlier
            // most of the time
            try {
                if (isValidMove(m))
                {
                    BoardPosition bp = new BoardPosition(m.get_toPos().get(0).getIndex(), true);
                    resultPositions.add(bp);

                    if(earlyReturn) return resultPositions; //early return to save time if possible
                }
            } catch (InvalidMoveException e) {
                // do nothing here
                // ik this is terrible, but our move is always well-formed in this case
            }
        }

        return resultPositions;
    }

    /**
     * Returns a list of all possible positions reachable from a given position with a jack
     * @param startPos the position we start in
     * @param earlyReturn if true, the method will return immediately after the first is found. The return List will ALWAYS have a length of 0 or 1.
     */
    private List<BoardPosition> reachableJackMoves(BoardPosition startPos, Card card, COLOR color, MARBLE ownMarble, boolean earlyReturn)
    {

        if(card.getValue() != CARDVALUE.JACK) return new ArrayList<>();
        if(startPos.isInGoal()) return new ArrayList<>();
        if(startPos.getIndex() == -1) return new ArrayList<>(); //not on field


        // make a new move with the card
        Move m = new Move();
        m.set_card(card);
        m.set_color(color);
        List<BoardPosition> resultPositions = new ArrayList<>();

        ArrayList<Integer> otherMarbles = new ArrayList<>();
        //get all other marbles on the main board
        for (int i = 0; i < _mainCircle.size(); i++) {
            if (_mainCircle.get(i) != ownMarble && _mainCircle.get(i) != MARBLE.NONE)
                otherMarbles.add(i);
        }

        for (int j = 0; j < otherMarbles.size(); j++) {

            // try the switch move for each combination
            m.set_fromPos(new ArrayList<>(Arrays.asList(new BoardPosition(startPos.getIndex(), false))));
            m.set_toPos(new ArrayList<>(Arrays.asList(new BoardPosition(otherMarbles.get(j), false))));

            // always early return, saves this O(scary) algorithm to terminate a lot earlier
            // most of the time
            try {
                if (isValidMove(m))
                {
                    BoardPosition bp = new BoardPosition(m.get_toPos().get(0).getIndex(), false);
                    resultPositions.add(bp);

                    if(earlyReturn) return resultPositions; //early return to save time if possible
                }
            } catch (InvalidMoveException e) {
                // do nothing here
                // ik this is terrible, but our move is always well-formed in this case
            }
        }

        return resultPositions;
    }

    private List<BoardPosition> reachableStartMoves(BoardPosition startPos, Card card, COLOR color)
    {
        if(startPos.getIndex() != -1) return new ArrayList<>();
        if(startPos.isInGoal()) return new ArrayList<>();

        // make a new move with the card
        Move m = new Move();
        m.set_card(card);
        m.set_color(color);

        // try start move
        if(card.getValue() != CARDVALUE.ACE && card.getValue() != CARDVALUE.KING) return new ArrayList<>();

        int intersect = 0;
        switch (color) {
            case YELLOW:
                intersect = YELLOWINTERSECT;
                break;
            case GREEN:
                intersect = GREENINTERSECT;
                break;
            case RED:
                intersect = REDINTERSECT;
                break;
            case BLUE:
                intersect = BLUEINTERSECT;
                break;
        }

        m.set_fromPos(new ArrayList<>(Arrays.asList(new BoardPosition(startPos.getIndex(), startPos.isInGoal()))));
        m.set_toPos(new ArrayList<>(Arrays.asList(new BoardPosition(intersect, false))));
        List<BoardPosition> resultPositions = new ArrayList<>();

        try {
            if (isValidMove(m))
            {
                BoardPosition bp = new BoardPosition(m.get_toPos().get(0).getIndex(), false);
                resultPositions.add(bp);
            }
        } catch (InvalidMoveException e) {
            // do nothing here
            // ik this is terrible, but our move is always well-formed in this case
        }

        return resultPositions;
    }


    //Functions that evaluate if there is at least 1 possible move

    private boolean isGenericMovePossible(List<Integer> marblesOnMain, List<Integer> moveValues, Card card, COLOR color)
    {
        // try normal moves
        for (int i = 0; i < marblesOnMain.size(); i++) {

            BoardPosition bp = new BoardPosition(marblesOnMain.get(i), false);

            if(reachableGenericMoves(bp, card, color, true).size() == 1)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isGenericGoalMovePossible(List<Integer> marblesOnMain, List<Integer> moveValues, Card card, COLOR color)
    {

        // try goal moves
        for (int i = 0; i < marblesOnMain.size(); i++) {

            BoardPosition bp = new BoardPosition(marblesOnMain.get(i), false);

            if(reachableGenericGoalMoves(bp, card, color, true).size() == 1)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isGoalToGoalMovePossible(List<Integer> marblesInGoal, List<Integer> moveValues, Card card, COLOR color)
    {

        // try goal to goal moves
        for (int i = 0; i < marblesInGoal.size(); i++) {

            BoardPosition bp = new BoardPosition(marblesInGoal.get(i), true);

            if(reachableGoalToGoalMoves(bp, card, color, true).size() == 1)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isJackMovePossible(List<Integer> marblesOnMain, Card card, COLOR color, MARBLE searchedMarbleCol)
    {
        // can we make any switch?
        for (int i = 0; i < marblesOnMain.size(); i++) {

            BoardPosition bp = new BoardPosition(marblesOnMain.get(i), false);

            if(reachableJackMoves(bp, card, color, searchedMarbleCol, true).size() == 1)
            {
                return true;
            }
        }

        return false;
    }

    private boolean isStartMovePossible(Card card, COLOR color)
    {
        BoardPosition bp = new BoardPosition(-1, false);

        if(reachableStartMoves(bp, card, color).size() == 1)
        {
            return true;
        }

        return false;
    }

    private boolean isJokerMovePossible(COLOR color)
    {
        ArrayList<CARDVALUE> allValues = new ArrayList<>(Arrays.asList(CARDVALUE.values()));
        allValues.remove(CARDVALUE.JOKER);

        for (CARDVALUE value : allValues) {
            Card c = new Card(value, CARDTYPE.DEFAULT, CARDSUITE.CLUBS);
            if (isAnyMovePossible(c, color))
                return true; // THIS IS RECURSIVE: MAKE SURE THERE IS NO JOKER IN THE LIST!!!
            // oh great, O(5n^6) just turned into O(scary)
        }

        return false;
    }

    private List<Integer[]> getMoveComboPermutations()
    {
        List<Integer[]> allPossibilites = new ArrayList<>();

        // pyramid of dooooooooom.
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                for (int k = 0; k < 8; k++) {
                    for (int l = 0; l < 8; l++) {
                        if (i + j + k + l == 7) {
                            // theoretically valid, so add
                            Integer[] movecombo = { i, j, k, l };
                            allPossibilites.add(movecombo);
                        }
                    }
                }
            }
        }

        return allPossibilites;
    }

    private boolean makeSevenMoveAndTest(Integer[] movecombo, List<Integer> marblesOnMain, List<Integer> marblesInGoal, COLOR color, Integer countToGoal, Integer maincount, Integer offcount)
    {
        // move init
        Move sevenmove = new Move();
        sevenmove.set_color(color);
        Card c = new Card(CARDVALUE.SEVEN, CARDTYPE.DEFAULT, CARDSUITE.CLUBS);
        sevenmove.set_card(c);

        ArrayList<BoardPosition> fromPos = new ArrayList<>();
        ArrayList<BoardPosition> toPos = new ArrayList<>();

        //count to goal destructor
        if(countToGoal != 0 && countToGoal != 1 && countToGoal != 2)
        {
            countToGoal = 0;
        }

        int marbleCount = 0;
        // assign to all marbles
        for (int i = 0; i < marblesOnMain.size(); i++) {

            //countToGoal destructor
            int toPosValue = -1;
            boolean toPosGoal = false;
            switch (countToGoal)
            {
                case 0:
                    toPosValue = getIndexAfterDistance(marblesOnMain.get(i), movecombo[marbleCount]);
                    toPosGoal = false;
                    break;
                case 1:
                    toPosValue = i == maincount ? getIndexInGoalAfterDistance(marblesOnMain.get(i), movecombo[marbleCount], color) : getIndexAfterDistance(marblesOnMain.get(i), movecombo[marbleCount]);
                    toPosGoal = i == maincount ? true : false;
                    break;
                case 2:
                    toPosValue = i == maincount || i == offcount ? getIndexInGoalAfterDistance(marblesOnMain.get(i), movecombo[marbleCount], color) : getIndexAfterDistance(marblesOnMain.get(i), movecombo[marbleCount]);
                    toPosGoal = i == maincount || i == offcount ? true : false;
                    break;
            }

            //if it ends up OOB, that's fine. we just dont consider it then
            if(toPosGoal != false || toPosValue != marblesOnMain.get(i))
            {
                try{
                    toPos.add(new BoardPosition(toPosValue, toPosGoal));
                    fromPos.add(new BoardPosition(marblesOnMain.get(i), false));
                }catch (IndexOutOfBoundsException e)
                {
                    //do nothing here
                }


            }
            marbleCount++;
        }
        //don't forget the goal, if it ends up OOB, that's fine. The validMove check will then just return false.
        for (int i = 0; i < marblesInGoal.size(); i++) {
            if(marblesInGoal.get(i) != marblesInGoal.get(i) + movecombo[marbleCount])
            {
                try{
                    toPos.add(new BoardPosition(marblesInGoal.get(i) + movecombo[marbleCount], true));
                    fromPos.add(new BoardPosition(marblesInGoal.get(i), true));
                }catch (IndexOutOfBoundsException e)
                {
                    //do nothing here
                }
            }
            marbleCount++;
        }

        boolean addsTo7 = true;

        for (int i = marbleCount; i < 4; i++) {
            if (movecombo[i] != 0) {
                // invalid, as it does not sum up to 7 then.
                addsTo7 = false;
            }
        }

        if(!addsTo7) return false;

        sevenmove.set_fromPos(fromPos);
        sevenmove.set_toPos(toPos);

        List<Move> allMoves = generatePermutations(sevenmove);
        for (Move move : allMoves) {
            // try the move
            try {
                if (isValidMove(move))
                    return true;
            } catch (InvalidMoveException | IndexOutOfBoundsException e) {
                // do nothing here
                // ik this is terrible
            }
        }

        return false;
    }

    private List<Move> generatePermutations(Move move)
    {
        //iterative Heap's algo

        List<Move> finalList = new ArrayList<>();

        List<BoardPosition> fp = new ArrayList<>(move.get_fromPos());
        List<BoardPosition> tp = new ArrayList<>(move.get_toPos());

        int n = move.get_fromPos().size();

        int[] indices = new int[n];
        for (int i = 0; i < n; i++) {
            indices[i] = 0;
        }

        //initial
        finalList.add(new Move(new ArrayList<>(fp), new ArrayList<>(tp), move.get_card(), move.getToken(), move.get_color()));

        int i = 0;
        while(i < n)
        {
            if(indices[i] < i)
            {
                //swap
                int swapindex1 = i % 2 == 0 ? 0 : indices[i];
                int swapindex2 = i;
                Collections.swap(fp, swapindex1, swapindex2);
                Collections.swap(tp, swapindex1, swapindex2);

                //momentary render out
                Move m = new Move(new ArrayList<>(fp), new ArrayList<>(tp), move.get_card(), move.getToken(), move.get_color());
                finalList.add(m);

                //go on
                indices[i]++;
                i = 0;
            }else
            {
                indices[i] = 0;
                i++;
            }
        }

        return finalList;
    }

    private boolean isSevenMovePossibleZeroGoal(List<Integer> marblesOnMain, List<Integer> marblesInGoal, List<Integer[]> moveCombos, COLOR color)
    {
        for (Integer[] movecombo : moveCombos) {
            if(makeSevenMoveAndTest(movecombo, marblesOnMain, marblesInGoal, color, 0, 0, 0)) return true;
        }
        return false;
    }

    private boolean isSevenMovePossibleOneGoal(List<Integer> marblesOnMain, List<Integer> marblesInGoal, List<Integer[]> moveCombos, COLOR color)
    {
        for (Integer[] movecombo : moveCombos) {
            for (int maincount = 0; maincount < marblesOnMain.size(); maincount++) { //try every one on main to get into a goal
                if(makeSevenMoveAndTest(movecombo, marblesOnMain, marblesInGoal, color, 1, maincount, 0)) return true;
            }
        }
        return false;
    }

    private boolean isSevenMovePossibleTwoGoal(List<Integer> marblesOnMain, List<Integer> marblesInGoal, List<Integer[]> moveCombos, COLOR color)
    {
        if(marblesOnMain.size() < 2) return false;

        for (Integer[] movecombo : moveCombos) {

            // permutations of what 2 marbles should go in a goal state
            // yes, you see correctly. more for loops
            for (int maincount = 0; maincount < marblesOnMain.size(); maincount++) {
                for (int offcount = maincount + 1; offcount < marblesOnMain.size(); offcount++) {
                    if(makeSevenMoveAndTest(movecombo, marblesOnMain, marblesInGoal, color, 2, maincount, offcount)) return true;
                }
            }
        }

        return false;
    }

    /**
     * This method is pretty heavy, try to call it as little as possible
     * It checks if for a color and a card, any move is possible given the current
     * board state.
     */
    public boolean isAnyMovePossible(Card card, COLOR col) {
        // marble color
        MARBLE searchedMarbleCol = MARBLE.NONE;
        ArrayList<MARBLE> colGoal = new ArrayList<>();

        //set the goal array correctly
        switch (col) {
            case YELLOW:
                searchedMarbleCol = MARBLE.YELLOW;
                colGoal = new ArrayList<>(_yellowGoal);
                break;
            case GREEN:
                searchedMarbleCol = MARBLE.GREEN;
                colGoal = new ArrayList<>(_greenGoal);
                break;
            case RED:
                searchedMarbleCol = MARBLE.RED;
                colGoal = new ArrayList<>(_redGoal);
                break;
            case BLUE:
                searchedMarbleCol = MARBLE.BLUE;
                colGoal = new ArrayList<>(_blueGoal);
                break;
        }

        // get the marbles of color
        ArrayList<Integer> marblesOnMain = new ArrayList<>();
        ArrayList<Integer> marblesInGoal = new ArrayList<>();

        for (int i = 0; i < _mainCircle.size(); i++) {
            if (_mainCircle.get(i) == searchedMarbleCol)
                marblesOnMain.add(i);
        }

        for (int i = 0; i < colGoal.size(); i++) {
            if (colGoal.get(i) == searchedMarbleCol)
                marblesInGoal.add(i);
        }

        // make a new move with the card
        Move m = new Move();
        m.set_card(card);
        m.set_color(col);

        List<Integer> allMoveValues = getMoveValuesBasedOnCard(card); //all except Joker and 7, since they are not fixed

        if(allMoveValues.size() != 0) //maybe some valid generic move
        {
            if(isGenericMovePossible(marblesOnMain, allMoveValues, card, col)) return true;
            if(isGenericGoalMovePossible(marblesOnMain, allMoveValues, card, col)) return true;
            if(isGoalToGoalMovePossible(marblesInGoal, allMoveValues, card, col)) return true;
            if(isStartMovePossible(card, col)) return true;
        }

        //jack
        if(isJackMovePossible(marblesOnMain, card, col, searchedMarbleCol)) return true;


        // try 7 move. OH, GOD
        if (card.getValue() == CARDVALUE.SEVEN) {
            if (isAnySevenMovePossible(marblesOnMain, marblesInGoal, col)) return true;
        }

        // try joker move
        if (card.getValue() == CARDVALUE.JOKER) {
            if(isJokerMovePossible(col)) return true;
        }

        return false;
    }

    /**
     * These submethods are an utter mess. Keep your sanity and do not even try to look at them.
     * The complexity is about O(5n^6), so if you want to improve this, feel free.
     * For better overview just add your hours wasted here
     * HoursWasted = 7;
     */
    private boolean isAnySevenMovePossible(ArrayList<Integer> marblesOnMain, ArrayList<Integer> marblesInGoal, COLOR col) {
        // try all possible splits
        List<Integer[]> allPossibilites = getMoveComboPermutations();

        // now we have all possible splits in all permutations possible

        // 0 moves into a goal
        if(isSevenMovePossibleZeroGoal(marblesOnMain, marblesInGoal, allPossibilites, col)) return true;

        // 1 move into a goal
        if(isSevenMovePossibleOneGoal(marblesOnMain, marblesInGoal, allPossibilites, col)) return true;

        // 2 moves into a goal
        if(isSevenMovePossibleTwoGoal(marblesOnMain, marblesInGoal, allPossibilites, col)) return true;

        // more than 2 moves into a goal with a 7 is not possible
        return false;
    }

    //Functions that evaluate all possible move destinations based on a position and card

    /**
     * Returns a list of all possible boardPositions that can be reached based on a position, card and color.
     */
    public List<BoardPosition> whatMovesPossible(BoardPosition bp, Card card, COLOR color)
    {
        List<BoardPosition> resultPositions = new ArrayList<>();

        //for jack
        MARBLE jackMarbleColor = MARBLE.NONE;
        switch (color)
        {
            case RED:
                jackMarbleColor = MARBLE.RED;
                break;
            case BLUE:
                jackMarbleColor = MARBLE.BLUE;
                break;
            case GREEN:
                jackMarbleColor = MARBLE.GREEN;
                break;
            case YELLOW:
                jackMarbleColor = MARBLE.YELLOW;

        }

        resultPositions.addAll(reachableStartMoves(bp, card, color));
        resultPositions.addAll(reachableGenericMoves(bp, card, color, false));
        resultPositions.addAll(reachableGenericGoalMoves(bp, card, color, false));
        resultPositions.addAll(reachableGoalToGoalMoves(bp, card, color, false));
        resultPositions.addAll(reachableJackMoves(bp, card, color, jackMarbleColor, false));

        return resultPositions;

    }


    private int getIndexAfterDistance(int start, int distance) throws IndexOutOfBoundsException {
        if (start < 0 || start >= 64 || distance <= -64 || distance >= 64)
            throw new IndexOutOfBoundsException();

        // normal
        int newPos = start + distance;

        // looping
        if (distance < 0) {
            if (newPos < 0) {
                newPos = 64 + newPos;
            }
        } else {
            if (newPos > 63) {
                newPos = newPos - 64;
            }
        }

        return newPos;
    }

    private int getIndexInGoalAfterDistance(int start, int distance, COLOR color) throws IndexOutOfBoundsException {
        if (start < 0 || start >= 64 || distance < 0 || distance >= 64)
            throw new IndexOutOfBoundsException();

        int intersect = 0;
        switch (color) {
            case BLUE:
                intersect = BLUEINTERSECT;
                break;
            case RED:
                intersect = REDINTERSECT;
                break;
            case GREEN:
                intersect = GREENINTERSECT;
                break;
            case YELLOW:
                intersect = YELLOWINTERSECT;
                break;
        }

        // only forwards in goal!
        int distToGoalStart = getDistanceInBetween(start, intersect);
        int restDistance = distance - distToGoalStart;

        // cannot reach a goal
        if (restDistance < 1 || restDistance > 4)
            return -1;

        return restDistance - 1;

    }


    public boardValidation createMoveValidation(){
        return new boardValidation(
            copyList(_mainCircle),
            copyList(_redGoal),
            copyList(_greenGoal),
            copyList(_blueGoal),
            copyList(_yellowGoal),
            this._redBase,
            this._greenBase,
            this._blueBase,
            this._yellowBase,
            this.REDBLOCKED,
            this.GREENBLOCKED,
            this.BLUEBLOCKED,
            this.YELLOWBLOCKED
        );
    }

    private ArrayList<MARBLE> copyList(ArrayList<MARBLE> list){
        ArrayList<MARBLE> newList = new ArrayList<>();
        for(int i = 0; i < list.size(); i++){
            newList.add(copyMarble(list.get(i)));
        }
        return newList;
    }

    private MARBLE copyMarble(MARBLE m){
        switch(m){
            case BLUE:
                return MARBLE.BLUE;
            case GREEN:
                return MARBLE.GREEN;
            case RED:
                return MARBLE.RED;
            case YELLOW:
                return MARBLE.YELLOW;
            default:
                return MARBLE.NONE;
        }
    }

    public int getNumberInBase(COLOR color){
        int numberOfMarble = 0;
        switch (color){
            case BLUE:
                for (MARBLE marble: _blueGoal){
                    if (marble!=null){
                        numberOfMarble++;
                    }
                }
                return numberOfMarble;

            case GREEN:
                for (MARBLE marble: _greenGoal){
                    if (marble!=null){
                        numberOfMarble++;
                    }
                }
                return numberOfMarble;
            case RED:
                for (MARBLE marble: _redGoal){
                    if (marble!=null){
                        numberOfMarble++;
                    }
                }
                return numberOfMarble;
            case YELLOW:
                for (MARBLE marble: _yellowGoal){
                    if (marble!=null){
                        numberOfMarble++;
                    }
                }
                return numberOfMarble;
            default:
                return 0;
        }

    }

    //for Test
    public int getCountStartingMove(){

        return 0;
    }
    //for Test
    public int getCountJackMove(){

        return 0;
    }
    //for Test
    public int getCountNormalMove(){

        return 0;
    }

    @Override
    public boolean isValidMove(Move move) throws InvalidMoveException {
        boardValidation validator = createMoveValidation();
        return validator.isValidMove(move);
    }
}
