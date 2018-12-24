package com.amazon.customskill;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;

import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ResourceUtils;

public class FactProvider {
	private String connectionString;
	private static FactProvider instance; 
	
	private FactProvider(String connectionString) throws ClassNotFoundException {
		Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
		this.connectionString = connectionString;
		
	}
	
	public ArrayList<String> getFact(String key) {
		ArrayList<String> facts = new ArrayList<String>();
		try {
			Connection con = DriverManager.getConnection(this.connectionString, "", "");
			PreparedStatement s = con.prepareStatement("SELECT e.Fact from EssensFakten e"
					+ " left join GerichtMapper m on m.Kueche = e.Key"
					+ " WHERE (e.Key= ? or m.Gericht = ?) ");
			s.setString(1, key);
			s.setString(2, key);
			ResultSet rs = s.executeQuery();
			while((rs!=null) && (rs.next())) {
				facts.add(rs.getString("Fact"));
			}
			s.close();
			con.close();
		} catch(SQLException e) {
			e.printStackTrace();
		}
		return facts;
	}
	
	public static FactProvider getFactProvider() {
		if(instance == null) {
			try {
				String path = ResourceUtils.getFile("classpath:EssensBesessen.accdb").getPath();
				System.out.println("PATH to database: " + path);
				instance = new FactProvider("jdbc:ucanaccess://" + path + ";memory=false");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return instance;
	}
}
