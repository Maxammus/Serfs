package mod.maxammus.serfs.questions;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Question;
import mod.maxammus.serfs.creatures.Serf;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import java.util.Properties;
import java.util.logging.Logger;


public class SerfQuestionQuestion implements ModQuestion {
    Logger logger = Logger.getLogger(this.getClass().getName());
    int width;
    int height;
    float xLoc;
    float yLoc;
    boolean resizeable;
    String content;
    int r;
    int g;
    int b;

    public SerfQuestionQuestion( int width, int height, float xLoc, float yLoc, boolean resizeable, String content, int r, int g, int b) {
        this.width = width;
        this.height = height;
        this.xLoc = xLoc;
        this.yLoc = yLoc;
        this.resizeable = resizeable;
        this.content = content;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static void create(Player player, long target, int width, int height, float xLoc, float yLoc, boolean resizeable, String content, int r, int g, int b) {
        Creature serf = Creatures.getInstance().getCreatureOrNull(target);
        ModQuestions.createQuestion(player, "Question from: " + serf.getName(), "", target, new SerfQuestionQuestion(width, height, xLoc, yLoc, resizeable, content, r, g, b)).sendQuestion();

    }
    @Override
    public void sendQuestion(Question question) {
        question.getResponder().getCommunicator().sendBml(width, height, xLoc, yLoc, resizeable, true, content, r, g, b, question.getTitle());
    }

    @Override
    public void answer(Question question, Properties answers) {
        Serf serf = (Serf)Creatures.getInstance().getCreatureOrNull(question.getTarget());
        if(serf != null)
            try {
                serf.handleOwnerQuestionResponse(answers);
            } catch (ClassCastException e) {
                question.getResponder().getCommunicator().sendNormalServerMessage("Question does not currently work with serfs.");
            }
    }
}
