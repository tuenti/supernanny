package com.tuenti.supernanny.repo.artifacts;


public enum ReqType {
	LT("<"), LE("<="), EQ("="), GE(">="), GT(">"), SW("=~");
	
	private String rep;

	ReqType(String rep) {
		this.rep = rep;
	}
	
	public static ReqType fromString(String r) {
		for (ReqType reqType : values()) {
			if (reqType.toString().equals(r)){
				return reqType;
			}
		}
		return null;
	}

	public static ReqType fromStringStart(String r) {
		// start with the 2 char strings first to match completely
		ReqType[] all = {SW, LE, GE, LT, EQ, GT};
		for (ReqType reqType : all) {
			if (r.startsWith(reqType.toString())){
				return reqType;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return rep;
	}
}
