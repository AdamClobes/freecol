/**
 *  Copyright (C) 2002-2013  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.i18n;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


public class MessagesTest extends FreeColTestCase {

    public static final String noSuchKey = "should.not.exist.and.thus.return.null";

    private static final Role armedBraveRole
        = spec().getRole("model.role.armedBrave");
    private static final Role cavalryRole
        = spec().getRole("model.role.cavalry");
    private static final Role defaultRole
        = spec().getDefaultRole();
    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final Role infantryRole
        = spec().getRole("model.role.infantry");
    private static final Role missionaryRole
        = spec().getRole("model.role.missionary");
    private static final Role mountedBraveRole
        = spec().getRole("model.role.mountedBrave");
    private static final Role nativeDragoonRole
        = spec().getRole("model.role.nativeDragoon");
    private static final Role pioneerRole
        = spec().getRole("model.role.pioneer");
    private static final Role soldierRole
        = spec().getRole("model.role.soldier");


    public static final UnitType artillery
        = spec().getUnitType("model.unit.artillery");
    public static final UnitType brave
        = spec().getUnitType("model.unit.brave");
    public static final UnitType caravel
        = spec().getUnitType("model.unit.caravel");
    public static final UnitType colonialRegular
        = spec().getUnitType("model.unit.colonialRegular");
    public static final UnitType hardyPioneer
        = spec().getUnitType("model.unit.hardyPioneer");
    public static final UnitType jesuitMissionary
        = spec().getUnitType("model.unit.jesuitMissionary");
    public static final UnitType kingsRegular
        = spec().getUnitType("model.unit.kingsRegular");
    public static final UnitType manOWar
        = spec().getUnitType("model.unit.manOWar");
    public static final UnitType treasureTrain
        = spec().getUnitType("model.unit.treasureTrain");
    public static final UnitType veteranSoldier
        = spec().getUnitType("model.unit.veteranSoldier");


    public void tearDown(){
        Messages.setMessageBundle(Locale.US);
    }

    public void testMessageString() {
        assertEquals("Press enter in order to end the turn.", Messages.message("infoPanel.endTurnPanel.text"));
        assertEquals("Trade Advisor", Messages.message("reportTradeAction.name"));

        // With parameters
        assertEquals("Score: %score%    |    Gold: %gold%    |    Tax: %tax%%    |    Year: %year%",
                     Messages.message("menuBar.statusLine"));

        // Long String
        assertEquals("Food is necessary to feed your colonists and to breed horses. "
                     + "A new colonist is born whenever a colony has 200 units of food or more.",
                     Messages.message("model.goods.food.description"));

        // Message not found
        assertEquals(noSuchKey, Messages.message(noSuchKey));
    }


    public void testChangeLocaleSettings() {
        Messages.setMessageBundle(Locale.US);

        assertEquals("Trade Advisor", Messages.message("reportTradeAction.name"));

        Messages.setMessageBundle(Locale.GERMANY);

        assertEquals("Handelsberater", Messages.message("reportTradeAction.name"));
    }

    // Tests if messages with special chars (like $) are well processed
    public void testMessageWithSpecialChars(){
        String errMsg = "Error setting up test.";
        String expected = "You establish the colony of %colony%.";
        String message = Messages.message("model.history.FOUND_COLONY");
        assertEquals(errMsg, expected, message);

        String colNameWithSpecialChars="$specialColName\\";
        errMsg = "Wrong message";
        expected = "You establish the colony of $specialColName\\.";
        try {
            message = Messages.message(StringTemplate.template("model.history.FOUND_COLONY")
                                       .addName("%colony%", colNameWithSpecialChars));
        } catch(IllegalArgumentException e){
            if(e.getMessage().contains("Illegal group reference")){
                fail("Does not process messages with special chars");
            }
            throw e;
        }
        assertEquals(errMsg, expected, message);
    }

    public void testStringTemplates() {
        Messages.setMessageBundle(Locale.US);

        // template with key not in message bundle
        StringTemplate s1 = StringTemplate.key("!no.such.string.template");
        assertEquals(s1.getId(), Messages.message(s1));

        StringTemplate s2 = StringTemplate.key("model.tile.plains.name");
        assertEquals("Plains", Messages.message(s2));

        StringTemplate t1 = StringTemplate.template("model.goods.goodsAmount")
            .add("%goods%", "model.goods.food.name")
            .addName("%amount%", "100");
        assertEquals(2, t1.getKeys().size());
        assertEquals(2, t1.getReplacements().size());
        assertEquals(StringTemplate.TemplateType.KEY,
                     t1.getReplacements().get(0).getTemplateType());
        assertEquals(StringTemplate.TemplateType.NAME,
                     t1.getReplacements().get(1).getTemplateType());
        assertEquals("model.goods.goodsAmount", t1.getId());
        assertEquals("100 Food", Messages.message(t1));

        StringTemplate t2 = StringTemplate.label(" / ")
            .add("model.goods.food.name")
            .addName("xyz");
        assertEquals("Food / xyz", Messages.message(t2));

        Game game = getGame();
        game.setMap(getTestMap());
        Colony colony = getStandardColony();
        assertEquals("New Amsterdam", colony.getName());

        StringTemplate t3 = StringTemplate.template("inLocation")
            .addName("%location%", colony.getName());
        assertEquals("In New Amsterdam", Messages.message(t3));

        StringTemplate t4 = StringTemplate.label("")
            .addName("(")
            .add("model.goods.food.name")
            .addName(")");
        assertEquals("(Food)", Messages.message(t4));

    }

    public void testReplaceGarbage() {
        // random garbage enclosed in double brackets should be
        // removed
        String mapping = "some.key={{}}abc   {{xyz}}def{{123|567}}\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }
        assertEquals("abc   def", Messages.message("some.key"));
    }

    public void testReplaceNumber() {

        double[] numbers = new double[] {
            -1.3, -1, -0.5, 0, 0.33, 1, 1.2, 2, 2.7, 3, 3.4, 11, 13, 27, 100
        };
        String mapping = "some.key=abc{{plural:%number%|zero=zero|one=one|two=two"
            + "|few=few|many=many|other=other}}|xyz";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        // default Number is Other
        Messages.setGrammaticalNumber(NumberRules.OTHER_NUMBER_RULE);
        for (double d : numbers) {
            assertEquals("abcother|xyz", Messages.message(StringTemplate.template("some.key")
                                                          .addAmount("%number%", d)));
        }
        // apply English rules
        Messages.setGrammaticalNumber(NumberRules.PLURAL_NUMBER_RULE);
        for (double d : numbers) {
            if (d == 1) {
                assertEquals("abcone|xyz", Messages.message(StringTemplate.template("some.key")
                                                            .addAmount("%number%", d)));
            } else {
                assertEquals("abcother|xyz", Messages.message(StringTemplate.template("some.key")
                                                              .addAmount("%number%", d)));
            }
        }

    }

    public void testReplaceArbitraryTag() {
        StringTemplate template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "east");
        String expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail eastward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "west");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail westward in order to discover "
            + "the New World and to claim it for the Crown.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("tutorial.startGame")
            .add("%direction%", "whatever");
        expected = "After months at sea, you have finally arrived off the "
            + "coast of an unknown continent. Sail into the wind in order to discover "
            + "the New World and to claim it for the Crown.";
        assertEquals(expected, Messages.message(template));

    }


    public void testReplaceChoicesPlural() {

        String mapping = "some.key=This is {{plural:%number%|one=a test|other=one of several tests"
            + "|default=not much of a test}}.\n"
            + "unit.template=%number% {{plural:%number%|%unit%}}\n"
            + "unit.key={{plural:%number%|one=piece of artillery|other=pieces of artillery|"
            + "default=artillery}}";
        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        assertEquals("artillery", Messages.message("unit.key"));

        assertEquals("This is one of several tests.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 0)));
        assertEquals("This is a test.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 1)));
        assertEquals("This is one of several tests.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 2)));
        assertEquals("This is one of several tests.",
                     Messages.message(StringTemplate.template("some.key")
                                      .addAmount("%number%", 24)));

        StringTemplate template = StringTemplate.template("unit.template")
            .addAmount("%number%", 1)
            .add("%unit%", "unit.key");

        assertEquals("1 piece of artillery", Messages.message(template));

    }

    public void testReplaceChoicesGrammar() {
        String mapping = "key.france={{randomTag:%randomKey%|country=France|people=French|"
            + "default=French people}}\n"
            + "greeting1=The {{otherRandomTag:default|%nation%}} are happy to see you.\n"
            + "greeting2=The {{otherRandomTag:people|%nation%}} are happy to see you.\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        assertEquals("French people", Messages.message("key.france"));

        StringTemplate t1 = StringTemplate.template("key.france")
            .add("%randomKey%", "country");
        assertEquals("France", Messages.message(t1));

        StringTemplate t2 = StringTemplate.template("greeting1")
            .add("%nation%", "key.france");
        assertEquals("The French people are happy to see you.", Messages.message(t2));

        StringTemplate t3 = StringTemplate.template("greeting2")
            .add("%nation%", "key.france");
        assertEquals("The French are happy to see you.", Messages.message(t3));


    }

    public void testNestedChoices() {
        String mapping = "key1=%colony% tuottaa tuotetta "
            + "{{tag:acc|%goods%}}.\n"
            + "key2={{plural:%amount%|one=ruoka|other=ruokaa|"
            + "default={{tag:|acc=viljaa|default=Vilja}}}}\n"
            + "key3={{tag:|acc=viljaa|default={{plural:%amount%|one=ruoka|other=ruokaa|default=Ruoka}}}}\n";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        StringTemplate t = StringTemplate.template("key1")
            .addName("%colony%", "someColony")
            .add("%goods%", "key2");

        assertEquals("someColony tuottaa tuotetta viljaa.", Messages.message(t));

        assertEquals("Ruoka", Messages.message(StringTemplate.key("key3")));
        assertEquals("Ruoka", Messages.message("key3"));

    }

    public void testTurnChoices() {
        String mapping = "monarch={{turn:%turn%|1492=bob|SPRING 1493=anson"
            + "|AUTUMN 1493-1588=paul|1589-SPRING 1612=james"
            + "|AUTUMN 1612-AUTUMN 1667=nathan|default=fred}}";

        ByteArrayInputStream stream = new ByteArrayInputStream(mapping.getBytes());
        try {
            Messages.loadMessages(stream);
        } catch (IOException ioe) { fail(); }

        StringTemplate t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(1));
        assertEquals("bob", Messages.message(t));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(2));
        assertEquals("anson", Messages.message(t));

        t = StringTemplate.template("monarch")
            .addName("%turn%", "AUTUMN 1493");
        assertEquals("paul", Messages.message(t));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(100));
        assertEquals("james", Messages.message(t));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(150));
        assertEquals("nathan", Messages.message(t));

        t = StringTemplate.template("monarch")
            .addName("%turn%", "YEAR 1624");
        assertEquals("nathan", Messages.message(t));

        t = StringTemplate.template("monarch")
            .addName("%turn%", Turn.toString(1000));
        assertEquals("fred", Messages.message(t));

    }


    public void testAbstractUnitLabels() {
        AbstractUnit unit = new AbstractUnit("model.unit.merchantman",
                                             Specification.DEFAULT_ROLE_ID, 1);
        assertEquals("one Merchantman", Messages.message(unit.getLabel()));

    }


    public void testLabels() {
        Game game = getStandardGame();
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        ServerPlayer dutchREF = new ServerPlayer(game, "dutchREF", false,
            dutch.getNation().getREFNation(), null, null);
        ServerPlayer sioux = (ServerPlayer)game.getPlayer("model.nation.sioux");
        Unit unit;

        // King's regulars
        unit = new ServerUnit(game, null, dutchREF,
                              kingsRegular, defaultRole);
        assertEquals("King's Regular", Messages.message(unit.getLabel()));

        unit.changeRole(infantryRole, 1);
        assertEquals("Infantry", Messages.message(unit.getLabel()));

        unit.changeRole(cavalryRole, 1);
        assertEquals("Cavalry", Messages.message(unit.getLabel()));

        // Colonial regulars
        unit = new ServerUnit(game, null, dutch, 
                              colonialRegular, defaultRole);
        assertEquals("Colonial Regular", Messages.message(unit.getLabel()));

        unit.changeRole(soldierRole, 1);
        assertEquals("Continental Army", Messages.message(unit.getLabel()));

        unit.changeRole(dragoonRole, 1);
        assertEquals("Continental Cavalry", Messages.message(unit.getLabel()));

        // Veteran Soldiers
        unit = new ServerUnit(game, null, dutch,
                              veteranSoldier, soldierRole);
        assertEquals("Dutch Veteran Soldier", Messages.message(unit.getFullLabel()));

        unit.changeRole(defaultRole, 0);
        assertEquals("Dutch Veteran Soldier (no muskets)",
                     Messages.message(unit.getFullLabel()));

        unit.changeRole(dragoonRole, 1);
        assertEquals("Veteran Dragoon", Messages.message(unit.getLabel()));

        // Indian Braves
        unit = new ServerUnit(game, null, sioux, brave);
        assertEquals("Brave", Messages.message(unit.getLabel()));

        unit.changeRole(armedBraveRole, 1);
        assertEquals("Armed Brave", Messages.message(unit.getLabel()));

        unit.changeRole(mountedBraveRole, 1);
        assertEquals("Mounted Brave", Messages.message(unit.getLabel()));

        unit.changeRole(nativeDragoonRole, 1);
        assertEquals("Native Dragoon", Messages.message(unit.getLabel()));

        // Hardy Pioneers
        unit = new ServerUnit(game, null, dutch,
                              hardyPioneer, pioneerRole);
        assertEquals("Hardy Pioneer", Messages.message(unit.getLabel()));

        unit.changeRole(defaultRole, 0);
        assertEquals("Dutch Hardy Pioneer (no tools)",
                     Messages.message(unit.getFullLabel()));

        // Jesuit Missionaries
        unit = new ServerUnit(game, null, dutch,
                              jesuitMissionary, missionaryRole);
        assertEquals("Jesuit Missionary", Messages.message(unit.getLabel()));

        unit.changeRole(defaultRole, 0);
        assertEquals("Dutch Jesuit Missionary (not commissioned)",
                     Messages.message(unit.getFullLabel()));

        // REF addition message
        StringTemplate template
            = StringTemplate.template("model.monarch.action.ADD_TO_REF")
                            .addAmount("%number%", 1)
                            .add("%unit%", kingsRegular.getNameKey());
        String expected = "The Crown has added 1 King's Regular"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
                                 .addAmount("%number%", 2)
                                 .add("%unit%", artillery.getNameKey());
        expected = "The Crown has added 2 Pieces of Artillery"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertEquals(expected, Messages.message(template));

        template = StringTemplate.template("model.monarch.action.ADD_TO_REF")
                                 .addAmount("%number%", 3)
                                 .add("%unit%", manOWar.getNameKey());
        expected = "The Crown has added 3 Men of War"
            + " to the Royal Expeditionary Force."
            + " Colonial leaders express concern.";
        assertEquals(expected, Messages.message(template));
    }

    public void testUnitLabel() {
        Game game = getStandardGame();
        ServerPlayer dutch = (ServerPlayer)game.getPlayer("model.nation.dutch");
        ServerPlayer dutchREF = new ServerPlayer(game, "dutchREF", false,
            dutch.getNation().getREFNation(), null, null);
        ServerPlayer sioux = (ServerPlayer)game.getPlayer("model.nation.sioux");

        Role scout = spec().getRole("model.role.scout");
        Role soldier = spec().getRole("model.role.soldier");
        Role dragoon = spec().getRole("model.role.dragoon");
        Role pioneer = spec().getRole("model.role.pioneer");
        Role missionary = spec().getRole("model.role.missionary");
        Unit unit;

        // Free Colonists
        unit = new ServerUnit(game, null, dutch, spec()
                              .getUnitType("model.unit.freeColonist"));
        assertEquals("Dutch Free Colonist", Messages.getLabel(unit));

        unit.setRole(soldier);
        assertEquals("Dutch Soldier (Free Colonist)", Messages.getLabel(unit));

        unit.setName("John Doe");
        assertEquals("John Doe (Dutch Soldier/Free Colonist)", Messages.getLabel(unit));

        // Master Carpenter
        unit = new ServerUnit(game, null, dutch, spec()
                              .getUnitType("model.unit.masterCarpenter"));
        assertEquals("Dutch Master Carpenter", Messages.getLabel(unit));

        unit.setRole(missionary);
        assertEquals("Dutch Missionary (Master Carpenter)", Messages.getLabel(unit));


        // King's regulars
        unit = new ServerUnit(game, null, dutchREF,
                              kingsRegular, defaultRole);
        assertEquals("Dutch Royal Expeditionary Force King's Regular",
                     Messages.getLabel(unit));

        unit.changeRole(infantryRole, 1);
        assertEquals("Dutch Royal Expeditionary Force Infantry",
                     Messages.getLabel(unit));

        unit.changeRole(cavalryRole, 1);
        assertEquals("Dutch Royal Expeditionary Force Cavalry", Messages.getLabel(unit));

        // Colonial regulars
        unit = new ServerUnit(game, null, dutch,
                              colonialRegular, defaultRole);
        assertEquals("Dutch Colonial Regular", Messages.getLabel(unit));

        unit.changeRole(soldierRole, 1);
        assertEquals("Dutch Continental Army", Messages.getLabel(unit));

        unit.changeRole(dragoonRole, 1);
        assertEquals("Dutch Continental Cavalry", Messages.getLabel(unit));

        // Veteran Soldiers
        unit = new ServerUnit(game, null, dutch,
                              veteranSoldier, soldierRole);
        assertEquals("Dutch Veteran Soldier", Messages.getLabel(unit));

        unit.changeRole(defaultRole, 0);
        assertEquals("Dutch Veteran Soldier (no muskets)",
                     Messages.getLabel(unit));

        unit.changeRole(dragoonRole, 1);
        assertEquals("Dutch Veteran Dragoon", Messages.getLabel(unit));

        unit.setName("Davy Crockett");
        assertEquals("Davy Crockett (Dutch Veteran Dragoon)",
                     Messages.getLabel(unit));

        // Indian Braves
        unit = new ServerUnit(game, null, sioux, brave, defaultRole);
        assertEquals("Sioux Brave", Messages.getLabel(unit));

        unit.changeRole(armedBraveRole, 1);
        assertEquals("Sioux Armed Brave", Messages.getLabel(unit));

        unit.changeRole(nativeDragoonRole, 1);
        assertEquals("Sioux Native Dragoon", Messages.getLabel(unit));

        unit.setName("Chingachgook");
        assertEquals("Chingachgook (Sioux Native Dragoon)",
                     Messages.getLabel(unit));

        // Hardy Pioneers
        unit = new ServerUnit(game, null, dutch,
                              hardyPioneer, pioneerRole);
        assertEquals("Dutch Hardy Pioneer (100 Tools)",
                     Messages.getLabel(unit));

        unit.changeRole(defaultRole, 0);
        assertEquals("Dutch Hardy Pioneer (no tools)",
                     Messages.getLabel(unit));

        unit.setName("Daniel Boone");
        assertEquals("Daniel Boone (Dutch Hardy Pioneer/no tools)",
                     Messages.getLabel(unit));

        // Jesuit Missionaries
        unit = new ServerUnit(game, null, dutch,
                              jesuitMissionary, missionaryRole);
        assertEquals("Dutch Jesuit Missionary", Messages.getLabel(unit));

        unit.changeRole(defaultRole, 0);
        assertEquals("Dutch Jesuit Missionary (not commissioned)",
                     Messages.getLabel(unit));

        // Treasure Train
        unit = new ServerUnit(game, null, dutch,
                              treasureTrain, defaultRole);
        unit.setTreasureAmount(4567);
        assertEquals("Dutch Treasure Train (4567 gold)",
                     Messages.getLabel(unit));

        unit.setName("The Gold of El Dorado");
        assertEquals("The Gold of El Dorado (Dutch Treasure Train/4567 gold)",
                     Messages.getLabel(unit));

        // Caravel
        unit = new ServerUnit(game, null, dutch,
                              caravel, defaultRole);
        assertEquals("Dutch Caravel", Messages.getLabel(unit));

        unit.setName("Santa Maria");
        assertEquals("Santa Maria (Dutch Caravel)", Messages.getLabel(unit));
    }
}
