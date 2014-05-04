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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Player.NoClaimReason;


/**
 * Represents a work location on a tile. Each ColonyTile except the
 * colony center tile provides a work place for a single unit and
 * produces a single type of goods. The colony center tile generally
 * produces two different of goods, one food type and one new world
 * raw material.
 */
public class ColonyTile extends WorkLocation {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The maximum number of units a ColonyTile can hold. */
    public static final int UNIT_CAPACITY = 1;

    /**
     * The tile to work.  This is accessed through getWorkTile().
     * Beware!  Do not confuse this with getTile(), which returns
     * the colony center tile (because every work location belongs to
     * the enclosing colony).
     */
    protected Tile workTile;


    /**
     * Constructor for ServerColonyTile.
     *
     * @param game The enclosing <code>Game</code>.
     * @param colony The <code>Colony</code> this object belongs to.
     * @param workTile The tile in which this <code>ColonyTile</code>
     *                 represents a <code>WorkLocation</code> for.
     */
    protected ColonyTile(Game game, Colony colony, Tile workTile) {
        super(game);
        
        this.colony = colony;
        this.workTile = workTile;
        updateProductionType();
    }

    /**
     * Create a new <code>ColonyTile</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public ColonyTile(Game game, String id) {
        super(game, id);
    }


    /**
     * Gets a description of the tile, with the name of the tile
     * and any improvements made to it (road/plow).
     *
     * @return The description label for this tile
     */
    public StringTemplate getLabel() {
        return workTile.getLabel();
    }

    /**
     * Is this the tile where the <code>Colony</code> is located?
     *
     * @return True if this is the colony center tile.
     */
    public boolean isColonyCenterTile() {
        return getWorkTile() == getTile();
    }

    /**
     * Gets the work tile, that is the actual tile being worked.
     *
     * @return The <code>Tile</code> in which this
     *     <code>ColonyTile</code> represents a <code>WorkLocation</code> for.
     */
    public Tile getWorkTile() {
        return workTile;
    }

    /**
     * Sets the work tile.  Needed to fix copied colonies.  Do not use
     * otherwise!
     *
     * @param The new work <code>Tile</code>.
     */
    public void setWorkTile(Tile workTile) {
        this.workTile = workTile;
    }

    /**
     * Gets a unit who is occupying the tile.
     *
     * @return A <code>Unit</code> who is occupying the work tile, if any.
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        return workTile.getOccupyingUnit();
    }

    /**
     * Is there a fortified enemy unit on the work tile?
     * Production can not occur on occupied tiles.
     *
     * @return True if an fortified enemy unit is in the tile.
     */
    public boolean isOccupied() {
        return workTile.isOccupied();
    }

    /**
     * Gets the basic production information for the colony tile,
     * ignoring any colony limits (which for now, should be irrelevant).
     *
     * In the original game, the following special rules apply to
     * colony center tiles: All tile improvements contribute to the
     * production of food. Only natural tile improvements, such as
     * rivers, contribute to the production of other types of goods.
     * Artificial tile improvements, such as plowing, are ignored.
     *
     * @return The raw production of this colony tile.
     * @see ProductionCache#update
     */
    public ProductionInfo getBasicProductionInfo() {
        final Colony colony = getColony();
        ProductionInfo pi = new ProductionInfo();
        if (isColonyCenterTile()) {
            for (AbstractGoods output : getOutputs()) {
                boolean onlyNaturalImprovements = getSpecification()
                    .getBoolean(GameOptions.ONLY_NATURAL_IMPROVEMENTS)
                    && !output.getType().isFoodType();
                int potential = output.getAmount();
                if (workTile.getTileItemContainer() != null) {
                    potential = workTile.getTileItemContainer()
                        .getTotalBonusPotential(output.getType(), null, potential,
                                                onlyNaturalImprovements);
                }
                potential += Math.max(0, colony.getProductionBonus());
                AbstractGoods production
                    = new AbstractGoods(output.getType(), potential);
                pi.addProduction(production);
            }
        } else {
            final Turn turn = getGame().getTurn();
            boolean onlyNaturalImprovements = false;
            for (AbstractGoods output : getOutputs()) {
                final GoodsType goodsType = output.getType();
                for (Unit unit : getUnitList()) {
                    final UnitType unitType = unit.getType();
                    int potential = (int)FeatureContainer
                        .applyModifiers(output.getAmount(), turn, 
                            getProductionModifiers(goodsType, unitType));
                    if (potential > 0) {
                        pi.addProduction(new AbstractGoods(goodsType, potential));
                    }
                }
            }
        }
        return pi;
    }


    // Interface Location
    // Inheriting
    //   FreeColObject.getId
    //   WorkLocation.getTile (Beware, this returns the colony center tile!),
    //   UnitLocation.getLocationNameFor
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   WorkLocation.remove
    //   UnitLocation.getUnitCount
    //   final UnitLocation.getUnitIterator
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer
    //   final WorkLocation getSettlement
    //   final WorkLocation getColony

