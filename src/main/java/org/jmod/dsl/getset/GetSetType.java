package org.jmod.dsl.getset;

import org.jmod.dsl.module.ExternalBaseType;

/**
 * Runtime base type for generated getter/setter holders.
 */
public class GetSetType<T extends GetSetConfiguration> extends ExternalBaseType<T> {
    public GetSetType(T configuration) {
        super(configuration);
    }
}
