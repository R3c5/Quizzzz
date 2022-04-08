package commons;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

public class GameSession {

    public long id;

    public List<Player> players;
    public List<Player> removedPlayers;

    public List<Joker> usedJokers;

    public Question currentQuestion;
    public List<Long> expectedAnswers;

    public AtomicInteger playersReady;
    public int questionCounter;
    public int gameRounds;
    public int difficultyFactor;
    public int timeJokers;
    public boolean isLeaderboardDisabled;

    public SessionType sessionType;

    public enum SessionType {
        SELECTING,
        WAITING_AREA,
        MULTIPLAYER,
        SINGLEPLAYER,
        TIME_ATTACK,
        SURVIVAL
    }

    public SessionStatus sessionStatus;

    public enum SessionStatus {
        SELECTING,
        WAITING_AREA,
        TRANSFERRING,
        ONGOING,
        STARTED,
        PAUSED,
        PLAY_AGAIN
    }

    @SuppressWarnings("unused")
    public GameSession() {
        // for object mapper
    }

    public GameSession(SessionType sessionType) {
        this(sessionType, new ArrayList<Player>(), new ArrayList<Long>());
    }

    public GameSession(SessionType sessionType, List<Player> players) {
        this(sessionType, players, new ArrayList<Long>());
    }

    public GameSession(SessionType sessionType, List<Player> players, List<Long> expectedAnswers) {
        this.removedPlayers = new ArrayList<Player>();
        this.usedJokers = new ArrayList<Joker>();
        this.players = players;
        this.sessionType = sessionType;
        this.expectedAnswers = expectedAnswers;
        this.playersReady = new AtomicInteger(0);
        this.questionCounter = 0;
        this.difficultyFactor = 1;
        this.timeJokers = 0;
        this.gameRounds = 20;
        this.isLeaderboardDisabled = false;

        this.sessionStatus = SessionStatus.STARTED;
        if (sessionType == SessionType.SELECTING) this.sessionStatus = SessionStatus.SELECTING;
        if (sessionType == SessionType.WAITING_AREA) this.sessionStatus = SessionStatus.WAITING_AREA;
    }

    public void setSessionType(SessionType type) {
        this.sessionType = type;
    }

    /**
     * Called when a new player has triggered a ready event
     */
    public void setPlayerReady() {
        if (playersReady.get() >= players.size()) return;
        playersReady.incrementAndGet();
    }

    /**
     * Called when a player has triggered a non-ready event
     */
    public void unsetPlayerReady() {
        if (playersReady.get() <= 0) return;
        playersReady.decrementAndGet();
    }

    /**
     * Adds a player to the list of players
     *
     * @param player Player to be added
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * Removes a player from the list of players
     *
     * @param player Player to be removed
     */
    public void removePlayer(Player player) {
        players.remove(player);
        if (sessionType == SessionType.SELECTING || sessionType == SessionType.WAITING_AREA) return;
        removedPlayers.add(player);
    }

    public void setCurrentQuestion(Question question) {
        this.currentQuestion = question;
    }

    public void setSessionStatus(SessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public void setQuestionCounter(int count) {
        this.questionCounter = count;
    }

    public void setGameRounds(int gameRounds) {
        this.gameRounds = gameRounds;
    }
    /**
     * Get the number of time jokers used in this round
     *
     * @return int representing the number of time jokers
     */
    public int getTimeJokers() {
        return this.timeJokers;
    }

    /**
     * Set the timeJoker to a new value
     *
     * @param timeJokers - the new value for time Joker
     */
    public void setTimeJokers(int timeJokers) {
        this.timeJokers = timeJokers;
    }

    /**
     * Returns the list of players in the game session
     *
     * @return list of players belonging to the game session
     */
    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != GameSession.class) return false;
        GameSession other = (GameSession) obj;
        if (((this.currentQuestion == null) != (other.currentQuestion == null)) ||
                ((this.expectedAnswers == null) != (other.expectedAnswers == null))) {
            return false;
        }
        return this.id == other.id && this.players.equals(other.players) &&
                this.removedPlayers.equals(other.removedPlayers) &&
                (this.currentQuestion == null || this.currentQuestion.equals(other.currentQuestion)) &&
                (this.expectedAnswers == null || this.expectedAnswers.equals(other.expectedAnswers)) &&
                this.playersReady.get() == other.playersReady.get() &&
                this.questionCounter == other.questionCounter &&
                this.difficultyFactor == other.difficultyFactor &&
                this.timeJokers == other.timeJokers &&
                this.sessionType.equals(other.sessionType) &&
                this.sessionStatus.equals(other.sessionStatus);
    }

    /**
     * Setter for disableLeaderboard field. Disables leaderboards for the given session
     */
    public void disableLeaderboard() {
        this.isLeaderboardDisabled = true;
    }

    /**
     * Setter for disableLeaderboard field. Enables leaderboards for the given session
     */
    public void enableLeaderboard() {
        this.isLeaderboardDisabled = false;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }

    /**
     * the method to add a used joker to the session
     *
     * @param joker the joker used by a player in this game session
     */
    public void addUsedJoker(Joker joker) {
        usedJokers.add(joker);
        Optional<Player> player = players.stream().filter(p -> p.username.equals(joker.username())).findFirst();
        if (player.isEmpty()) return;
        Player p = player.get();
        p.jokerStates.put(joker.jokerName(), Joker.JokerStatus.USED_HOT);
    }
}
