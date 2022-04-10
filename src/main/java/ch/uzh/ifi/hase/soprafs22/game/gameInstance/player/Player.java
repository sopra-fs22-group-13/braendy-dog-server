package ch.uzh.ifi.hase.soprafs22.game.gameInstance.player;

import ch.uzh.ifi.hase.soprafs22.game.constants.COLOR;
import ch.uzh.ifi.hase.soprafs22.game.gameInstance.cards.Card;
import com.sun.istack.NotNull;

import java.util.ArrayList;

public class Player {

    private final ArrayList<Card> _hand = new ArrayList<Card>();
    private final COLOR _turn;

    public Player(@NotNull int playerNr) throws IllegalArgumentException{
        if (playerNr < 0 || playerNr > 3) throw new IllegalArgumentException("Illegal player number. PlayerNr must be in range [0,3]");

        this._turn = COLOR.values()[playerNr];
    }

    public void removeCard(Card reqCard) {
        for (Card card: _hand) {
            if (reqCard.equalsContent(card)) {
                _hand.remove(card);
                return;
            }
        }
    }

    public boolean isCardAvailable(Card reqCard) {
        for (Card card: _hand) {
            if (reqCard.equalsContent(card)) return true;
        }

        return false;
    }

    public void addCard(Card card) {
        _hand.add(card);
    }

    public int getCardCount() {
        return _hand.size();
    }

    /**
     * TODO
     */
    /*
    public String getFormattedCard() {

    }
     */

    public COLOR getTurn() {
        return this._turn;
    }
}
