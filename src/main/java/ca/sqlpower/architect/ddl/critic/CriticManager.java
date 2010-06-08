/*
 * Copyright (c) 2010, SQL Power Group Inc.
 *
 * This file is part of SQL Power Architect.
 *
 * SQL Power Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.architect.ddl.critic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.architect.ddl.critic.CriticAndSettings.Severity;
import ca.sqlpower.architect.ddl.critic.CriticAndSettings.StarterPlatformTypes;
import ca.sqlpower.architect.ddl.critic.impl.MySQLCommentCritic;
import ca.sqlpower.architect.ddl.critic.impl.OraclePhysicalNameCritic;
import ca.sqlpower.architect.ddl.critic.impl.PrimaryKeyCritic;
import ca.sqlpower.architect.ddl.critic.impl.RelationshipMappingTypeCritic;
import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.sqlobject.SQLObject;

/**
 * A collection of settings that defines what critics are enabled in the system
 * and their settings to decide how to critique the object model.
 */
public class CriticManager extends AbstractSPObject {
    
    /**
     * Defines an absolute ordering of the child types of this class.
     */
    public static final List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
                Collections.singletonList(CriticGrouping.class)));

    /**
     * These are the critics that the critic manager will start with when it is
     * first created.
     */
    private static final List<CriticAndSettings> STARTING_CRITICS = 
        Collections.unmodifiableList(Arrays.asList(
                new OraclePhysicalNameCritic(), 
                new MySQLCommentCritic(),
                new PrimaryKeyCritic(),
                new RelationshipMappingTypeCritic()));

    /**
     * All of the critic groups known to this system.
     */
    private final List<CriticGrouping> criticGroupings = new ArrayList<CriticGrouping>();
    
    @Constructor
    public CriticManager() {
        setName("Critic Manager");
    }

    /**
     * Returns the list of starting critics. This list is final and will always
     * return the critics in the same order.
     */
    public List<CriticAndSettings> getStartingCritics() {
        return STARTING_CRITICS;
    }

    /**
     * Call this method to register the critics that come by default with
     * Architect.
     */
    public void registerStartingCritics() {
        for (CriticAndSettings criticType : STARTING_CRITICS) {
            registerCritic(criticType);
        }
    }

    /**
     * Used to add a critic to the manager. Once registered the critic can have
     * properties set on it to decide the error level and other properties. If
     * the critic has already been registered this will do nothing.
     * <p>
     * The {@link CriticFactory} must be able to make critics of this type to be
     * able to actually use the critics in practice.
     * 
     * @param criticClass
     *            The class that defines the critic. Will be used by the
     *            {@link CriticFactory} to create new critics.
     * @param platformType
     *            Normally one of the values defined in
     *            {@link StarterPlatformTypes} but can really be any value. It
     *            is used to group critics defined in the system logically.
     */
    public void registerCritic(CriticAndSettings critic) {
        for (CriticGrouping grouping : criticGroupings) {
            if (grouping.getPlatformType().equals(critic.getPlatformType())) {
                //already registered
                grouping.addChild(critic, grouping.getSettings().size());
                return;
            }
        }
        final CriticGrouping newGrouping = new CriticGrouping(critic.getPlatformType());
        addChild(newGrouping, criticGroupings.size());
        newGrouping.addChild(critic, 0);
    }
    
    /**
     * Returns a list of critics to calculate criticisms of objects passed to
     * them. These critics are immutable after they are created.
     */
    public List<Critic<SQLObject>> createCritics() {
        List<Critic<SQLObject>> critics = new ArrayList<Critic<SQLObject>>();
        for (CriticGrouping grouping : criticGroupings) {
            if (!grouping.isEnabled()) continue;
            for (CriticAndSettings singleSettings : grouping.getSettings()) {
                if (Severity.IGNORE.equals(singleSettings.getSeverity())) continue;
                critics.add(singleSettings);
            }
        }
        return Collections.unmodifiableList(critics);
    }
    
    @Override
    protected void addChildImpl(SPObject child, int index) {
        if (child instanceof CriticGrouping) {
            final CriticGrouping critic = (CriticGrouping) child;
            criticGroupings.add(index, critic);
            critic.setParent(this);
            fireChildAdded(CriticGrouping.class, child, index);
        } else {
            throw new IllegalStateException("Invalid child type " + child);
        }
    }

    @Override
    protected boolean removeChildImpl(SPObject child) {
        int index = criticGroupings.indexOf(child);
        boolean removed = criticGroupings.remove(child);
        if (removed) {
            fireChildRemoved(CriticAndSettings.class, child, index);
        }
        return removed;
    }

    public boolean allowsChildren() {
        return true;
    }

    public int childPositionOffset(Class<? extends SPObject> childType) {
        return 0;
    }

    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return allowedChildTypes;
    }

    public List<? extends SPObject> getChildren() {
        return Collections.unmodifiableList(criticGroupings);
    }

    public List<? extends SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        for (CriticGrouping grouping : criticGroupings) {
            grouping.removeDependency(dependency);
        }
    }
    
    @NonProperty
    public List<CriticGrouping> getCriticGroupings() {
        return Collections.unmodifiableList(criticGroupings);
    }
}
