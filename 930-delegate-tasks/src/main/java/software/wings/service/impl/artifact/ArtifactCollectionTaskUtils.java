package software.wings.service.impl.artifact;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class ArtifactCollectionTaskUtils {
  /**
   * getNewBuildDetails returns new BuildDetails after removing Artifact already present in DB.
   *
   * @param savedBuildDetailsKeys    the artifact keys for artifacts already stored in DB
   * @param buildDetails             the new build details fetched from third-party repo
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return the new build details
   */
  public static List<BuildDetails> getNewBuildDetails(Set<String> savedBuildDetailsKeys,
      List<BuildDetails> buildDetails, String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    return ArtifactCollectionTaskUtils.getNewBuildDetails(
        savedBuildDetailsKeys, buildDetails, artifactStreamType, artifactStreamAttributes);
  }

  /**
   * getBuildDetailsKeyFn returns a function that can extract a unique key for a BuildDetails object so that it can be
   * compared with an Artifact object.
   *
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return the function that can used to get the key for a BuildDetails
   */
  public static Function<BuildDetails, String> getBuildDetailsKeyFn(
      String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    return ArtifactCollectionTaskUtils.getBuildDetailsKeyFn(artifactStreamType, artifactStreamAttributes);
  }

  /**
   * isGenericArtifactStream returns true if we need to compare artifact paths to check if two artifacts - one stored in
   * our DB and one from an artifact repo - are different.
   *
   * @param artifactStreamType       the artifact stream type
   * @param artifactStreamAttributes the artifact stream attributes - used only for ARTIFACTORY
   * @return true, if generic artifact stream - uses artifact path as key
   */
  static boolean isGenericArtifactStream(String artifactStreamType, ArtifactStreamAttributes artifactStreamAttributes) {
    return ArtifactCollectionTaskUtils.isGenericArtifactStream(artifactStreamType, artifactStreamAttributes);
  }
}
