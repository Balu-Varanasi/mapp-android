package nl.appcetera.mapp;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Polygon data management
 * @author Mathijs
 *
 */
public class PolygonData extends SQLiteOpenHelper
{
	private static final String DATABASE_NAME = "mapp.db";
	private static final int DATABASE_VERSION = 4;
	
	public static final String POLYGON_TABLE_NAME 	= "polygondata";
	public static final String POLYGON_ID 			= BaseColumns._ID;
	public static final String POLYGON_COLOR 		= "color";
	public static final String POLYGON_LAST_EDITED	= "last_edited";
	public static final String POLYGON_CLOSED		= "is_closed";
	public static final String POLYGON_GROUP		= "groupid";
	
	public static final String POLYGON_POINTS_TABLE_NAME 	= "polygon_points";
	public static final String POLYGON_POINTS_ID			= "polygon_id";
	public static final String POLYGON_POINTS_X 			= "coord_x";
	public static final String POLYGON_POINTS_Y 			= "coord_y";
	public static final String POLYGON_POINTS_ORDERING		= "ordering";
	
	public static final String GROUPS_TABLE_NAME	= "groups";
	public static final String GROUPS_ID			= BaseColumns._ID;
	public static final String GROUPS_NAME			= "group_name";
	
	
	public PolygonData(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		onUpgrade(getWritableDatabase(), 1, 2); // Handig tijdens debuggen
	}

	/**
	 * Maakt de tabellen als ze nog niet bestaan
	 */
	@Override
	public void onCreate(SQLiteDatabase db)
	{
		String sql3 =
		    "CREATE TABLE " + GROUPS_TABLE_NAME + " ("
		      + GROUPS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		      + GROUPS_NAME + " TEXT"
		      + ");";
		db.execSQL(sql3);
		db.execSQL("INSERT INTO " + GROUPS_TABLE_NAME + " (" + GROUPS_NAME + ") VALUES ('Default')");
		
		String sql =
		    "CREATE TABLE " + POLYGON_TABLE_NAME + " ("
		      + POLYGON_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		      + POLYGON_COLOR + " INTEGER NOT NULL, "
		      + POLYGON_LAST_EDITED + " INTEGER, "
		      + POLYGON_CLOSED + " INTEGER, "
		      + POLYGON_GROUP + " INTEGER NOT NULL, "
		      + "FOREIGN KEY(" + POLYGON_GROUP + ") REFERENCES " + GROUPS_TABLE_NAME 
		      + "(" + GROUPS_ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
		      + ");";
		 
		db.execSQL(sql);
		  
		String sql2 =
			    "CREATE TABLE " + POLYGON_POINTS_TABLE_NAME + " ("
			      + POLYGON_POINTS_ID + " INTEGER, "
			      + POLYGON_POINTS_X + " INTEGER, "
			      + POLYGON_POINTS_Y + " INTEGER, "
			      + POLYGON_POINTS_ORDERING + " INTEGER, "
			      + "FOREIGN KEY(" + POLYGON_POINTS_ID + ") REFERENCES " + POLYGON_TABLE_NAME 
			      + "(" + POLYGON_ID + ") ON UPDATE CASCADE ON DELETE CASCADE"
			      + ");";
			 
		db.execSQL(sql2);
	}

