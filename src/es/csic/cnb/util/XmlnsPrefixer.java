package es.csic.cnb.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

public class XmlnsPrefixer {
	protected Transformer xformer;
	
	public XmlnsPrefixer()
		throws TransformerConfigurationException
	{
		// Create transformer factory
		TransformerFactory factory = TransformerFactory.newInstance();

		// Use the factory to create a template containing the xsl file
		Templates template = factory.newTemplates(new StreamSource(
			this.getClass().getResourceAsStream("/resources/xmlns-prefixer.xsl")
		));
		
		// Use the template to create a transformer
		xformer = template.newTransformer();
	}
	
	public void doPrefix(File inFile, File outFile)
		throws FileNotFoundException, TransformerException
	{
		// Prepare the input and output files
		Source source = new StreamSource(inFile);
		Result result = new StreamResult(outFile);
		
		// Resetting before using
		xformer.reset();
		// Apply the xsl file to the source file and write the result to the output file
		xformer.transform(source, result);
	}
}
	
