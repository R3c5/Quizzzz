package client.utils;

import commons.Answer;
import commons.Evaluation;
import commons.Question;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import org.glassfish.jersey.client.ClientConfig;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class QuestionUtils {

    public static String serverConnection = "http://localhost:8080/";

    /**
     * Fetches a question from the server database
     *
     * @param sessionId Session to check
     * @return Question object related to the session with the provided id
     */
    public Question fetchOneQuestion(long sessionId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(serverConnection).path("api/questions/" + sessionId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<Question>() {
                });
    }

    /**
     * Submits an answer to the server database
     *
     * @param sessionId Session Id to send the answer to
     * @param answer    Answer object to be sent
     * @return Evaluation object to check the provided answers
     */
    public Evaluation submitAnswer(long sessionId, long playerId, Answer answer) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(serverConnection).path("api/questions/" + sessionId + "/" + playerId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .post(Entity.entity(answer, APPLICATION_JSON), Evaluation.class);
    }

    /**
     * Gets the list of positions of correct answers for a question from the server
     *
     * @param sessionId - long representing the current session
     * @return a list of integer corresponding to the positions of correct answers for a question
     */
    public List<Integer> getCorrectAnswers(long sessionId) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(serverConnection).path("api/questions/answers/" + sessionId)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<List<Integer>>() {
                });
    }

    /**
     * Fetches the image corresponding to the file path.
     *
     * @param path The image path.
     * @return The buffferedImage.
     */
    public byte[] fetchImage(String path) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(serverConnection).path("api/questions/image/" + path)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get(new GenericType<byte[]>() {
                });
    }
}
