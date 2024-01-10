/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package usace.rowcps.headless.assocexport;

/**
 *
 * @author stephen
 */
public interface ScriptableExportAssociations
{
	/**
	 * Export all Time Series Associations in the database
	 *
	 * @param fileLoc The file to write the CSV to
	 * @param lineDelimiter the delimiter to use between lines (\n recommended)
	 * @param valueDelimiter The delimiter to use between values (\t
	 * recommended)
	 */
	public void exportAllTSAssociations(String fileLoc, String lineDelimiter, String valueDelimiter);

	/**
	 * Export the Time Series Associations for a given project
	 *
	 * @param project The project to export for
	 * @param fileLoc The file to write the CSV to
	 * @param lineDelimiter the delimiter to use between lines (\n recommended)
	 * @param valueDelimiter The delimiter to use between values (\t
	 * recommended)
	 */
	public void exportTSAssociations(String project, String fileLoc, String lineDelimiter, String valueDelimiter);
}
