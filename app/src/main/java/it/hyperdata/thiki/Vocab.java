package it.hyperdata.thiki;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class Vocab {
	
	private static Model model = ModelFactory.createDefaultModel();
	
	public static final String ns ="http://purl.org/stuff/wiki#";
	
	public static final Resource Page = model.createResource(ns+"Page");
	
	public static final Property content = model.createProperty(ns+"content");
	
}
