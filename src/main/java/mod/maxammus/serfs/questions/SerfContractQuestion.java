package mod.maxammus.serfs.questions;

import com.wurmonline.server.Items;
import com.wurmonline.server.LoginHandler;
import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.intra.IntraServerConnection;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.utils.BMLBuilder;
import mod.maxammus.serfs.creatures.Serf;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import java.awt.*;
import java.util.Properties;
import java.util.logging.Logger;

import static com.wurmonline.server.utils.BMLBuilder.createGenericBuilder;

public class SerfContractQuestion implements ModQuestion {
    static final Logger logger = Logger.getLogger(SerfContractQuestion.class.getName());
    boolean male;

    public SerfContractQuestion(boolean male) {
        this.male = male;
    }

    public static void create(Creature player, long target, boolean male) {
        ModQuestions.createQuestion(player, "Call serf", "", target, new SerfContractQuestion(male)).sendQuestion();
    }

    @Override
    public void sendQuestion(Question question) {
        BMLBuilder bmlBuilder = createGenericBuilder();

        bmlBuilder
                .addLabel("Name:", null, BMLBuilder.TextType.BOLD, Color.white)
                .addInput("name", null, true, null, 0,0, null, null, null, 70, 16)
                .addRadioButton("male", "sex", "Male", male)
                .addRadioButton("female", "sex", "Female", !male);
        bmlBuilder.addLabel("");
        String content = ModQuestions.getBmlHeader(question) + bmlBuilder +
                //Close the header without a button that all the ending methods give
            ModQuestions.createAnswerButton2(question);
        int height = 256;
        question.getResponder().getCommunicator().sendBml(300, height, true, true, content, 200, 200, 200, question.getTitle());
    }

    @Override
    public void answer(Question question, Properties answers) {
        Creature responder = question.getResponder();
        String name = answers.getProperty("name");
        male = answers.getProperty("sex").equals("male");
        if (LoginHandler.containsIllegalCharacters(name) || name.length() > 35) {
            responder.getCommunicator().sendNormalServerMessage("Invalid name.");
            create(responder, question.getTarget(), male);
            return;
        }
        name = "Serf " + name;
        if (Players.getInstance().doesPlayerNameExist(name)) {
            responder.getCommunicator().sendNormalServerMessage("Serf with that name already exists.");
            create(responder, question.getTarget(), male);
            return;
        }
        Serf serf = Serf.createSerf(name, responder.getWurmId());
        if(serf == null) {
            logger.warning("Failed to log in " + name);
            return;
        }
        ((Player)(Creature)serf).setBlood(IntraServerConnection.calculateBloodFromKingdom(responder.getKingdomId()));
        //        serf.setName("Serf " + LoginHandler.raiseFirstLetter(serf.getName().substring(5)));
        serf.setSex((byte) (male ? 0 : 1), false);
        serf.calledBy(responder);
        Items.destroyItem(question.getTarget());
    }
}