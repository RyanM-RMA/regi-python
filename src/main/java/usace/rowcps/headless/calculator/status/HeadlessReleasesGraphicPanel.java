package usace.rowcps.headless.calculator.status;

import java.awt.Dimension;
import java.awt.FlowLayout;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicPanel;

/**
 *
 * @author ryan
 */
public class HeadlessReleasesGraphicPanel extends ReleasesGraphicPanel {

	public HeadlessReleasesGraphicPanel() {
		super();
        setPreferredSize(new Dimension(400, 300));
        setSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setLayout(new FlowLayout());
	}
	
	
	
	@Override
	public void fireDataUpdateRequest() {
		super.fireDataUpdateRequest();
	}
	
}
