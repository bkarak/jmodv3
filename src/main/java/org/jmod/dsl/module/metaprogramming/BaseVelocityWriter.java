package org.jmod.dsl.module.metaprogramming;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Thin Velocity wrapper used by external modules for Java code generation.
 */
public class BaseVelocityWriter {
    private final VelocityEngine engine;
    private final String templatePath;
    private final VelocityContext context = new VelocityContext();

    public BaseVelocityWriter(String tf, String modName) {
        this.templatePath = tf;
        this.engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        engine.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.init();
        context.put("MODULE_NAME", modName);
    }

    public void add(String key, Object value) {
        context.put(key, value);
    }

    public StringWriter prepare() {
        StringWriter writer = new StringWriter();
        Template template = engine.getTemplate(templatePath, StandardCharsets.UTF_8.name());
        template.merge(context, writer);
        return writer;
    }

    public boolean save(File of) {
        try {
            File parent = of.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
            Files.writeString(of.toPath(), prepare().toString(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
