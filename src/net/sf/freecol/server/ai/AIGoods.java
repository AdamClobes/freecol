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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.TransportMission;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link Goods}.
 */
public class AIGoods extends AIObject implements Transportable {

    private static final Logger logger = Logger.getLogger(AIGoods.class.getName());

    /** The underlying goods. */
    private Goods goods;

    /** The destination location for the goods. */
    private Location destination;

    /** The transport priority. */
    private int transportPriority;

    /** The AI unit assigned to provide the transport. */
    private AIUnit transport = null;


    /**
     * Creates a new uninitialized <code>AIGoods</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public AIGoods(AIMain aiMain, String id) {
        super(aiMain, id);

        this.goods = null;
        this.destination = null;
        this.transportPriority = -1;
        this.transport = null;
    }

    /**
     * Creates a new <code>AIGoods</code>.
     *
     * @param aiMain The main AI-object.
     * @param location The location of the goods.
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @param destination The destination of the goods. This is the
     *      <code>Location</code> to which the goods should be transported.
     */
    public AIGoods(AIMain aiMain, Location location, GoodsType type,
                   int amount, Location destination) {
        this(aiMain, getXMLElementTagName() + ":" + aiMain.getNextId());

        this.goods = new Goods(aiMain.getGame(), location, type, amount);
        this.destination = destination;

        uninitialized = false;
    }

    /**
     * Creates a new <code>AIGoods</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *       of a <code>Wish</code>.
     */
    public AIGoods(AIMain aiMain, Element element) {
        super(aiMain, element);

        uninitialized = getGoods() == null;
    }
    
    /**
     * Creates a new <code>AIGoods</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public AIGoods(AIMain aiMain,
                   FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        uninitialized = getGoods() == null;
    }


    /**
     * Gets the goods this <code>AIGoods</code> is controlling.
     *
     * @return The <code>Goods</code>.
     */
    public Goods getGoods() {
        return goods;
    }

    /**
     * Sets the goods this <code>AIGoods</code> is controlling.
     *
     * @param goods The new <code>Goods</code>.
     */
    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    /**
     * Gets the type of goods this <code>AIGoods</code> is controlling.
     *
     * @return The <code>GoodsType</code>.
     */
    public GoodsType getGoodsType() {
        return goods.getType();
    }

    /**
     * Gets the amount of goods this <code>AIGoods</code> is controlling.
     *
     * @return The amount of goods.
     */
    public int getGoodsAmount() {
        return goods.getAmount();
    }


    // Interface Transportable

    /**
     * Gets the number of cargo slots taken by these AI goods.
     *
     * @return The number of cargo slots.
     */
    public int getSpaceTaken() {
        return (goods == null) ? 0 : goods.getSpaceTaken();
    }

    /**
     * Returns the source for this <code>Transportable</code>.
     * This is normally the location of the
     * {@link #getTransportLocatable locatable}.
     *
     * @return The source for this <code>Transportable</code>.
     */
    public Location getTransportSource() {
        return (goods == null) ? null : goods.getLocation();
    }

    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the tansport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */
    public Location getTransportDestination() {
        return destination;
    }

    /**
     * Sets the destination for this <code>Transportable</code>.
     * This should only be called when a goods transportable destination
     * becomes invalid and we need to retarget.
     *
     * @param destination The new destination <code>Location</code>.
     */
    public void setTransportDestination(Location destination) {
        this.destination = destination;
    }

    /**
     * Gets the priority of transporting this <code>Transportable</code>
     * to it's destination.
     *
     * @return The priority of the transport.
     */
    public int getTransportPriority() {
        return transportPriority;
    }

    /**
     * Sets the priority of getting the goods to the {@link
     * #getTransportDestination}.
     *
     * @param transportPriority The priority.
     */
    public void setTransportPriority(int transportPriority) {
        this.transportPriority = transportPriority;
    }

    /**
     * Increases the transport priority of this <code>Transportable</code>.
     * This method gets called every turn the <code>Transportable</code>
     * have not been put on a carrier's transport list.
     */
    public void increaseTransportPriority() {
        transportPriority++;
    }

