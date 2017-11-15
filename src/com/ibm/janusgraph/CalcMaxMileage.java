package com.ibm.janusgraph;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.tinkerpop.shaded.minlog.Log;

public class CalcMaxMileage {
	private static void load(String inputFile, String outputFile) throws Exception {
		try {
           
			File file = new File(outputFile);
			file.createNewFile();
			
			BufferedReader br = Files.newBufferedReader(Paths.get(inputFile), StandardCharsets.UTF_8);
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFile), StandardCharsets.UTF_8);
			
			for (String line = null; (line = br.readLine()) != null;) {
				String[] strLine = line.split(",");
				
				String origAirport = strLine[0];
				String origCountry = strLine[1];
				String lat1 = strLine[2];
				String lon1 = strLine[3];
				String destAirport = strLine[4];
				String destCountry = strLine[5];
				String lat2 = strLine[6];
				String lon2 = strLine[7];
				long dirMileage = calcDirMileage(Double.parseDouble(lat1), Double.parseDouble(lon1), Double.parseDouble(lat2), Double.parseDouble(lon2));
				long maxMileage = calcMaxMileage(dirMileage, origCountry, destCountry);
				
				String outputDel = ",";
				String outputStr = origAirport + outputDel + destAirport + outputDel + dirMileage + outputDel + maxMileage;
				bw.write(outputStr);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (Exception e) {
			Log.error(ExceptionUtils.getFullStackTrace(e));
		}
	}

	private static long calcDirMileage(double lat1, double lon1, double lat2, double lon2) throws Exception {
        double x = (Math.sin(lat1) * Math.sin(lat2)) + (Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        // calculate distance based on approx. diameter of earth and round up
        double y = (Math.acos(x) * 3959.0) + 0.5;

        // return the mileage
        return (long)y;		
	}

	private static long calcMaxMileage(long directMileage, String origCountry, String destCountry) throws Exception {
		int factor = 2;

        return calcMaxMileage(directMileage, factor, origCountry.equals(destCountry));		
	}

	private static long calcMaxMileage(long directMileage, int factor, boolean isDomestic) throws Exception {
	       // return the mileage
	        return calcMaxMileage(directMileage, factor, (isDomestic?500:1000));		
	}
	
	private static long calcMaxMileage(long directMileage, int factor, int extraMiles) throws Exception {
	       // return the mileage
	        return directMileage * factor + extraMiles;		
	}

	public static void main(String[] args) throws Exception {
		org.apache.log4j.LogManager.getRootLogger().setLevel(Level.INFO);

		load(args[0], args[1]);
		System.exit(0);
	}

}
