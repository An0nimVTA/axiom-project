// website/src/components/TechnologyTree.js
import React, { useState, useEffect } from 'react';
import './TechnologyTree.scss';

function TechnologyTree() {
  const [techData, setTechData] = useState(null);
  const [selectedNation, setSelectedNation] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Simulate API call
    setTimeout(() => {
      setTechData({
        branches: {
          military: {
            name: 'Military Technologies',
            description: 'Warfare and combat advancements',
            techs: [
              { id: 'basic_weapons', name: 'Basic Weapons', tier: 1, unlocked: true, dependencies: [], bonus: 'warStrength +10%' },
              { id: 'firearms', name: 'Firearms', tier: 2, unlocked: true, dependencies: ['basic_weapons'], bonus: 'weaponDamage +25%' },
              { id: 'advanced_armament', name: 'Advanced Armament', tier: 3, unlocked: false, dependencies: ['firearms'], bonus: 'warStrength +35%' }
            ]
          },
          industry: {
            name: 'Industrial Technologies',
            description: 'Production and manufacturing advancements',
            techs: [
              { id: 'basic_machinery', name: 'Basic Machinery', tier: 1, unlocked: true, dependencies: [], bonus: 'productionEfficiency +15%' },
              { id: 'advanced_machinery', name: 'Advanced Machinery', tier: 2, unlocked: true, dependencies: ['basic_machinery'], bonus: 'productionEfficiency +25%' },
              { id: 'automation', name: 'Automation', tier: 3, unlocked: false, dependencies: ['advanced_machinery'], bonus: 'autoCrafting +40%' }
            ]
          },
          economy: {
            name: 'Economic Technologies',
            description: 'Financial and trade systems',
            techs: [
              { id: 'basic_trading', name: 'Basic Trading', tier: 1, unlocked: true, dependencies: [], bonus: 'tradeBonus +10%' },
              { id: 'advanced_finance', name: 'Advanced Finance', tier: 2, unlocked: false, dependencies: ['basic_trading'], bonus: 'economicEfficiency +20%' }
            ]
          }
        }
      });
      setLoading(false);
    }, 1000);
  }, []);

  if (loading) {
    return <div className="loading">Loading technology data...</div>;
  }

  return (
    <div className="technology-tree">
      <h2>Technology Tree</h2>
      
      <div className="tech-selector">
        <select onChange={(e) => setSelectedNation(e.target.value)}>
          <option value="">Select Nation</option>
          <option value="nation1">Test Nation</option>
          <option value="nation2">Second Nation</option>
        </select>
      </div>
      
      {selectedNation && (
        <div className="tech-display">
          {Object.entries(techData.branches).map(([branchId, branch]) => (
            <div key={branchId} className="tech-branch">
              <h3>{branch.name}</h3>
              <p>{branch.description}</p>
              
              <div className="tech-grid">
                {branch.techs.map(tech => (
                  <div key={tech.id} className={`tech-node ${tech.unlocked ? 'unlocked' : 'locked'}`}>
                    <div className="tech-header">
                      <h4>{tech.name}</h4>
                      <span className={`tier-badge tier-${tech.tier}`}>Tier {tech.tier}</span>
                    </div>
                    <div className="tech-description">
                      {tech.dependencies.length > 0 && (
                        <div className="dependencies">
                          Requires: {tech.dependencies.join(', ')}
                        </div>
                      )}
                      <div className="bonus">
                        Bonus: {tech.bonus}
                      </div>
                    </div>
                    <div className="tech-actions">
                      {!tech.unlocked && (
                        <button className="btn btn-unlock">Unlock ({tech.tier * 10000} AXC)</button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default TechnologyTree;