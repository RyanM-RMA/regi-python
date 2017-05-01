package usace.rowcps.headless.calculator.status;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import usace.rowcps.decisionsupport.ui.graphics.releases.ReleasesGraphicDetailArea;

/**
 *
 * @author ryan
 */
public class HeadlessReleasesGraphicDetailArea extends ReleasesGraphicDetailArea {

	public HeadlessReleasesGraphicDetailArea() {
		super();
		_repaintTimer.stop();
		_repaintTimer.removeActionListener(this);
	}

    @Override
    public Component getComponent()
    {
        return null;
    }
	
	@Override
	public void draw(Graphics g, Component parentComponent) {
		System.out.println("headless");
		super.draw(g, parentComponent);
        _detailComponent.setBounds(getScrollArea());
        _detailComponent.print(g);
	}

    @Override
    public Rectangle getScrollArea()
    {
        return _scrollPane.getBounds();
    }

}
