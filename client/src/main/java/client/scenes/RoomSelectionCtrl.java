package client.scenes;

import client.utils.GameSessionUtils;
import client.utils.LongPollingUtils;
import com.google.inject.Inject;
import commons.GameSession;
import commons.Player;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RoomSelectionCtrl extends SceneCtrl implements Initializable {

    private final GameSessionUtils gameSessionUtils;
    private LongPollingUtils longPollUtils;
    private SoundManager soundManager;
    private final MainCtrl mainCtrl;

    private long playerId;

    @FXML
    private TableView<GameSession> availableRooms;
    @FXML
    private TableColumn<GameSession, String> roomNumber;
    @FXML
    private TextField gameID;

    @Inject
    public RoomSelectionCtrl(GameSessionUtils gameSessionUtils, LongPollingUtils longPollUtils,
                              SoundManager soundManager, MainCtrl mainCtrl) {
        this.gameSessionUtils = gameSessionUtils;
        this.longPollUtils = longPollUtils;
        this.soundManager = soundManager;
        this.mainCtrl = mainCtrl;
    }

    @Override
    public void initialize(URL loc, ResourceBundle res) {
        roomNumber.setCellValueFactory(r -> new SimpleStringProperty("Room: " + r.getValue().id
                + " - Session Status: " + r.getValue().sessionStatus
                + " - Player(s) active: " + r.getValue().players.size()));
    }

    /**
     * Setter for playerId
     *
     * @param playerId - new Id for the player
     */
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        gameSessionUtils.removePlayer(MainCtrl.SELECTION_ID, playerId);
        longPollUtils.haltUpdates("selectionRoom");
    }

    /**
     * {@inheritDoc}
     */
    public void back() {
        soundManager.halt();
        soundManager.playSound("Button");
        shutdown();
        mainCtrl.showSplash();
    }

    /**
     * Switch method that maps keyboard key presses to functions.
     *
     * @param e KeyEvent to be switched
     */
    public void keyPressed(KeyEvent e) {
        switch (e.getCode()) {
            case ESCAPE -> back();
        }
    }

    /**
     * Initialize setup for main controller's showWaitingArea() method. Creates a new session.
     */
    public void hostRoom() {
        soundManager.playSound("Button");
        Player player = gameSessionUtils.removePlayer(MainCtrl.SELECTION_ID, playerId);
        GameSession session = new GameSession(GameSession.SessionType.WAITING_AREA);
        session.addPlayer(player);
        session = gameSessionUtils.addWaitingRoom(session);
        long waitingId = session.id;
        longPollUtils.haltUpdates("selectionRoom");
        mainCtrl.showWaitingArea(playerId, waitingId);
        soundManager.halt();
        soundManager.playSound("Waiting");
    }

    /**
     * Player is added to the selected session.
     */
    public void joinSelectedRoom() {
        soundManager.playSound("Button");
        if (availableRooms.getSelectionModel().getSelectedItem() == null) {
            soundManager.playSound("Alert");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No room selected");
            alert.setHeaderText("You have not selected a room");
            alert.setContentText("Choose a room to continue");
            mainCtrl.addCSS(alert);
            alert.show();
            return;
        }
        GameSession session = availableRooms.getSelectionModel().getSelectedItem();
        joinSession(session);
    }

    /**
     * Player is added to the session searched
     */
    public void joinSearchedRoom() {
        soundManager.playSound("Button");
        String sessionIdString = gameID.getText();
        if (!isSessionIdValid(sessionIdString)) {
            soundManager.playSound("Alert");
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid ID");
            alert.setHeaderText("You have entered an invalid game session ID");
            alert.setContentText("Please enter a valid session ID to continue");
            mainCtrl.addCSS(alert);
            alert.show();
            return;
        }
        long sessionId = Long.parseLong(gameID.getText());
        GameSession session = gameSessionUtils.getSession(sessionId);
        joinSession(session);
    }

    /**
     * Checks whether the specified session ID is valid or not. It is valid if it is present in the list of available
     * rooms
     *
     * @param sessionIdString the session Id string to be checked
     * @return true iff valid, false otherwise
     */
    public boolean isSessionIdValid(String sessionIdString) {
        long sessionId;
        if (sessionIdString.isBlank()) return false;
        try {
            sessionId = Long.parseLong(sessionIdString);
        } catch (NumberFormatException e) {
            return false;
        }
        if (availableRooms.getItems() == null) return false;
        for (GameSession gs : availableRooms.getItems()) {
            if (gs.id == sessionId) return true;
        }
        return false;
    }

    /**
     * Player is added to random waiting area
     * or creates a new waiting area if there are none available.
     */
    public void quickJoin() {
        soundManager.playSound("Button");
        var joinableRooms = availableRooms.getItems()
                .stream()
                .filter(room -> (room.sessionStatus == GameSession.SessionStatus.PLAY_AGAIN) ||
                        (room.sessionStatus == GameSession.SessionStatus.WAITING_AREA))
                .collect(Collectors.toList());
        if (joinableRooms.isEmpty()) {
            hostRoom();
            return;
        }
        int randomId = new Random().nextInt(joinableRooms.size());
        joinSession(joinableRooms.get(randomId));
    }

    /**
     * Finds the player ID of the player from the specified session using the username
     *
     * @param sessionId - session ID of the session
     * @param username  - username of the player whose ID is to be determined
     * @return player's ID
     */
    public long findPlayerIdByUsername(long sessionId, String username) {
        return gameSessionUtils
                .getPlayers(sessionId)
                .stream().filter(p -> p.username.equals(username))
                .findFirst().get().id;
    }

    /**
     * Adds the player to the specified game session
     *
     * @param session - session to join
     */
    private void addPlayerToSession(GameSession session) {
        Player player = gameSessionUtils.removePlayer(MainCtrl.SELECTION_ID, playerId);
        gameSessionUtils.addPlayer(session.id, player);
        if (playerId == 0L) playerId = findPlayerIdByUsername(session.id, player.username);
    }

    /**
     * Player is added to the specified session if the game session is of the status Play Again or Waiting Room.
     * If not, the user is not added, simply alerted.
     *
     * @param session - GameSession to which the player is added.
     */
    public void joinSession(GameSession session) {
        switch (session.sessionStatus) {
            case WAITING_AREA:
                addPlayerToSession(session);
                longPollUtils.haltUpdates("selectionRoom");
                soundManager.halt();
                soundManager.playSound("Waiting");
                mainCtrl.showWaitingArea(playerId, session.id);
                break;
            case PLAY_AGAIN:
                addPlayerToSession(session);
                longPollUtils.haltUpdates("selectionRoom");
                mainCtrl.showEndGameScreen(session.id, playerId);
                break;
            default:
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Ongoing game");
                alert.setHeaderText("The selected game session is still going on");
                alert.setContentText("You can wait for it to get over, or join a new game");
                mainCtrl.addCSS(alert);
                alert.show();
        }
    }

    /**
     * Refresh the list of available waiting rooms
     *
     * @return True iff the refresh should continue
     */
    public void refresh(Pair<String, GameSession> update) {
        if (update == null) {
            List<GameSession> availableSessions = gameSessionUtils.getAvailableSessions();
            if (availableSessions == null) availableSessions = new ArrayList<>();
            availableRooms.setItems(FXCollections.observableList(availableSessions));
            return;
        }
        String op = update.getKey();
        GameSession room = update.getValue();
        switch (op) {
            case "[add]" -> availableRooms.getItems().add(room);
            case "[remove]" -> {
                for (GameSession gs : availableRooms.getItems()) {
                    if (gs.id == room.id) {
                        availableRooms.getItems().remove(gs);
                        break;
                    }
                }
            }
            case "[update]" -> {
                for (GameSession gs : availableRooms.getItems()) {
                    if (gs.id == room.id) {
                        availableRooms.getItems().set(availableRooms.getItems().indexOf(gs), room);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Registers clients for session selection updates
     */
    public void registerForUpdates() {
        longPollUtils.registerForSelectionRoomUpdates(this::refresh);
    }
}

