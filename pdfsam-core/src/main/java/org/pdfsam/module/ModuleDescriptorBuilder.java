/*
 * This file is part of the PDF Split And Merge source code
 * Created on 08/mag/2014
 * Copyright 2017 by Sober Lemur S.a.s. di Vacondio Andrea (info@pdfsam.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pdfsam.module;

/**
 * Builder for the {@link ModuleDescriptor}
 *
 * @author Andrea Vacondio
 */
public final class ModuleDescriptorBuilder {
    private ModuleDescriptorBuilderProduct moduleDescriptorBuilderProduct = new ModuleDescriptorBuilderProduct();

    private int priority = ModulePriority.DEFAULT.getPriority();

    private ModuleDescriptorBuilder() {
        // hide
    }

    public ModuleDescriptorBuilder category(ModuleCategory category) {
        return moduleDescriptorBuilderProduct.category(category, this);
    }

    public ModuleDescriptorBuilder inputTypes(ModuleInputOutputType... inputTypes) {
        return moduleDescriptorBuilderProduct.inputTypes(this, inputTypes);
    }

    public ModuleDescriptorBuilder name(String name) {
        return moduleDescriptorBuilderProduct.name(name, this);
    }

    public ModuleDescriptorBuilder description(String description) {
        return moduleDescriptorBuilderProduct.description(description, this);
    }

    public ModuleDescriptorBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    public ModuleDescriptorBuilder priority(ModulePriority priority) {
        this.priority = priority.getPriority();
        return this;
    }

    public ModuleDescriptorBuilder supportURL(String supportURL) {
        return moduleDescriptorBuilderProduct.supportURL(supportURL, this);
    }

    /**
     * factory method
     *
     * @return the builder instance
     */
    public static ModuleDescriptorBuilder builder() {
        return new ModuleDescriptorBuilder();
    }

    public ModuleDescriptor build() {
        return moduleDescriptorBuilderProduct.build(this.priority);
    }
}
