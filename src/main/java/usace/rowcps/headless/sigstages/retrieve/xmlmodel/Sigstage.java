/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.sigstages.retrieve.xmlmodel;

/**
 *
 * @author stephen
 */
public interface Sigstage
{
    public float getValue();
    public String getUnits();
 
    public Type getType();
    
    public enum Type
    {
        ACTION ("Action"),
        BANKFULL ("Bankfull"),
        FLOOD ("Flood"),
        LOW ("Low"),
        MAJOR ("Major"),
        MODERATE ("Moderate"),
        RECORD ("Record");
        
        private String _typeName;
        
        private Type(String s)
        {
            _typeName = s;
        }
        
        @Override
        public String toString()
        {
            return _typeName;
        }
        
        public String generateID(String location, String parameter, String parameterType, String duration, String specifiedLevel)
        {
            StringBuilder build = new StringBuilder();
            if(specifiedLevel == null || specifiedLevel.equals(""))
            {
                specifiedLevel = _typeName;
            }
            build.append(location);
            build.append(".");
            build.append(parameter);
            build.append(".");
            build.append(parameterType);
            build.append(".");
            build.append(duration);
            build.append(".");
            build.append(specifiedLevel);
            return build.toString();
        }
    }
}
