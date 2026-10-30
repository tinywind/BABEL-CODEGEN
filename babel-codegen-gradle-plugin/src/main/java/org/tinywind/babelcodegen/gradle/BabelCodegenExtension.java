package org.tinywind.babelcodegen.gradle;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class BabelCodegenExtension {
    private final NamedDomainObjectContainer<BabelSource> sources;

    @Inject
    public BabelCodegenExtension(ObjectFactory objects) {
        sources = objects.domainObjectContainer(BabelSource.class,
                name -> objects.newInstance(BabelSource.class, name));
        getPreset().convention("REACT");
        getMinified().convention(false);
        getSkip().convention(false);
    }

    public NamedDomainObjectContainer<BabelSource> getSources() {
        return sources;
    }

    public void sources(Action<? super NamedDomainObjectContainer<BabelSource>> action) {
        action.execute(sources);
    }

    public abstract Property<String> getPreset();

    public abstract Property<Boolean> getMinified();

    public abstract Property<Boolean> getSkip();
}
