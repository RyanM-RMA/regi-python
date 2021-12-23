/*
 * Copyright (c) 2021
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package usace.rowcps.headless.calculator.flowgroup;

import java.io.IOException;
import java.io.Reader;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class Html2Text extends HTMLEditorKit.ParserCallback
{

	private final StringBuilder _sb = new StringBuilder();
	private int _tabDepth = 0;
	
	public void parse(Reader in) throws IOException
	{
		_sb.setLength(0);
		ParserDelegator delegator = new ParserDelegator();
		delegator.parse(in, this, true);
	}

	@Override
	public void handleText(char[] data, int pos)
	{
		for (int i = 0; i < _tabDepth; i++)
		{
			_sb.append("\t");
		}
		_sb.append(data);
	}

	@Override
	public void handleEndTag(HTML.Tag t, int pos)
	{
		if (t == HTML.Tag.BLOCKQUOTE)
		{
			_tabDepth--;
		}
	}

	@Override
	public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos)
	{
		if (t == HTML.Tag.BLOCKQUOTE)
		{
			_tabDepth++;
		}
		
		if (t.breaksFlow())
		{
			_sb.append(System.lineSeparator());
		}
	}
	
	public String getText()
	{
		return _sb.toString();
	}
}
