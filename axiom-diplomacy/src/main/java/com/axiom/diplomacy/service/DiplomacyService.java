package com.axiom.diplomacy.service;

import com.axiom.core.EventPublisher;
import com.axiom.core.model.Nation;
import com.axiom.core.service.NationManager;

import java.util.*;

/**
 * Service managing diplomatic relations.
 */
public class DiplomacyService {
    private final NationManager nationManager;
    private final EventPublisher eventPublisher;
    
    // Relation types
    public enum Relation { NEUTRAL, ALLY, ENEMY, TRUCE, VASSAL }
    
    // nationId -> targetNationId -> Relation
    private final Map<String, Map<String, Relation>> relations = new HashMap<>();

    public DiplomacyService(NationManager nationManager, EventPublisher eventPublisher) {
        this.nationManager = nationManager;
        this.eventPublisher = eventPublisher;
    }

    public void setRelation(String nation1, String nation2, Relation relation) {
        relations.computeIfAbsent(nation1, k -> new HashMap<>()).put(nation2, relation);
        relations.computeIfAbsent(nation2, k -> new HashMap<>()).put(nation1, relation); // Symmetric for simplicity
        
        // Update Nation models for persistence (compatibility)
        updateNationModel(nation1, nation2, relation);
        updateNationModel(nation2, nation1, relation);
    }
    
    public Relation getRelation(String nation1, String nation2) {
        return relations.getOrDefault(nation1, Collections.emptyMap()).getOrDefault(nation2, Relation.NEUTRAL);
    }
    
    public boolean areAllies(String n1, String n2) {
        return getRelation(n1, n2) == Relation.ALLY;
    }
    
    public boolean areEnemies(String n1, String n2) {
        return getRelation(n1, n2) == Relation.ENEMY;
    }
    
    public void declareWar(String attackerId, String defenderId) {
        setRelation(attackerId, defenderId, Relation.ENEMY);
        // eventPublisher.publish(new WarDeclaredEvent(attackerId, defenderId));
    }
    
    public void formAlliance(String n1, String n2) {
        setRelation(n1, n2, Relation.ALLY);
        // eventPublisher.publish(new AllianceFormedEvent(n1, n2));
    }
    
    private void updateNationModel(String me, String other, Relation rel) {
        Nation n = nationManager.getNationById(me);
        if (n == null) return;
        
        // Clear old
        n.getAllies().remove(other);
        n.getEnemies().remove(other);
        
        if (rel == Relation.ALLY) n.getAllies().add(other);
        if (rel == Relation.ENEMY) n.getEnemies().add(other);
        
        try {
            nationManager.save(n);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
