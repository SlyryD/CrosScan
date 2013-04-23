package edu.dcc.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import edu.dcc.crosswordscan.R;
import edu.dcc.game.CrosswordGame;

/**
 * This class helps open, create, and upgrade the database file.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String TAG = "DatabaseHelper";

	public static final int DATABASE_VERSION = 8;

	private Context mContext;

	DatabaseHelper(Context context) {
		super(context, CrosswordDatabase.DATABASE_NAME, null, DATABASE_VERSION);
		this.mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ " (" + CrosswordColumns._ID + " INTEGER PRIMARY KEY,"
				+ CrosswordColumns.FOLDER_ID + " INTEGER,"
				+ CrosswordColumns.CREATED + " INTEGER,"
				+ CrosswordColumns.STATE + " INTEGER," + CrosswordColumns.TIME
				+ " INTEGER," + CrosswordColumns.LAST_PLAYED + " INTEGER,"
				+ CrosswordColumns.DATA + " Text,"
				+ CrosswordColumns.PUZZLE_NOTE + " Text" + ");");

		db.execSQL("CREATE TABLE " + CrosswordDatabase.FOLDER_TABLE_NAME + " ("
				+ FolderColumns._ID + " INTEGER PRIMARY KEY,"
				+ CrosswordColumns.CREATED + " INTEGER," + FolderColumns.NAME
				+ " TEXT" + ");");

		insertFolder(db, 1, "crosswords");
		insertCrossword(
				db,
				1,
				1,
				"Easy1",
				"052006000160900004049803620400000800083201590001000002097305240200009056000100970");
		insertCrossword(
				db,
				1,
				2,
				"Easy2",
				"052400100100002030000813025400007010683000597070500002890365000010700006006004970");
		insertCrossword(
				db,
				1,
				3,
				"Easy3",
				"302000089068052734009000000400007000083201590000500002000000200214780350530000908");
		insertCrossword(
				db,
				1,
				4,
				"Easy4",
				"402000007000080420050302006090030050503060708070010060900406030015070000200000809");
		insertCrossword(
				db,
				1,
				5,
				"Easy5",
				"060091080109680405050040106600000200023904710004000003907020030305079602040150070");
		insertCrossword(
				db,
				1,
				6,
				"Easy6",
				"060090380009080405050300106001008200020904010004200900907006030305070600046050070");
		insertCrossword(
				db,
				1,
				7,
				"Easy7",
				"402000380109607400008300106090030004023964710800010060907006500005809602046000809");
		insertCrossword(
				db,
				1,
				8,
				"Easy8",
				"400091000009007425058340190691000000003964700000000963087026530315800600000150009");
		insertCrossword(
				db,
				1,
				9,
				"Easy9",
				"380001004002600070000487003000040239201000406495060000600854000070006800800700092");
		insertCrossword(
				db,
				1,
				10,
				"Easy10",
				"007520060002009008006407000768005009031000450400300781000804300100200800050013600");
		insertCrossword(
				db,
				1,
				11,
				"Easy11",
				"380000000540009078000407503000145209000908000405362000609804000170200045000000092");
		insertCrossword(
				db,
				1,
				12,
				"Easy12",
				"007001000540609078900487000760100230230000056095002081000854007170206045000700600");
		insertCrossword(
				db,
				1,
				13,
				"Easy13",
				"007021900502009078006407500000140039031908450490062000009804300170200805004710600");
		insertCrossword(
				db,
				1,
				14,
				"Easy14",
				"086500204407008090350009000009080601010000080608090300000200076030800409105004820");
		insertCrossword(
				db,
				1,
				15,
				"Easy15",
				"086507000007360100000000068249003050500000007070100342890000000002056400000904820");
		insertCrossword(
				db,
				1,
				16,
				"Easy16",
				"000007230420368000050029768000080650000602000078090000894230070000856019065900000");
		insertCrossword(
				db,
				1,
				17,
				"Easy17",
				"906000200400368190350400000209080051013040980670090302000001076032856009005000803");
		insertCrossword(
				db,
				1,
				18,
				"Easy18",
				"095002000700804001810076500476000302000000000301000857003290075500307006000400130");
		insertCrossword(
				db,
				1,
				19,
				"Easy19",
				"005002740002850901810000500070501302008723600301609050003000075509017200087400100");
		insertCrossword(
				db,
				1,
				20,
				"Easy20",
				"605102740732004001000000020400501300008020600001609007060000000500300286087405109");
		insertCrossword(
				db,
				1,
				21,
				"Easy21",
				"695102040700800000000970023076000090900020004020000850160098000000007006080405139");
		insertCrossword(
				db,
				1,
				22,
				"Easy22",
				"090002748000004901800906500470500090008000600020009057003208005509300000287400030");
		insertCrossword(
				db,
				1,
				23,
				"Easy23",
				"001009048089070030003106005390000500058602170007000094900708300030040860870300400");
		insertCrossword(
				db,
				1,
				24,
				"Easy24",
				"600039708000004600000100025002017506408000103107850200910008000005900000806320009");
		insertCrossword(
				db,
				1,
				25,
				"Easy25",
				"620500700500270631040100005302000086000090000160000204900008050235041007006005019");
		insertCrossword(
				db,
				1,
				26,
				"Easy26",
				"080130002140902007273080000000070206007203900502040000000060318600308024400021050");
		insertCrossword(
				db,
				1,
				27,
				"Easy27",
				"980100402046950000200684001010009086007000900590800070700465008000098720408001059");
		insertCrossword(
				db,
				1,
				28,
				"Easy28",
				"085100400000950007073684001010070080067203940090040070700465310600098000008001650");
		insertCrossword(
				db,
				1,
				29,
				"Easy29",
				"085100460146000807070004001300009080067000940090800003700400010601000724038001650");
		insertCrossword(
				db,
				1,
				30,
				"Easy30",
				"085130462006000007270680090000009200060213040002800000020065018600000700438021650");

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
				+ "', null);";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "Upgrading database from version " + oldVersion + " to "
				+ newVersion + ".");

		createIndexes(db);
	}

	private void createIndexes(SQLiteDatabase db) {
		db.execSQL("create index " + CrosswordDatabase.CROSSWORD_TABLE_NAME
				+ "_idx1 on " + CrosswordDatabase.CROSSWORD_TABLE_NAME + " ("
				+ CrosswordColumns.FOLDER_ID + ");");
	}
}
