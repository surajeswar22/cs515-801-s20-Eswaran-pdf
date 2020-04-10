package org.pdfsam.module;

import static org.sejda.commons.util.RequireUtils.requireNotBlank;
import static org.sejda.commons.util.RequireUtils.requireNotNullArg;

public class ModuleDescriptorBuilderProduct {
    private ModuleCategory category;
    private ModuleInputOutputType[] inputTypes;
    private String name;
    private String description;
    private String supportURL;

    public ModuleDescriptorBuilder category(ModuleCategory category, ModuleDescriptorBuilder moduleDescriptorBuilder) {
        this.category = category;
        return moduleDescriptorBuilder;
    }

    public ModuleDescriptor build(int thisPriority) {
        requireNotNullArg(category, "Module category cannot be null");
        requireNotBlank(name, "Module name cannot be blank");
        requireNotBlank(description, "Module description cannot be blank");
        return new ModuleDescriptor(category, name, description, thisPriority, supportURL, inputTypes);
    }

    public ModuleDescriptorBuilder inputTypes(ModuleDescriptorBuilder moduleDescriptorBuilder, ModuleInputOutputType... inputTypes) {
        this.inputTypes = inputTypes;
        return moduleDescriptorBuilder;
    }

    public ModuleDescriptorBuilder name(String name, ModuleDescriptorBuilder moduleDescriptorBuilder) {
        this.name = name;
        return moduleDescriptorBuilder;
    }

    public ModuleDescriptorBuilder description(String description, ModuleDescriptorBuilder moduleDescriptorBuilder) {
        this.description = description;
        return moduleDescriptorBuilder;
    }

    public ModuleDescriptorBuilder supportURL(String supportURL, ModuleDescriptorBuilder moduleDescriptorBuilder) {
        this.supportURL = supportURL;
        return moduleDescriptorBuilder;
    }
}