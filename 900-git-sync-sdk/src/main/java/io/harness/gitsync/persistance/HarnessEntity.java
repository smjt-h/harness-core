package io.harness.gitsync.persistance;

public interface HarnessEntity {

  enum Store {INLINE, REMOTE}

  Store getStore();

  String getData();

}
