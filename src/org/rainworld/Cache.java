package org.rainworld;

import java.util.HashMap;
import java.util.function.Function;

public class Cache<I, O> {

  private HashMap<I, O> cache;

  public Cache() {
    this.cache = new HashMap<I, O>();
  }

  public O load(Function<I, O> method, I params) { 
    // use cached
    O output = cache.get(params);
    if (output != null) return output;
    // use original method
    output = method.apply(params);
    // cache result
    cache.put(params, output);
    return output;
  }
}