    /**
     * Gets the <code>Locatable</code> which should be transported.
     * @return The <code>Locatable</code>.
     */
    public Locatable getTransportLocatable() {
        return getGoods();
    }

    /**
     * Gets the carrier responsible for transporting this
     * <code>Transportable</code>.
     *
     * @return The <code>AIUnit</code> which has this
     *         <code>Transportable</code> in it's transport list. This
     *         <code>Transportable</code> has not been scheduled for
     *         transport if this value is <code>null</code>.
     *
     */
    public AIUnit getTransport() {
        return transport;
    }

    /**
     * Sets the carrier responsible for transporting this
     * <code>Transportable</code>.
     *
     * @param transport The <code>AIUnit</code> which has this
     *            <code>Transportable</code> in it's transport list. This
     *            <code>Transportable</code> has not been scheduled for
     *            transport if this value is <code>null</code>.
     * @param reason A reason for changing the transport.
     */
    public void setTransport(AIUnit transport, String reason) {
        logger.finest("setTransport " + this + " -> " + transport
            + ": " + reason);
        this.transport = transport;
    }

    /**
     * Aborts the given <code>Wish</code>.
     *
     * @param w The <code>Wish</code> to be aborted.
     */
    public void abortWish(Wish w) {
        if (destination == w.getDestination()) destination = null;
        if (w.getTransportable() == this) w.dispose();
    }

    /**
     * Goods leaves a ship.
     *
     * @param amount The amount of goods to unload.
     * @return True if the unload succeeds.
     */
    public boolean leaveTransport(int amount) {
        if (!(goods.getLocation() instanceof Unit)) return false;
        final Unit carrier = (Unit)goods.getLocation();
        final GoodsType type = goods.getType();
        if (carrier.getGoodsCount(type) < amount) return false;

        final AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        Colony colony = carrier.getColony();
        int oldAmount = carrier.getGoodsCount(type);
        Goods newGoods = new Goods(carrier.getGame(), carrier, type, amount);
        boolean result;
        if (carrier.isInEurope()) {
            if (carrier.getOwner().canTrade(type)) {
                result = AIMessage.askSellGoods(aiCarrier, newGoods);
                logger.finest("Sell " + newGoods + " in Europe "
                    + ((result) ? "succeeds" : "fails")
                    + ": " + this);
            } else { // dump
                result = AIMessage.askUnloadCargo(aiCarrier, newGoods);
            }
        } else {
            result = AIMessage.askUnloadCargo(aiCarrier, newGoods);
        }
        if (result) {
            int newAmount = carrier.getGoodsCount(type);
            if (oldAmount - newAmount != amount) {
                logger.warning(carrier + " at " + carrier.getLocation()
                    + " only unloaded " + (oldAmount - newAmount)
                    + " " + type + " (" + amount + " expected)");
                // TODO: sort this out.
                // For now, do not tolerate partial unloads.
                result = false;
            }
        }   
        return result;
    }

    /**
     * Goods leaves a ship.
     * Completes a wish if possible.
     *
     * @param direction The <code>Direction</code> to unload (not applicable).
     * @return True if the unload succeeds.
     */
    public boolean leaveTransport(Direction direction) {
        if (direction != null) return false;
        return leaveTransport(goods.getAmount());
    }

