package org.openoffice.ide.eclipse.cpp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.openoffice.ide.eclipse.core.PluginLogger;

public class CDTHelper {

    public static void addCDTNature( IProject pProject, IProgressMonitor pMonitor ) throws CoreException {
            if (!pProject.exists()) {
                pProject.create( pMonitor );
                PluginLogger.debug(
                        "Project created during language specific operation"); //$NON-NLS-1$
            }
            
            if (!pProject.isOpen()) {
                pProject.open( pMonitor );
                PluginLogger.debug("Project opened"); //$NON-NLS-1$
            }
            
            CProjectNature.addCNature(pProject, pMonitor );
            CCProjectNature.addCCNature( pProject, pMonitor );
            ManagedCProjectNature.addManagedNature( pProject, pMonitor );
    }
    
    public static void addEntries( IProject pProject, ICLanguageSettingEntry[] pNewEntries, int pEntriesType ) {
        changeEntries( pProject, pNewEntries, pEntriesType, false );
    }
    
    public static void removeEntries( IProject pProject, ICLanguageSettingEntry[] pOldEntries, int pEntriesType ) {
        changeEntries( pProject, pOldEntries, pEntriesType, true );
    }
    
    private static void changeEntries( IProject pProject, ICLanguageSettingEntry[] pEntries, 
            int pEntriesType, boolean pRemove ) {
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription( pProject );
        ICConfigurationDescription[] configs = prjDesc.getConfigurations();
        
        // Set them on all the languages of all the configurations
        for (ICConfigurationDescription config : configs) {
            ICFolderDescription folder = config.getRootFolderDescription();
            ICLanguageSetting[] languages = folder.getLanguageSettings();
            for (ICLanguageSetting lang : languages) {
                List<ICLanguageSettingEntry> entries = lang.getSettingEntriesList( pEntriesType );
                for ( ICLanguageSettingEntry entry : pEntries ) {
                    boolean contained = entries.contains( entry );
                    if ( contained && pRemove ) {
                        entries.remove( entry );
                    } else if ( !contained && !pRemove ) {
                        entries.add( entry );
                    }
                }
                lang.setSettingEntries( pEntriesType, entries );
            }
        }
        
        try {
            CoreModel.getDefault().setProjectDescription( pProject, prjDesc );
        } catch ( CoreException e ) {
            PluginLogger.error( "Error setting a path entry", e );
        }
    }
    
    public static void addLibs( IProject pProject, String[] pLibNames ) {
        changeLibs( pProject, pLibNames, false );
    }
    
    public static void removeLibs( IProject pProject, String[] pLibNames ) {
        changeLibs( pProject, pLibNames, true );
    }
    
    private static void changeLibs( IProject pProject, String[] pLibNames, boolean pRemove ) {
        IManagedBuildInfo infos = ManagedBuildManager.getBuildInfo( pProject );
        IConfiguration[] configs = infos.getManagedProject().getConfigurations();
        
        for (IConfiguration config : configs ) {
            ITool tool = config.calculateTargetTool();
            IOption[] options = tool.getOptions();
            for (IOption option : options) {
                try {
                    if ( option.getValueType() == IOption.LIBRARIES ) {
                        // Append the libraries if not already set
                        String[] libs = option.getLibraries();
                        ArrayList<String> newLibs = new ArrayList<String>( Arrays.asList( libs ) );
                        for ( String lib : pLibNames ) {
                            boolean contained = newLibs.contains( lib );
                            if ( !contained && !pRemove ) {
                                newLibs.add( lib );
                            } else if ( contained && pRemove ) {
                                newLibs.remove( lib );
                            }
                        }
                        ManagedBuildManager.setOption(config, tool, option, newLibs.toArray( new String[ newLibs.size() ] ) );
                    }
                } catch ( Exception e ) {
                    PluginLogger.error( "Error setting linker libraries option", e );
                }
            }
        }
        ManagedBuildManager.saveBuildInfo( pProject, false );
    }
}