package edu.dcc.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import edu.dcc.game.CrosswordGame;

/**
 * This class helps open, create, and upgrade the database file.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	public static final int DATABASE_VERSION = 8;

	DatabaseHelper(Context context) {
		super(context, CrosswordDatabase.DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ " (" + CrosswordColumns._ID + " INTEGER PRIMARY KEY,"
				+ CrosswordColumns.FOLDER_ID + " INTEGER,"
				+ CrosswordColumns.CREATED + " INTEGER,"
				+ CrosswordColumns.STATE + " INTEGER," + CrosswordColumns.TIME
				+ " INTEGER," + CrosswordColumns.LAST_PLAYED + " INTEGER,"
				+ CrosswordColumns.DATA + " Text," + CrosswordColumns.TITLE
				+ " Text);");

		db.execSQL("CREATE TABLE " + CrosswordDatabase.FOLDER_TABLE_NAME + " ("
				+ FolderColumns._ID + " INTEGER PRIMARY KEY,"
				+ CrosswordColumns.CREATED + " INTEGER," + FolderColumns.NAME
				+ " TEXT" + ");");

		insertFolder(db, 1, "crosswords");
		insertCrossword(db, 1, 1, "My Puzzle 1", "1A1B1C1D001010101010001010"
				+ "10101010001010101010001010" + "10101010001010101010001010"
				+ "10101010100000101010101010" + "00000010101010001010101000"
				+ "10101010101010100010101010" + "10101000101010101000101010"
				+ "10101010001010101010001010" + "10101010100010101010100010"
				+ "00101010101000101010101010" + "00000010101010001010101000"
				+ "00101010101010100000101010" + "10101010001010101010001010");

		createIndexes(db);
	}

	private void insertFolder(SQLiteDatabase db, long folderID,
			String folderName) {
		long now = System.currentTimeMillis();
		db.execSQL("INSERT INTO " + CrosswordDatabase.FOLDER_TABLE_NAME
				+ " VALUES (" + folderID + ", " + now + ", '" + folderName
				+ "');");
	}

	// TODO: crosswordName is not used
	private void insertCrossword(SQLiteDatabase db, long folderID,
			long crosswordID, String crosswordName, String data) {
		String sql = "INSERT INTO " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ " VALUES (" + crosswordID + ", " + folderID + ", 0, "
				+ CrosswordGame.GAME_STATE_NOT_STARTED + ", 0, null, '" + data
				+ "', '" + crosswordName + "');";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("DatabaseHelper", "Upgrading database from version " + oldVersion
				+ " to " + newVersion + ".");

		createIndexes(db);
	}

	private void createIndexes(SQLiteDatabase db) {
		db.execSQL("create index " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ "_idx1 on " + CrosswordDatabase.CROSSWORD_TABLE_NAME + " ("
				+ CrosswordColumns.FOLDER_ID + ");");
	}
}
