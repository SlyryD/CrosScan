package edu.dcc.db;

public class CrosswordInvalidFormatException extends Exception {

	private static final long serialVersionUID = -5415032786641425594L;

	private final String mData;

	public CrosswordInvalidFormatException(String data) {
		super("Invalid format of crossword.");
		mData = data;
	}

	public String getData() {
		return mData;
	}

}
