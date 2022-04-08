package client.scenes;

import client.utils.GameSessionUtils;
import commons.GameSession;
import commons.Player;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GamemodeCtrl extends SceneCtrl implements Initializable {

    private final GameSessionUtils gameSessionUtils;
    private final MainCtrl mainCtrl;
    private final SoundManager soundManager;
    private final GameAnimation gameAnimation;
    @FXML
    protected Button defaultButton;
    @FXML
    protected Button survivalButton;
    @FXML
    protected Button timeAttackButton;
    @FXML
    protected Label alertText;
    @FXML
    protected Slider questionSlider;
    @FXML
    protected Label questionText;
    @FXML
    protected Slider survivalSlider;
    @FXML
    protected Label survivalText;
    @FXML
    protected Slider timeAttackSlider;
    @FXML
    protected Label timeAttackText;

    private long playerId;

    @Inject
    public GamemodeCtrl(GameSessionUtils gameSessionUtils, GameAnimation gameAnimation,
                         SoundManager soundManager, MainCtrl mainCtrl) {
        this.gameSessionUtils = gameSessionUtils;
        this.mainCtrl = mainCtrl;
        this.soundManager = soundManager;
        this.gameAnimation = gameAnimation;
    }

    /**
     * Enable the animation for the gameButtons
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gameAnimation.startBatteryAnimation(List.of(defaultButton, survivalButton, timeAttackButton));
        alertText.setText("Note: Results from games with changed default settings are not reflected on leaderboards!");
        questionSlider.valueProperty().addListener((a, b, newVal) -> {
            questionSlider.setValue(newVal.intValue());
            questionText.setText("Questions: " + newVal.intValue());
            updateAlertText();
        });

        survivalSlider.valueProperty().addListener((a, b, newVal) -> {
            survivalSlider.setValue(newVal.intValue());
            survivalText.setText("Lives: " + newVal.intValue());
            updateAlertText();
        });

        timeAttackSlider.valueProperty().addListener((a, b, newVal) -> {
            timeAttackSlider.setValue(newVal.intValue());
            timeAttackText.setText("Timer: " + newVal.intValue() + " seconds");
            updateAlertText();
        });
    }

    /**
     * Updates alert text opacity based on slider values
     */
    public void updateAlertText() {
        boolean isDefault = timeAttackSlider.getValue() == 60 && survivalSlider.getValue() == 3
                && questionSlider.getValue() == 20;

        if (alertText.getOpacity() == 1 && isDefault) {
            alertText.setOpacity(0);
        } else if (alertText.getOpacity() == 0 && !isDefault) {
            alertText.setOpacity(1);
        }
    }

    /**
     * Setter for playerId
     *
     * @param playerId - Id for the player
     */
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
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
     * Removes player from session. Also called if controller is closed forcibly
     */
    public void shutdown() {
        gameSessionUtils.removePlayer(MainCtrl.SELECTION_ID, playerId);
    }

    /**
     * Reverts the player to the splash screen and remove him from the current game session.
     */
    public void back() {
        soundManager.halt();
        soundManager.playSound("Button");
        shutdown();
        mainCtrl.showSplash();
    }

    /**
     * Creates a session for the mode that is called and adds the player to it.
     *
     * @param type The type of game the player wants to play.
     * @return The sessionId of the new session.
     */
    public long createId(GameSession.SessionType type) {
        Player player = gameSessionUtils.removePlayer(MainCtrl.SELECTION_ID, playerId);
        GameSession newSession = new GameSession(type);
        newSession = gameSessionUtils.addSession(newSession);
        gameSessionUtils.addPlayer(newSession.id, player);

        return newSession.id;
    }

    /**
     * Starts the default singleplayer game.
     */
    public void showDefault() {
        soundManager.playSound("Button");
        long sessionId = createId(GameSession.SessionType.SINGLEPLAYER);
        int questions = (int) questionSlider.getValue();
        gameSessionUtils.setGameRounds(sessionId, questions);
        if (questions != 20) gameSessionUtils.disableLeaderboard(sessionId);
        mainCtrl.showDefaultSinglePlayer(sessionId, playerId, questions);
    }

    /**
     * Starts the survival singleplayer game.
     */
    public void showSurvival() {
        soundManager.playSound("Button");
        long sessionId = createId(GameSession.SessionType.SURVIVAL);
        gameSessionUtils.setGameRounds(sessionId, Integer.MAX_VALUE);
        mainCtrl.showSurvival(sessionId, playerId, survivalSlider.getValue());
    }

    /**
     * Starts the time attack singleplayer game.
     */
    public void showTimeAttack() {
        soundManager.playSound("Button");
        long sessionId = createId(GameSession.SessionType.TIME_ATTACK);
        gameSessionUtils.setGameRounds(sessionId, Integer.MAX_VALUE);
        mainCtrl.showTimeAttack(sessionId, playerId, timeAttackSlider.getValue());
    }

}
