package org.smartrplace.appstore.util;

import java.util.Objects;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.smartrplace.appstore.api.MavenBundleVersioned;

/**
 *
 * @author jlapp
 */
public class DefaultMavenBundleVersioned implements MavenBundleVersioned {

    final String groupId;
    final String artifactId;
    final String version;
    final ArtifactVersion av;

    public DefaultMavenBundleVersioned(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.av = version != null
                ? new DefaultArtifactVersion(version)
                : null;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public int compareToVersion(String version) {
        if (av == null){
            return -1;
        }
        ArtifactVersion otherVersion = new DefaultArtifactVersion(version);
        return av.compareTo(otherVersion);
    }

    @Override
    public String groupId() {
        return groupId;
    }

    @Override
    public String artifactId() {
        return artifactId;
    }

    @Override
    public int hashCode() {
        return artifactId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultMavenBundleVersioned other = (DefaultMavenBundleVersioned) obj;
        if (!Objects.equals(this.groupId, other.groupId)) {
            return false;
        }
        if (!Objects.equals(this.artifactId, other.artifactId)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return "DefaultMavenBundleVersioned{" + "groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + '}';
    }

}
