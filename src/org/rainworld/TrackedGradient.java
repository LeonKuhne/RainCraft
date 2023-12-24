package org.rainworld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.block.Block;

public class TrackedGradient extends Gradient {

  private Set<Block> tracked;
  private Consumer<Block> trackMethod;
  private boolean iterating;
  private List<Runnable> postActions;

  TrackedGradient(String metadataKey) { this(metadataKey, null); }
  TrackedGradient(String metadataKey, Function<Block, Boolean> trackFilter) { 
    super(metadataKey);
    this.tracked = new HashSet<>();
    this.iterating = false;
    this.postActions = new ArrayList<>();
    // precompile filter
    if (trackFilter == null) trackMethod = tracked::add;
    else trackMethod = block -> { if (trackFilter.apply(block)) tracked.add(block); };
  }

  // track on update
  @Override
  public void update(Block block, double value) {
    if (iterating) { postActions.add(() -> this.update(block, value)); return; } 
    super.update(block, value);
    trackMethod.accept(block);
  }

  // untrack on reset
  @Override
  public void reset(Block block) {
    if (iterating) { postActions.add(() -> this.reset(block)); return; } 
    super.reset(block);
    tracked.remove(block);
  }

  // iterate safely
  public void each(Consumer<Block> callback) {
    this.iterating = true;
    for (Block block : new HashSet<>(tracked)) callback.accept(block);
    this.iterating = false;
    // apply modifications 
    for (Runnable action : postActions) action.run();
    postActions.clear();
  }
}
