package io.harness.gitsync.persistance;

public interface GitAware extends HarnessEntity{

  String getConnector();

  String getRepo();

  String getBranch();

  String getPath();

  @Override
  default Store getStore() {
    return Store.REMOTE;
  }
}
