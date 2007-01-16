/*************************************************************************
 *
 * $RCSfile: PackageExportWizard.java,v $
 *
 * $Revision: 1.2 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2006/12/06 07:49:21 $
 *
 * The Contents of this file are made available subject to the terms of
 * either of the GNU Lesser General Public License Version 2.1
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
package org.openoffice.ide.eclipse.core.wizards;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.openoffice.ide.eclipse.core.OOEclipsePlugin;
import org.openoffice.ide.eclipse.core.builders.ServicesBuilder;
import org.openoffice.ide.eclipse.core.internal.helpers.FileHelper;
import org.openoffice.ide.eclipse.core.internal.helpers.UnoidlProjectHelper;
import org.openoffice.ide.eclipse.core.model.IUnoidlProject;
import org.openoffice.ide.eclipse.core.model.UnoPackage;

/**
 * A wizard to export the project as a UNO package.
 * 
 * @author cedricbosdo
 *
 */
public class PackageExportWizard extends Wizard implements IExportWizard {

	private IStructuredSelection mSelection;
	private PackageExportWizardPage mPage;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish() {
		
		IUnoidlProject prj = mPage.getProject();
		String extension = mPage.getPackageExtension();
		File outputDir = mPage.getOutputPath();
		
		new PackageExportJob(prj, extension, outputDir).schedule();
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		mSelection = selection;
		mPage = new PackageExportWizardPage("main", mSelection); //$NON-NLS-1$
		addPage(mPage);
	}

	/**
	 * The class performing the task
	 * 
	 * @author cedricbosdo
	 */
	private class PackageExportJob extends Job {

		private IUnoidlProject mPrj;
		private String mExtension;
		private File mOutputDir;
		
		public PackageExportJob(IUnoidlProject prj, String version, File output) {
			super(Messages.getString("PackageExportWizard.JobTitle")); //$NON-NLS-1$
			setPriority(Job.INTERACTIVE);
			
			mPrj = prj;
			mExtension = version;
			mOutputDir = output;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// First run the services builder
			Status status = ServicesBuilder.syncRun(mPrj, monitor);
			
			if (status.getSeverity() == Status.CANCEL || status.getSeverity() == Status.ERROR) {
				return status;
			}
			
			// Remove the temporarily created Manifest and keep the library
			try {
				mPrj.getFile("MANIFEST.MF").delete(true, monitor); //$NON-NLS-1$
				mPrj.getFile("services.rdb").delete(true, monitor); //$NON-NLS-1$
			} catch (CoreException e) {
				// Not important
			}
			
			
			// Create the package
			IPath prjPath = mPrj.getProjectPath();
			File dir = new File(prjPath.toOSString());
			
			File dest = new File(mOutputDir, mPrj.getName() + "." + mExtension); //$NON-NLS-1$
			
			UnoPackage unoPackage = new UnoPackage(dest, dir);
			
			// Add the content of the package
			unoPackage.addTypelibraryFile(new File(dir, "types.rdb"), "RDB"); //$NON-NLS-1$ //$NON-NLS-2$
			mPrj.getLanguage().getLanguageBuidler().fillUnoPackage(unoPackage, mPrj);
		
			// Close and write the package
			dest = unoPackage.close();
			
			// Clean up the library file and META-INF directory
			FileHelper.remove(new File(dir, "META-INF")); //$NON-NLS-1$
			File libFile = new File(mPrj.getLanguage().getProjectHandler().getLibraryPath(mPrj));
			FileHelper.remove(libFile);
			
			// Refresh the project and return the status
			UnoidlProjectHelper.refreshProject(mPrj, monitor);
			
			// Propose to update the package in OpenOffice.org instance
			Display.getDefault().asyncExec(new DeployerJob(mPrj, dest));
			
			
			return new Status(IStatus.OK, 
					OOEclipsePlugin.OOECLIPSE_PLUGIN_ID, 
					IStatus.OK, 
					Messages.getString("PackageExportWizard.ExportedMessage"), //$NON-NLS-1$
					null);
		}
	}
	
	class DeployerJob implements Runnable {
		
		private IUnoidlProject mPrj;
		private File mDest;
		
		DeployerJob(IUnoidlProject prj, File dest) {
			mPrj = prj;
			mDest = dest;
		}
		
		public void run() {
			if (mPrj.getOOo().canManagePackages()) {
				// Ask to update the package
				if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), 
						Messages.getString("PackageExportWizard.DeployPackageTitle"),  //$NON-NLS-1$
						Messages.getString("PackageExportWizard.DeployPackageMessage"))) { //$NON-NLS-1$
					mPrj.getOOo().updatePackage(mDest);
				}
			}
		}
	}
}