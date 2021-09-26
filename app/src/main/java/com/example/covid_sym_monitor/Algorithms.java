package com.example.covid_sym_monitor;

// This class contains the algorithms that are used for computing the heart rate and respiratory rate

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Algorithms
{
    // This functions initialize the Moving average class and returns the final averages data
    public List<Double> calc_mov_avg(int period, List<Double> data)
    {
        SimpleMovingAverage sma = new SimpleMovingAverage(period);
        List<Double> avg_data = sma.getMA(data);

        return avg_data;

    }

    // This function counts the normal zero crossing in the given data
    public int count_zero_crossings(List<Double> points)
    {
        List<Double> extremes = new ArrayList<Double>();
        double previous = points.get(0);
        double previousSlope = 0;
        double p;
        int peak_count = 0;
        for (int i = 1; i < points.size(); i++) {
            p = points.get(i);
            double slope = p - previous;
            if (slope * previousSlope < 0) {
                extremes.add(previous);
                peak_count += 1;
            }

            previousSlope = slope;
            previous = p;

        }

        System.out.println(peak_count);


        return peak_count;
    }


    // This function counts the number of zero crossings having a width greater than the threshold calculated
    public int count_zero_crossings_thres(List<Double> points)
    {
        List<Double> extremes = new ArrayList<Double>();
        double previous = points.get(0);
        double previousSlope = 0;
        double p;
        int peak_count = 0;
        for (int i = 1; i < points.size(); i++) {
            p = points.get(i);
            double slope = p - previous;
            if (slope * previousSlope < 0) {
                extremes.add(previous);
                peak_count += 1;
            }

            previousSlope = slope;
            previous = p;

        }

        System.out.println(peak_count);
        List<Double> widths = new ArrayList<Double>();
        for (int i=1; i < extremes.size(); i++ )
        {

            widths.add(Math.abs(extremes.get(i) - extremes.get(i-1)));
        }

        double sum_width = 0.0;
        for (int i=0; i<widths.size(); i++)
        {
            sum_width += widths.get(i);
        }

        double avg_width = sum_width/widths.size();
        System.out.println("Avg width: " + avg_width);

        int new_peaks = 0;
        for (int i=1; i < extremes.size(); i++ )
        {
            if ( Math.abs(extremes.get(i) - extremes.get(i-1)) >= avg_width  )
            {
                new_peaks += 1;
            }

        }

        System.out.println("New Peak values: " + new_peaks);


        return new_peaks;
    }

}

// This class calculates the Simple Moving Average of the given data
class SimpleMovingAverage {
    Queue<Double> window = new LinkedList<Double>();
    private final int period;
    private double sum;

    public List<Double> getMA(List<Double> data){
        List<Double> ma_data = new ArrayList<Double>(data.size());
        for (double x : data) {
            newNum(x);
            ma_data.add(getAvg());
        }
        return ma_data;
    }

    public SimpleMovingAverage(int period) {
        assert period > 0 : "Period must be a positive integer!";
        this.period = period;
    }

    public void newNum(double num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public double getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return sum / window.size();
    }

}
