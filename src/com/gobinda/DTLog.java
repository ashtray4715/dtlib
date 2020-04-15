package com.gobinda;

import com.gobinda.DTManager.DTManagerLog;

class DTLog {

	private static DTManagerLog dtManagerLog;

	static void setLogHandler(DTManagerLog dtManagerLog) {
		DTLog.dtManagerLog = dtManagerLog;
	}

	static void d(String debugTag, String message) {
		if (dtManagerLog != null) {
			dtManagerLog.d("[" + debugTag + "] " + message);
		}
	}

}
