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
	private static int id = 1;

	public DatabaseHelper(Context context) {
		super(context, CrosswordDatabase.DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Puzzle table
		db.execSQL("CREATE TABLE " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ " (" + CrosswordColumns._ID + " INTEGER PRIMARY KEY,"
				+ CrosswordColumns.FOLDER_ID + " INTEGER,"
				+ CrosswordColumns.CREATED + " INTEGER,"
				+ CrosswordColumns.STATE + " INTEGER," + CrosswordColumns.TIME
				+ " INTEGER," + CrosswordColumns.LAST_PLAYED + " INTEGER,"
				+ CrosswordColumns.DATA + " Text," + CrosswordColumns.TITLE
				+ " Text);");

		// Folder table
		db.execSQL("CREATE TABLE " + CrosswordDatabase.FOLDER_TABLE_NAME + " ("
				+ FolderColumns._ID + " INTEGER PRIMARY KEY,"
				+ CrosswordColumns.CREATED + " INTEGER," + FolderColumns.NAME
				+ " TEXT" + ");");

		// Create folder crosswords
		insertFolder(db, 1, "crosswords");

		// Construct example puzzle
		StringBuilder sb = new StringBuilder();
		sb.append("15|");
		sb.append("10|10|10|10|00|00|10|10|10|10|00|10|10|10|10|")
				.append("10|10|10|10|10|00|10|10|10|10|00|10|10|10|10|")
				.append("10|10|10|10|10|00|10|10|10|10|00|10|10|10|10|")
				.append("10|10|10|00|10|10|10|10|00|10|10|10|10|10|10|")
				.append("00|00|00|10|10|10|10|00|10|10|10|10|10|10|00|")
				.append("10|10|10|10|10|10|10|00|10|10|10|00|00|00|00|")
				.append("10|10|10|10|10|00|00|10|10|10|10|10|10|10|10|")
				.append("10|10|10|10|00|10|10|10|10|10|00|10|10|10|10|")
				.append("10|10|10|10|10|10|10|10|00|00|10|10|10|10|10|")
				.append("00|00|00|00|10|10|10|00|10|10|10|10|10|10|10|")
				.append("00|10|10|10|10|10|10|00|10|10|10|10|00|00|00|")
				.append("10|10|10|10|10|10|00|10|10|10|10|00|10|10|10|")
				.append("10|10|10|10|00|10|10|10|10|00|10|10|10|10|10|")
				.append("10|10|10|10|00|10|10|10|10|00|10|10|10|10|10|")
				.append("10|10|10|10|00|10|10|10|10|00|00|10|10|10|10|");
		sb.append("Tiny stream|Building add-on|Sulk|")
				.append("Asimov or Hayes|Popular cookie|Air France destination|")
				.append("Lion, to Tarzan|After-bath wear|-- fixe|")
				.append("Dangerous curve|Sketch|Mustard or mayo|")
				.append("Freighter hazard|Basket willows|")
				.append("Wasted time|Quick lunch|")
				.append("Duelers'' weapons|Secure position|")
				.append("Out on the briny|Bookcase-kit item|On vaction|")
				.append("With ceremony|Kate''s sitcom friend|")
				.append("Princess perturber|Waylays|")
				.append("Blew hard|Celts, to Romans|")
				.append("Fragrant rose|Thin fog|Sugary drink|")
				.append("John, in Siberia|Adams or McClurg|Cove|")
				.append("Goose-down garment|Salt, to a chemist|Kid who rode Diablo|")
				.append("Anagram of \"\"seal\"\"|Risked a ticket|Kind of companion|");
		sb.append("Hilltop|Osiris'' wife|Takes a powder|Frankenstein milieu|")
				.append("Dosed the dog|Hematite yield|Corn belt st.|Is cautious (2 wds.)|")
				.append("Watered silk|Mandate|Urgent appeals|Took a gander|")
				.append("Port opposite Dover|\"\"Percent\"\" ending|Brad -- of \"\"Moneyball\"\"|Faint flicker|")
				.append("\"\"Peter and the Wolf\"\" duck|Fell on --ears| Lhasa --|Lascivious look|")
				.append("Cook bacon|Moon rings|Nocturnal birds|Milk, to Yves|")
				.append("Batik need|Uses Brilliantine|Not nude|Living qtrs.|")
				.append("Acid in vinegar|Off course|Variety of lettuce|Mallet|")
				.append("Amherst sch.|End-of-year temp|Go in headfirst|Nursery rhyme trio|")
				.append("To boot|Retro art style|007''s alma mater|Skip stones|Tip of a pen");
		insertCrossword(db, 1, getNextId(), "My Puzzle 1", sb.toString());

		// Construct example puzzle
		sb = new StringBuilder();
		sb.append("13|");
		sb.append("10|10|10|10|00|10|10|10|10|10|00|10|10|")
				.append("10|10|10|10|00|10|10|10|10|10|00|10|10|")
				.append("10|10|10|10|00|10|10|10|10|10|00|10|10|")
				.append("10|10|10|10|10|00|00|10|10|10|10|10|10|")
				.append("00|00|00|10|10|10|10|00|10|10|10|10|00|")
				.append("10|10|10|10|10|10|10|10|00|10|10|10|10|")
				.append("10|10|10|00|10|10|10|10|10|00|10|10|10|")
				.append("10|10|10|10|00|10|10|10|10|10|00|10|10|")
				.append("10|10|10|10|10|00|10|10|10|10|10|00|10|")
				.append("00|10|10|10|10|10|00|10|10|10|10|10|10|")
				.append("00|00|00|10|10|10|10|00|10|10|10|10|00|")
				.append("00|10|10|10|10|10|10|10|00|00|10|10|10|")
				.append("10|10|10|10|00|10|10|10|10|10|00|10|10|");
		insertCrossword(db, 1, getNextId(), "My Puzzle 2", sb.toString());

		// Create indices
		createIndices(db);

		System.out.println(db.toString());
	}

	public static int getNextId() {
		return id++;
	}

	private void insertFolder(SQLiteDatabase db, long folderID,
			String folderName) {
		long now = System.currentTimeMillis();
		db.execSQL("INSERT INTO " + CrosswordDatabase.FOLDER_TABLE_NAME
				+ " VALUES (" + folderID + ", " + now + ", '" + folderName
				+ "');");
	}

	private void insertCrossword(SQLiteDatabase db, long folderID,
			long crosswordID, String crosswordName, String data) {
		String sql = "INSERT INTO " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ " VALUES (" + crosswordID + ", " + folderID + ", 0, "
				+ CrosswordGame.GAME_STATE_NOT_STARTED + ", 0, 0, '" + data
				+ "', '" + crosswordName + "');";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i("DatabaseHelper", "Upgrading database from version " + oldVersion
				+ " to " + newVersion + ".");

		createIndices(db);
	}

	private void createIndices(SQLiteDatabase db) {
		db.execSQL("CREATE INDEX " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ "_idx1 on " + CrosswordDatabase.CROSSWORD_TABLE_NAME + " ("
				+ CrosswordColumns.FOLDER_ID + ");");
	}
}
