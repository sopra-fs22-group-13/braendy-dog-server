package ch.uzh.ifi.hase.soprafs22.game.gameInstance.cards;

import ch.uzh.ifi.hase.soprafs22.game.constants.CARDSUITE;
import ch.uzh.ifi.hase.soprafs22.game.constants.CARDTYPE;
import ch.uzh.ifi.hase.soprafs22.game.constants.CARDVALUE;
import ch.uzh.ifi.hase.soprafs22.game.constants.COLOR;

public class Card {

    private final CARDVALUE value;
    private final CARDSUITE suite;
    private final CARDTYPE type;

    public Card(CARDVALUE value, CARDTYPE type, CARDSUITE suite) {
        this.value = value;
        this.type = type;
        this.suite = suite;
    }

    // gets the move distance depending on the move
    public int getCorrespondingMoveDistance() {
        
        return 0;
    }

    public boolean isJoker() {
        return type == CARDTYPE.JOKER;
    }

    public boolean isAce() {
        return value == CARDVALUE.ACE;
    }

    public boolean isSeven() {
        return value == CARDVALUE.SEVEN;
    }

    public boolean isFour() {
        return value == CARDVALUE.FOUR;
    }

    public boolean canOpenMove() {
        return value == CARDVALUE.ACE || value == CARDVALUE.KING || value == CARDVALUE.JOKER;
    }

    public CARDSUITE getSuite() {
        return this.suite;
    }

    public CARDVALUE getValue() {
        return this.value;
    }

    public CARDTYPE getType() {
        return this.type;
    }

    /**
     * compares cards not by their reference, but by apparent equality
     * @return true if both cards are equal ignoring their references
     */
    public boolean equalsContent(Card card) {
        return this.suite == card.getSuite() && this.type == card.getType() && this.value == card.getValue();
    }

    public String getFormatted() {
        String formatted = "";
        switch (this.value) {
            case TWO -> formatted += "2";
            case THREE -> formatted += "3";
            case FOUR -> formatted += "4";
            case FIVE -> formatted += "5";
            case SIX -> formatted += "6";
            case SEVEN -> formatted += "7";
            case EIGHT -> formatted += "8";
            case NINE -> formatted += "9";
            case TEN -> formatted += "10";
            case JACK -> formatted += "J";
            case QUEEN -> formatted += "Q";
            case KING -> formatted += "K";
            case ACE -> formatted += "A";
            case JOKER -> { formatted += "Joker"; return formatted; }
        }
        switch (this.suite) {
            case CLUBS -> formatted += "C";
            case HEARTS -> formatted += "H";
            case SPADES -> formatted += "S";
            case DIAMOND -> formatted += "D";
        }
        return formatted;
    }
}
