package com.split_keyboard;

class Gaussian {

	final double PI = Math.acos(-1);
	double mean, std;
	double c0, c1;
	
	public Gaussian(double mean, double std) {
		this.mean = mean;
		this.std = std;
		c0 = 1.0 / Math.sqrt(2 * PI) / std;
		c1 = -1.0 / 2 / std / std;
	}
	
	public double probability(double x) {
		return c0 * Math.exp((x - mean) * (x - mean) * c1);
	}
}


public class BivariateGaussian {
	
	Gaussian gx, gy;
	
	public BivariateGaussian(double x_mean, double x_std, double y_mean, double y_std) {
		gx = new Gaussian(x_mean, x_std);
		gy = new Gaussian(y_mean, y_std);
	}
	
	public double probability(double x, double y) {
		return gx.probability(x) * gy.probability(y);
	}
}