package client.scenes;

import client.utils.*;
import commons.*;
import jakarta.ws.rs.BadRequestException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class GameCtrl extends SceneCtrl implements Initializable {

    protected final static int GAME_ROUND_TIME = 9;
    protected final static int PODIUM_TIME = 10;
    protected final static int MIDGAME_BREAK_TIME = 6;
    protected final static int TIMER_UPDATE_INTERVAL_MS = 50;
    protected final static int GAME_ROUND_DELAY = 2;
    protected final static int MULTIPLAYER_ROUNDS = 20;
    protected final static int IN_GAME_LEADERBOARD_WIDTH = 193;
    protected final static int IN_GAME_COLUSERNAME_WIDTH = 92;
    protected final static int MID_GAME_LEADERBOARD_WIDTH = 649;
    protected final static int MID_GAME_COLUSERNAME_WIDTH = 548;

    @FXML
    protected StackPane answerArea;

    @FXML
    protected Label questionPrompt;

    @FXML
    protected ImageView imagePanel;

    @FXML
    protected Label pointsLabel;

    @FXML
    protected Label questionCount;

    @FXML
    protected Label countdown;

    @FXML
    protected ProgressBar timeProgress;

    @FXML
    protected Button submitButton;

    @FXML
    protected Button removeOneButton;

    @FXML
    protected Button decreaseTimeButton;

    @FXML
    protected Button doublePointsButton;

    @FXML
    protected TableView<Player> leaderboard;

    @FXML
    protected TableColumn<Player, Integer> colRank;

    @FXML
    protected TableColumn<Player, String> colUserName;

    @FXML
    protected TableColumn<Player, Integer> colPoints;

    @FXML
    protected Label jokerRefreshLabel;

    @FXML
    protected Button weirdButton;

    protected WebSocketsUtils webSocketsUtils;
    protected GameSessionUtils gameSessionUtils;
    protected LeaderboardUtils leaderboardUtils;
    protected QuestionUtils questionUtils;
    protected GameAnimation gameAnimation;
    protected SoundManager soundManager;

    protected MainCtrl mainCtrl;

    protected List<RadioButton> multiChoiceAnswers;
    protected TextField estimationAnswer;
    protected long sessionId;
    protected long playerId;
    protected Question currentQuestion;
    protected int points = 0;
    protected int rounds = 0;
    protected int gameRounds;
    protected Thread timerThread;
    protected Evaluation evaluation;

    protected boolean doublePointsJoker;
    protected boolean doublePointsActive;
    protected boolean decreaseTimeJoker;
    protected boolean removeOneJoker;
    protected boolean inScene;

    public GameCtrl(WebSocketsUtils webSocketsUtils, GameSessionUtils gameSessionUtils,
                    LeaderboardUtils leaderboardUtils, QuestionUtils questionUtils,
                    GameAnimation gameAnimation, SoundManager soundManager, MainCtrl mainCtrl) {
        this.webSocketsUtils = webSocketsUtils;
        this.gameSessionUtils = gameSessionUtils;
        this.leaderboardUtils = leaderboardUtils;
        this.questionUtils = questionUtils;
        this.gameAnimation = gameAnimation;
        this.soundManager = soundManager;

        this.mainCtrl = mainCtrl;
        this.multiChoiceAnswers = new ArrayList<RadioButton>();

        doublePointsJoker = true;
        doublePointsActive = false;
        decreaseTimeJoker = true;
        removeOneJoker = true;
        inScene = true;

        // Set to defaults
        this.sessionId = 0L;
        this.playerId = 0L;
    }

    /**
     * Setter for inScene
     */
    public void setInScene() {
        this.inScene = true;
    }

    /**
     * Setter for sessionId.
     *
     * @param sessionId the id of the sessions
     */
    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Setter for playerId.
     *
     * @param playerId the id of the player
     */
    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    /**
     * Load general question information
     *
     * @param q the question to be rendered
     */
    protected void renderGeneralInformation(Question q) {
        weirdButton.setText(soundManager.soundProfile.toString());
        this.questionPrompt.setText(q.prompt);
        this.questionPrompt.setStyle("-fx-text-alignment: left");
        if (q.type != Question.QuestionType.RANGE_GUESS && q.type != Question.QuestionType.EQUIVALENCE &&
                q.type != Question.QuestionType.MULTIPLE_CHOICE) {
            return;
        }

        try {
            ImageUtils image = new ImageUtils(questionUtils, q.imagePath);
            image.setOnSucceeded(ev -> {
                imagePanel.setImage(image.getValue());
            });
            Thread t = new Thread(image);
            t.start();
        } catch (Exception ignore) {
        }
    }

    /**
     * Displays the count of the current question
     */
    public void renderQuestionCount() {
        questionCount.setText(String.format("Question: %d", rounds + 1));
    }

    /**
     * Switch method to render answer options for a given question
     *
     * @param q Question from which to take the possible answers
     */
    protected void renderAnswerFields(Question q) {
        switch (q.type) {
            case MULTIPLE_CHOICE:
            case COMPARISON:
            case EQUIVALENCE:
                renderMultipleChoiceQuestion(q);
                disableButton(removeOneButton, !removeOneJoker);
                break;
            case RANGE_GUESS:
                renderEstimationQuestion();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported question type when rendering answers");
        }
    }


    private void renderEstimationQuestion() {
        this.countdown.setText("");
        this.estimationAnswer = new TextField();
        answerArea.getChildren().clear();
        answerArea.getChildren().add(estimationAnswer);
    }

    /**
     * Render question of the multiple choice question variant
     *
     * @param q the question to be rendered
     */
    protected void renderMultipleChoiceQuestion(Question q) {
        double yPosition = 0.0;
        this.countdown.setText("Options:");
        multiChoiceAnswers.clear();
        answerArea.getChildren().clear();
        for (String opt : q.answerOptions) {
            RadioButton choice = new RadioButton(opt);
            choice.setWrapText(true);
            choice.setTranslateY(yPosition);
            yPosition += 50;
            multiChoiceAnswers.add(choice);
            answerArea.getChildren().add(choice);
        }
        answerArea.setStyle("-fx-text-alignment: left");
    }

    /**
     * Change the image displayed upon hovering the activity answer options
     */
    public void imageHover() {
        Question q = this.currentQuestion;
        if (q.type != Question.QuestionType.COMPARISON && q.type != Question.QuestionType.EQUIVALENCE) return;
        try {
            AtomicReference<Image> defaultImage = new AtomicReference<Image>(null);
            ImageUtils imageConvert = new ImageUtils(questionUtils, q.imagePath);
            imageConvert.setOnSucceeded(ev -> {
                defaultImage.set(imageConvert.getValue());
                for (int i = 0; i < multiChoiceAnswers.size(); i++) {
                    RadioButton rb = multiChoiceAnswers.get(i);
                    ImageUtils image = new ImageUtils(questionUtils, q.activityPath.get(i));
                    image.setOnSucceeded(event -> {
                        rb.setOnMouseEntered(e -> imagePanel.setImage(image.getValue()));
                    });
                    Thread b = new Thread(image);
                    b.start();

                    if (q.type == Question.QuestionType.EQUIVALENCE) {
                        rb.setOnMouseExited(e -> imagePanel.setImage(defaultImage.get()));
                    }
                }
            });
            Thread t = new Thread(imageConvert);
            t.start();
        } catch (IllegalArgumentException ignore) {
        }
    }

    /**
     * Switch method to render correct answers based on the question type
     */
    protected void renderCorrectAnswer() {
        switch (this.evaluation.type) {
            case MULTIPLE_CHOICE:
            case COMPARISON:
            case EQUIVALENCE:
                renderMultipleChoiceAnswers(this.evaluation.correctAnswers);
                break;
            case RANGE_GUESS:
                renderEstimationAnswers(this.evaluation.correctAnswers);
                break;
            default:
                throw new UnsupportedOperationException("Currently only multiple choice answers can be rendered");
        }
    }

    private void renderEstimationAnswers(List<Long> correctAnswers) {
        long givenAnswer = 0L;
        long actualAnswer = correctAnswers.get(0);
        try {
            givenAnswer = Long.parseLong(estimationAnswer.getText());
        } catch (NumberFormatException ex) {
            givenAnswer = actualAnswer;
        }

        long diff = givenAnswer - actualAnswer;
        String correctAnswer = "Correct Answer: " + actualAnswer;

        if (diff > 0) {
            correctAnswer += " (+" + diff + ")";
        } else if (diff < 0) {
            correctAnswer += " (" + diff + ")";
        }

        Label resultText = new Label(correctAnswer);
        answerArea.getChildren().clear();
        answerArea.getChildren().add(resultText);
    }

    /**
     * Render answers of the multiple choice question variant
     *
     * @param correctIndices Indexes of the correct answer(s)
     */
    protected void renderMultipleChoiceAnswers(List<Long> correctIndices) {
        for (int i = 0; i < multiChoiceAnswers.size(); ++i) {
            if (correctIndices.contains((long) i)) {
                multiChoiceAnswers.get(i).setStyle("-fx-background-color: green");
            } else {
                multiChoiceAnswers.get(i).setDisable(true);
            }
        }
    }

    /**
     * Starts reading time countdown and updates label accordingly to inform the user.
     */
    public void countdown() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            int counter = 5;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (!inScene) cancel();
                    else if (counter < 0){
                        cancel();
                        loadAnswer();
                    } else {
                        countdown.setText("The answer option will appear in " + counter + " sec");
                        counter--;
                    }
                });
            }
        }, 0, 1000);
    }

    /**
     * Loads a question and starts reading time.
     */
    public void loadQuestion() {
        disableButton(removeOneButton, true);
        disableButton(doublePointsButton, true);
        disableButton(decreaseTimeButton, true);
        disableButton(submitButton, true);
        this.answerArea.getChildren().clear();

        try {
            Question q = this.questionUtils.fetchOneQuestion(this.sessionId);
            this.currentQuestion = q;
            renderGeneralInformation(q);
            renderQuestionCount();
            countdown();
        } catch (BadRequestException ignore) { /* happens when session is removed before question is loaded */ }
    }

    /**
     * Loads the answers of the current question and updates the timer after reading time is over
     */
    public void loadAnswer() {
        soundManager.playSound("InGame" + new Random().nextInt(1,3));
        Question q = this.currentQuestion;
        if (q == null) return;
        renderAnswerFields(q);

        disableButton(removeOneButton, q.type == Question.QuestionType.RANGE_GUESS || !removeOneJoker);
        disableButton(submitButton, false);

        TimeUtils roundTimer = new TimeUtils(GAME_ROUND_TIME, TIMER_UPDATE_INTERVAL_MS);
        roundTimer.setTimeBooster(this::getTimeJokers);
        roundTimer.setOnSucceeded((event) -> Platform.runLater(() -> {
            System.out.println("roundTimer is done");
            submitAnswer(true);
        }));

        timeProgress.progressProperty().bind(roundTimer.progressProperty());
        this.timerThread = new Thread(roundTimer);
        this.timerThread.start();
        imageHover();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        if (this.timerThread != null && this.timerThread.isAlive()) this.timerThread.interrupt();
        if (sessionId != 0) {
            try {
                gameSessionUtils.removePlayer(sessionId, playerId);
            } catch (BadRequestException ignore) { /* session might be removed at this point */ }
            setPlayerId(0);
        }
        setSessionId(0);
    }

    /**
     * {@inheritDoc}
     */
    public void back() {
        soundManager.halt();
        soundManager.playSound("Button");
        inScene = false;
        shutdown();
        reset();
        mainCtrl.showSplash();
    }

    /**
     * Resets all fields and the screen for a new game.
     */
    public void reset() {
        removeMidGameLeaderboard();
        this.questionPrompt.setText("[Question]");
        this.answerArea.getChildren().clear();
        this.pointsLabel.setText("Points: 0");
        this.multiChoiceAnswers.clear();
        this.points = 0;
        this.rounds = 0;
        this.currentQuestion = null;
        this.questionCount.setText("Question: 1");
        this.imagePanel.setImage(null);

        //re-enable jokers
        doublePointsJoker = true;
        doublePointsActive = false;
        decreaseTimeJoker = true;
        removeOneJoker = true;

        disableButton(submitButton, true);
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
     * Updates the point counter in client side, and then updates database entry
     */
    public void updatePoints() {
        if (doublePointsActive) {
            evaluation.points *= 2;
            switchStatusOfDoublePoints();
        }
        points += evaluation.points;
        renderPoints();
    }

    /**
     * Called when player points are rendered or updated
     */
    public void renderPoints() {
        pointsLabel.setText(String.format("Points: %d", this.points));
    }

    /**
     * Submit button click event handler
     */
    public void submitAnswerButton() {
        submitAnswer(false);
    }

    /**
     * Submit an answer to the server
     */
    public void submitAnswer(boolean initiatedByTimer) {
        Answer ans = new Answer(currentQuestion.type);
        ans.timeFactor = timeProgress.getProgress();

        switch (currentQuestion.type) {
            case MULTIPLE_CHOICE:
            case COMPARISON:
            case EQUIVALENCE:
                for (int i = 0; i < multiChoiceAnswers.size(); ++i) {
                    if (multiChoiceAnswers.get(i).isSelected()) {
                        ans.addAnswer(i);
                    }
                }
                break;
            case RANGE_GUESS:
                try {
                    ans.addAnswer(Long.parseLong(estimationAnswer.getText()));
                } catch (NumberFormatException ex) {
                    System.out.println("Invalid answer yo");
                    if (!initiatedByTimer) {
                        soundManager.playSound("Alert");
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid answer");
                        alert.setHeaderText("Invalid answer");
                        alert.setContentText("You should only enter an integer number");
                        mainCtrl.addCSS(alert);
                        alert.show();

                        return;
                    } else {
                        ans.addAnswer(0);
                    }
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported question type when parsing answer");
        }

        if (!initiatedByTimer) soundManager.halt();
        else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    soundManager.halt();
                }
            }, 3000);
        }

        if (this.timerThread != null && this.timerThread.isAlive()) this.timerThread.interrupt();
        disableButton(submitButton, true);
        disableButton(removeOneButton, true);

        this.evaluation = questionUtils.submitAnswer(sessionId, playerId, ans);

        gameSessionUtils.toggleReady(sessionId, true);
    }

    /**
     * the method to handle the podium part of a game
     */
    public abstract void handleGamePodium();

    /**
     * Proceeds the user onto the next round of the game
     */
    protected void handleNextRound() {
        try {
            gameSessionUtils.toggleReady(sessionId, false);
            imagePanel.setImage(null);
            fetchJokerStates();
            loadQuestion();
        } catch (BadRequestException e) {
            System.out.println("takingover");
        }
    }

    /**
     * Gets the user's answer, starts the evaluation and loads a new question or ends the game.
     */
    public void startEvaluation() {

        if (this.evaluation == null) return;

        disableButton(removeOneButton, true);
        disableButton(decreaseTimeButton, true);
        disableButton(doublePointsButton, true);

        updatePoints();
        renderCorrectAnswer();

        this.evaluation = null;

        // TODO disable button while waiting
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (currentQuestion == null) return; // happens if shutdown is called before triggering
                    rounds++;
                    if (rounds == gameRounds) {
                        handleGamePodium();
                    } else if (rounds == gameRounds / 2 &&
                            gameSessionUtils.getSession(sessionId).sessionType == GameSession.SessionType.MULTIPLAYER) {
                        displayMidGameScreen();
                        countdown.setOpacity(0);
                    } else {
                        handleNextRound();
                    }
                });
            }
        }, GAME_ROUND_DELAY * 1000);
    }

    /**
     * Updates the items in the leaderboard and makes sure the leaderboard remains visible
     */
    public void renderLeaderboard() {
        var players = gameSessionUtils.getPlayers(sessionId);
        var data = FXCollections.observableList(players);
        leaderboard.setItems(data);
        leaderboard.setOpacity(1);
    }

    /**
     * Displays the current session's leaderboard and hides the question screen attributes
     */
    public void displayLeaderboard() {
        renderLeaderboard();
        leaderboard.setPrefWidth(MID_GAME_LEADERBOARD_WIDTH);
        colUserName.setPrefWidth(MID_GAME_COLUSERNAME_WIDTH);
        answerArea.setOpacity(0);
        submitButton.setOpacity(0);
        removeOneButton.setOpacity(0);
        doublePointsButton.setOpacity(0);
        decreaseTimeButton.setOpacity(0);
        questionPrompt.setOpacity(0);
        multiChoiceAnswers.clear();
        answerArea.getChildren().clear();
        imagePanel.setImage(null);
        leaderboard.setOpacity(1);
    }

    /**
     * Displays the question screen attributes.
     */
    public void removeMidGameLeaderboard() {
        answerArea.setOpacity(1);
        questionPrompt.setOpacity(1);
        submitButton.setOpacity(1);
        removeOneButton.setOpacity(1);
        doublePointsButton.setOpacity(1);
        decreaseTimeButton.setOpacity(1);
    }

    /**
     * Display mid-game leaderboard
     */
    public void displayMidGameScreen() {
        displayLeaderboard();

        TimeUtils roundTimer = new TimeUtils(MIDGAME_BREAK_TIME, TIMER_UPDATE_INTERVAL_MS);
        roundTimer.setOnSucceeded((event) -> Platform.runLater(() -> {
            removeMidGameLeaderboard();
            fetchJokerStates();
            loadQuestion();
        }));

        timeProgress.progressProperty().bind(roundTimer.progressProperty());
        this.timerThread = new Thread(roundTimer);
        this.timerThread.start();

        gameSessionUtils.toggleReady(sessionId, false);
    }

    /**
     * Disable button so the player can not interact with it
     *
     * @param button  - Button to be disabled
     * @param disable - boolean value whether the button should be disabled or enabled
     */
    public void disableButton(Button button, boolean disable) {
        if (button == null) return;
        button.setOpacity(disable ? 0.5 : 1);
        button.setDisable(disable);
    }

    /**
     * Toggles sound profile for all scenes
     */
    public void toggleWeirdSound() {
        soundManager.toggleProfile();
        weirdButton.setText(soundManager.soundProfile.toString());
    }

    /**
     * Remove One Answer Joker
     * When this joker is used it removes one incorrect answer from the answers list for the player that used it
     */
    public void removeOneAnswer() {
        soundManager.playSound("Joker");
        removeOneJoker = false;
        disableButton(removeOneButton, true);

        switch (currentQuestion.type) {
            case COMPARISON:
            case EQUIVALENCE:
            case MULTIPLE_CHOICE:
                List<Integer> incorrectAnswers = new ArrayList<>();
                List<Integer> correctAnswers = questionUtils.getCorrectAnswers(sessionId);
                for (int i = 0; i < multiChoiceAnswers.size(); ++i) {
                    if (!correctAnswers.contains(i)) {
                        incorrectAnswers.add(i);
                    }
                }
                int randomIndex = new Random().nextInt(incorrectAnswers.size());
                RadioButton button = multiChoiceAnswers.get(incorrectAnswers.get(randomIndex));
                if (button.isSelected()) {
                    button.setSelected(false);
                }
                button.setDisable(true);
                break;
            default:
                disableButton(removeOneButton, false);
        }
        String username = leaderboardUtils.getPlayerByIdInLeaderboard(playerId).getUsername();
        Joker joker = new Joker(
                username,
                "RemoveOneAnswerJoker"
        );
        gameSessionUtils.addUsedJoker(sessionId, joker);
    }


    /**
     * Get number of time Jokers for the current session
     *
     * @return int representing number of time jokers
     */
    public double getTimeJokers() {
        return gameSessionUtils.getSession(sessionId).getTimeJokers();
    }

    /**
     * Setter for the gameRounds
     */
    public void setGameRounds(int rounds) {
        gameRounds = rounds;
    }

    /**
     * Decrease Time Joker
     * When this joker is used, the timer speeds up
     * This joker becomes Increase Time Joker in Singleplayer
     */
    public void decreaseTime() {
        soundManager.playSound("Joker");
        decreaseTimeJoker = false;
        disableButton(decreaseTimeButton, true);
        gameSessionUtils.updateTimeJokers(sessionId, (int) getTimeJokers() + 1);
        String username = leaderboardUtils.getPlayerByIdInLeaderboard(playerId).getUsername();
        gameSessionUtils.addUsedJoker(sessionId, new Joker(username, "DecreaseTimeJoker"));
    }

    /**
     * Double Points Joker
     * When this joker is used, it doubles the points gained for the question when it was used.
     */
    public void doublePoints() {
        soundManager.playSound("Joker");
        doublePointsJoker = false;
        disableButton(doublePointsButton, true);
        switchStatusOfDoublePoints();
        String username = leaderboardUtils.getPlayerByIdInLeaderboard(playerId).getUsername();
        gameSessionUtils.addUsedJoker(sessionId, new Joker(username, "DoublePointsJoker"));
    }

    /**
     * Switch the doublePointsJoker status from true to false and from false to true
     */
    private void switchStatusOfDoublePoints() {
        doublePointsActive = !doublePointsActive;
    }

    /**
     * Generic event handler for clicking on an emoji
     *
     * @param ev The event information
     */
    public void emojiEventHandler(Event ev) {
        Node source = (Node) ev.getSource();
        String nodeId = source.getId();
        Emoji.EmojiType type;
        switch (nodeId) {
            case "emojiFunny":
                type = Emoji.EmojiType.FUNNY;
                break;
            case "emojiSad":
                type = Emoji.EmojiType.SAD;
                break;
            case "emojiAngry":
                type = Emoji.EmojiType.ANGRY;
                break;
            default:
                return;
        }
        webSocketsUtils.sendEmoji(sessionId, playerId, type);
    }

    protected String getJokerDisplayName(String jokerName) {
        String val;
        switch (jokerName) {
            case "DoublePointsJoker" -> val = "Double points";
            case "DecreaseTimeJoker" -> val = "Decrease time";
            case "RemoveOneAnswerJoker" -> val = "Remove one answer";
            default -> val = "Unknown";
        }
        return val;
    }

    /**
     * Fetch joker state from the server and update local state accordingly
     */
    public void fetchJokerStates() {
        // TODO: maybe display animation once joker is refilled.
        Map<String, Joker.JokerStatus> states = gameSessionUtils.getJokerStates(sessionId, playerId);
        StringBuilder refreshText = new StringBuilder();
        int refreshedJokers = 0;
        for (var joker : states.entrySet()) {
            switch (joker.getKey()) {
                case "DoublePointsJoker" -> {
                    if (doublePointsJoker != (joker.getValue() == Joker.JokerStatus.AVAILABLE)) {
                        ++refreshedJokers;
                        doublePointsJoker = true;
                        refreshText.append(getJokerDisplayName(joker.getKey())).append(", ");
                    }
                }
                case "DecreaseTimeJoker" -> {
                    if (decreaseTimeJoker != (joker.getValue() == Joker.JokerStatus.AVAILABLE)) {
                        ++refreshedJokers;
                        decreaseTimeJoker = true;
                        refreshText.append(getJokerDisplayName(joker.getKey())).append(", ");
                    }
                }
                case "RemoveOneAnswerJoker" -> {
                    if (removeOneJoker != (joker.getValue() == Joker.JokerStatus.AVAILABLE)) {
                        ++refreshedJokers;
                        removeOneJoker = true;
                        refreshText.append(getJokerDisplayName(joker.getKey())).append(", ");
                    }
                }
            }
        }
        if (refreshText.isEmpty()) return;

        String finalText = String.format("Lucky you, %s joker%s refreshed",
                refreshText.substring(0, refreshText.length() - 2),
                (refreshedJokers != 1) ? "s were" : " was");

        jokerRefreshLabel.setText(finalText);
        new Timeline(new KeyFrame(Duration.seconds(5), e -> jokerRefreshLabel.setText(""))).play();
    }
}
