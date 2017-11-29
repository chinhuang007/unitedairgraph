package com.ibm.janusgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.shaded.minlog.Log;
import org.fusesource.hawtjni.runtime.T32;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.JanusGraphTransaction;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphManagement;

import groovy.lang.GroovyClassLoader;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.T;

public class UnitedAirlinesFactory {

	public static final int BATCH_NUM = 1000;
	static final SimpleDateFormat DATE_PARSER_1 = new SimpleDateFormat("MM/dd/yy");
	static final SimpleDateFormat DATE_PARSER_0 = new SimpleDateFormat("dd/MM/yyyy");
	static final SimpleDateFormat TIME_PARSER_0 = new SimpleDateFormat("HHmm");

	/*
	 * private String[] readHeaderFromFile(String fileName){
	 *
	 * }
	 */

	private static long countLines(String filePath) throws Exception {
		LineNumberReader lnr = new LineNumberReader(new FileReader(new File(filePath)));
		try {
			lnr.skip(Long.MAX_VALUE);
			return lnr.getLineNumber() + 1;
		} finally {
			lnr.close();
		}
	}

	private static Vertex getOrCreateStation(String id, String countryCode, JanusGraphTransaction tx) throws Exception {
        Vertex stationVertex = null;
		Iterator<Vertex> iter = tx.traversal().V().has("StationCode", id);

		if (iter.hasNext()) {
			stationVertex = iter.next();
		} else {
			stationVertex = tx.addVertex(T.label, "station", "StationCode", id, "CountryCode", countryCode, "MCTdd", 60, "MCTdi", 60, "MCTid", 120, "MCTii", 120);
		}
        return stationVertex;
	}

	private static Date convertDate(final String inputDate) throws ParseException {
		// Detect the date format and convert it
		if (inputDate.matches("[0-9]{1,2}/[0-9]{1,2}/[0-9]{2}")) {
			// Use mm/dd/yy format
			return DATE_PARSER_1.parse(inputDate);
		} else {
			// Default use dd/mm/yyyy
			return DATE_PARSER_0.parse(inputDate);
		}
	}
	
	private static Date convertTime(final String inputTime) throws ParseException {
		Calendar t=Calendar.getInstance();
		t.clear();
		
		switch (inputTime.length()) {
			case 1:
				t.setTime(TIME_PARSER_0.parse("000"+inputTime));
				break;
			case 2:
				t.setTime(TIME_PARSER_0.parse("00"+inputTime));
				break;
			case 3:
				t.setTime(TIME_PARSER_0.parse("0"+inputTime));
				break;
			default:
				t.setTime(TIME_PARSER_0.parse(inputTime));
		}
		
		return t.getTime();
	}

	private static String getDate(final String inputDate) throws ParseException {
		String[] strLine = inputDate.split(" ");
		return strLine[0];
	}
	
	private static void loadSchema(String propFileName, String schemaFile, String groovyFile) throws Exception {

		JanusGraph g = JanusGraphFactory.open(propFileName);
				
		loadSchema(g, schemaFile, groovyFile);
		
		g.close();
    }
	
	private static void loadSchema(JanusGraph g, String schemaFile, String groovyFile) throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class groovyclass = gcl.parseClass(new File (groovyFile));
		
		Constructor constructor = groovyclass.getConstructor(new Class[]{JanusGraph.class});
		Object scriptInstance = constructor.newInstance(g);
		
