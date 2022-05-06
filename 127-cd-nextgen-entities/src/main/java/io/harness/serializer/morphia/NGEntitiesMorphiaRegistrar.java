package io.harness.serializer.morphia;

import io.harness.cdng.artifact.outcome.AcrArtifactOutcome;
import io.harness.cdng.artifact.outcome.DockerArtifactOutcome;
import io.harness.cdng.artifact.outcome.GcrArtifactOutcome;
import io.harness.cdng.service.beans.ServiceConfigOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.service.entity.ServiceEntity;

import java.util.Set;

public class NGEntitiesMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ServiceEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("cdng.service.beans.ServiceOutcome", ServiceOutcome.class);
    h.put("cdng.service.beans.ServiceOutcome$ArtifactsOutcome", ServiceOutcome.ArtifactsOutcome.class);
    h.put("ngpipeline.artifact.bean.DockerArtifactOutcome", DockerArtifactOutcome.class);
    h.put("ngpipeline.artifact.bean.GcrArtifactOutcome", GcrArtifactOutcome.class);
    h.put("ngpipeline.artifact.bean.AcrArtifactOutcome", AcrArtifactOutcome.class);
    h.put("cdng.service.beans.ServiceConfigOutcome", ServiceConfigOutcome.class);
  }
}
