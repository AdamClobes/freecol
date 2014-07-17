/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.pathfinding.CostDeciders;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;


/**
 * Mission for realizing a <code>Wish</code>.
 */
public class WishRealizationMission extends Mission {

    private static final Logger logger = Logger.getLogger(WishRealizationMission.class.getName());

    /** The tag for this mission. */
    private static final String tag = "AI wisher";

    /** The wish to be realized. */
    private Wish wish;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param wish The <code>Wish</code> which will be realized by
     *        the unit and this mission.
     */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit, Wish wish) {
        super(aiMain, aiUnit);

        this.wish = wish;
        logger.finest(tag + " starting with destination "
            + wish.getDestination() + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>WishRealizationMission</code> and reads the
     * given element.
     *
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission is created for.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit,
                                  FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, aiUnit);

        readFromXML(xr);
        uninitialized = getAIUnit() == null;
    }


    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        if (wish != null) {
            wish.setTransportable(null);
            wish = null;
        }
        super.dispose();
    }


    // Fake Transportable interface wholly inherited from Mission.

    // Mission interface

    /**
     * {@inheritDoc}
     */
    public Location getTarget() {
        return (wish == null) ? null : wish.getDestination();
    }

    /**
     * {@inheritDoc}
     */
    public void setTarget(Location target) {
        throw new IllegalStateException("Target is fixed.");
    }

    /**
     * {@inheritDoc}
     */
    public Location findTarget() {
        throw new IllegalStateException("Target is fixed.");
    }

    // Omitted invalidReason(AIUnit), not needed

    /**
     * Why would this mission be invalid with the given AI unit and location?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        String reason;
        return ((reason = invalidAIUnitReason(aiUnit)) != null) ? reason
            : ((reason = invalidTargetReason(loc,
                        aiUnit.getUnit().getOwner())) != null) ? reason
            : null;
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        return (wish == null) ? "wish-null"
            : invalidReason(getAIUnit(), getTarget());
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * {@inheritDoc}
     */
    public Mission doMission(StringBuilder sb) {
        logSB(sb, tag);
        String reason = invalidReason();
        if (reason != null) {
            logSBbroken(sb, reason);
            return null;
        }

        // Move towards the target.
        final Unit unit = getUnit();
        Location target = getTarget();
        Unit.MoveType mt = travelToTarget(target,
            CostDeciders.avoidSettlementsAndBlockingUnits(), sb);
        switch (mt) {
        case MOVE:
            break;
        case MOVE_ILLEGAL:
        case MOVE_NO_MOVES: case MOVE_NO_REPAIR: case MOVE_NO_TILE:
            return this;
        default:
            logSBmove(sb, unit, mt);
            return this;
        }

        if (target instanceof Colony) {
            final AIMain aiMain = getAIMain();
            final Colony colony = (Colony)target;
            final AIUnit aiUnit = getAIUnit();
            final AIColony aiColony = aiMain.getAIColony(colony);
            aiColony.completeWish(wish, "mission(" + unit + ")");
            // Replace the mission, with a defensive one if this is a
            // military unit or a simple working one if not.
            if (unit.getType().isOffensive()) {
                logSBdone(sb, " ready to defend ", colony);
                return new DefendSettlementMission(aiMain, aiUnit, colony);
            } else {                
                aiColony.requestRearrange();
                logSBdone(sb, " ready to work at ", colony);
                return new WorkInsideColonyMission(aiMain, aiUnit, aiColony);
            }
        } else {
            logSBfail(sb, " broken wish: ", wish);
            wish.dispose();
            wish = null;
            return null;
        }
    }

    // Serialization

    private static final String WISH_TAG = "wish";
    // @compat 0.10.3
    private static final String OLD_GOODS_WISH_TAG = "GoodsWish";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WISH_TAG, wish);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        final String wid = xr.getAttribute(WISH_TAG, (String)null);
        wish = xr.getAttribute(aiMain, WISH_TAG, Wish.class, (Wish)null);
        if (wish == null) {
            if (wid.startsWith(GoodsWish.getXMLElementTagName())
                // @compat 0.10.3
                || wid.startsWith(OLD_GOODS_WISH_TAG)
                // end @compat
                ) {
                wish = new GoodsWish(aiMain, wid);

            } else if (wid.startsWith(WorkerWish.getXMLElementTagName())) {
                wish = new WorkerWish(aiMain, wid);

            } else {
                throw new XMLStreamException("Unknown wish tag: " + wid);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "wishRealizationMission";
    }
}
