package org.smartrplace.appstore.api;

public interface SourceCodeBundle extends ArtifactoryBundle {
	GitRepository repository();
	String pathInRepository();
}
