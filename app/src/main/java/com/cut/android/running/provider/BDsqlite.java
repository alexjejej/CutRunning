package com.cut.android.running.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class BDsqlite extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "pasosDatabase";
    public static final String TABLE_NAME = "data";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_FIRSTNAME = "nombre";
    public static final String COLUMN_LASTNAME = "apellido";
    public static final String COLUMN_PASOS_HOY = "pasoshoy";
    public static final String COLUMN_PASOS_TOTALES = "pasostotales";
    public static final String COLUMN_DISTANCIA = "distancia";

    public static final String COLUMN_DISTANCIA_HOY = "distanciahoy";
    public static final String COLUMN_CODE = "codigo";
    public static final String COLUMN_PESO = "peso";
    public static final String COLUMN_EDAD = "edad";
    public static final String COLUMN_ESTATURA = "estatura";
    public static final String COLUMN_DISTANCEPERSTEP = "distanciaporpaso";
    public static final String COLUMN_CENTRO_UNIVERSITARIO = "centro_universitario";
    public static final String COLUMN_SPECIALITYID = "carrera";
    public static final String COLUMN_UPDATE_DATE = "date";



    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_EMAIL + " TEXT PRIMARY KEY, " +
                    COLUMN_CODE + " INTEGER, " +
                    COLUMN_FIRSTNAME + " TEXT, " +
                    COLUMN_LASTNAME + " TEXT, " +
                    COLUMN_EDAD + " INTEGER, " +
                    COLUMN_DISTANCIA + " FLOAT, " +
                    COLUMN_DISTANCIA_HOY + " FLOAT, " +
                    COLUMN_ESTATURA + " REAL, " +
                    COLUMN_DISTANCEPERSTEP + " REAL, " +
                    COLUMN_PESO + " REAL, " +
                    COLUMN_PASOS_HOY + " INTEGER, " +
                    COLUMN_PASOS_TOTALES + " INTEGER, " +
                    COLUMN_CENTRO_UNIVERSITARIO + " TEXT, " +
                    COLUMN_SPECIALITYID + " TEXT, " +
                    COLUMN_UPDATE_DATE + " TEXT" + ")";




    public BDsqlite(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void upsertData(String email, int pasoshoy, int pasostotales, float distancia, float distanciahoy) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASOS_HOY, pasoshoy);
        values.put(COLUMN_PASOS_TOTALES, pasostotales);
        values.put(COLUMN_DISTANCIA, distancia);
        values.put(COLUMN_DISTANCIA_HOY, distanciahoy);

        int rowsAffected = db.update(TABLE_NAME, values, COLUMN_EMAIL + " = ?", new String[]{email});
        if (rowsAffected == 0) {
            db.insert(TABLE_NAME, null, values);
        }
        db.close();
    }

    public void newData(String email, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Verificar si ya existe un registro con el mismo email
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});
        if(cursor.moveToFirst()) {
            // Si existe, actualizar el registro
            db.update(TABLE_NAME, values, COLUMN_EMAIL + " = ?", new String[]{email});
        } else {
            // Si no existe, insertar un nuevo registro
            db.insert(TABLE_NAME, null, values);
        }
        cursor.close();
        db.close();
    }


    public Cursor getData(String columnName, String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + columnName + " FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});
    }


    public static String getColumnPasosHoy() {
        return COLUMN_PASOS_HOY;
    }

    public static String getColumnPasosTotales() {
        return COLUMN_PASOS_TOTALES;
    }

    public static String getColumnDistancia() {
        return COLUMN_DISTANCIA;
    }
    //Método Auxiliar para Datos de Tipo int
    public int getIntData(String email, String columnName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        int data = 0; // Valor predeterminado

        try {
            cursor = db.rawQuery("SELECT " + columnName + " FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex != -1) {
                    data = cursor.getInt(columnIndex);
                }
            }
        } catch (Exception e) {
            // Manejo de errores
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return data;
    }

    // Método Auxiliar para Datos de Tipo String
    public String getStringData(String email, String columnName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String data = null; // Valor predeterminado

        try {
            cursor = db.rawQuery("SELECT " + columnName + " FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex != -1) {
                    data = cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            // Manejo de errores
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return data;
    }

    // Método para insertar o actualizar
    public void insertOrUpdate(String email, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Filtrar y añadir valores significativos
        ContentValues filteredValues = new ContentValues();
        for (String key : values.keySet()) {
            Object value = values.get(key);
            if (value != null) {
                filteredValues.put(key, value.toString());
            }
        }

        // Solo proceder si hay valores significativos
        if (filteredValues.size() > 0) {
            int rowsAffected = db.update(TABLE_NAME, filteredValues, COLUMN_EMAIL + " = ?", new String[]{email});
            if (rowsAffected == 0) {
                db.insert(TABLE_NAME, null, filteredValues);
            }
        }

        db.close();
    }

    // Método Auxiliar para Datos de Tipo Float
    public float getFloatData(String email, String columnName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        float data = 0.0f; // Valor predeterminado

        try {
            cursor = db.rawQuery("SELECT " + columnName + " FROM " + TABLE_NAME + " WHERE " + COLUMN_EMAIL + " = ?", new String[]{email});
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(columnName);
                if (columnIndex != -1) {
                    data = cursor.getFloat(columnIndex);
                }
            }
        } catch (Exception e) {
            // Manejo de errores
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return data;
    }





}


