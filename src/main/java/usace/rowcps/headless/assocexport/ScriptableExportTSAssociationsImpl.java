/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.assocexport;

import hec.data.location.LocationTemplate;
import hec.db.DbConnectionException;
import hec.db.DbIoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import usace.rowcps.data.association.IAssociationProvider;
import usace.rowcps.data.association.ITimeSeriesAssociation;
import usace.rowcps.data.outputformatter.CSVOutputFormatter;
import usace.rowcps.data.outputformatter.OutputFormatter;
import usace.rowcps.data.project.AtProjectDescriptor;
import usace.rowcps.data.project.IProject;
import usace.rowcps.data.project.IProjectCatalog;
import usace.rowcps.headless.interfaces.ScriptableCalc;
import usace.rowcps.regi.model.AtProjectManager;
import usace.rowcps.regi.model.CacheUsage;
import usace.rowcps.regi.model.ManagerId;
import usace.rowcps.regi.model.RegiDomain;

/**
 *
 * @author stephen
 */
public class ScriptableExportTSAssociationsImpl implements ScriptableExportAssociations, ScriptableCalc
{
	private final ManagerId _managerId;
	private final RegiDomain _regiDomain;
	private static final Logger LOGGER = Logger.getLogger(ScriptableExportTSAssociationsImpl.class.getSimpleName());
	private static final String GLOBAL = "?GLOBAL?";
	
	public ScriptableExportTSAssociationsImpl(RegiDomain regiDomain, ManagerId managerId)
	{
		_managerId = managerId;
		_regiDomain = regiDomain;
	}

	private List<IProject> getAllProjects()
	{
		try
		{
			AtProjectManager manager = _regiDomain.getAtProjectManager(_managerId);
			IProjectCatalog projectCatalog = manager.getProjectCatalog(CacheUsage.NORMAL);
			List<AtProjectDescriptor> projectDescriptorList = projectCatalog.getProjectDescriptorList();

			Set<LocationTemplate> templatesForDescriptors = projectDescriptorList.stream()
					.map(projDesc -> projDesc.getProjectLocationRef())
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			NavigableMap<LocationTemplate, IProject> iProjects = manager.getIProjects(templatesForDescriptors, CacheUsage.NORMAL);

			return new ArrayList<>(iProjects.values());
		}
		catch(DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "Error Retrieving Associations!", ex);
			return Collections.emptyList();
		}
	}

	@Override
	public void exportAllTSAssociations(String fileLoc, String lineDelimiter, String valueDelimiter)
	{
		OutputFormatter formatter = new CSVOutputFormatter(lineDelimiter, valueDelimiter);
		List<IProject> allProjects = getAllProjects();
		for(IProject project : allProjects)
		{
			configureFormatter(formatter, project.getProjectId());
			IAssociationProvider<ITimeSeriesAssociation> timeSeriesAssociationProvider = project.getTimeSeriesAssociationProvider();
			if(timeSeriesAssociationProvider != null)
			{
				timeSeriesAssociationProvider.serializeToFormat(formatter);
			}
		}
		formatter.write(fileLoc);
	}

	private IProject getProject(String projName)
	{
		try
		{
			AtProjectManager manager = _regiDomain.getAtProjectManager(_managerId);
			IProjectCatalog projectCatalog = manager.getProjectCatalog(CacheUsage.NORMAL);
			List<AtProjectDescriptor> projectDescriptorList = projectCatalog.getProjectDescriptorList();

			Set<LocationTemplate> templatesForDescriptors = projectDescriptorList.stream()
					.map(projDesc -> projDesc.getProjectLocationRef())
					.filter(Objects::nonNull)
					.filter(projDesc -> projDesc.getLocationId().equals(projName))
					.collect(Collectors.toSet());

			NavigableMap<LocationTemplate, IProject> iProjects = manager.getIProjects(templatesForDescriptors, CacheUsage.NORMAL);

			return iProjects.values().stream()
					.filter(project -> project.getProjectId().equals(projName))
					.findFirst()
					.orElse(null);
		}
		catch(DbConnectionException | DbIoException ex)
		{
			LOGGER.log(Level.SEVERE, "Error Retrieving Associations!", ex);
			return null;
		}
	}

	@Override
	public void exportTSAssociations(String projectId, String fileLoc, String lineDelimiter, String valueDelimiter)
	{
		OutputFormatter formatter = new CSVOutputFormatter(lineDelimiter, valueDelimiter);
		IProject project = getProject(projectId);
		if(project == null)
		{
			LOGGER.log(Level.SEVERE, "Error: Project with name {0} unable to be retrieved.", project);
		}
		else
		{
			IAssociationProvider<ITimeSeriesAssociation> timeSeriesAssociationProvider = project.getTimeSeriesAssociationProvider();
			if(timeSeriesAssociationProvider != null)
			{
				timeSeriesAssociationProvider.serializeToFormat(formatter);
			}
			else
			{
				LOGGER.log(Level.SEVERE, "Error: Project with name {0} has no TS associations provider.", project);
			}
			formatter.write(fileLoc);
		}
	}
	
	private void configureFormatter(OutputFormatter formatter, String projectId)
	{
		formatter.addReplacementString(GLOBAL, projectId);
	}
}
