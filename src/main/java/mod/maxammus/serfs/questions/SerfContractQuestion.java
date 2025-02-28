package mod.maxammus.serfs.questions;

import com.wurmonline.server.Items;
import com.wurmonline.server.LoginHandler;
import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.intra.IntraServerConnection;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.questions.Question;
import com.wurmonline.server.utils.BMLBuilder;
import mod.maxammus.serfs.creatures.CustomPlayerClass;
import mod.maxammus.serfs.creatures.Serf;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestion;
import org.gotti.wurmunlimited.modsupport.questions.ModQuestions;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static com.wurmonline.server.utils.BMLBuilder.createGenericBuilder;

public class SerfContractQuestion implements ModQuestion {

    static Logger logger = Logger.getLogger(ManageSerfQuestion.class.getName());

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
                .addInput("name", null, true, "test", 0,0, null, null, null, 70, 16)
                .addRadioButton("male", "sex", "Male", male)
                .addRadioButton("female", "sex", "Female", !male)
                .closeBracket();
        bmlBuilder.addLabel("");
        String content = ModQuestions.getBmlHeader(question) + bmlBuilder +
                //Close the header without a button that all the ending methods give
            ModQuestions.createOkAnswerButton(question);
        int height = 256;
        question.getResponder().getCommunicator().sendBml(300, height, true, true, content, 200, 200, 200, question.getTitle());
    }

    @Override
    public void answer(Question question, Properties answers) {
        Creature responder = question.getResponder();
        String name = answers.getProperty("name");
        boolean male = Boolean.parseBoolean(answers.getProperty("male"));
        if(LoginHandler.containsIllegalCharacters(name) || name.length() > 35){
            question.getResponder().getCommunicator().sendNormalServerMessage("Invalid name.");
            create(question.getResponder(), question.getTarget(), male);
            return;
        }
        name = "Serf " + name;
        if (Players.getInstance().doesPlayerNameExist(name)) {
            question.getResponder().getCommunicator().sendNormalServerMessage("Serf with that name already exists.");
            create(question.getResponder(), question.getTarget(), male);
            return;
        }
        Creature performer = question.getResponder();
        CustomPlayerClass.doLogIn(name);
        Serf serf = (Serf)(Creature)Players.getInstance().getPlayerOrNull(name);
        ((Player)(Creature)serf).setBlood(IntraServerConnection.calculateBloodFromKingdom(performer.getKingdomId()));
        serf.calledBy(question.getResponder());
        Items.destroyItem(question.getTarget());
    }
}