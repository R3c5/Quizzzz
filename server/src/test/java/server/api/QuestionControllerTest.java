package server.api;

import commons.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import server.service.SessionManager;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class QuestionControllerTest {
    private QuestionController sut;
    private TestPlayerRepository repo;
    private SessionController sessionCtrl;
    private static ActivityController activityCtrl;
    private static TestActivityRepository activityRepo;

    @BeforeAll
    public static void setupAll() {
        activityRepo = new TestActivityRepository();
        activityRepo.save(new Activity("test","42","test","test"));
        activityRepo.save(new Activity("test2","43","test2","test2"));
        activityRepo.save(new Activity("test3","44","test3","test3"));
        activityRepo.save(new Activity("test4","45","test4","test4"));
        activityCtrl = new ActivityController(new Random(), activityRepo);
    }

    @BeforeEach
    public void setupEach() {
        repo = new TestPlayerRepository();
        sessionCtrl = new SessionController(new Random(), repo, "test", new SessionManager(),
                activityCtrl);
        ResponseEntity<GameSession> cur = sessionCtrl.addSession(
                new GameSession(GameSession.SessionType.MULTIPLAYER, List.of(new Player("test",0))));
        sut = new QuestionController(sessionCtrl);
    }

    @Test
    public void testGetQuestionNoSession() {
        ResponseEntity<Question> q = sut.getOneQuestion(42L);
        assertEquals(HttpStatus.BAD_REQUEST, q.getStatusCode());
    }

    @Test
    public void testGetQuestion() {
        GameSession s = sessionCtrl.getAllSessions().get(0);
        ResponseEntity<Question> resp = sut.getOneQuestion(s.id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        Question q = resp.getBody();
        Question serverQuestion = sessionCtrl.getAllSessions().get(0).currentQuestion;

        assertEquals(serverQuestion, q);
    }

    @Test
    public void submitAnswerNoSessionTest() {
        ResponseEntity<Evaluation> resp = sut.submitAnswer(42L,
                new Answer(Question.QuestionType.MULTIPLE_CHOICE));

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void submitAnswerTest() {
        GameSession s = sessionCtrl.getAllSessions().get(0);
        List<Integer> expectedAnswers = List.copyOf(s.expectedAnswers);
        Question q = s.currentQuestion;

        ResponseEntity<Evaluation> resp = sut.submitAnswer(s.id,
                new Answer(Question.QuestionType.MULTIPLE_CHOICE));

        Evaluation eval = resp.getBody();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(expectedAnswers, eval.correctAnswers);
        assertEquals(q.type, eval.type);
    }

    @Test
    public void testGetAnswers() {
        GameSession s = sessionCtrl.getAllSessions().get(0);
        ResponseEntity<List<Integer>> resp = sut.getCorrectAnswers(s.id);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<Integer> list = resp.getBody();
        List<Integer> answers = sessionCtrl.getAllSessions().get(0).expectedAnswers;
        assertEquals(answers, list);
    }
}