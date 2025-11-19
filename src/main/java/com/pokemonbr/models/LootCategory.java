package com.pokemonbr.models;

import java.util.ArrayList;
import java.util.List;

/**
 * 战利品品类模型
 *
 * @author l1ang_Y5n
 * @qq 235236127
 */
public class LootCategory {

    private final String name;
    private final int weight;
    private final List<LootItem> items;

    public LootCategory(String name, int weight) {
        this.name = name;
        this.weight = weight;
        this.items = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public List<LootItem> getItems() {
        return items;
    }

    public void addItem(LootItem item) {
        items.add(item);
    }
}
