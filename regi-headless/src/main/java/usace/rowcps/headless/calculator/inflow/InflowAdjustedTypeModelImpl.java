package usace.rowcps.headless.calculator.inflow;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import usace.rowcps.computation.inflow.InflowAdjustedType;
import usace.rowcps.computation.inflow.InflowAdjustedTypeModel;

/**
 *
 * @author ryan
 */
public class InflowAdjustedTypeModelImpl implements InflowAdjustedTypeModel
{
	 private final Map<Date, InflowAdjustedType> adjustedInflowsStatus = new HashMap<>();

	@Override
	public InflowAdjustedType getAdjustedInflowStatus(Date date)
	{
		return adjustedInflowsStatus.get(date);
	}

	@Override
	public void setAdjustedInflowStatus(Date date, InflowAdjustedType newStatus)
	{
		adjustedInflowsStatus.put(date, newStatus);
	}

	@Override
	public void setAdjustedInflowStatus(Collection<Date> dates, InflowAdjustedType newStatus)
	{
		if(dates != null && !dates.isEmpty()){
			for (Date date : dates) {
				setAdjustedInflowStatus(date, newStatus);
			}
		}
	}

}
