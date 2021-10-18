package androidx.room.paging

import androidx.paging.PagingSource

abstract class LimitOffsetPagingSource<Value: Any> : PagingSource<Int, Value>()
