/*******************************************************************************
 * Copyright (c) 2010, 2014 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2.publisher;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.BuildOutputDirectory;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.model.Category;
import org.eclipse.tycho.p2.tools.FacadeException;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;

/**
 * <p>
 * Publishes the category definitions from the <tt>category.xml</tt> in the root of the project.
 * </p>
 * 
 * @see http://wiki.eclipse.org/Equinox/p2/Publisher
 */
@Mojo(name = "publish-categories", defaultPhase = LifecyclePhase.PACKAGE)
public final class PublishCategoriesMojo extends AbstractPublishMojo {

    @Override
    protected Collection<DependencySeed> publishContent(PublisherServiceFactory publisherServiceFactory)
            throws MojoExecutionException, MojoFailureException {
        PublisherService publisherService = publisherServiceFactory.createPublisher(getReactorProject(),
                getEnvironments());

        try {
            List<DependencySeed> categoryIUs = new ArrayList<DependencySeed>();
            for (Category category : getCategories()) {
                final File buildCategoryFile = prepareBuildCategory(category, getBuildDirectory());

                Collection<DependencySeed> ius = publisherService.publishCategories(buildCategoryFile);
                categoryIUs.addAll(ius);
            }
            return categoryIUs;
        } catch (FacadeException e) {
            throw new MojoExecutionException("Exception while publishing categories: " + e.getMessage(), e);
        }
    }

    /**
     * Writes the Tycho-internal representation of categories back to a category.xml.
     * 
     * @param category
     *            a category, with "qualifier" literals already replaced by the build qualifier.
     */
    private File prepareBuildCategory(Category category, BuildOutputDirectory buildFolder)
            throws MojoExecutionException {
        try {
            File ret = buildFolder.getChild("category.xml");
            buildFolder.getLocation().mkdirs();
            Category.write(category, ret);
            copySiteI18nFiles(buildFolder);
            return ret;
        } catch (IOException e) {
            throw new MojoExecutionException("I/O exception while writing category definition to disk", e);
        }
    }

    private void copySiteI18nFiles(BuildOutputDirectory buildFolder) throws IOException {
        File[] i18nFiles = getProject().getBasedir().listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                String fileName = file.getName();
                return fileName.startsWith("site") && fileName.endsWith(".properties");
            }
        });
        if (i18nFiles == null) {
            return;
        }
        for (File i18nFile : i18nFiles) {
            FileUtils.copyFile(i18nFile, buildFolder.getChild(i18nFile.getName()));
        }
    }

    private List<Category> getCategories() {
        return getEclipseRepositoryProject().loadCategories(getProject());
    }
}
