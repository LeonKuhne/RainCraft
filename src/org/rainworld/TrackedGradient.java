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
  private Function<Block, Boolean> trackOn;
  private Function<Block, Boolean> untrackOn;
  private boolean iterating;
  private List<Runnable> postActions;

  TrackedGradient(String metadataKey) { 
    super(metadataKey);
    this.tracked = new HashSet<>();
    this.iterating = false;
    this.postActions = new ArrayList<>();
    trackOn = block -> true;
    untrackOn = block -> false;
  }

  // NOTE: you can only have one
  public void trackWhen(Function<Block, Boolean> trackFilter) {
    Function<Block, Boolean> oldTrackOn = this.trackOn;
    this.trackOn = block -> oldTrackOn.apply(block) && trackFilter.apply(block);
  }
  public void untrackWhen(Function<Block, Boolean> trackFilter) {
    Function<Block, Boolean> oldUntrackOn = this.untrackOn;
    this.untrackOn = block -> oldUntrackOn.apply(block) && trackFilter.apply(block);
  }

  public void track(Set<Block> blocks) {
    this.tracked.addAll(blocks);
  }

  // iterate safely
  public void each(Consumer<Block> callback) { this.each(callback, () -> {}); }
  public void each(Consumer<Block> callback, Runnable beforeRelease) {
    this.iterating = true;
    for (Block block : new HashSet<>(tracked)) callback.accept(block);
    beforeRelease.run();
    this.iterating = false;
    // apply modifications 
    for (Runnable action : postActions) action.run();
    postActions.clear();
  }

  public boolean isEmpty() { return tracked.isEmpty(); }
  public int size() { return tracked.size(); }

  // track on update
  @Override
  public void update(Block block, double value) {
    if (iterating) { postActions.add(() -> this.update(block, value)); return; } 
    super.update(block, value);
    // untrack
    if (this.tracked.contains(block)) {
      if (untrackOn.apply(block)) tracked.remove(block);
      return;
    }
    // track
    if (trackOn.apply(block)) tracked.add(block);
  }

  // untrack on reset
  @Override
  public void reset(Block block) {
    if (iterating) { postActions.add(() -> this.reset(block)); return; } 
    super.reset(block);
    tracked.remove(block);
  }
}