	/**
	 * Upgrade de database als er een nieuwer versienummer is
	 * Momenteel door de oude te verwijderen en hem opnieuw aan te maken,
	 * misschien een handigere manier? TODO
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS " + POLYGON_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + POLYGON_POINTS_TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + GROUPS_TABLE_NAME);
		onCreate(db);
	}
	
	/**
	 * Maakt een nieuwe polygoon aan in de database en geeft het id terug
	 * @param color de kleur van de polygoon als int (voor Java.Color)
	 * @return het id dat de polygoon gekregen heeft
	 */
	public int addPolygon(int color, boolean isClosed, int group)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_COLOR, color);
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		values.put(POLYGON_CLOSED, isClosed == true ? 1 : 0);
		values.put(POLYGON_GROUP, group);
		return (int) db.insertOrThrow(POLYGON_TABLE_NAME, null, values);
	}
	
	/**
	 * Bewerkt de polygoon met het gegeven id
	 * @param polygonid het id van de polygoon
	 * @param color de kleur van de polygoon, als integer
	 * @param isClosed of de polygoon gesloten is of niet
	 */
	public void editPolygon(int polygonid, int color, boolean isClosed)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_COLOR, color);
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		values.put(POLYGON_CLOSED, isClosed == true ? 1 : 0);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Geeft alle polygonen terug, gesorteerd op bewerkdatum
	 * @return een cursor met alle polygonen, gesorteerd op bewerkdatum
	 */
	public Cursor getAllPolygons(Activity activity, int group)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_TABLE_NAME, new String[]{POLYGON_ID, POLYGON_COLOR, POLYGON_CLOSED}, 
				POLYGON_GROUP + "=" + group, null, null, null, POLYGON_LAST_EDITED);
		activity.startManagingCursor(c);
		return c;
	}
	
	/**
	 * Verwijder de polygoon met het gegeven id
	 * @param polygonid het id van de te verwijderen polygoon
	 */
	public void removePolygon(int polygonid)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_TABLE_NAME, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Voegt een punt toe aan een polygoon op gegeven index
	 * @param polygonid id van de polygoon waar het punt aan toegevoegd wordt
	 * @param x positie van het punt (latitude)
	 * @param y positie van het punt (longtitude)
	 * @param ordering index van het punt, waarbij 0 het startpunt is
	 */
	public void addPolygonPoint(int polygonid, long x, long y, int ordering)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_POINTS_ID, polygonid);
		values.put(POLYGON_POINTS_X, x);
		values.put(POLYGON_POINTS_Y, y);
		values.put(POLYGON_POINTS_ORDERING, ordering);
		db.insertOrThrow(POLYGON_POINTS_TABLE_NAME, null, values);
		
		// Laatst-bewerkt datum bijwerken
		values = new ContentValues();
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Wijzig de coördinaten van een punt
	 * @param polygonid het id van de polygoon waar het te wijzigen punt bij hoort
	 * @param x nieuwe positie van het punt (latitude)
	 * @param y nieuwe positie van het punt (longtitude)
	 * @param ordering (nieuwe) index van het te wijzigen punt
	 */
	public void editPolygonPoint(int polygonid, long x, long y, int ordering)
	{
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(POLYGON_POINTS_X, x);
		values.put(POLYGON_POINTS_Y, y);
		values.put(POLYGON_POINTS_ORDERING, ordering);
		db.update(POLYGON_POINTS_TABLE_NAME, values, POLYGON_POINTS_ID + " = " 
				+ polygonid	+ " AND " + POLYGON_POINTS_ORDERING + " = " + ordering, null);
		
		// Laatst-bewerkt datum bijwerken
		values = new ContentValues();
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Verwijder een gegeven punt uit een polygoon
	 * @param polygonid het id van de polygoon waar een punt uit moet
	 * @param ordering de index van het te verwijderen punt
	 */
	public void removePolygonPoint(int polygonid, int ordering)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_POINTS_TABLE_NAME, POLYGON_POINTS_ID + " = " 
				+ polygonid	+ " AND " + POLYGON_POINTS_ORDERING + " = " + ordering, null);
		
		// Laatst-bewerkt datum bijwerken
		ContentValues values = new ContentValues();
		values.put(POLYGON_LAST_EDITED, System.currentTimeMillis()/1000);
		db.update(POLYGON_TABLE_NAME, values, POLYGON_ID + "=" + polygonid, null);
	}
	
	/**
	 * Verwijder alle bij een polygoon behorende punten
	 * @param polygonid het id van de polygoon waar alle punten van weg moeten
	 */
	public void removePolygonPoints(int polygonid)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.delete(POLYGON_POINTS_TABLE_NAME, POLYGON_POINTS_ID + " = " 
				+ polygonid, null);
	}

	/**
	 * Geeft alle bij een polygoon behorende punten terug, op juiste wijze gesorteerd
	 * @param polygonid het id van de polygoon waar je de punten bij wilt hebben
	 * @return een cursor met alle polygoonpunten
	 */
	public Cursor getAllPolygonPoints(Activity activity, int polygonid)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.query(POLYGON_POINTS_TABLE_NAME, 
				new String[]{POLYGON_POINTS_X, POLYGON_POINTS_Y, POLYGON_POINTS_ORDERING},
				POLYGON_POINTS_ID + " = " + polygonid, null, null, null, POLYGON_POINTS_ORDERING);
		activity.startManagingCursor(c);
		return c;
	}
	
	/**
	 * Verandert de volgorde van een groepje polygoonpunten, bijvoorbeeld handig
	 * als je tussenin een punt wilt toevoegen of verwijderen
	 * @param polygonid het id van de polygoon waar de punten bij horen
	 * @param index alle punten groter dan of gelijk aan deze index worden verplaatst
	 * @param diff het verschil waarmee je de punten wilt verplaatsen, 1 voor alle indices 1 omhoog, -1 voor alle indices 1 omlaag
	 */
	public void movePolygonPointsIndexes(int polygonid, int index, int diff)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("UPDATE " + POLYGON_POINTS_TABLE_NAME + " SET " + POLYGON_POINTS_ORDERING
				+ "=" + POLYGON_POINTS_ORDERING + "+" + diff + " WHERE "
				+ POLYGON_POINTS_ORDERING + ">=" + index + " AND " + POLYGON_POINTS_ID
				+ "=" + polygonid);
	}
}
