package org.processmining.plugins.joosbuijs.processtrees.geneticminer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class ExperimentLogAnalyser {

	private final static String SEP = ";";
	private final static DecimalFormat df = new DecimalFormat("#.###");

	public static void main(String[] args) throws Exception {
		String location = "C:/Users/jbuijs/Desktop/Temp/geneticmining";
		//		String location = "D:/Data/Process Mining/Genetic Experiments/Experiments_20111215";
		//		String location = "D:/Data/Process Mining/Genetic Experiments/Experiment 03";

		File logFile = new File(location + "/generationDetails.csv");
		logFile.createNewFile();
		Writer writer = new FileWriter(logFile);

				processLogScaling(location);
//		processLogGenerationDetails(location, writer);

		writer.close();
	}

	/**
	 * Goes through all the generation0 files to get the scaling factors for the
	 * quality dimensions
	 * 
	 * @param location
	 * @param logWriter
	 * @throws IOException
	 */
	protected static void processLogScaling(String location) throws IOException {
		DescriptiveStatistics statsF = new DescriptiveStatistics();
		DescriptiveStatistics statsP = new DescriptiveStatistics();
		DescriptiveStatistics statsG = new DescriptiveStatistics();
		DescriptiveStatistics statsS = new DescriptiveStatistics();

		File dir = new File(location);
		//for (File base : dir.listFiles()) {
		for (int e = 0; e < 324; e++) {
			File base = new File(dir.getAbsolutePath() + "/experiment" + e);
			if (!base.isDirectory()) {
				//continue;
			}

			//81-161 = a12

			//Open file gen0
			File f = new File(base.getAbsolutePath() + "/generation0.log");

			BufferedReader in = new BufferedReader(new FileReader(f));
			String details = "";
			String strLine;

			int l = 0;

			//Read File Line By Line
			while ((strLine = in.readLine()) != null) {
				l++;
				// find best fitting candidate
				int i = strLine.indexOf("[");
				int j = strLine.indexOf("]");
				if (i < 0 || j < 0) {
					// file error;
					continue;
				}
				details = strLine.substring(i + 2, j - 1);

				//Now extract the 4 Q dimension details
				//f:9.083 p:0.609 s:5.000 g:0.000
				int space1 = details.indexOf(' ');
				int space2 = details.indexOf(' ', space1 + 1);
				int space3 = details.indexOf(' ', space2 + 1);

				String fString = details.substring(2, space1);
				statsF.addValue(Double.parseDouble(fString));
				String pString = details.substring(space1 + 3, space2);
				statsP.addValue(Double.parseDouble(pString));
				String sString = details.substring(space2 + 3, space3);
				statsS.addValue(Double.parseDouble(sString));
				String gString = details.substring(space3 + 3);
				statsG.addValue(Double.parseDouble(gString));
			}
			//			int e = Integer.parseInt(base.getName().substring("experiment".length()));
			//			logWriter.append(e + SEP + (g + 1) + SEP + fitness + SEP + details + SEP + "\"" + tree + "\"\n");

			/*-* /
			if (l < 200) {
				System.out.println("<200 for " + f.getAbsolutePath());
			}/**/
		} //for e

		//Output stats per Q
		System.out.println("F: " + statsF.getMean() + "( N " + statsF.getN() + ", max " + statsF.getMax() + ", min "
				+ statsF.getMin() + ", sDev" + statsF.getStandardDeviation() + ")");
		System.out.println("P: " + statsP.getMean() + "( N " + statsP.getN() + ", max " + statsP.getMax() + ", min "
				+ statsP.getMin() + ", sDev" + statsP.getStandardDeviation() + ")");
		System.out.println("S: " + statsS.getMean() + "( N " + statsS.getN() + ", max " + statsS.getMax() + ", min "
				+ statsS.getMin() + ", sDev" + statsS.getStandardDeviation() + ")");
		System.out.println("G: " + statsG.getMean() + "( N " + statsG.getN() + ", max " + statsG.getMax() + ", min "
				+ statsG.getMin() + ", sDev" + statsG.getStandardDeviation() + ")");

		//And scaling factors
		double fScaling = statsF.getMean() / statsF.getMean();
		double pScaling = statsP.getMean() / statsF.getMean();
		double sScaling = statsS.getMean() / statsF.getMean();
		double gScaling = statsG.getMean() / statsF.getMean();
		DecimalFormat df = new DecimalFormat("#.###");
		System.out.println("F:P:S:G " + df.format(fScaling) + ":" + df.format(pScaling) + ":" + df.format(sScaling)
				+ ":" + df.format(gScaling));
	}

	/**
	 * Goes through all the generation files to get details on fitness(es), tree
	 * length etc.
	 * 
	 * @param location
	 * @param logWriter
	 * @throws IOException
	 */
	protected static void processLogGenerationDetails(String location, final Writer logWriter) throws IOException {
		logWriter.append("Experiment" + SEP + "Generation" + SEP + "avg overall Fitness" + SEP + "best overall Fitness"
				+ SEP + "avg Fitness" + SEP + "best Fitness" + SEP + "avg Precision" + SEP + "best Precision" + SEP
				+ "avg Simplicity" + SEP + "best Simplicity" + SEP + "avg Generalisation" + SEP + "best Generalisation"
				+ SEP + "avg nodes\n");

		File dir = new File(location);
		for (int e = 0; e < 241; e++) {
			//For each experiment
			File base = new File(dir.getAbsolutePath() + "/experiment" + e);
			if (!base.isDirectory()) {
				continue;
			}

			for (int g = 0; g < 100; g++) {
				DescriptiveStatistics statsOverallF = new DescriptiveStatistics();
				DescriptiveStatistics statsF = new DescriptiveStatistics();
				DescriptiveStatistics statsP = new DescriptiveStatistics();
				DescriptiveStatistics statsS = new DescriptiveStatistics();
				DescriptiveStatistics statsG = new DescriptiveStatistics();

				DescriptiveStatistics statsTreeNodes = new DescriptiveStatistics();

				//Open file generation..log
				File f = new File(base.getAbsolutePath() + "/generation" + g + ".log");

				if (!f.exists()) {
					continue;
				}

				BufferedReader in = new BufferedReader(new FileReader(f));
				String strLine;

				int l = 0;

				//Read File Line By Line
				while ((strLine = in.readLine()) != null) {
					l++;
					int i = strLine.indexOf("[");
					int j = strLine.indexOf("]");
					if (i < 0 || j < 0) {
						// file error;
						continue;
					}
					double fitness = Double.parseDouble(strLine.substring(2, i - 2));
					statsOverallF.addValue(fitness);

					String details = strLine.substring(i + 2, j - 1);
					String tree = strLine.substring(j + 2);

					//Now extract the 4 Q dimension details
					//f:9.083 p:0.609 s:5.000 g:0.000
					int space1 = details.indexOf(' ');
					int space2 = details.indexOf(' ', space1 + 1);
					int space3 = details.indexOf(' ', space2 + 1);

					String fString = details.substring(2, space1);
					statsF.addValue(Double.parseDouble(fString));
					String pString = details.substring(space1 + 3, space2);
					statsP.addValue(Double.parseDouble(pString));
					String sString = details.substring(space2 + 3, space3);
					statsS.addValue(Double.parseDouble(sString));
					String gString = details.substring(space3 + 3);
					statsG.addValue(Double.parseDouble(gString));

					String[] nodess = tree.trim().split(" ");
					int nodes = 0;
					for (String node : nodess) {
						if (!(node.equals(",") || node.equals(")") || node.equals("LEAF:"))) {
							nodes++;
						}
					}
					statsTreeNodes.addValue(nodes);

				}//while lines in file

				//For each experiment/generation combi write a line in the file
				logWriter.append(e + SEP + g + SEP + df.format(statsOverallF.getMean()) + SEP
						+ df.format(statsOverallF.getMin()) + SEP + df.format(statsF.getMean()) + SEP
						+ df.format(statsF.getMin()) + SEP + df.format(statsP.getMean()) + SEP
						+ df.format(statsP.getMin()) + SEP + df.format(statsS.getMean()) + SEP
						+ df.format(statsS.getMin()) + SEP + df.format(statsG.getMean()) + SEP
						+ df.format(statsG.getMin()) + SEP + df.format(statsTreeNodes.getMean()) + "\n");
			}//for g

		}//for e
	}
}