		Method m = scriptInstance.getClass().getMethod("readFile", String.class);
		m.invoke(scriptInstance, schemaFile);
		
	}
	
	private static boolean qualifyToFly(int targetBit, int bitMask) throws Exception {
		return (targetBit & bitMask)!=0;
	}

	private static void loadData(String propFileName, String dataFile) throws Exception {

		long startTime = System.nanoTime();
		
		JanusGraph g = JanusGraphFactory.open(propFileName);
		 
		JanusGraphTransaction tx = g.newTransaction();
		  
		try {
            long linesCount = countLines(dataFile);
            long currentLine = 0;
            int oldProgress = -1;
            
			BufferedReader br = Files.newBufferedReader(Paths.get(dataFile), StandardCharsets.UTF_8);
			
			for (String line = null; (line = br.readLine()) != null;) {
				String[] strLine = line.split(",");

				if (strLine[0].equals("ID"))
					continue;

				String ID = strLine[0];
				String FlightIdentifier = strLine[1];
				String AirlineCode = strLine[2];
				String FlightNumber = strLine[3];
				String LegSequenceNumber = strLine[4];
				String AircraftSTD = strLine[8];
				String UtcOrLocalTimeVariationDeparture = strLine[9];
				String AircraftSTA = strLine[13];
				String UtcOrLocalTimeVariationArrival = strLine[14];
				String AircraftType = strLine[16];
				String LegEffectiveDate = strLine[29];
				String LegDiscontinueDate = strLine[30];
				String FlightDistance = strLine[33];
				String FlightDuration = strLine[34];
				String LegBitMask = strLine[36];
				
				String departAirport = strLine[6];
				String arrivalAirport = strLine[11];
				String departCountry = strLine[31];
				String arrivalCountry = strLine[32];
				
				
                int progress = (int) ((float) currentLine / (float) linesCount * 100.0);
                if (progress % 3 == 0 && oldProgress != progress) {
                    Log.info("Edge insert: " + progress + "% ");
                    oldProgress = progress;
                }
                currentLine++;
                
				// Create/load airport code Vertexes
				Vertex departVertex = getOrCreateStation(departAirport, departCountry, tx);
				Vertex arrivalVertex = getOrCreateStation(arrivalAirport, arrivalCountry, tx);
				
				Date startDate=convertDate(getDate(LegEffectiveDate));
				Date endDate=convertDate(getDate(LegDiscontinueDate));
				Date departTime=convertTime(AircraftSTD);
				Date arrivalTime=convertTime(AircraftSTA);
				
				Boolean flySun = qualifyToFly(1, Integer.parseInt(LegBitMask));
				Boolean flyMon = qualifyToFly(2, Integer.parseInt(LegBitMask));
				Boolean flyTue = qualifyToFly(4, Integer.parseInt(LegBitMask));
				Boolean flyWed = qualifyToFly(8, Integer.parseInt(LegBitMask));
				Boolean flyThu = qualifyToFly(16, Integer.parseInt(LegBitMask));
				Boolean flyFri = qualifyToFly(32, Integer.parseInt(LegBitMask));
				Boolean flySat = qualifyToFly(64, Integer.parseInt(LegBitMask));

				
				departVertex.addEdge("routes", arrivalVertex, 
						"ID", ID,
						"FlightIdentifier", FlightIdentifier, 
						"FlightNumber", Integer.parseInt(FlightNumber),
						"AircraftSTDString",AircraftSTD,
						"AircraftSTD",departTime,
						"AircraftSTA",arrivalTime,
						"FlightDistance",FlightDistance,
						"FlightDuration",FlightDuration,
						"LegEffectiveDateString", LegEffectiveDate,
						"LegEffectiveDate", startDate,
						"LegDiscountinueDate", endDate,
						"FlyMon", flyMon,
						"FlyTue", flyTue,
						"FlyWed", flyWed,
						"FlyThu", flyThu,
						"FlyFri", flyFri,
						"FlySat", flySat,
						"FlySun", flySun,
						"FromAirport", departAirport,
						"ToAirport", arrivalAirport,
						"LegBitMask", LegBitMask
						);
				
				if (currentLine % BATCH_NUM == 0) {
					try {
						Log.info(": Commiting current transaction... at " + currentLine);
						tx.commit();
						tx.close();
						Log.info(": Commit transaction done.");
					} catch (Exception e) {
						// ignore
						Log.error(": Commit failed");
					} finally {
						if (tx.isOpen()) {
							Log.error("failed and rollback");
							tx.rollback();
							tx.close();
						}
						tx = g.newTransaction();
					}
				}
			}

		} catch (Exception e) {
			Log.error(ExceptionUtils.getFullStackTrace(e));
		} finally {
			tx.commit();
			tx.close();			
		}

		long totalTime = (System.nanoTime() - startTime) / 1000000000;
		Log.info("All done! It took " + totalTime + " seconds!");

    	g.close();
	}

	public static void main(String args[]) throws Exception {
		if (null == args || args.length < 3) {
			System.err.println("Usage: UnitedAirlinesFactory <janusgraph-config-file> <schema-file> <data-file> {groovy-file}");
			System.exit(1);
		}

		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.INFO);
        
		String groovyFile = "src/main/groovy/JanusgraphGSONSchema.groovy";
		if (args.length > 3)
			groovyFile = args[3];
		
		loadSchema(args[0], args[1], groovyFile);
		loadData(args[0], args[2]);

		System.exit(0);
	}

}
