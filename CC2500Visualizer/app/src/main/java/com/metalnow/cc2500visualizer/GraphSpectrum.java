package com.metalnow.cc2500visualizer;

import java.util.ArrayList;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.XYSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.model.CategorySeries;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint.Align;

public class GraphSpectrum {
	
    private GraphicalView view;
  
	//private XYSeries dataset = new XYSeries("2.4GHz Spectrum");

    XYSeries datasetCurrent = new XYSeries("Current");
    XYSeries datasetMaximum = new XYSeries("Maximum");
    XYSeries datasetAverage = new XYSeries("Average");
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	
//	private XYSeriesRenderer renderer = new XYSeriesRenderer(); // This will be used to customize line 1
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph  
  
	int maxresults[];

	public String getName() {return "Signal Across Spectrum";}
	public String getDesc() {return "The average temperature in 4 Greek islands (line chart)";}
	
    public GraphSpectrum ()
    {
        // Add single dataset to multiple dataset
        //mDataset.addSeries(dataset);
        mDataset.addSeries(datasetMaximum);
        mDataset.addSeries(datasetAverage);
        mDataset.addSeries(datasetCurrent);

        int[] colors = new int[] { Color.CYAN, Color.GRAY, Color.BLUE };
        //PointStyle[] styles = new PointStyle[] { PointStyle.POINT, PointStyle.POINT, PointStyle.POINT, PointStyle.POINT };

        int length = colors.length;
        for (int i = 0; i < length; i++)
        {
            /*
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors[i]);
            r.setPointStyle(styles[i]);
            r.setFillPoints(true);
            mRenderer.addSeriesRenderer(r);
            */
            SimpleSeriesRenderer r = new SimpleSeriesRenderer();
            r.setColor(colors[i]);
            mRenderer.addSeriesRenderer(r);

        }

        mRenderer.setAxisTitleTextSize(16);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(15);


        mRenderer.setXLabels(12);
        mRenderer.setYLabels(10);
        mRenderer.setShowGrid(true);
        mRenderer.setXLabelsAlign(Align.RIGHT);
        mRenderer.setYLabelsAlign(Align.RIGHT);
        mRenderer.setPanLimits(new double[] { 2.4, 2.50368, 0, 100 });
        mRenderer.setZoomLimits(new double[] { 2.4, 2.50368, 0, 100 });

        mRenderer.setChartTitle("2.4GHz Spectrum");
        mRenderer.setXTitle("Frequency");
        mRenderer.setYTitle("Signal Strength (dBm)");
        mRenderer.setXAxisMin(2.400);
        mRenderer.setXAxisMax(2.50368);
        mRenderer.setYAxisMin(0);
        mRenderer.setYAxisMax(100);
        mRenderer.setAxesColor(Color.LTGRAY);
        mRenderer.setLabelsColor(Color.LTGRAY);

        // Enable Zoom
        mRenderer.setZoomButtonsVisible(true);

        // Add single renderer to multiple renderer
        //    mRenderer.addSeriesRenderer(renderer);
	}

	public GraphicalView getView(Context context) 
	{
		view =  ChartFactory.getBarChartView(context, mDataset, mRenderer, Type.STACKED);
		return view;
	}

    public void cleanData()
    {
        datasetAverage.clear();
        datasetCurrent.clear();
        datasetMaximum.clear();
    }

    public void addValues( double freq, double current, double maximum, double average )
    {
        datasetCurrent.add(freq, current);
        datasetMaximum.add(freq, maximum);
        datasetAverage.add(freq, average);
    }

	public void updateCurrentValue(double freq, double db)
	{
        try {
            datasetCurrent.remove(datasetCurrent.getIndexForKey(freq));
        } catch (Exception e) {
        }

        datasetCurrent.add(freq, db);
	}

    public void updateMaximumValue(double freq, double db)
    {
        try {
            datasetMaximum.remove(datasetMaximum.getIndexForKey(freq));
        } catch (Exception e) {
        }

        datasetMaximum.add(freq, db);
    }

    public void updateAverageValue(double freq, double db)
    {
        try {
            datasetAverage.remove(datasetAverage.getIndexForKey(freq));
        } catch (Exception e) {
        }

        datasetAverage.add(freq, db);
    }

}
