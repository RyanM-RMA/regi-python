/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.calculator.status;

import java.util.ArrayList;
import java.util.List;
import rma.services.annotations.ServiceProvider;
import usace.rowcps.decisionsupport.ui.graphics.DrawableArea;
import usace.rowcps.decisionsupport.ui.graphics.DrawableAreaFactory;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicHeader;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicStatus;

/**
 *
 * @author Ryan A. Miles (ryanm@rmanet.com)
 */
@ServiceProvider(service = DrawableAreaFactory.class)
public class HeadlessReleasesDrawableAreaFactory implements DrawableAreaFactory
{

    @Override
    public Class getGraphicClass()
    {
        return HeadlessReleasesGraphicPanel.class;
    }

    @Override
    public List<DrawableArea> getDrawableAreas()
    {
        List<DrawableArea> areas = new ArrayList<>();
        
        areas.add(new HeadlessReleasesGraphicDetailArea());
        areas.add(new ReleasesGraphicHeader());
        areas.add(new ReleasesGraphicStatus());
        
        return areas;
    }
    
}
