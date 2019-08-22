/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package usace.rowcps.headless.calculator.status;

import com.rma.ui.pinnable.IPinnableComponentContainer;
import com.rma.ui.pinnable.PinnableComponentManager;
import hec.data.location.LocationGroup;
import hec.data.location.LocationTemplate;
import java.util.List;
import usace.rowcps.basinpie.ui.annotations.BasinPieAnnotationLayer;
import usace.rowcps.data.maptemplate.annotations.AnnotationData;
import usace.rowcps.data.maptemplate.annotations.BarChartAnnotationPayload;
import usace.rowcps.data.maptemplate.annotations.PieChartAnnotationPayload;
import usace.rowcps.decisionsupport.ui.annotations.chartpanel.BarChartAnnotationPanel;
import usace.rowcps.regi.interfaces.model.ManagerIdProvider;

/**
 *
 * @author josh
 */
public class HeadlessBasinPieAnnotationLayer extends BasinPieAnnotationLayer
{

    private final List<LocationTemplate> _activeLocations;
    
    public HeadlessBasinPieAnnotationLayer(ManagerIdProvider managerIdProvider,
                                           LocationGroup locationGroup, List<LocationTemplate> activeLocations)
    {
        super(managerIdProvider, locationGroup);
        _activeLocations = activeLocations;
    }

    @Override
    protected List<LocationTemplate> getActiveLocations()
    {
        return _activeLocations;
    }
    
    @Override
    public void addAnnotation(AnnotationData annotationData)
    {
        IPinnableComponentContainer iPinnableComponentContainer = createAnnotation(annotationData);
        if (iPinnableComponentContainer != null)
        {
            PinnableComponentManager pcm = PinnableComponentManager.getManager(this);
            pcm.addPinnableComponent(iPinnableComponentContainer);
            
            applyAnnotationData(iPinnableComponentContainer, annotationData, true);
            annotationData.addPropertyChangeListener(this);            
        }                
    }
	
	public void updateBarChartGraphics()
	{
		getContainers().keySet().stream()
				.filter(BarChartAnnotationPanel.class::isInstance)
				.map(BarChartAnnotationPanel.class::cast)
				.forEach(BarChartAnnotationPanel::updateBarChart);
	}
    
    @Override
    protected void updateAnnotationActiveProjects(AnnotationData data)
    {
        if (data.getPayload() instanceof PieChartAnnotationPayload)
        {
            PieChartAnnotationPayload payload = (PieChartAnnotationPayload) data.getPayload();
            payload.setActiveLocations(_activeLocations);
        }
        else if (data.getPayload() instanceof BarChartAnnotationPayload)
        {
            BarChartAnnotationPayload payload = (BarChartAnnotationPayload) data.getPayload();
            payload.setActiveLocations(_activeLocations);
        }
    }
}
