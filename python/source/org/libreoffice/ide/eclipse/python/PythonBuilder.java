/*************************************************************************
 *
 * $RCSfile: JavaBuilder.java,v $
 *
 * $Revision: 1.7 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2008/12/13 13:43:02 $
 *
 * The Contents of this file are made available subject to the terms of
 * the GNU Lesser General Public License Version 2.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 *
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.libreoffice.ide.eclipse.python;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.libreoffice.ide.eclipse.core.PluginLogger;
import org.libreoffice.ide.eclipse.core.model.IUnoidlProject;
import org.libreoffice.ide.eclipse.core.model.ProjectsManager;
import org.libreoffice.ide.eclipse.core.model.config.IOOo;
import org.libreoffice.ide.eclipse.core.model.config.ISdk;
import org.libreoffice.ide.eclipse.core.model.language.ILanguageBuilder;
import org.libreoffice.ide.eclipse.core.model.utils.SystemHelper;
import org.libreoffice.ide.eclipse.python.build.FilesVisitor;
import org.libreoffice.plugin.core.model.UnoPackage;

/**
 * The language builder implementation for Python.
 */
public class PythonBuilder implements ILanguageBuilder {

    private Language mLanguage;

    /**
     * Constructor.
     *
     * @param pLanguage the Java Language object
     */
    public PythonBuilder(Language pLanguage) {
        mLanguage = pLanguage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFile createLibrary(IUnoidlProject pUnoProject) throws Exception {
        IFile jarFile = ((PythonProjectHandler) mLanguage.getProjectHandler()).getJarFile(pUnoProject);

        return jarFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateFromTypes(ISdk pSdk, IOOo pOoo, IProject pPrj, File pTypesFile,
        File pBuildFolder, String pRootModule, IProgressMonitor pMonitor) {

        if (pTypesFile.exists()) {

            if (null != pSdk && null != pOoo) {

                String[] paths = pOoo.getTypesPath();
                String oooTypesArgs = ""; //$NON-NLS-1$
                for (String path : paths) {
                    IPath ooTypesPath = new Path(path);
                    oooTypesArgs += " -X\"" + ooTypesPath.toOSString() + "\""; //$NON-NLS-1$ //$NON-NLS-2$
                }

                // TODO What if the user creates other root modules ?
                String firstModule = pRootModule.split("::")[0]; //$NON-NLS-1$

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getBuildEnv(IUnoidlProject pUnoProject) {

        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillUnoPackage(UnoPackage pUnoPackage, IUnoidlProject pUnoPrj) {

        //All the constituent Python files of the project are added
        File prjFile = SystemHelper.getFile(pUnoPrj);
        IFolder sourceFolder = pUnoPrj.getFolder(pUnoPrj.getSourcePath());
        ArrayList<IFile> pythonFiles = new ArrayList<IFile>();
        getPythonFiles(sourceFolder, pythonFiles, pUnoPrj);

        for (IFile pythonFile : pythonFiles) {
            File eachFile = SystemHelper.getFile(pythonFile);
            pUnoPackage.addComponentFile(
                UnoPackage.getPathRelativeToBase(eachFile, prjFile),
                eachFile, "Python"); //$NON-NLS-1$
        }

    }

    /**
     * Get the Python files that are located in the project
     * directory or one of its sub-folder.
     *
     * @param pPythonPrj the project from which to get the Python files
     * @return a list of all the those Python Files 
     */
    private void getPythonFiles(IFolder sourceFolder, ArrayList<IFile> pythonFiles, IUnoidlProject pUnoPrj) {
        try {
            for (IResource member : sourceFolder.members()) {
                if (member.getType() == 2) { // '1' is for file and '2' is for folder
                    IFolder subSourceFolder = ResourcesPlugin.getWorkspace().getRoot().getFolder(member.getFullPath());
                    getPythonFiles(subSourceFolder, pythonFiles, pUnoPrj);
                } else {
                    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(member.getFullPath());
                    pythonFiles.add(file);
                }

            }
        } catch (Exception e) {
            PluginLogger.error(
                Messages.getString("PythonExport.SourceFolderError"), e);

        }

    }
}