/*
 * Copyright (c) 2011 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the GNU Lesser General Public License, Version 2.1. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.gnu.org/licenses/lgpl-2.1.txt. The Original Code is Time Series 
 * Forecasting.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the GNU Lesser Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 */

/*
 *    WekaForecasterTest.java
 *    Copyright (C) 2011 Pentaho Corporation
 */

package weka.classifiers.timeseries;

import weka.core.Instances;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.core.TSLagMaker;

import java.io.*;
import java.util.List;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs a regression test on the output of the WekaForecaster for
 * a sample time series data set
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 */
public class WekaForecasterTest extends TestCase {
  
  public WekaForecasterTest(String name) {
    super(name);
  }

  private String predsToString(List<List<NumericPrediction>> preds, int steps) {
    StringBuffer b = new StringBuffer();

    for (int i = 0; i < steps; i++) {
      List<NumericPrediction> predsForTargetsAtStep = 
        preds.get(i);
      
      for (int j = 0; j < predsForTargetsAtStep.size(); j++) {
        NumericPrediction p = predsForTargetsAtStep.get(j);
        double[][] limits = p.predictionIntervals();
        b.append(p.predicted() + " ");
        if (limits != null && limits.length > 0) {
          b.append(limits[0][0] + " " + limits[0][1] + " ");
        }
      }
      b.append("\n");
    }

    return b.toString();
  }

  public Instances getData(String name) {
    Instances data = null;
    try {
      data =
        new Instances(new BufferedReader(new InputStreamReader(
          ClassLoader.getSystemResourceAsStream("weka/classifiers/timeseries/data/" + name))));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return data;
  }

  public void testRegressionForecastTwoTargetsConfidenceIntervals() throws Exception {

    boolean success = false;
    Instances wine = getData("wine_date.arff");
    weka.test.Regression reg = new weka.test.Regression(this.getClass());

    WekaForecaster forecaster = new WekaForecaster();
    TSLagMaker lagMaker = forecaster.getTSLagMaker();

    try {
      forecaster.setFieldsToForecast("Fortified,Dry-white");
      forecaster.setCalculateConfIntervalsForForecasts(12);
      lagMaker.setTimeStampField("Date");
      lagMaker.setMinLag(1);
      lagMaker.setMaxLag(12);
      lagMaker.setAddMonthOfYear(true);
      lagMaker.setAddQuarterOfYear(true);
      forecaster.buildForecaster(wine, System.out);
      forecaster.primeForecaster(wine);

      int numStepsToForecast = 12;
      List<List<NumericPrediction>> forecast = 
        forecaster.forecast(numStepsToForecast, System.out);
    
      String forecastString = predsToString(forecast, numStepsToForecast);
      success = true;
      reg.println(forecastString);
    } catch (Exception ex) {
      ex.printStackTrace();
      String msg = ex.getMessage().toLowerCase();
      if (msg.indexOf("not in classpath") > -1) {
        return;
      }
    }
    
    if (!success) {
      fail("Problem during regression testing: no successful predictions generated");
    }

    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating.");
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } catch (IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }

  public static Test suite() {
    return new TestSuite(weka.classifiers.timeseries.WekaForecasterTest.class);
  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }
}
