package com.axiom.testsupport;

import com.axiom.AXIOM;
import com.axiom.domain.model.Nation;
import com.axiom.domain.service.state.NationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryNationManager extends NationManager {
    private final Map<String, Nation> nations = new HashMap<>();
    private final Map<UUID, Nation> byPlayer = new HashMap<>();

    public InMemoryNationManager(AXIOM plugin) {
        super(plugin);
    }

    public synchronized void addNation(Nation nation) {
        if (nation == null || nation.getId() == null) {
            return;
        }
        nations.put(nation.getId(), nation);
        indexNation(nation);
    }

    private void indexNation(Nation nation) {
        if (nation.getCitizens() != null) {
            for (UUID id : nation.getCitizens()) {
                byPlayer.put(id, nation);
            }
        }
    }

    @Override
    public synchronized Nation getNationById(String id) {
        return nations.get(id);
    }

    @Override
    public synchronized Collection<Nation> getAll() {
        return new ArrayList<>(nations.values());
    }

    @Override
    public synchronized Optional<Nation> getNationOfPlayer(UUID uuid) {
        return Optional.ofNullable(byPlayer.get(uuid));
    }

    @Override
    public synchronized void save(Nation nation) throws IOException {
        if (nation == null || nation.getId() == null) {
            return;
        }
        nations.put(nation.getId(), nation);
        indexNation(nation);
    }
}
