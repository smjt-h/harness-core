package io.harness.debezium;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonClientCreator {
  public static RedissonClient getRedissonClient(String redisAddress) {
    Config config = new Config();
    config.useSingleServer().setAddress(redisAddress);
    return Redisson.create(config);
  }
}
