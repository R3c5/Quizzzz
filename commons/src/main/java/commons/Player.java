package commons;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import javax.persistence.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    public long id;

    /*
    @ManyToOne
    @JoinColumn(name = "session_id")
    private GameSession session;
    */

    public String username;

    @OneToOne(cascade = CascadeType.ALL)
    public String answer;

    @SuppressWarnings("unused")
    private Player() {
        // for object mapper
    }

    public Player(String username) {
        this.username = username;
        this.answer = null;
    }

    public void setAnswer(Answer ans) {
        this.answer = ans.toString();
    }

    public Answer parsedAnswer() {
        String[] splitAnswer = this.answer.split("=");
        String answer = splitAnswer[1].substring(0,splitAnswer[1].indexOf("]"));
        List<Integer> answers = new ArrayList<>(
                Arrays.stream(answer.split(","))
                        .map(Integer::valueOf)
                        .collect(Collectors.toList()));
        Question.QuestionType type = null;
        switch (splitAnswer[2].substring(0,splitAnswer[2].indexOf("]"))) {
            case "MULTIPLE_CHOICE" -> type = Question.QuestionType.MULTIPLE_CHOICE;
            default -> type = Question.QuestionType.UNKNOWN;
        }
        return new Answer(answers, type);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }
}
