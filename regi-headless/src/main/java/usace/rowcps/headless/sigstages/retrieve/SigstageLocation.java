/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.retrieve;

import usace.rowcps.headless.sigstages.retrieve.xmlmodel.Site;

/**
 *
 * @author stephen
 */
public class SigstageLocation
{
    private String _originalLocation;
    private String _nwsLocation;
    private Site _site;
    
    public SigstageLocation()
    {
        super();
        _site = null;
        _originalLocation = "";
        _nwsLocation = "";
    }
    
    public SigstageLocation(String orig, String nws)
    {
        this();
        _originalLocation = orig;
        _nwsLocation = nws;
    }
    
    public String getOriginal()
    {
        return _originalLocation;
    }
    
    public String getNWS()
    {
        return _nwsLocation;
    }
    
    public void setOriginal(String original)
    {
        _originalLocation = original;
    }
    
    public void setNWS(String nws)
    {
        _nwsLocation = nws;
    }
    
    public Site getSite()
    {
        return _site;
    }
    
    public void setSite(Site site)
    {
        _site = site;
    }
    
    @Override
    public String toString()
    {
        return getOriginal() + ";" + getNWS() + " " + getSite().getName();
    }
}
