/**
 * 
 */
package org.thoughtcatchers.thiki;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;

/**
 * @author danny
 *
 */
public class RdfRepresentation {
	
	private static final String filename = "meta.ttl";
	
	public static SimpleDateFormat isoDate =
			new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");
	
	private static Model model = ModelFactory.createDefaultModel();
	
	public static final String PAGE_NS ="http://hyperdata.it/pages#";

	protected static final String EXT = ".ttl";

	public static void newPage(String pageName) {
		pageName = pageName.replace(" ", "_");
		Resource page = model.createResource(PAGE_NS+pageName)
		             .addProperty(DC.title, pageName)
		             .addProperty(DC.date, toISODate(new Date()));
		// System.out.println("in newPage, Turtle :");
		model.write(System.out, "Turtle");
		
	}

	public static String toISODate(Date date) {
		return isoDate.format(date);
	}

	public static void saveMeta() {
		File metaFile = new File(Environment.getExternalStorageDirectory() + File.separator + filename);
		Log.d("RdfRepresentation, saving : ", metaFile.getAbsolutePath());
		// File metaFile = new File(Constants.FILES_DIR+"/"+filename);
		try {
			metaFile.createNewFile();
			FileOutputStream fos = new FileOutputStream(metaFile);
			model.write(fos, "Turtle");
			System.out.println("Turtle :");
			model.write(System.out, "Turtle");
		} catch (FileNotFoundException e) {
			Log.e("RdfRepresentation", e.getMessage()+" on "+metaFile.getAbsolutePath());
		} catch (IOException e) {
			Log.e("RdfRepresentation", e.getMessage()+" on "+metaFile.getAbsolutePath());
		}
		Log.d("RdfRepresentation, maybe saved : ", metaFile.getAbsolutePath());
	}
}
