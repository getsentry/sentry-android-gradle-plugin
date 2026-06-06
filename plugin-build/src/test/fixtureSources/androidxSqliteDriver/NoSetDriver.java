package com.example;

/** Negative case: no setDriver call site, so the visitor must pass the class through unchanged. */
public class NoSetDriver {

  public int unrelated(int value) {
    return value + 1;
  }
}
