package androidx.room

import androidx.sqlite.db.SupportSQLiteProgram
import androidx.sqlite.db.SupportSQLiteQuery

abstract class RoomSQLiteQuery : SupportSQLiteQuery, SupportSQLiteProgram
