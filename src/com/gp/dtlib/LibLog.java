package com.gp.dtlib;

public class LibLog {
	
	private static DTLogPrinter dtLogPrinter;
	
	public static void initialize(DTLogPrinter dtLogPrinter){
		LibLog.dtLogPrinter = dtLogPrinter;
	}
	
	public static void d(String tag, String message){
		if(dtLogPrinter != null){
			dtLogPrinter.d(tag, message);
		}
	}
	
	public interface DTLogPrinter{
		void d(String tag, String message);
	}

}