    /**
     * Goods joins a ship.
     *
     * @param carrier The carrier <code>Unit</code> to join.
     * @param direction The <code>Direction</code> to unload (not applicable).
     * @return True if the load succeeds.
     */
    public boolean joinTransport(Unit carrier, Direction direction) {
        if (direction != null) return false;
        final AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        if (aiCarrier == null) return false;

        GoodsType goodsType = goods.getType();
        int goodsAmount = goods.getAmount();
        int oldAmount = carrier.getGoodsCount(goodsType);
        boolean result;
        if (carrier.isInEurope()) {
            result = AIMessage.askBuyGoods(aiCarrier, goodsType, goodsAmount);
        } else {
            result = AIMessage.askLoadCargo(aiCarrier, goods);
        }
        if (result) {
            int newAmount = carrier.getGoodsCount(goodsType);
            if (newAmount - oldAmount != goodsAmount) {
                logger.warning(carrier + " at " + carrier.getLocation()
                    + " only loaded " + (newAmount - oldAmount)
                    + " " + goodsType
                    + " (" + goodsAmount + " expected)");
                goodsAmount = newAmount - oldAmount;
                // TODO: sort this out.  For now, tolerate partial loads.
                result = goodsAmount > 0;
            }
        }
        if (result) {
            Colony colony = carrier.getColony();
            if (colony != null) {
                getAIMain().getAIColony(colony).removeAIGoods(this);
            }
            setGoods(new Goods(getGame(), carrier, goodsType, goodsAmount));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean carriableBy(Unit carrier) {
        return carrier.couldCarry(getGoods());
    }

    /**
     * {@inheritDoc}
     */
    public String invalidReason() {
        String reason = Mission.invalidTransportableReason(this);
        Settlement s;
        return (reason != null)
            ? reason
            : (goods.getLocation() instanceof Unit
                && destination instanceof Settlement
                && !((Unit)goods.getLocation()).getOwner().owns(s = (Settlement)destination))
            ? "transportableDestination-" + s.getName() + "-captured-by-"
                + s.getOwner().getNation().getSuffix()
            : null;
    }

    // Override AIObject

    /**
     * Disposes this object.
     */
    @Override
    public void dispose() {
        setTransport(null, "disposing");
        if (destination != null) {
            if (destination instanceof Colony) {
                AIColony aic = getAIMain().getAIColony((Colony)destination);
                if (aic != null) aic.removeAIGoods(this);
            } else if (destination instanceof Europe) {
                // Nothing to remove.
            } else {
                logger.warning("Unknown type of destination: " + destination);
            }
            destination = null;
        }
        goods = null;
        super.dispose();
    }

    /**
     * Checks the integrity of a this AIGoods.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        String why = (result < 0) ? "super"
            : (goods == null) ? "null-goods"
            : (goods.getType() == null) ? "null-goods-type"
            : (goods.getAmount() <= 0) ? "non-positive-goods-amount"
            : (goods.getLocation() == null) ? "null-location"
            : (((FreeColGameObject)goods.getLocation()).isDisposed()) ? "disposed-location"
            : null;
        if (destination != null
            && ((FreeColGameObject)destination).isDisposed()) {
            if (fix) {
                logger.warning("Fixing disposed destination for " + this);
                destination = null;
                if (result > 0) result = 0;
            } else {
                why = "disposed-destination";
            }
        }
        if (why != null) {
            logger.finest("checkIntegrity(" + this + ") = " + why);
            result = -1;
        }
        return result;
    }


    // Serialization

    private static final String DESTINATION_TAG = "destination";
    private static final String TRANSPORT_TAG = "transport";
    private static final String TRANSPORT_PRIORITY_TAG = "transportPriority";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (destination != null) {
            xw.writeAttribute(DESTINATION_TAG, destination.getId());

            xw.writeAttribute(TRANSPORT_PRIORITY_TAG, transportPriority);

            if (transport != null) {
                if (transport.isDisposed()) {
                    logger.warning("broken reference to transport");
                } else {
                    xw.writeAttribute(TRANSPORT_TAG, transport);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        goods.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();

        destination = xr.getLocationAttribute(aiMain.getGame(),
                                              DESTINATION_TAG, false);

        transportPriority = xr.getAttribute(TRANSPORT_PRIORITY_TAG, -1);

        transport = (xr.hasAttribute(TRANSPORT_TAG))
            ? xr.makeAIObject(aiMain, TRANSPORT_TAG,
                              AIUnit.class, (AIUnit)null, true)
            : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        if (getGoods() != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (Goods.getXMLElementTagName().equals(tag)) {
            if (goods != null) {
                goods.readFromXML(xr);
            } else {
                goods = new Goods(getAIMain().getGame(), xr);
            }

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("[", getId(), " ", goods);
        if (goods != null) lb.add(" at ", goods.getLocation());
        lb.add(" -> ", destination);
        AIUnit transport = getTransport();
        if (transport != null) lb.add(" using ", transport.getUnit());
        lb.add("/", getTransportPriority(), "]");
        return lb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "aiGoods"
     */
    public static String getXMLElementTagName() {
        return "aiGoods";
    }
}