    /**
     * {@inheritDoc}
     */
    public StringTemplate getLocationName() {
        String name = getColony().getName();
        return (isColonyCenterTile()) ? StringTemplate.name(name)
            : StringTemplate.template("nearLocation")
                .add("%direction%", "direction."
                     + getTile().getDirection(workTile).toString())
                .addName("%location%", name);
    }


    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.canBuildEquipment
    //   UnitLocation.canBuildRoleEquipment
    //   UnitLocation.equipForRole

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        NoAddReason reason = getNoWorkReason();
        return (reason != NoAddReason.NONE) ? reason
            : super.getNoAddReason(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCapacity() {
        return (isColonyCenterTile()) ? 0 : UNIT_CAPACITY;
    }


    // Interface WorkLocation

    /**
     * {@inheritDoc}
     */
    public NoAddReason getNoWorkReason() {
        Tile tile = getWorkTile();
        NoClaimReason claim;

        return (isColonyCenterTile())
            ? NoAddReason.COLONY_CENTER
            : (!getColony().hasAbility(Ability.PRODUCE_IN_WATER)
                && !tile.isLand())
            ? NoAddReason.MISSING_ABILITY
            : (tile.getOwningSettlement() == getColony())
            ? NoAddReason.NONE
            : ((claim = getOwner().canClaimForSettlementReason(tile))
                == NoClaimReason.NONE)
            ? NoAddReason.CLAIM_REQUIRED
            : (claim == NoClaimReason.TERRAIN
                || claim == NoClaimReason.RUMOUR
                || claim == NoClaimReason.WATER)
            ? NoAddReason.MISSING_ABILITY
            : (claim == NoClaimReason.SETTLEMENT)
            ? ((getOwner().owns(tile.getSettlement()))
                ? NoAddReason.ANOTHER_COLONY
                : NoAddReason.OWNED_BY_ENEMY)
            : (claim == NoClaimReason.OCCUPIED)
            ? NoAddReason.OCCUPIED_BY_ENEMY
            : (claim == NoClaimReason.WORKED)
            ? NoAddReason.ANOTHER_COLONY
            : (claim == NoClaimReason.EUROPEANS)
            ? NoAddReason.OWNED_BY_ENEMY
            : (claim == NoClaimReason.NATIVES)
            ? NoAddReason.CLAIM_REQUIRED
            : NoAddReason.WRONG_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAutoProduce() {
        return isColonyCenterTile();
    }

    /**
     * {@inheritDoc}
     */
    public int getProductionOf(Unit unit, GoodsType goodsType) {
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        }
        return getPotentialProduction(goodsType, unit.getType());
    }

    /**
     * {@inheritDoc}
     */
    public int getPotentialProduction(GoodsType goodsType, UnitType unitType) {
        int production = 0;
        TileType tileType = workTile.getType();
        if (isColonyCenterTile()) {
            if (unitType == null) {
                production = getBaseProduction(goodsType);
            } else {
                production = 0;
            }
        } else if (workTile.isLand()
            || getColony().hasAbility(Ability.PRODUCE_IN_WATER)) {
            production = tileType.getProductionOf(goodsType, unitType);
            List<Modifier> mods = getProductionModifiers(goodsType, unitType);
            if (!mods.isEmpty()) {
                production = (int)FeatureContainer.applyModifiers(production,
                    getGame().getTurn(), mods);
            }
        }
        return Math.max(0, production);
    }

    /**
     * {@inheritDoc}
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        if (goodsType == null) {
            throw new IllegalArgumentException("Null GoodsType.");
        }
        List<Modifier> result = new ArrayList<Modifier>();
        final Colony colony = getColony();
        final Player owner = colony.getOwner();
        final TileType tileType = getWorkTile().getType();
        final String id = goodsType.getId();
        final Turn turn = getGame().getTurn();
        if (isColonyCenterTile()) {
            for (AbstractGoods output : getOutputs()) {
                if (goodsType == output.getType()) {
                    result.addAll(workTile.getProductionModifiers(goodsType, null));
                    result.addAll(colony.getModifierSet(id, null, turn));
                    result.add(colony.getProductionModifier(goodsType));
                    if (owner != null) {
                        result.addAll(owner.getModifierSet(id, null, turn));
                    }
                }
            }
        } else {
            result.addAll(workTile.getProductionModifiers(goodsType, unitType));
            // special case: a resource might provide base production
            if (FeatureContainer.applyModifiers(0f, turn, result) > 0
                || produces(goodsType)) {
                result.addAll(colony.getModifierSet(id, null, turn));
                if (unitType != null) {
                    result.add(colony.getProductionModifier(goodsType));
                    result.addAll(unitType.getModifierSet(id, tileType, turn));
                    if (owner != null) {
                        result.addAll(owner.getModifierSet(id, null, turn));
                    }
                } else {
                    if (owner != null) {
                        result.addAll(owner.getModifierSet(id, tileType, turn));
                    }
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public List<ProductionType> getProductionTypes() {
        if (workTile != null
            && workTile.getType() != null) {
            return workTile.getType()
                .getProductionTypes(isColonyCenterTile());
        } else {
            return new ArrayList<ProductionType>();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductionType getBestProductionType(GoodsType goodsType) {
        ProductionType best = super.getBestProductionType(goodsType);
        if (best == null) {
            TileItemContainer container = workTile.getTileItemContainer();
            if (container != null) {
                int amount = container.getTotalBonusPotential(goodsType, null, 0, false);
                if (amount > 0) {
                    // special case: resource is present
                    best = new ProductionType(null, goodsType, 0);
                }
            }
        }
        return best;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getClaimTemplate() {
        return (isColonyCenterTile()) ? super.getClaimTemplate()
            : StringTemplate.template("workClaimColonyTile")
                .add("%direction%", "direction."
                    + getTile().getDirection(workTile).toString());
    }


    // Serialization

    private static final String WORK_TILE_TAG = "workTile";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WORK_TILE_TAG, workTile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        workTile = xr.makeFreeColGameObject(getGame(), WORK_TILE_TAG,
                                            Tile.class, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" ").append(getWorkTile())
            .append("/").append(getColony().getName())
            .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "colonyTile".
     */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
