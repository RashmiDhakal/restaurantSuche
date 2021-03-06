package com.amazon.customskill;

import java.util.ArrayList;

public class Restaurant {

	String name;
	String address;
	String phone;
	double distance;
	double rating;
	ArrayList<String> alias;
	ArrayList<String> title;
	boolean isClosed;
	
	public Restaurant(String name, String address, String phone, double distance, double rating, ArrayList<String> alias, ArrayList<String> title, boolean isClosed) {
		this.name = name;
		this.address = address;
		this.phone = phone;
		this.distance = distance;
		this.rating = rating;
		this.alias = alias;
		this.title = title;
		this.isClosed = isClosed;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getAddress() {
		return this.address;
	}
	
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	public String getPhone() {
		return this.phone;
	}
	
	public void setDistance(double distance) {
		this.distance = distance;
	}
	
	public double getDistance() {
		return this.distance;
	}
	
	public void setRating(double rating) {
		this.rating = rating;
	}
	
	public double getRating() {
		return this.rating;
	}
	
	public void setAlias(ArrayList<String> alias) {
		this.alias = alias;
	}
	
	public ArrayList<String> getAlias() {
		return this.alias;
	}
	
	public void setTitle(ArrayList<String> title) {
		this.title = title;
	}
	
	public ArrayList<String> getTitle() {
		return this.title;
	}
	
	public void setIsClosed(boolean isClosed) {
		this.isClosed = isClosed;
	}
	
	public boolean getIsClosed() {
		return this.isClosed;
	}
}
